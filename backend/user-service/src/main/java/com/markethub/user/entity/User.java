package com.markethub.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String avatarUrl;
    private Integer roleId;
    private Integer status;
    private String shopName;
    @JsonIgnore
    private Integer tokenVersion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Role role;
}
