package com.markethub.order.mapper;

import com.markethub.order.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    List<OrderItem> findByOrderId(@Param("orderId") Integer orderId);

    @Insert("INSERT INTO order_item (order_id, product_id, product_name, product_image, quantity, unit_price, total_price, create_time) " +
            "VALUES (#{orderId}, #{productId}, #{productName}, #{productImage}, #{quantity}, #{unitPrice}, #{totalPrice}, NOW())")
    int insert(OrderItem item);

    @Insert("<script>INSERT INTO order_item (order_id, product_id, product_name, product_image, quantity, unit_price, total_price, create_time) VALUES " +
            "<foreach collection='items' item='item' separator=','>(#{item.orderId}, #{item.productId}, #{item.productName}, #{item.productImage}, #{item.quantity}, #{item.unitPrice}, #{item.totalPrice}, NOW())</foreach></script>")
    int batchInsert(@Param("items") List<OrderItem> items);

    /** 评价资格校验用：确认这件商品确实出现在该订单里 */
    @Select("SELECT COUNT(*) > 0 FROM order_item WHERE order_id = #{orderId} AND product_id = #{productId}")
    boolean existsProductInOrder(@Param("orderId") Integer orderId, @Param("productId") Integer productId);
}
