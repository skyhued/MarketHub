package com.markethub.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Permission {
    private Integer id;
    private String permName;
    private String permCode;
    private String permType;
    private Integer parentId;
    private String path;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
}
