package com.example.poverty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AlertEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private AlertRule rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "county_id")
    private PovertyCounty county;

    private Integer year;
    private Double metricValue;
    private LocalDateTime triggeredAt;

    private Long acknowledgedBy;
    private LocalDateTime acknowledgedAt;

    // Explicit getters/setters to avoid Lombok dependency at compile time
    public Long getEventId() { return this.eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public AlertRule getRule() { return this.rule; }
    public void setRule(AlertRule rule) { this.rule = rule; }

    public PovertyCounty getCounty() { return this.county; }
    public void setCounty(PovertyCounty county) { this.county = county; }

    public Integer getYear() { return this.year; }
    public void setYear(Integer year) { this.year = year; }

    public Double getMetricValue() { return this.metricValue; }
    public void setMetricValue(Double metricValue) { this.metricValue = metricValue; }

    public LocalDateTime getTriggeredAt() { return this.triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

    public Long getAcknowledgedBy() { return this.acknowledgedBy; }
    public void setAcknowledgedBy(Long acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

    public LocalDateTime getAcknowledgedAt() { return this.acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    // Minimal builder replacement for Lombok.builder() usage
    public static AlertEventBuilder builder() {
        return new AlertEventBuilder();
    }

    public static class AlertEventBuilder {
        private AlertRule rule;
        private PovertyCounty county;
        private Integer year;
        private Double metricValue;
        private LocalDateTime triggeredAt;

        public AlertEventBuilder rule(AlertRule rule) { this.rule = rule; return this; }
        public AlertEventBuilder county(PovertyCounty county) { this.county = county; return this; }
        public AlertEventBuilder year(Integer year) { this.year = year; return this; }
        public AlertEventBuilder metricValue(Double metricValue) { this.metricValue = metricValue; return this; }
        public AlertEventBuilder triggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; return this; }

        public AlertEvent build() {
            AlertEvent e = new AlertEvent();
            e.setRule(this.rule);
            e.setCounty(this.county);
            e.setYear(this.year);
            e.setMetricValue(this.metricValue);
            e.setTriggeredAt(this.triggeredAt);
            return e;
        }
    }
}