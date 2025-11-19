package com.example.poverty.controller;

import com.example.poverty.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/indicators")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping("/summary")
    public Map<String,Object> getSummary(@RequestParam(required = false) Integer year) {
        return dashboardService.getSummary(year);
    }

    @GetMapping("/charts")
    public Map<String, Object> getChartsData(@RequestParam(required = false) Integer year) {
        return dashboardService.getChartsData(year);
    }
}