package com.markethub.product.mapper;

import com.markethub.product.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillActivityMapper {

    @Select("SELECT * FROM seckill_activity WHERE id = #{id}")
    SeckillActivity findById(@Param("id") Integer id);

    @Update("UPDATE seckill_activity SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int deductStock(@Param("id") Integer id);

    @Update("UPDATE seckill_activity SET stock = stock + 1 WHERE id = #{id}")
    int restoreStock(@Param("id") Integer id);
}
