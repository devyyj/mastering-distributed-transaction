package com.joyopi.order.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PaymentActivity {

    @ActivityMethod
    void processPayment(Long orderId, Long amount);

    @ActivityMethod
    void cancelPayment(Long orderId, Long amount);
}
