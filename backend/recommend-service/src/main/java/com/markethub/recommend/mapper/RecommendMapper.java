package com.markethub.recommend.mapper;

import com.markethub.recommend.entity.RecommendationCache;
import com.markethub.recommend.entity.UserBehavior;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RecommendMapper {

    @Select("SELECT r.*, p.name product_name, p.image_url product_image, p.price product_price " +
            "FROM recommendation_cache r LEFT JOIN product p ON r.product_id = p.id " +
            "WHERE r.user_id = #{userId} ORDER BY r.score DESC LIMIT #{limit}")
    List<RecommendationCache> findRecommendByUserId(@Param("userId") Integer userId, @Param("limit") int limit);

    @Select("SELECT r.*, p.name product_name, p.image_url product_image, p.price product_price " +
            "FROM recommendation_cache r LEFT JOIN product p ON r.product_id = p.id " +
            "WHERE r.user_id = -1 ORDER BY r.score DESC LIMIT #{limit}")
    List<RecommendationCache> findHotProducts(@Param("limit") int limit);

    @Select("SELECT r.*, p.name product_name, p.image_url product_image, p.price product_price " +
            "FROM recommendation_cache r LEFT JOIN product p ON r.product_id = p.id " +
            "WHERE r.user_id = #{productId} ORDER BY r.score DESC LIMIT #{limit}")
    List<RecommendationCache> findRelatedProducts(@Param("productId") Integer productId, @Param("limit") int limit);

    @Insert("INSERT INTO user_behavior (user_id, product_id, behavior_type, weight, create_time) VALUES (#{userId}, #{productId}, #{behaviorType}, #{weight}, NOW())")
    int insertBehavior(UserBehavior behavior);

    @Select("SELECT * FROM user_behavior WHERE user_id = #{userId} AND product_id = #{productId} AND behavior_type = #{behaviorType} LIMIT 1")
    UserBehavior findBehavior(@Param("userId") Integer userId, @Param("productId") Integer productId, @Param("behaviorType") Integer behaviorType);

    @Delete("DELETE FROM recommendation_cache WHERE user_id = #{userId}")
    int clearRecommendByUserId(@Param("userId") Integer userId);

    @Insert("<script>INSERT INTO recommendation_cache (user_id, product_id, score, reason, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>(#{item.userId}, #{item.productId}, #{item.score}, #{item.reason}, NOW())</foreach></script>")
    int batchInsertRecommend(@Param("list") List<RecommendationCache> items);
}
