package com.joyopi.order.service;

import com.joyopi.order.service.dto.OrderCommand;
import com.joyopi.order.temporal.workflow.OrderWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private WorkflowClient workflowClient;

    @Mock
    private OrderWorkflow orderWorkflow;

    @Test
    @DisplayName("주문 요청 시 Temporal WorkflowClient를 통해 워크플로우를 실행한다")
    void order_workflow_start() {
        // given
        OrderCommand command = new OrderCommand(1L, 10000L, 1000L);

        given(workflowClient.newWorkflowStub(eq(OrderWorkflow.class), any(WorkflowOptions.class)))
                .willReturn(orderWorkflow);
        given(orderWorkflow.processOrder(command)).willReturn(100L);

        // when
        Long orderId = orderService.order(command);

        // then
        assertThat(orderId).isEqualTo(100L);
        verify(workflowClient).newWorkflowStub(eq(OrderWorkflow.class), any(WorkflowOptions.class));
        verify(orderWorkflow).processOrder(command);
    }
}
