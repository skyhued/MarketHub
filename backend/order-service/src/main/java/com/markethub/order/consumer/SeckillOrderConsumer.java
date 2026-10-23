package com.markethub.order.consumer;

import com.markethub.common.constant.ApiConstants;
import com.markethub.common.dto.SeckillGrabMessage;
import com.markethub.common.dto.SeckillSnapshotDTO;
import com.markethub.common.exception.BusinessException;
import com.markethub.order.entity.Order;
import com.markethub.order.entity.OrderItem;
import com.markethub.order.feign.SeckillInternalFeignClient;
import com.markethub.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 秒杀名额抢到后异步建单，扣的还是 product.stock，与活动名额是两套账。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final SeckillInternalFeignClient seckillInternalFeignClient;
    private final OrderService orderService;

    @RabbitListener(queues = ApiConstants.QUEUE_SECKILL_CREATE)
    public void handleSeckillCreate(SeckillGrabMessage message) {
        Integer activityId = message.getActivityId();
        Integer userId = message.getUserId();

        String existingOrderNo = seckillInternalFeignClient.orderNo(activityId, userId).getData();
        if (existingOrderNo != null) {
            log.info("秒杀订单已存在，跳过重复建单: activityId={}, userId={}, orderNo={}",
                    activityId, userId, existingOrderNo);
            return;
        }

        SeckillSnapshotDTO snapshot = seckillInternalFeignClient.snapshot(activityId).getData();

        OrderItem item = new OrderItem();
        item.setProductId(snapshot.getProductId());
        item.setProductName(snapshot.getProductName());
        item.setProductImage(snapshot.getProductImage());
        item.setQuantity(1);
        item.setUnitPrice(snapshot.getSeckillPrice());
        item.setTotalPrice(snapshot.getSeckillPrice());

        Order order = new Order();
        order.setUserId(userId);
        order.setSellerId(snapshot.getSellerId());
        order.setTotalAmount(snapshot.getSeckillPrice());
        order.setFreight(BigDecimal.ZERO);

        try {
            Order created = orderService.createOrder(order, List.of(item));
            seckillInternalFeignClient.attachOrder(activityId, userId, created.getOrderNo());
            log.info("秒杀订单建单成功: activityId={}, userId={}, orderNo={}",
                    activityId, userId, created.getOrderNo());
        } catch (BusinessException e) {
            log.warn("秒杀订单建单失败，释放名额: activityId={}, userId={}, error={}",
                    activityId, userId, e.getMessage());
            seckillInternalFeignClient.release(activityId, userId);
        }
    }
}
