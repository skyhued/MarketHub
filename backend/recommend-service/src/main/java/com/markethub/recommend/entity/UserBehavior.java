package com.markethub.recommend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserBehavior {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private Integer behaviorType;
    private BigDecimal weight;
    private LocalDateTime createTime;
}
