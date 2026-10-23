package com.markethub.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "开店申请")
public class MerchantApplyRequest {

    @Schema(description = "店铺名，最长 20 字，全站唯一")
    private String shopName;

    @Schema(description = "主营类目 id，取自 /api/categories")
    private Integer categoryId;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "申请说明，最长 500 字")
    private String description;
}
