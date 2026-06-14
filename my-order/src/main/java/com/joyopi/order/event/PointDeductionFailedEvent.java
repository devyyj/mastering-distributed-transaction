package com.joyopi.order.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointDeductionFailedEvent {
    private Long orderId;
    private String reason;
}
