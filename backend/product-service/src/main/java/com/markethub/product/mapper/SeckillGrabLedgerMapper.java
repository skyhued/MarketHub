package com.markethub.product.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillGrabLedgerMapper {

    @Select("SELECT COUNT(*) FROM seckill_grab_ledger WHERE activity_id = #{activityId} AND user_id = #{userId}")
    int exists(@Param("activityId") Integer activityId, @Param("userId") Integer userId);

    @Insert("INSERT IGNORE INTO seckill_grab_ledger (activity_id, user_id, create_time) "
            + "VALUES (#{activityId}, #{userId}, NOW())")
    int tryRecord(@Param("activityId") Integer activityId, @Param("userId") Integer userId);

    @Select("SELECT order_no FROM seckill_grab_ledger WHERE activity_id = #{activityId} AND user_id = #{userId}")
    String findOrderNo(@Param("activityId") Integer activityId, @Param("userId") Integer userId);

    @Update("UPDATE seckill_grab_ledger SET order_no = #{orderNo} "
            + "WHERE activity_id = #{activityId} AND user_id = #{userId} AND order_no IS NULL")
    int attachOrderNo(@Param("activityId") Integer activityId,
                       @Param("userId") Integer userId,
                       @Param("orderNo") String orderNo);

    @Delete("DELETE FROM seckill_grab_ledger "
            + "WHERE activity_id = #{activityId} AND user_id = #{userId} AND order_no IS NULL")
    int deleteIfNoOrder(@Param("activityId") Integer activityId, @Param("userId") Integer userId);
}
