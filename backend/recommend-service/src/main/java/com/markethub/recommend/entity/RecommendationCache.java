package com.markethub.recommend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecommendationCache {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private BigDecimal score;
    private String reason;
    private LocalDateTime createTime;

    private String productName;
    private String productImage;
    private BigDecimal productPrice;
}
