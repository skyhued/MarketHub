package com.markethub.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private Integer quantity;
    private Integer checked;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private Integer stock;
    private Integer sellerId;
}