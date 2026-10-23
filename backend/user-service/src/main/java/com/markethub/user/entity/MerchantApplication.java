package com.markethub.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantApplication {
    private Integer id;
    private Integer userId;
    private String shopName;
    private Integer categoryId;
    private String contactPhone;
    private String description;
    private Integer status;
    private String rejectReason;
    private Integer reviewerId;
    private LocalDateTime reviewTime;
    private String revokeReason;
    private Integer revokerId;
    private LocalDateTime revokeTime;
    private Integer resultSeen;
    private LocalDateTime createTime;

    private String username;
    private String nickname;
    private String avatarUrl;
    private String categoryName;
}
