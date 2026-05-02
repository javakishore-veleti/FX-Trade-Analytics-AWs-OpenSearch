package com.jk.fx.trade_mgmt.trade.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class WorkflowMetrics {

    private final MeterRegistry registry;

    public WorkflowMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordTask(String taskName) {
        registry.counter("workflow.task.executed", "task", taskName).increment();
    }

    public void recordFailure(String taskName) {
        registry.counter("workflow.task.failed", "task", taskName).increment();
    }
}
