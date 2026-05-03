package com.joyopi.point.controller.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PointRequest {
    private Long userId;
    private Long amount;
}
