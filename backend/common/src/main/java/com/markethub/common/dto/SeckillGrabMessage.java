package com.markethub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGrabMessage implements Serializable {
    private Integer activityId;
    private Integer userId;
}
