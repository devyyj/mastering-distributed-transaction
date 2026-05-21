package com.joyopi.point.common.config;

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
    public io.temporal.worker.Worker pointWorker(WorkerFactory workerFactory, 
                                                 com.joyopi.point.temporal.activity.PointActivity pointActivity) {
        io.temporal.worker.Worker worker = workerFactory.newWorker(TASK_QUEUE);
        worker.registerActivitiesImplementations(pointActivity);
        workerFactory.start();
        return worker;
    }
}
