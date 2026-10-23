package com.markethub.common.constant;

public final class PermissionConstants {

    private PermissionConstants() {}

    public static final String USER_LIST = "user:list";
    public static final String USER_CREATE = "user:create";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";
    public static final String USER_STATUS = "user:status";
    public static final String USER_ROLE = "user:role";

    public static final String PRODUCT_LIST = "product:list";
    public static final String PRODUCT_CREATE = "product:create";
    public static final String PRODUCT_UPDATE = "product:update";
    public static final String PRODUCT_DELETE = "product:delete";
    public static final String PRODUCT_AUDIT = "product:audit";

    public static final String ORDER_LIST = "order:list";
    public static final String ORDER_CREATE = "order:create";
    public static final String ORDER_PAY = "order:pay";
    public static final String ORDER_SHIP = "order:ship";
    public static final String ORDER_CONFIRM = "order:confirm";
    public static final String ORDER_CANCEL = "order:cancel";

    public static final String CART_LIST = "cart:list";
    public static final String CART_OPERATE = "cart:operate";

    public static final String REVIEW_CREATE = "review:create";
    public static final String REVIEW_LIST = "review:list";

    /** 普通用户提交开店申请；商家和管理员没有这个权限 */
    public static final String MERCHANT_APPLY = "merchant:apply";
    /** 审核开店申请、修改店铺名 */
    public static final String MERCHANT_AUDIT = "merchant:audit";

    public static final String AI_CHAT = "ai:chat";

    public static final String ADMIN_DASHBOARD = "admin:dashboard";
    public static final String ADMIN_STATS = "admin:stats";
}
