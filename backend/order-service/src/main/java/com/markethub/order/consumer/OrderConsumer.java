package com.markethub.order.consumer;

import com.markethub.common.constant.ApiConstants;
import com.markethub.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = ApiConstants.QUEUE_ORDER_CREATE)
    public void handleOrderCreate(Integer orderId) {
        log.info("接收订单创建消息: orderId={}", orderId);
    }

    @RabbitListener(queues = ApiConstants.QUEUE_ORDER_PAY)
    public void handleOrderPay(Integer orderId) {
        log.info("接收订单支付消息: orderId={}", orderId);
    }

    /** 延时消息到期后取消待付款订单并还库存，已付款则忽略。 */
    @RabbitListener(queues = ApiConstants.QUEUE_ORDER_TIMEOUT)
    public void handleOrderTimeout(Integer orderId) {
        log.info("接收订单超时消息: orderId={}", orderId);
        orderService.cancelIfUnpaid(orderId);
    }
}
