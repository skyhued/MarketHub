package com.markethub.product.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Favorite {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private LocalDateTime createTime;

    private String productName;
    private String productImage;
    private BigDecimal productPrice;
}