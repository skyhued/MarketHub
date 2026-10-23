package com.markethub.user.mapper;

import com.markethub.user.entity.Permission;
import com.markethub.user.entity.Role;
import com.markethub.user.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT u.*, r.id role_id, r.role_name, r.role_code, r.description role_desc " +
            "FROM user u LEFT JOIN role r ON u.role_id = r.id WHERE u.username = #{username}")
    @Results(id = "userResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "username", property = "username"),
            @Result(column = "password", property = "password"),
            @Result(column = "nickname", property = "nickname"),
            @Result(column = "avatar_url", property = "avatarUrl"),
            @Result(column = "role_id", property = "roleId"),
            @Result(column = "status", property = "status"),
            @Result(column = "shop_name", property = "shopName"),
            @Result(column = "token_version", property = "tokenVersion"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(column = "role_id", property = "role.id"),
            @Result(column = "role_name", property = "role.roleName"),
            @Result(column = "role_code", property = "role.roleCode"),
            @Result(column = "role_desc", property = "role.description")
    })
    User findByUsername(@Param("username") String username);

    @Select("SELECT u.*, r.id role_id, r.role_name, r.role_code, r.description role_desc " +
            "FROM user u LEFT JOIN role r ON u.role_id = r.id WHERE u.id = #{id}")
    @ResultMap("userResult")
    User findById(@Param("id") Integer id);

    @Insert("INSERT INTO user (username, password, nickname, role_id, status, create_time, update_time) " +
            "VALUES (#{username}, #{password}, #{nickname}, COALESCE(#{roleId}, 2), 1, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update({"<script>",
            "UPDATE user",
            "<set>",
            "<if test='nickname != null'>nickname = #{nickname},</if>",
            "<if test='avatarUrl != null'>avatar_url = #{avatarUrl},</if>",
            "<if test='password != null'>password = #{password},</if>",
            "<if test='status != null'>status = #{status},</if>",
            "<if test='roleId != null'>role_id = #{roleId},</if>",
            "update_time = NOW()",
            "</set>",
            "WHERE id = #{id}",
            "</script>"})
    int update(User user);

    @Update("UPDATE user SET token_version = token_version + 1 WHERE id = #{id}")
    int incrementTokenVersion(@Param("id") Integer id);

    @Select("SELECT token_version FROM user WHERE id = #{id}")
    Integer findTokenVersion(@Param("id") Integer id);


    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    @Select("SELECT COUNT(*) FROM user")
    int countAll();

    @Select("SELECT u.*, r.id role_id, r.role_name, r.role_code, r.description role_desc " +
            "FROM user u LEFT JOIN role r ON u.role_id = r.id ORDER BY u.create_time DESC")
    @ResultMap("userResult")
    List<User> findAll();

    @Select("SELECT p.* FROM permission p " +
            "JOIN role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} AND p.status = 1")
    List<Permission> findPermissionsByRoleId(@Param("roleId") Integer roleId);

    /* 这两个方法都不动 token_version：作废 token 统一走 UserService.revokeTokens()。 */

    /** 审核通过：写入店铺名并升为商家；调用方必须再调 revokeTokens。 */
    @Update("UPDATE user SET shop_name = #{shopName}, role_id = #{roleId}, update_time = NOW() WHERE id = #{id}")
    int promoteToMerchant(@Param("id") Integer id, @Param("shopName") String shopName,
                          @Param("roleId") Integer roleId);

    /** 收回商家资格：只降角色，shop_name 按设计保留，别人不能占用这个店名。 */
    @Update("UPDATE user SET role_id = #{roleId}, update_time = NOW() WHERE id = #{id}")
    int demoteToUser(@Param("id") Integer id, @Param("roleId") Integer roleId);

    @Select("SELECT u.*, r.id role_id, r.role_name, r.role_code, r.description role_desc " +
            "FROM user u LEFT JOIN role r ON u.role_id = r.id " +
            "WHERE r.role_code = 'MERCHANT' ORDER BY u.shop_name")
    @ResultMap("userResult")
    List<User> findMerchants();

    /** 店铺名查重（已通过部分）：收回资格后 shop_name 仍保留，需排除申请人自己。 */
    @Select("SELECT COUNT(*) FROM user WHERE shop_name = #{shopName} AND id != #{excludeUserId}")
    int countByShopName(@Param("shopName") String shopName, @Param("excludeUserId") Integer excludeUserId);
}
