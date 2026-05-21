package com.joyopi.order.temporal.activity;

import com.joyopi.order.service.dto.OrderCommand;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ActivityInterface
public interface OrderActivity {

    @ActivityMethod
    OrderResult createPendingOrder(OrderCommand command);

    @ActivityMethod
    void completeOrder(Long orderId);

    @ActivityMethod
    void cancelOrder(Long orderId);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class OrderResult {
        private Long orderId;
        private Long userId;
        private Long paymentAmount;
        private Long usePoint;
    }
}
