package com.markethub.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Category {
    private Integer id;
    private String name;
    private Integer parentId;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
