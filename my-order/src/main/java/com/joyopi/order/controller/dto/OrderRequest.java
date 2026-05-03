package com.joyopi.order.controller.dto;

import com.joyopi.order.service.dto.OrderCommand;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 요청을 위한 컨트롤러 DTO
 */
@Getter
@NoArgsConstructor
public class OrderRequest {
    private Long userId;
    private Long productPrice;
    private Long usePoint;

    public OrderCommand toCommand() {
        return OrderCommand.builder()
                .userId(this.userId)
                .productPrice(this.productPrice)
                .usePoint(this.usePoint)
                .build();
    }
}
