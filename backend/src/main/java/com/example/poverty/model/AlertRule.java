package com.example.poverty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alert_rule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AlertRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;
    private String ruleName;
    private String metricKey;
    private String comparator;
    private Double threshold;
    private Integer durationYears;
    private Boolean enabled;

    // Explicit getter in case Lombok annotation processing is not active during build
    public Integer getDurationYears() { return this.durationYears; }
    public void setDurationYears(Integer durationYears) { this.durationYears = durationYears; }

    // Explicit getters/setters to avoid depending on Lombok at compile time
    public Long getRuleId() { return this.ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return this.ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getMetricKey() { return this.metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }

    public String getComparator() { return this.comparator; }
    public void setComparator(String comparator) { this.comparator = comparator; }

    public Double getThreshold() { return this.threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }

    public Boolean getEnabled() { return this.enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}