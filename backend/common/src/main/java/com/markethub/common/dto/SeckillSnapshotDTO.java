package com.markethub.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SeckillSnapshotDTO implements Serializable {
    private Integer activityId;
    private Integer productId;
    private String productName;
    private String productImage;
    private Integer sellerId;
    private BigDecimal seckillPrice;
}
