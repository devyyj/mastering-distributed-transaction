package com.joyopi.order.temporal.workflow;

import com.joyopi.order.service.dto.OrderCommand;
import com.joyopi.order.temporal.activity.OrderActivity;
import com.joyopi.order.temporal.activity.PaymentActivity;
import com.joyopi.order.temporal.activity.PointActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class OrderWorkflowImpl implements OrderWorkflow {

    // 주문 서비스용 액티비티 옵션 (기본 OrderSagaTaskQueue 사용)
    private final ActivityOptions orderOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumAttempts(3)
                    .build())
            .build();

    // 포인트 서비스용 액티비티 옵션 (PointTaskQueue 라우팅)
    private final ActivityOptions pointOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setTaskQueue("PointTaskQueue")
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumAttempts(3)
                    .build())
            .build();

    // 결제 서비스용 액티비티 옵션 (PaymentTaskQueue 라우팅)
    private final ActivityOptions paymentOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setTaskQueue("PaymentTaskQueue")
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumAttempts(3)
                    .build())
            .build();

    private final OrderActivity orderActivity = Workflow.newActivityStub(OrderActivity.class, orderOptions);
    private final PointActivity pointActivity = Workflow.newActivityStub(PointActivity.class, pointOptions);
    private final PaymentActivity paymentActivity = Workflow.newActivityStub(PaymentActivity.class, paymentOptions);

    @Override
    public Long processOrder(OrderCommand command) {
        Saga.Options sagaOptions = new Saga.Options.Builder().setParallelCompensation(false).build();
        Saga saga = new Saga(sagaOptions);

        OrderActivity.OrderResult orderResult = null;

        try {
            // 1. 주문 생성
            orderResult = orderActivity.createPendingOrder(command);
            final Long orderId = orderResult.getOrderId();
            final Long usePoint = orderResult.getUsePoint();
            final Long paymentAmount = orderResult.getPaymentAmount();

            saga.addCompensation(orderActivity::cancelOrder, orderId);

            // 2. 포인트 차감 (재고 확보 역할)
            pointActivity.usePoint(command.getUserId(), usePoint);
            saga.addCompensation(pointActivity::restorePoint, command.getUserId(), usePoint);

            // 3. 결제 처리
            paymentActivity.processPayment(orderId, paymentAmount);
            saga.addCompensation(paymentActivity::cancelPayment, orderId, paymentAmount);

            // 4. 주문 완료
            orderActivity.completeOrder(orderId);

            return orderId;

        } catch (Exception e) {
            // 실패 시 보상 트랜잭션 실행
            saga.compensate();
            throw Workflow.wrap(e);
        }
    }
}
