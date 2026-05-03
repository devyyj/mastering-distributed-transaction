package com.joyopi.order.service.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 처리를 위한 서비스용 DTO (Command)
 */
@Getter
@Builder
public class OrderCommand {
    private Long userId;
    private Long productPrice;
    private Long usePoint;
}
