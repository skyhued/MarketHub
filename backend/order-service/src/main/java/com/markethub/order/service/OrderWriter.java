package com.markethub.order.service;

import com.markethub.common.constant.ApiConstants;
import com.markethub.order.entity.Order;
import com.markethub.order.entity.OrderItem;
import com.markethub.order.mapper.OrderItemMapper;
import com.markethub.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 订单落库单独成 bean，保证 @Transactional 生效；事务只包写库和发超时消息。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWriter {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(rollbackFor = Exception.class)
    public Order insertAndPublish(Order order, List<OrderItem> items) {
        orderMapper.insert(order);

        items.forEach(item -> item.setOrderId(order.getId()));
        orderItemMapper.batchInsert(items);

        // 延时消息到期不成死信就没人管，所以这两条必须在订单真正写进去之后发
        rabbitTemplate.convertAndSend(ApiConstants.EXCHANGE_ORDER,
                ApiConstants.ROUTING_ORDER_DELAY, order.getId());
        rabbitTemplate.convertAndSend(ApiConstants.EXCHANGE_ORDER,
                ApiConstants.ROUTING_ORDER_CREATE, order.getId());

        log.info("订单落库成功: orderNo={}, orderId={}", order.getOrderNo(), order.getId());
        return order;
    }
}
