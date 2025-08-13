package com.oceanbase.odc.service.datasecurity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 单表扫描任务管理器
 * 提供轻量级的异步任务管理功能
 */
@Slf4j
@Component
public class SingleTableScanTaskManager {

    private final Map<String, SingleTableScanTask> tasks = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("scanSensitiveColumnExecutor")
    private ThreadPoolTaskExecutor executor;

    /**
     * 启动单表扫描任务
     */
    public String startTask(String taskId, Runnable scanTask) {
        SingleTableScanTask task = new SingleTableScanTask(taskId);
        tasks.put(taskId, task);

        // 使用Spring的ThreadPoolTaskExecutor，它会自动传递Spring Security上下文
        executor.submit(() -> {
            try {
                task.setStatus(TaskStatus.RUNNING);
                scanTask.run();
                task.setStatus(TaskStatus.COMPLETED);
            } catch (Exception e) {
                log.error("Single table scan task failed, taskId={}", taskId, e);
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
            }
        });

        return taskId;
    }

    /**
     * 启动单表扫描任务（自动生成taskId）
     */
    public String startTask(Runnable scanTask) {
        String taskId = UUID.randomUUID().toString();
        return startTask(taskId, scanTask);
    }

    /**
     * 获取任务状态
     */
    public SingleTableScanTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 设置任务结果
     */
    public void setTaskResult(String taskId, List<SensitiveColumn> result) {
        SingleTableScanTask task = tasks.get(taskId);
        if (task != null) {
            task.setResult(result);
        }
    }

    /**
     * 设置任务错误
     */
    public void setTaskError(String taskId, String errorMessage) {
        SingleTableScanTask task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
        }
    }

    /**
     * 清理已完成的任务（可选的清理机制）
     */
    public void cleanupTask(String taskId) {
        tasks.remove(taskId);
    }

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    /**
     * 单表扫描任务信息
     */
    @Data
    public static class SingleTableScanTask {
        private final String                taskId;
        private       TaskStatus            status = TaskStatus.PENDING;
        private       List<SensitiveColumn> result;
        private       String                errorMessage;
    }
}
