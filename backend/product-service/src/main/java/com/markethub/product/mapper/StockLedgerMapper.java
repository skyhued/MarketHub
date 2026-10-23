package com.markethub.product.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockLedgerMapper {

    /** 记一笔库存/销量变更；返回 0 表示已经记过，调用方应跳过。 */
    @Insert("INSERT IGNORE INTO stock_ledger (order_no, product_id, op, quantity, create_time) "
            + "VALUES (#{orderNo}, #{productId}, #{op}, #{quantity}, NOW())")
    int tryRecord(@Param("orderNo") String orderNo,
                  @Param("productId") Integer productId,
                  @Param("op") String op,
                  @Param("quantity") Integer quantity);
}
