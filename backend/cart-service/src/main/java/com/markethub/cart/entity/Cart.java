package com.markethub.cart.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Cart {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private Integer quantity;
    private Integer checked;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String productName;
    private String productImage;
    private java.math.BigDecimal productPrice;
    private Integer stock;
    private Integer sellerId;
}
