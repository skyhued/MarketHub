package com.markethub.common.constant;

public final class ApiConstants {

    private ApiConstants() {}

    public static final String SERVICE_USER = "markethub-user-service";
    public static final String SERVICE_PRODUCT = "markethub-product-service";
    public static final String SERVICE_ORDER = "markethub-order-service";
    public static final String SERVICE_CART = "markethub-cart-service";
    public static final String SERVICE_REVIEW = "markethub-review-service";
    public static final String SERVICE_RECOMMEND = "markethub-recommend-service";

    public static final int USER_STATUS_NORMAL = 1;
    public static final int USER_STATUS_DISABLED = 0;

    public static final int PRODUCT_STATUS_OFF = 0;
    public static final int PRODUCT_STATUS_ON = 1;

    public static final int AUDIT_PENDING = 0;
    public static final int AUDIT_PASSED = 1;
    public static final int AUDIT_REJECTED = 2;

    public static final int ORDER_PENDING_PAY = 0;
    public static final int ORDER_PAID = 1;
    public static final int ORDER_SHIPPED = 2;
    public static final int ORDER_COMPLETED = 3;
    public static final int ORDER_CANCELLED = 4;

    public static final int BEHAVIOR_VIEW = 1;
    public static final int BEHAVIOR_FAVORITE = 2;
    public static final int BEHAVIOR_CART = 3;
    public static final int BEHAVIOR_PURCHASE = 4;

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MERCHANT = "MERCHANT";

    /** 角色 id，与 schema.sql 里 role 表的初始数据一致 */
    public static final int ROLE_ID_USER = 2;
    public static final int ROLE_ID_MERCHANT = 3;

    public static final int APPLICATION_PENDING = 0;
    public static final int APPLICATION_APPROVED = 1;
    public static final int APPLICATION_REJECTED = 2;
    public static final int APPLICATION_REVOKED = 3;

    public static final String REDIS_KEY_PRODUCT = "product:";
    public static final String REDIS_KEY_CATEGORY = "category:";
    public static final String REDIS_KEY_CART = "cart:";
    /** token:version:{userId} -> 当前有效的 token 版本号（权威值在 user.token_version） */
    public static final String REDIS_KEY_TOKEN_VERSION = "token:version:";
    public static final String REDIS_KEY_USER = "user:";

    /** 存放 JWT 的 httpOnly Cookie 名 */
    public static final String AUTH_COOKIE_NAME = "TH_TOKEN";

    /** 可选登录接口上 token 失效时返回的响应头，跨域下需在 expose-headers 中放行。 */
    public static final String TOKEN_EXPIRED_HEADER = "X-Token-Expired";

    public static final String QUEUE_ORDER_CREATE = "order.create.queue";
    public static final String QUEUE_ORDER_PAY = "order.pay.queue";
    /** 延时队列只等 TTL 到期，没有消费者，到期后作为死信转进超时队列。 */
    public static final String QUEUE_ORDER_DELAY = "order.delay.queue";
    /** 超时未支付订单的延迟取消队列，真正被消费的是这个 */
    public static final String QUEUE_ORDER_TIMEOUT = "order.timeout.queue";
    /** 超时取消消息重试耗尽后的兜底队列，留着人工排查，不再自动消费 */
    public static final String QUEUE_ORDER_TIMEOUT_DLQ = "order.timeout.dlq";
    public static final String QUEUE_BEHAVIOR = "behavior.queue";
    public static final String EXCHANGE_ORDER = "order.exchange";
    public static final String ROUTING_ORDER_CREATE = "order.create";
    public static final String ROUTING_ORDER_PAY = "order.pay";
    public static final String ROUTING_ORDER_DELAY = "order.delay";
    public static final String ROUTING_ORDER_TIMEOUT = "order.timeout";
    public static final String ROUTING_ORDER_TIMEOUT_DLQ = "order.timeout.dlq";

    /** 超时未支付自动取消时写入 orders.cancel_reason 的说明 */
    public static final String CANCEL_REASON_TIMEOUT = "超时未支付，系统自动取消";

    /** 秒杀：抢到名额后异步建单的消息队列 */
    public static final String EXCHANGE_SECKILL = "seckill.exchange";
    public static final String QUEUE_SECKILL_CREATE = "seckill.create.queue";
    public static final String ROUTING_SECKILL_CREATE = "seckill.create";

    /** 秒杀 Redis Key 前缀，都要拼上 activityId（用户标记还要拼 userId） */
    public static final String REDIS_KEY_SECKILL_STOCK = "seckill:stock:";
    public static final String REDIS_KEY_SECKILL_USER = "seckill:grabbed:";
    public static final String REDIS_KEY_SECKILL_ACTIVITY = "seckill:activity:";
    public static final String REDIS_KEY_SECKILL_INIT_LOCK = "seckill:init-lock:";

    /** 推荐训练任务的全局锁，多实例部署时只让一个实例真正跑训练 */
    public static final String REDIS_KEY_RECOMMEND_TRAIN_LOCK = "recommend:train-lock";
}
