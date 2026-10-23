package com.markethub.review.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Review {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private Integer orderId;
    private Integer rating;
    private String content;
    private String reply;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String username;
    private String avatarUrl;
}
