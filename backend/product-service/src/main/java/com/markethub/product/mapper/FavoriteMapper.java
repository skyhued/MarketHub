package com.markethub.product.mapper;

import com.markethub.product.entity.Favorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FavoriteMapper {

    @Select("SELECT f.id, f.user_id, f.product_id, f.create_time, p.name product_name, p.image_url product_image, p.price product_price " +
            "FROM favorite f LEFT JOIN product p ON f.product_id = p.id " +
            "WHERE f.user_id = #{userId} ORDER BY f.create_time DESC")
    List<Favorite> findByUserId(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId} AND product_id = #{productId}")
    int exists(@Param("userId") Integer userId, @Param("productId") Integer productId);

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Integer userId);

    @Insert("INSERT IGNORE INTO favorite (user_id, product_id, create_time) VALUES (#{userId}, #{productId}, NOW())")
    int insert(@Param("userId") Integer userId, @Param("productId") Integer productId);

    @Delete("DELETE FROM favorite WHERE user_id = #{userId} AND product_id = #{productId}")
    int delete(@Param("userId") Integer userId, @Param("productId") Integer productId);
}