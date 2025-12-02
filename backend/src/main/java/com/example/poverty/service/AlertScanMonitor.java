package com.example.poverty.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AlertScanMonitor {

    public record ScanStatus(
            String state,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            Long durationMs,
            String message
    ) {
        public static ScanStatus idle() {
            return new ScanStatus("IDLE", null, null, null, "等待触发");
        }
    }

    private final AtomicReference<ScanStatus> status = new AtomicReference<>(ScanStatus.idle());

    public void markStarted() {
        status.set(new ScanStatus("RUNNING", LocalDateTime.now(), null, null, "正在执行告警扫描"));
    }

    public void markCompleted(long durationMs) {
        LocalDateTime now = LocalDateTime.now();
        ScanStatus current = status.get();
        LocalDateTime started = current != null ? current.startedAt : null;
        status.set(new ScanStatus("COMPLETED", started, now, durationMs, "扫描已完成"));
    }

    public void markFailed(String message) {
        LocalDateTime now = LocalDateTime.now();
        ScanStatus current = status.get();
        LocalDateTime started = current != null ? current.startedAt : null;
        status.set(new ScanStatus("FAILED", started, now, null, message));
    }

    public ScanStatus currentStatus() {
        return status.get();
    }
}



