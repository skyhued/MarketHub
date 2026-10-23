package com.markethub.order.service;

import com.markethub.common.constant.ApiConstants;
import com.markethub.common.exception.BusinessException;
import com.markethub.common.security.CurrentUser;
import com.markethub.common.util.OrderNoGenerator;
import com.markethub.order.dto.CartItemDTO;
import com.markethub.order.entity.Order;
import com.markethub.order.entity.OrderItem;
import com.markethub.order.feign.CartFeignClient;
import com.markethub.order.feign.ProductInternalFeignClient;
import com.markethub.order.mapper.OrderItemMapper;
import com.markethub.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RabbitTemplate rabbitTemplate;
    private final CartFeignClient cartFeignClient;
    private final ProductInternalFeignClient productInternalFeignClient;
    private final OrderWriter orderWriter;

    public Order findById(Integer id, Integer operatorId) {
        Order order = requireVisible(orderMapper.findById(id), operatorId);
        order.setItems(orderItemMapper.findByOrderId(id));
        return order;
    }

    public Order findByOrderNo(String orderNo, Integer operatorId) {
        Order order = requireVisible(orderMapper.findByOrderNo(orderNo), operatorId);
        order.setItems(orderItemMapper.findByOrderId(order.getId()));
        return order;
    }

    /** 订单含收货人信息，只有买家、卖家本人和管理员能看。 */
    private Order requireVisible(Order order, Integer operatorId) {
        if (order == null) throw new BusinessException(404, "订单不存在");
        boolean party = Objects.equals(order.getUserId(), operatorId) || Objects.equals(order.getSellerId(), operatorId);
        if (!party && !CurrentUser.isAdmin()) {
            throw new BusinessException(403, "无权查看该订单");
        }
        return order;
    }

    public List<Order> findByUserId(Integer userId) {
        List<Order> orders = orderMapper.findByUserId(userId);
        orders.forEach(o -> o.setItems(orderItemMapper.findByOrderId(o.getId())));
        return orders;
    }

    public List<Order> findBySellerId(Integer sellerId) {
        List<Order> orders = orderMapper.findBySellerId(sellerId);
        orders.forEach(o -> o.setItems(orderItemMapper.findByOrderId(o.getId())));
        return orders;
    }

    public List<Order> findAll() {
        List<Order> orders = orderMapper.findAll();
        orders.forEach(o -> o.setItems(orderItemMapper.findByOrderId(o.getId())));
        return orders;
    }

    /** 创建订单：事务外扣库存，再由 OrderWriter 开事务写库并发超时消息，失败补偿库存。 */
    public Order createOrder(Order order, List<OrderItem> items) {
        order.setOrderNo(OrderNoGenerator.generate());
        order.setStatus(ApiConstants.ORDER_PENDING_PAY);
        order.setActualAmount(order.getTotalAmount());

        deductStock(order.getOrderNo(), items);

        try {
            return orderWriter.insertAndPublish(order, items);
        } catch (RuntimeException e) {
            // 订单没落库、或者超时消息没发出去，库存都不能悬着
            compensateStock(order.getOrderNo(), items);
            throw e;
        }
    }

    /** 按件扣库存，中途失败就把已扣的全部还回去。 */
    private void deductStock(String orderNo, List<OrderItem> items) {
        List<OrderItem> deducted = new ArrayList<>();
        for (OrderItem item : items) {
            try {
                productInternalFeignClient.deductStock(item.getProductId(), orderNo, item.getQuantity());
                deducted.add(item);
            } catch (RuntimeException e) {
                log.warn("扣库存失败，回滚已扣部分: orderNo={}, productId={}, error={}",
                        orderNo, item.getProductId(), e.getMessage());
                compensateStock(orderNo, deducted);
                throw e;
            }
        }
    }

    /** 下单失败的补偿：单件归还失败只记日志，不覆盖原始错误。 */
    private void compensateStock(String orderNo, List<OrderItem> items) {
        for (OrderItem item : items) {
            try {
                productInternalFeignClient.restoreStock(item.getProductId(), orderNo, item.getQuantity());
            } catch (RuntimeException e) {
                log.error("归还库存失败，需人工核对: orderNo={}, productId={}, quantity={}, error={}",
                        orderNo, item.getProductId(), item.getQuantity(), e.getMessage());
            }
        }
    }

    /** 取消订单还库存：失败必须抛出让事务回滚，重试由幂等账本兜住。 */
    private void restoreStock(String orderNo, List<OrderItem> items) {
        for (OrderItem item : items) {
            productInternalFeignClient.restoreStock(item.getProductId(), orderNo, item.getQuantity());
        }
    }

    /** 付款时累加销量，失败就让整个付款事务回滚，和扣库存一个待遇；重试由幂等账本兜住。 */
    private void increaseSales(String orderNo, List<OrderItem> items) {
        for (OrderItem item : items) {
            productInternalFeignClient.increaseSales(item.getProductId(), orderNo, item.getQuantity());
        }
    }

    /** 取消已付款订单时扣回销量，失败同样回滚；同样靠幂等账本保证重试不重复施加。 */
    private void decreaseSales(String orderNo, List<OrderItem> items) {
        for (OrderItem item : items) {
            productInternalFeignClient.decreaseSales(item.getProductId(), orderNo, item.getQuantity());
        }
    }

    /** 支付订单：条件更新，只有待付款才改得动。 */
    @Transactional(rollbackFor = Exception.class)
    public void pay(Integer id, Integer operatorId) {
        Order order = loadOwnedOrder(id, operatorId, false, ApiConstants.ORDER_PENDING_PAY, "支付");
        if (orderMapper.markPaidIfPending(id) == 0) {
            throw new BusinessException("当前订单状态不能支付");
        }

        increaseSales(order.getOrderNo(), orderItemMapper.findByOrderId(id));

        rabbitTemplate.convertAndSend(ApiConstants.EXCHANGE_ORDER,
                ApiConstants.ROUTING_ORDER_PAY, order.getId());

        log.info("订单支付成功: orderId={}, orderNo={}", id, order.getOrderNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public void ship(Integer id, Integer operatorId) {
        Order order = loadOwnedOrder(id, operatorId, true, ApiConstants.ORDER_PAID, "发货");
        order.setStatus(ApiConstants.ORDER_SHIPPED);
        order.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Integer id, Integer operatorId) {
        Order order = loadOwnedOrder(id, operatorId, false, ApiConstants.ORDER_SHIPPED, "确认收货");
        order.setStatus(ApiConstants.ORDER_COMPLETED);
        order.setReceiveTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    /** 取消订单：待付款和已付款未发货可取消，已发货不允许。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Integer id, Integer operatorId, String reason) {
        Order order = requireOwnedOrder(id, operatorId, false, "取消");
        if (!isCancelable(order)) {
            throw new BusinessException("当前订单状态不能取消");
        }

        List<OrderItem> items = orderItemMapper.findByOrderId(id);

        if (orderMapper.cancelIfPending(id, reason) == 1) {
            restoreStock(order.getOrderNo(), items);
            log.info("待付款订单已取消: orderId={}", id);
            return;
        }
        if (orderMapper.cancelIfPaid(id, reason) == 1) {
            restoreStock(order.getOrderNo(), items);
            decreaseSales(order.getOrderNo(), items);
            log.info("已付款订单已取消，库存与销量已退回: orderId={}", id);
            return;
        }
        // 检查通过之后状态又被别人改走了（比如刚被支付或已被超时取消）
        throw new BusinessException("当前订单状态不能取消");
    }

    /** 待付款、已付款未发货可取消；已发货、已完成、已取消失败 */
    private boolean isCancelable(Order order) {
        return Objects.equals(order.getStatus(), ApiConstants.ORDER_PENDING_PAY)
                || Objects.equals(order.getStatus(), ApiConstants.ORDER_PAID);
    }

    /** 超时未付款自动取消：仍待付款才取消并还库存，失败重试后进兜底队列。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancelIfUnpaid(Integer orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            log.warn("超时取消忽略：订单不存在 orderId={}", orderId);
            return;
        }
        if (!Objects.equals(order.getStatus(), ApiConstants.ORDER_PENDING_PAY)) {
            log.info("超时取消忽略：订单已不是待付款 orderId={}, status={}", orderId, order.getStatus());
            return;
        }
        if (orderMapper.cancelIfPending(orderId, ApiConstants.CANCEL_REASON_TIMEOUT) == 0) {
            log.info("超时取消忽略：订单状态刚被其他操作改掉 orderId={}", orderId);
            return;
        }
        restoreStock(order.getOrderNo(), orderItemMapper.findByOrderId(orderId));
        log.info("订单超时未支付，已自动取消: orderId={}", orderId);
    }

    /** 评价资格：订单属于当前用户、已完成且包含该商品。 */
    public boolean isReviewable(Integer orderId, Integer userId, Integer productId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            return false;
        }
        if (!Objects.equals(order.getStatus(), ApiConstants.ORDER_COMPLETED)) {
            return false;
        }
        return orderItemMapper.existsProductInOrder(orderId, productId);
    }

    /** 收回商家资格前核对：该卖家名下不能还有未完成订单。 */
    public int countUnfinishedBySeller(Integer sellerId) {
        return orderMapper.countUnfinishedBySeller(sellerId);
    }

    /** 管理员不受归属限制，但仍必须满足状态流转。 */
    private Order loadOwnedOrder(Integer id, Integer operatorId, boolean asSeller, int expectedStatus, String action) {
        Order order = requireOwnedOrder(id, operatorId, asSeller, action);
        if (!Objects.equals(order.getStatus(), expectedStatus)) {
            throw new BusinessException("当前订单状态不能" + action);
        }
        return order;
    }

    /** 只校验存在性和归属，不看状态 */
    private Order requireOwnedOrder(Integer id, Integer operatorId, boolean asSeller, String action) {
        Order order = orderMapper.findById(id);
        if (order == null) throw new BusinessException("订单不存在");
        Integer ownerId = asSeller ? order.getSellerId() : order.getUserId();
        if (!CurrentUser.isAdmin() && !Objects.equals(ownerId, operatorId)) {
            throw new BusinessException(403, "只有" + (asSeller ? "卖家" : "买家") + "本人可以" + action + "该订单");
        }
        return order;
    }

    public Order createOrderFromCart(List<Integer> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            throw new BusinessException("请选择要结算的商品");
        }

        var response = cartFeignClient.getCart();
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BusinessException("获取购物车失败");
        }

        Set<Integer> requestedIds = new LinkedHashSet<>(cartIds);
        List<CartItemDTO> selected = response.getData().stream()
                .filter(item -> requestedIds.contains(item.getId()))
                .toList();
        if (selected.size() != requestedIds.size()) {
            throw new BusinessException("部分购物车商品不存在");
        }

        Integer userId = selected.get(0).getUserId();
        Integer sellerId = selected.get(0).getSellerId();
        boolean mixedSeller = selected.stream().anyMatch(item -> !java.util.Objects.equals(sellerId, item.getSellerId()));
        if (mixedSeller) {
            throw new BusinessException("暂不支持跨商家结算");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (CartItemDTO cart : selected) {
            int quantity = cart.getQuantity() == null ? 1 : cart.getQuantity();
            BigDecimal price = cart.getProductPrice() == null ? BigDecimal.ZERO : cart.getProductPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);

            OrderItem item = new OrderItem();
            item.setProductId(cart.getProductId());
            item.setProductName(cart.getProductName());
            item.setProductImage(cart.getProductImage());
            item.setQuantity(quantity);
            item.setUnitPrice(price);
            item.setTotalPrice(subtotal);
            items.add(item);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setSellerId(sellerId);
        order.setTotalAmount(total);
        order.setFreight(BigDecimal.ZERO);
        Order created = createOrder(order, items);

        selected.forEach(item -> cartFeignClient.removeItem(item.getProductId()));
        return created;
    }
}
