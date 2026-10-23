package com.markethub.review.mapper;

import com.markethub.review.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper {

    @Select("SELECT r.*, u.nickname username, u.avatar_url avatarUrl FROM review r LEFT JOIN user u ON r.user_id = u.id WHERE r.product_id = #{productId} ORDER BY r.create_time DESC")
    List<Review> findByProductId(@Param("productId") Integer productId);

    /** 当前用户对这件商品已经发表过的评价，前端据此把已评过的订单从下拉里剔除 */
    @Select("SELECT r.*, u.nickname username, u.avatar_url avatarUrl FROM review r LEFT JOIN user u ON r.user_id = u.id "
            + "WHERE r.product_id = #{productId} AND r.user_id = #{userId} ORDER BY r.create_time DESC")
    List<Review> findByUserAndProduct(@Param("userId") Integer userId, @Param("productId") Integer productId);

    @Insert("INSERT INTO review (user_id, product_id, order_id, rating, content, create_time, update_time) VALUES (#{userId}, #{productId}, #{orderId}, #{rating}, #{content}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Review review);

    @Select("SELECT COUNT(*) FROM review")
    int countAll();

    /** 同一订单里的同一件商品只能评价一次 */
    @Select("SELECT COUNT(*) > 0 FROM review WHERE user_id = #{userId} AND order_id = #{orderId} AND product_id = #{productId}")
    boolean existsByUserOrderProduct(@Param("userId") Integer userId,
                                     @Param("orderId") Integer orderId,
                                     @Param("productId") Integer productId);

    @Update("UPDATE review SET reply = #{reply}, update_time = NOW() WHERE id = #{id}")
    int updateReply(@Param("id") Integer id, @Param("reply") String reply);
}
