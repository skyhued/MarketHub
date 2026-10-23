package com.markethub.user.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Role {
    private Integer id;
    private String roleName;
    private String roleCode;
    private String description;
    private Integer status;
    private LocalDateTime createTime;

    private List<Permission> permissions;
}
