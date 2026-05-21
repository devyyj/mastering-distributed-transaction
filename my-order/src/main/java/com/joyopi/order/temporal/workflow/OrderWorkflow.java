package com.joyopi.order.temporal.workflow;

import com.joyopi.order.service.dto.OrderCommand;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderWorkflow {

    @WorkflowMethod
    Long processOrder(OrderCommand command);
}
