package com.example.poverty.service;

import com.example.poverty.model.*;
import com.example.poverty.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final PovertyCountyRepository countyRepo;
    private final EconomicIndicatorRepository indicatorRepo;
    private final AlertEventRepository alertEventRepo;

    public DashboardService(PovertyCountyRepository countyRepo, EconomicIndicatorRepository indicatorRepo,
                            AlertEventRepository alertEventRepo) {
        this.countyRepo = countyRepo;
        this.indicatorRepo = indicatorRepo;
        this.alertEventRepo = alertEventRepo;
    }

    public Map<String,Object> getSummary(Integer year) {
        Map<String, Object> res = new HashMap<>();
        List<EconomicIndicator> list = (year==null) ? indicatorRepo.findAll() : indicatorRepo.findByYear(year);
        double avgGdpYoy = list.stream().filter(i->i.getGdpYoy()!=null).mapToDouble(EconomicIndicator::getGdpYoy).average().orElse(0.0);
        long atRiskCount = alertEventRepo.findByAcknowledgedByIsNull().size();
        res.put("avgGdpYoy", avgGdpYoy);
        res.put("atRiskCount", atRiskCount);
        List<PovertyCounty> counties = countyRepo.findAll();
        List<Map<String,Object>> map = counties.stream().map(c -> {
            Map<String,Object> m = new HashMap<>();
            m.put("countyId", c.getCountyId());
            m.put("countyName", c.getCountyName());
            List<EconomicIndicator> eList = indicatorRepo.findByCounty_OrderByYearAsc(c);
            Double metric = 0.0;
            if (eList != null && eList.size()>0) {
                EconomicIndicator last = eList.get(eList.size()-1);
                metric = last.getGdpYoy()!=null ? last.getGdpYoy() : 0.0;
            }
            m.put("value", metric);
            return m;
        }).collect(Collectors.toList());
        res.put("map", map);
        return res;
    }

    public Map<String, Object> getChartsData(Integer year) {
        Map<String, Object> chartsData = new HashMap<>();

        // Example: GDP data for bar chart
        List<EconomicIndicator> indicators = (year == null) ? indicatorRepo.findAll() : indicatorRepo.findByYear(year);
        List<Map<String, Object>> gdpData = indicators.stream().map(indicator -> {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("county", indicator.getCounty().getCountyName());
            dataPoint.put("gdp", indicator.getGdp());
            return dataPoint;
        }).collect(Collectors.toList());

        chartsData.put("gdpBarChart", gdpData);

        // Example: Poverty rate data for heatmap
        List<Map<String, Object>> povertyData = indicators.stream().map(indicator -> {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("county", indicator.getCounty().getCountyName());
            dataPoint.put("povertyRate", indicator.getPovertyRate());
            return dataPoint;
        }).collect(Collectors.toList());

        chartsData.put("povertyHeatmap", povertyData);

        return chartsData;
    }
}