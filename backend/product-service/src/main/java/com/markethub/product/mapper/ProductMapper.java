package com.markethub.product.mapper;

import com.markethub.product.entity.Category;
import com.markethub.product.entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT p.*, c.name category_name FROM product p LEFT JOIN category c ON p.category_id = c.id WHERE p.id = #{id}")
    @Results(id = "productResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "name", property = "name"),
            @Result(column = "description", property = "description"),
            @Result(column = "price", property = "price"),
            @Result(column = "stock", property = "stock"),
            @Result(column = "image_url", property = "imageUrl"),
            @Result(column = "category_id", property = "categoryId"),
            @Result(column = "status", property = "status"),
            @Result(column = "audit_status", property = "auditStatus"),
            @Result(column = "sales_count", property = "salesCount"),
            @Result(column = "view_count", property = "viewCount"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(column = "category_name", property = "categoryName"),
            @Result(column = "seller_name", property = "sellerName")
    })
    Product findById(@Param("id") Integer id);

    @Select("SELECT p.*, c.name category_name FROM product p LEFT JOIN category c ON p.category_id = c.id ORDER BY p.create_time DESC")
    @ResultMap("productResult")
    List<Product> findAll();

    @Select("SELECT p.*, c.name category_name FROM product p LEFT JOIN category c ON p.category_id = c.id WHERE p.status = 1 AND p.audit_status = 1 ORDER BY p.create_time DESC")
    @ResultMap("productResult")
    List<Product> findOnSale();

    @Select("<script>SELECT p.*, c.name category_name, COALESCE(u.shop_name, u.nickname) seller_name " +
            "FROM product p LEFT JOIN category c ON p.category_id = c.id LEFT JOIN user u ON p.user_id = u.id WHERE 1=1 " +
            "<if test='auditStatus != null'>AND p.audit_status = #{auditStatus}</if> " +
            "ORDER BY p.create_time DESC</script>")
    @ResultMap("productResult")
    List<Product> findForAdmin(@Param("auditStatus") Integer auditStatus);

    @Select("SELECT p.*, c.name category_name FROM product p LEFT JOIN category c ON p.category_id = c.id WHERE p.user_id = #{userId} ORDER BY p.create_time DESC")
    @ResultMap("productResult")
    List<Product> findByUserId(@Param("userId") Integer userId);

    @Select("<script>" +
            "SELECT p.*, c.name category_name FROM product p LEFT JOIN category c ON p.category_id = c.id WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR p.description LIKE CONCAT('%', #{keyword}, '%'))</if> " +
            "<if test='categoryId != null'>AND p.category_id = #{categoryId}</if> " +
            "AND p.status = 1 AND p.audit_status = 1 ORDER BY p.create_time DESC</script>")
    @ResultMap("productResult")
    List<Product> search(@Param("keyword") String keyword, @Param("categoryId") Integer categoryId);

    @Insert("INSERT INTO product (user_id, name, description, price, stock, image_url, category_id, status, audit_status, sales_count, view_count, create_time, update_time) VALUES (#{userId}, #{name}, #{description}, #{price}, #{stock}, #{imageUrl}, #{categoryId}, 0, 0, 0, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Update({"<script>", "UPDATE product", "<set>",
            "<if test='name != null'>name = #{name},</if>",
            "<if test='description != null'>description = #{description},</if>",
            "<if test='price != null'>price = #{price},</if>",
            "<if test='stock != null'>stock = #{stock},</if>",
            "<if test='imageUrl != null'>image_url = #{imageUrl},</if>",
            "<if test='categoryId != null'>category_id = #{categoryId},</if>",
            "<if test='status != null'>status = #{status},</if>",
            "<if test='auditStatus != null'>audit_status = #{auditStatus},</if>",
            "<if test='salesCount != null'>sales_count = #{salesCount},</if>",
            "<if test='viewCount != null'>view_count = #{viewCount},</if>",
            "update_time = NOW()", "</set>", "WHERE id = #{id}", "</script>"})
    int update(Product product);

    @Delete("DELETE FROM product WHERE id = #{id}")
    int delete(@Param("id") Integer id);

    @Update("UPDATE product SET view_count = view_count + 1 WHERE id = #{id}")
    int updateViewCount(@Param("id") Integer id);

    /** 条件更新扣库存：库存够且在售才扣得动，影响 0 行需回读区分原因。 */
    @Update("UPDATE product SET stock = stock - #{quantity}, update_time = NOW() "
            + "WHERE id = #{id} AND stock >= #{quantity} AND status = 1 AND audit_status = 1")
    int deductStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /** 增加可售库存：归还和补货都是加，没有下限条件，不会减成负数 */
    @Update("UPDATE product SET stock = stock + #{quantity}, update_time = NOW() WHERE id = #{id}")
    int addStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET sales_count = sales_count + #{quantity}, update_time = NOW() WHERE id = #{id}")
    int increaseSales(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /** 条件更新扣回销量，减不成负数；影响 0 行表示销量对不上（种子数据或历史脏数据） */
    @Update("UPDATE product SET sales_count = sales_count - #{quantity}, update_time = NOW() "
            + "WHERE id = #{id} AND sales_count >= #{quantity}")
    int decreaseSales(@Param("id") Integer id, @Param("quantity") Integer quantity);

    @Select("SELECT COUNT(*) FROM product")
    int countAll();

    /** 卖家展示名：开过店显示店铺名，否则昵称。 */
    @Select("SELECT COALESCE(shop_name, nickname) FROM user WHERE id = #{userId}")
    String findSellerName(@Param("userId") Integer userId);

    /** 下架某个卖家的全部在售商品，收回商家资格时用；返回下架了几件 */
    @Update("UPDATE product SET status = 0, update_time = NOW() WHERE user_id = #{sellerId} AND status = 1")
    int takeDownBySeller(@Param("sellerId") Integer sellerId);

    @Select("SELECT * FROM category ORDER BY sort_order")
    List<Category> findAllCategories();
}