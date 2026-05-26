package com.joyopi.payment.common.config;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    // 결제 서비스 전용 Temporal Task Queue 이름
    public static final String TASK_QUEUE = "PaymentTaskQueue";

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
    public io.temporal.worker.Worker paymentWorker(WorkerFactory workerFactory, 
                                                   com.joyopi.payment.temporal.activity.PaymentActivity paymentActivity) {
        io.temporal.worker.Worker worker = workerFactory.newWorker(TASK_QUEUE);
        worker.registerActivitiesImplementations(paymentActivity);
        workerFactory.start();
        return worker;
    }
}
