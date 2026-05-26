package com.joyopi.order.temporal.workflow;

import com.joyopi.order.service.dto.OrderCommand;
import com.joyopi.order.temporal.activity.OrderActivity;
import com.joyopi.order.temporal.activity.PaymentActivity;
import com.joyopi.order.temporal.activity.PointActivity;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowTest {

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient workflowClient;

    @Mock
    private OrderActivity orderActivity;
    @Mock
    private PointActivity pointActivity;
    @Mock
    private PaymentActivity paymentActivity;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();

        // 1. 주문 서비스 전용 워커 등록 (주문 워크플로우 및 주문 액티비티)
        Worker orderWorker = testEnv.newWorker("OrderSagaTaskQueue");
        orderWorker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
        orderWorker.registerActivitiesImplementations(
            new OrderActivity() {
                @Override public OrderResult createPendingOrder(OrderCommand command) { return orderActivity.createPendingOrder(command); }
                @Override public void completeOrder(Long orderId) { orderActivity.completeOrder(orderId); }
                @Override public void cancelOrder(Long orderId) { orderActivity.cancelOrder(orderId); }
            }
        );

        // 2. 포인트 서비스 전용 워커 등록 (포인트 액티비티)
        Worker pointWorker = testEnv.newWorker("PointTaskQueue");
        pointWorker.registerActivitiesImplementations(
            new PointActivity() {
                @Override public void usePoint(Long userId, Long amount) { pointActivity.usePoint(userId, amount); }
                @Override public void restorePoint(Long userId, Long amount) { pointActivity.restorePoint(userId, amount); }
            }
        );

        // 3. 결제 서비스 전용 워커 등록 (결제 액티비티)
        Worker paymentWorker = testEnv.newWorker("PaymentTaskQueue");
        paymentWorker.registerActivitiesImplementations(
            new PaymentActivity() {
                @Override public void processPayment(Long orderId, Long amount) { paymentActivity.processPayment(orderId, amount); }
                @Override public void cancelPayment(Long orderId, Long amount) { paymentActivity.cancelPayment(orderId, amount); }
            }
        );

        workflowClient = testEnv.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    @DisplayName("주문 정상 처리 워크플로우")
    void processOrder_success() {
        // given
        OrderCommand command = new OrderCommand(1L, 10000L, 1000L);
        OrderActivity.OrderResult orderResult = new OrderActivity.OrderResult(100L, 1L, 9000L, 1000L);
        given(orderActivity.createPendingOrder(any(OrderCommand.class))).willReturn(orderResult);

        testEnv.start();

        OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue("OrderSagaTaskQueue").build());

        // when
        Long resultOrderId = workflow.processOrder(command);

        // then
        assertThat(resultOrderId).isEqualTo(100L);
        verify(orderActivity).createPendingOrder(any(OrderCommand.class));
        verify(pointActivity).usePoint(1L, 1000L);
        verify(paymentActivity).processPayment(100L, 9000L);
        verify(orderActivity).completeOrder(100L);
    }

    @Test
    @DisplayName("포인트 차감 실패 시 주문 취소 보상 트랜잭션 실행")
    void processOrder_fail_point() {
        // given
        OrderCommand command = new OrderCommand(1L, 10000L, 1000L);
        OrderActivity.OrderResult orderResult = new OrderActivity.OrderResult(100L, 1L, 9000L, 1000L);
        given(orderActivity.createPendingOrder(any(OrderCommand.class))).willReturn(orderResult);
        doThrow(new RuntimeException("Point insufficient")).when(pointActivity).usePoint(any(Long.class), any(Long.class));

        testEnv.start();

        OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue("OrderSagaTaskQueue").build());

        // when
        Throwable thrown = catchThrowable(() -> workflow.processOrder(command));

        // then
        assertThat(thrown).isNotNull();
        verify(orderActivity).createPendingOrder(any(OrderCommand.class));
        verify(pointActivity, org.mockito.Mockito.times(3)).usePoint(1L, 1000L);
        // 결제는 호출 안됨
        // verify(paymentActivity, never()).processPayment(any(), any());
        
        // 보상 트랜잭션: 주문 취소 호출
        verify(orderActivity).cancelOrder(100L);
    }

    @Test
    @DisplayName("결제 실패 시 포인트 복구 및 주문 취소 보상 트랜잭션 실행")
    void processOrder_fail_payment() {
        // given
        OrderCommand command = new OrderCommand(1L, 10000L, 1000L);
        OrderActivity.OrderResult orderResult = new OrderActivity.OrderResult(100L, 1L, 9000L, 1000L);
        given(orderActivity.createPendingOrder(any(OrderCommand.class))).willReturn(orderResult);
        doThrow(new RuntimeException("Payment failed")).when(paymentActivity).processPayment(any(Long.class), any(Long.class));

        testEnv.start();

        OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue("OrderSagaTaskQueue").build());

        // when
        Throwable thrown = catchThrowable(() -> workflow.processOrder(command));

        // then
        assertThat(thrown).isNotNull();
        verify(orderActivity).createPendingOrder(any(OrderCommand.class));
        verify(pointActivity).usePoint(1L, 1000L);
        verify(paymentActivity, org.mockito.Mockito.times(3)).processPayment(100L, 9000L);
        
        // 보상 트랜잭션 (역순)
        verify(pointActivity).restorePoint(1L, 1000L);
        verify(orderActivity).cancelOrder(100L);
    }
}
