package com.joyopi.order.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 처리를 위한 서비스용 DTO (Command)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommand {
    private Long userId;
    private Long productPrice;
    private Long usePoint;
}
