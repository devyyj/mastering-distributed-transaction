package com.joyopi.order.service;

import com.joyopi.order.service.dto.OrderCommand;
import com.joyopi.order.temporal.workflow.OrderWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 주문 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final WorkflowClient workflowClient;

    public Long order(OrderCommand command) {
        log.info("주문 프로세스(Saga) 시작 - userId: {}, productPrice: {}", command.getUserId(), command.getProductPrice());

        OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("OrderSagaTaskQueue")
                        .setWorkflowId("order-saga-" + UUID.randomUUID().toString())
                        .build());

        // 워크플로우 동기 실행 (결과 반환 대기)
        return workflow.processOrder(command);
    }
}
