package com.markethub.order.config;

import com.markethub.common.constant.ApiConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /** 未付款订单多久后自动取消，测试时可以调短（ORDER_PAY_TIMEOUT_MS） */
    @Value("${order.pay-timeout-ms:1800000}")
    private int payTimeoutMs;

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ApiConstants.EXCHANGE_ORDER);
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(ApiConstants.QUEUE_ORDER_CREATE).build();
    }

    @Bean
    public Queue orderPayQueue() {
        return QueueBuilder.durable(ApiConstants.QUEUE_ORDER_PAY).build();
    }

    /** 延时队列故意不绑消费者；改 TTL 前必须删掉旧队列，否则服务起不来。 */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ApiConstants.QUEUE_ORDER_DELAY)
                .ttl(payTimeoutMs)
                .deadLetterExchange(ApiConstants.EXCHANGE_ORDER)
                .deadLetterRoutingKey(ApiConstants.ROUTING_ORDER_TIMEOUT)
                .build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ApiConstants.QUEUE_ORDER_TIMEOUT)
                // 重试耗尽后不丢弃、也不无限重投，转进兜底队列等人工排查
                .deadLetterExchange(ApiConstants.EXCHANGE_ORDER)
                .deadLetterRoutingKey(ApiConstants.ROUTING_ORDER_TIMEOUT_DLQ)
                .build();
    }

    /** 兜底队列：故意不绑消费者，消息停在这里等人工处理 */
    @Bean
    public Queue orderTimeoutDlq() {
        return QueueBuilder.durable(ApiConstants.QUEUE_ORDER_TIMEOUT_DLQ).build();
    }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(orderExchange()).with(ApiConstants.ROUTING_ORDER_CREATE);
    }

    @Bean
    public Binding orderPayBinding() {
        return BindingBuilder.bind(orderPayQueue()).to(orderExchange()).with(ApiConstants.ROUTING_ORDER_PAY);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderExchange()).with(ApiConstants.ROUTING_ORDER_DELAY);
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderExchange()).with(ApiConstants.ROUTING_ORDER_TIMEOUT);
    }

    @Bean
    public Binding orderTimeoutDlqBinding() {
        return BindingBuilder.bind(orderTimeoutDlq()).to(orderExchange()).with(ApiConstants.ROUTING_ORDER_TIMEOUT_DLQ);
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(ApiConstants.EXCHANGE_SECKILL);
    }

    @Bean
    public Queue seckillCreateQueue() {
        return QueueBuilder.durable(ApiConstants.QUEUE_SECKILL_CREATE).build();
    }

    @Bean
    public Binding seckillCreateBinding() {
        return BindingBuilder.bind(seckillCreateQueue()).to(seckillExchange()).with(ApiConstants.ROUTING_SECKILL_CREATE);
    }
}
