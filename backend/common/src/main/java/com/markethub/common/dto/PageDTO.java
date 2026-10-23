package com.markethub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageDTO {
    private int page = 1;
    private int size = 10;
    private String sortBy;
    private String sortOrder = "DESC";
}
