package com.markethub.order.mapper;

import com.markethub.common.constant.ApiConstants;
import com.markethub.order.entity.Order;
import com.markethub.order.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT * FROM orders WHERE id = #{id}")
    Order findById(@Param("id") Integer id);

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo}")
    Order findByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> findByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM orders WHERE seller_id = #{sellerId} ORDER BY create_time DESC")
    List<Order> findBySellerId(@Param("sellerId") Integer sellerId);

    @Select("SELECT * FROM orders ORDER BY create_time DESC")
    List<Order> findAll();

    @Insert("INSERT INTO orders (order_no, user_id, seller_id, total_amount, actual_amount, freight, status, receiver_name, receiver_phone, receiver_address, remark, create_time, update_time) " +
            "VALUES (#{orderNo}, #{userId}, #{sellerId}, #{totalAmount}, #{actualAmount}, #{freight}, #{status}, #{receiverName}, #{receiverPhone}, #{receiverAddress}, #{remark}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update({"<script>", "UPDATE orders", "<set>",
            "<if test='status != null'>status = #{status},</if>",
            "<if test='paymentTime != null'>payment_time = #{paymentTime},</if>",
            "<if test='deliveryTime != null'>delivery_time = #{deliveryTime},</if>",
            "<if test='receiveTime != null'>receive_time = #{receiveTime},</if>",
            "<if test='cancelTime != null'>cancel_time = #{cancelTime},</if>",
            "<if test='cancelReason != null'>cancel_reason = #{cancelReason},</if>",
            "update_time = NOW()", "</set>", "WHERE id = #{id}", "</script>"})
    int update(Order order);

    @Select("SELECT COUNT(*) FROM orders")
    int countAll();

    /** 卖家名下未完成订单数：已付款待发货 + 已发货。 */
    @Select("SELECT COUNT(*) FROM orders WHERE seller_id = #{sellerId} AND status IN (" +
            ApiConstants.ORDER_PAID + ", " + ApiConstants.ORDER_SHIPPED + ")")
    int countUnfinishedBySeller(@Param("sellerId") Integer sellerId);

    /** 条件更新：只有待付款才标记为已付款，重复支付只有先到的生效。 */
    @Update("UPDATE orders SET status = " + ApiConstants.ORDER_PAID
            + ", payment_time = NOW(), update_time = NOW() WHERE id = #{id} AND status = " + ApiConstants.ORDER_PENDING_PAY)
    int markPaidIfPending(@Param("id") Integer id);

    /** 条件更新：只有当前还是待付款才取消，语义同上。 */
    @Update("UPDATE orders SET status = " + ApiConstants.ORDER_CANCELLED
            + ", cancel_time = NOW(), cancel_reason = #{reason}, update_time = NOW() WHERE id = #{id} AND status = "
            + ApiConstants.ORDER_PENDING_PAY)
    int cancelIfPending(@Param("id") Integer id, @Param("reason") String reason);

    /** 条件更新：只有已付款未发货才取消；拆两条是因为两种状态要做的事不同。 */
    @Update("UPDATE orders SET status = " + ApiConstants.ORDER_CANCELLED
            + ", cancel_time = NOW(), cancel_reason = #{reason}, update_time = NOW() WHERE id = #{id} AND status = "
            + ApiConstants.ORDER_PAID)
    int cancelIfPaid(@Param("id") Integer id, @Param("reason") String reason);
}
