package com.joyopi.order.common.config;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    public static final String TASK_QUEUE = "OrderSagaTaskQueue";

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newLocalServiceStubs();
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        return WorkflowClient.newInstance(workflowServiceStubs);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    public io.temporal.worker.Worker orderWorker(WorkerFactory workerFactory, 
                                                 com.joyopi.order.temporal.activity.OrderActivity orderActivity) {
        io.temporal.worker.Worker worker = workerFactory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(com.joyopi.order.temporal.workflow.OrderWorkflowImpl.class);
        worker.registerActivitiesImplementations(orderActivity);
        workerFactory.start();
        return worker;
    }
}
