package com.example.poverty.controller;

import com.example.poverty.model.AlertEvent;
import com.example.poverty.model.AlertRule;
import com.example.poverty.model.PovertyCounty;
import com.example.poverty.model.Province;
import com.example.poverty.model.SysUser;
import com.example.poverty.repository.AlertEventRepository;
import com.example.poverty.repository.AlertRuleRepository;
import com.example.poverty.repository.SysUserRepository;
import com.example.poverty.service.AlertScanMonitor;
import com.example.poverty.service.AlertScannerService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertRuleRepository ruleRepo;
    private final AlertEventRepository eventRepo;
    private final AlertScannerService scannerService;
    private final SysUserRepository userRepository;
    private final AlertScanMonitor scanMonitor;

    public AlertController(AlertRuleRepository ruleRepo,
                           AlertEventRepository eventRepo,
                           AlertScannerService scannerService,
                           SysUserRepository userRepository,
                           AlertScanMonitor scanMonitor) {
        this.ruleRepo = ruleRepo;
        this.eventRepo = eventRepo;
        this.scannerService = scannerService;
        this.userRepository = userRepository;
        this.scanMonitor = scanMonitor;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping("/rules")
    public List<AlertRule> listRules() {
        return ruleRepo.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rules")
    public AlertRule createRule(@RequestBody AlertRule r) {
        if (r.getEnabled() == null) r.setEnabled(true);
        return ruleRepo.save(r);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/rules/{ruleId}")
    public AlertRule updateRule(@PathVariable Long ruleId, @RequestBody AlertRule payload) {
        AlertRule existing = ruleRepo.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "警告规则不存在"));
        if (payload.getRuleName() != null) existing.setRuleName(payload.getRuleName());
        if (payload.getMetricKey() != null) existing.setMetricKey(payload.getMetricKey());
        if (payload.getComparator() != null) existing.setComparator(payload.getComparator());
        if (payload.getThreshold() != null) existing.setThreshold(payload.getThreshold());
        if (payload.getDurationYears() != null) existing.setDurationYears(payload.getDurationYears());
        if (payload.getEnabled() != null) existing.setEnabled(payload.getEnabled());
        return ruleRepo.save(existing);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/rules/{ruleId}")
    public void deleteRule(@PathVariable Long ruleId) {
        if (!ruleRepo.existsById(ruleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "警告规则不存在");
        }
        ruleRepo.deleteById(ruleId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping
    public List<AlertEventResponse> listEvents() {
        return eventRepo.findAll().stream()
                .map(AlertEventResponse::fromEntity)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/scan")
    public String scanNow() {
        scannerService.scanAllRulesAsync();
        return "scan-started";
    }

    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping("/scan/status")
    public AlertScanMonitor.ScanStatus getScanStatus() {
        return scanMonitor.currentStatus();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{eventId}/resolve")
    public AlertEventResponse resolveEvent(@PathVariable Long eventId, Authentication authentication) {
        AlertEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "警告事件不存在"));
        if (event.getAcknowledgedAt() == null) {
            event.setAcknowledgedAt(LocalDateTime.now());
            event.setAcknowledgedBy(resolveCurrentUserId(authentication));
            event = eventRepo.save(event);
        }
        return AlertEventResponse.fromEntity(event);
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName())
                .map(SysUser::getUserId)
                .orElse(null);
    }

    record AlertEventResponse(
            Long eventId,
            Integer year,
            Double metricValue,
            LocalDateTime triggeredAt,
            LocalDateTime acknowledgedAt,
            Long acknowledgedBy,
            RuleSummary rule,
            CountySummary county
    ) {
        static AlertEventResponse fromEntity(AlertEvent event) {
            return new AlertEventResponse(
                    event.getEventId(),
                    event.getYear(),
                    event.getMetricValue(),
                    event.getTriggeredAt(),
                    event.getAcknowledgedAt(),
                    event.getAcknowledgedBy(),
                    RuleSummary.from(event.getRule()),
                    CountySummary.from(event.getCounty())
            );
        }
    }

    record RuleSummary(
            Long ruleId,
            String ruleName,
            String metricKey,
            String comparator,
            Double threshold,
            Integer durationYears,
            Boolean enabled
    ) {
        static RuleSummary from(AlertRule rule) {
            if (rule == null) return null;
            return new RuleSummary(
                    rule.getRuleId(),
                    rule.getRuleName(),
                    rule.getMetricKey(),
                    rule.getComparator(),
                    rule.getThreshold(),
                    rule.getDurationYears(),
                    rule.getEnabled()
            );
        }
    }

    record CountySummary(
            Long countyId,
            String countyName,
            ProvinceSummary province
    ) {
        static CountySummary from(PovertyCounty county) {
            if (county == null) return null;
            return new CountySummary(
                    county.getCountyId(),
                    county.getCountyName(),
                    ProvinceSummary.from(county.getProvince())
            );
        }
    }

    record ProvinceSummary(
            Long provinceId,
            String provinceName
    ) {
        static ProvinceSummary from(Province province) {
            if (province == null) return null;
            return new ProvinceSummary(
                    province.getProvinceId(),
                    province.getProvinceName()
            );
        }
    }
}