package com.markethub.user.mapper;

import com.markethub.user.entity.MerchantApplication;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MerchantApplicationMapper {

    String SELECT_WITH_JOIN = "SELECT a.*, u.username, u.nickname, u.avatar_url, c.name category_name " +
            "FROM merchant_application a " +
            "LEFT JOIN user u ON a.user_id = u.id " +
            "LEFT JOIN category c ON a.category_id = c.id ";

    @Select(SELECT_WITH_JOIN + "WHERE a.user_id = #{userId} ORDER BY a.id DESC LIMIT 1")
    MerchantApplication findLatestByUserId(@Param("userId") Integer userId);

    @Select(SELECT_WITH_JOIN + "WHERE a.user_id = #{userId} AND a.status = 0 LIMIT 1")
    MerchantApplication findPendingByUserId(@Param("userId") Integer userId);

    @Select(SELECT_WITH_JOIN + "WHERE a.id = #{id}")
    MerchantApplication findById(@Param("id") Integer id);

    /** 管理员列表：待审核的排在最前，其余按提交时间倒序。status 传 null 表示全部。 */
    @Select("<script>" + SELECT_WITH_JOIN +
            "<where><if test='status != null'>a.status = #{status}</if></where> " +
            "ORDER BY (a.status = 0) DESC, a.create_time DESC</script>")
    List<MerchantApplication> findAll(@Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM merchant_application WHERE status = 0")
    int countPending();

    /** 店铺名查重（待审核部分），排除申请人自己，避免重复提交时被自己挡住。 */
    @Select("SELECT COUNT(*) FROM merchant_application WHERE shop_name = #{shopName} AND status = 0 " +
            "AND user_id != #{excludeUserId}")
    int countPendingByShopName(@Param("shopName") String shopName, @Param("excludeUserId") Integer excludeUserId);

    @Insert("INSERT INTO merchant_application " +
            "(user_id, shop_name, category_id, contact_phone, description, status, result_seen, create_time) " +
            "VALUES (#{userId}, #{shopName}, #{categoryId}, #{contactPhone}, #{description}, 0, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MerchantApplication application);

    @Update("UPDATE merchant_application SET status = 1, reviewer_id = #{reviewerId}, " +
            "review_time = NOW(), reject_reason = NULL WHERE id = #{id} AND status = 0")
    int approve(@Param("id") Integer id, @Param("reviewerId") Integer reviewerId);

    @Update("UPDATE merchant_application SET status = 2, reviewer_id = #{reviewerId}, " +
            "review_time = NOW(), reject_reason = #{reason} WHERE id = #{id} AND status = 0")
    int reject(@Param("id") Integer id, @Param("reviewerId") Integer reviewerId,
               @Param("reason") String reason);

    /** 收回：只动这个人已通过的那一条，历史里的驳回记录不受影响。 */
    @Update("UPDATE merchant_application SET status = 3, revoker_id = #{revokerId}, " +
            "revoke_time = NOW(), revoke_reason = #{reason} WHERE user_id = #{userId} AND status = 1")
    int revokeByUserId(@Param("userId") Integer userId, @Param("revokerId") Integer revokerId,
                       @Param("reason") String reason);

    /** 申请人看过"审核通过"的提示后打标记，之后不再重复弹。 */
    @Update("UPDATE merchant_application SET result_seen = 1 " +
            "WHERE user_id = #{userId} AND status = 1 AND result_seen = 0")
    int markResultSeen(@Param("userId") Integer userId);
}
