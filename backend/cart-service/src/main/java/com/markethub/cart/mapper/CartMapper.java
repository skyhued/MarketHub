package com.markethub.cart.mapper;

import com.markethub.cart.entity.Cart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CartMapper {

    @Select("SELECT c.*, p.name product_name, p.image_url product_image, p.price product_price, p.stock, p.user_id seller_id " +
            "FROM cart c LEFT JOIN product p ON c.product_id = p.id WHERE c.user_id = #{userId} ORDER BY c.create_time DESC")
    List<Cart> findByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM cart WHERE user_id = #{userId} AND product_id = #{productId}")
    Cart findByUserAndProduct(@Param("userId") Integer userId, @Param("productId") Integer productId);

    @Insert("INSERT INTO cart (user_id, product_id, quantity, checked, create_time, update_time) VALUES (#{userId}, #{productId}, #{quantity}, #{checked}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Cart cart);

    @Update({"<script>", "UPDATE cart", "<set>",
            "<if test='quantity != null'>quantity = #{quantity},</if>",
            "<if test='checked != null'>checked = #{checked},</if>",
            "update_time = NOW()", "</set>",
            "WHERE user_id = #{userId} AND product_id = #{productId}", "</script>"})
    int update(Cart cart);

    @Delete("DELETE FROM cart WHERE user_id = #{userId} AND product_id = #{productId}")
    int deleteByUserAndProduct(@Param("userId") Integer userId, @Param("productId") Integer productId);

    @Delete("DELETE FROM cart WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);
}
