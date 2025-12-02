package com.example.poverty.service;

import com.example.poverty.model.EconomicIndicator;
import com.example.poverty.model.PovertyCounty;
import com.example.poverty.model.Province;
import com.example.poverty.repository.EconomicIndicatorRepository;
import com.example.poverty.repository.PovertyCountyRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    
    private final PovertyCountyRepository countyRepository;
    private final EconomicIndicatorRepository indicatorRepository;

    // 手动添加构造器
    public DashboardService(PovertyCountyRepository countyRepository, 
                           EconomicIndicatorRepository indicatorRepository) {
        this.countyRepository = countyRepository;
        this.indicatorRepository = indicatorRepository;
    }

    public Map<String, Object> getSummary(Integer year) {
        int targetYear = year != null ? year : 2022;
        
        // 从数据库获取真实数据
        List<PovertyCounty> allCounties = countyRepository.findAll();
        List<EconomicIndicator> indicators = indicatorRepository.findByYear(targetYear);
        
        long totalCounties = allCounties.size();
        long delistedCounties = allCounties.stream()
                .filter(county -> county.getDelistingYear() != null && county.getDelistingYear() <= targetYear)
                .count();
        
        double coverageRate = totalCounties > 0 ? (double) delistedCounties / totalCounties * 100 : 0;
        
        // 计算平均贫困率和收入
        double avgPovertyRate = indicators.stream()
                .mapToDouble(indicator -> indicator.getPovertyRate() != null ? indicator.getPovertyRate() : 0)
                .average()
                .orElse(0);
        
        double avgRuralIncome = indicators.stream()
                .mapToDouble(indicator -> indicator.getRuralDisposableIncome() != null ? indicator.getRuralDisposableIncome() : 0)
                .average()
                .orElse(0);
        
        double totalGDP = indicators.stream()
                .mapToDouble(indicator -> indicator.getGdp() != null ? indicator.getGdp() : 0)
                .sum();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCounties", totalCounties);
        summary.put("delistedCounties", delistedCounties);
        summary.put("coverageRate", Math.round(coverageRate * 10.0) / 10.0);
        summary.put("funding", 156.8);
        summary.put("avgPovertyRate", Math.round(avgPovertyRate * 10.0) / 10.0);
        summary.put("avgRuralIncome", Math.round(avgRuralIncome));
        summary.put("totalGDP", Math.round(totalGDP * 10.0) / 10.0);
        
        return summary;
    }

    public Map<String, Object> getChartsData(Integer year) {
        int targetYear = year != null ? year : 2022;
        List<EconomicIndicator> indicators = indicatorRepository.findByYear(targetYear);
        
        Map<String, Object> chartsData = new HashMap<>();
        
        // 贫困率分布 - 基于真实数据计算
        long countBelow2 = indicators.stream()
                .filter(ind -> ind.getPovertyRate() != null && ind.getPovertyRate() < 2)
                .count();
        long count2to5 = indicators.stream()
                .filter(ind -> ind.getPovertyRate() != null && ind.getPovertyRate() >= 2 && ind.getPovertyRate() < 5)
                .count();
        long count5to10 = indicators.stream()
                .filter(ind -> ind.getPovertyRate() != null && ind.getPovertyRate() >= 5 && ind.getPovertyRate() < 10)
                .count();
        long countAbove10 = indicators.stream()
                .filter(ind -> ind.getPovertyRate() != null && ind.getPovertyRate() >= 10)
                .count();
        
        Map<String, Object> povertyDistribution = new HashMap<>();
        povertyDistribution.put("categories", Arrays.asList("<2%", "2%-5%", "5%-10%", ">10%"));
        povertyDistribution.put("data", Arrays.asList(countBelow2, count2to5, count5to10, countAbove10));
        chartsData.put("povertyDistribution", povertyDistribution);
        
        // 收入增长趋势 - 计算近5年平均收入
        List<Integer> years = Arrays.asList(2018, 2019, 2020, 2021, 2022);
        List<Double> avgIncomes = years.stream().map(y -> {
            List<EconomicIndicator> yearIndicators = indicatorRepository.findByYear(y);
            return yearIndicators.stream()
                    .mapToDouble(ind -> ind.getRuralDisposableIncome() != null ? ind.getRuralDisposableIncome() : 0)
                    .average()
                    .orElse(0);
        }).collect(Collectors.toList());
        
        Map<String, Object> incomeTrend = new HashMap<>();
        incomeTrend.put("years", years);
        incomeTrend.put("income", avgIncomes.stream().map(income -> Math.round(income)).collect(Collectors.toList()));
        chartsData.put("incomeTrend", incomeTrend);
        
        // 地区分布 - 按省份分组（修复这里的问题）
        Map<String, Long> regionCounts = new HashMap<>();
        List<PovertyCounty> counties = countyRepository.findAll();
        
        for (PovertyCounty county : counties) {
            Province province = county.getProvince();
            if (province != null && province.getProvinceName() != null) {
                String provinceName = province.getProvinceName();
                regionCounts.put(provinceName, regionCounts.getOrDefault(provinceName, 0L) + 1);
            }
        }
        
        Map<String, Object> regionDistribution = new HashMap<>();
        regionDistribution.put("regions", new ArrayList<>(regionCounts.keySet()));
        regionDistribution.put("counts", new ArrayList<>(regionCounts.values()));
        chartsData.put("regionDistribution", regionDistribution);
        
        return chartsData;
    }

    public Map<String, Object> getAnalysisData(Integer year) {
        int targetYear = year != null ? year : 2022;
        List<EconomicIndicator> indicators = indicatorRepository.findByYear(targetYear);
        List<PovertyCounty> counties = countyRepository.findAll();
        
        Map<String, Object> analysisData = new HashMap<>();
        
        // GDP 与贫困率散点
        List<Map<String, Object>> gdpPovertyData = indicators.stream()
                .filter(ind -> ind.getGdp() != null && ind.getPovertyRate() != null && ind.getCounty() != null)
                .map(ind -> {
                    Map<String, Object> dataItem = new HashMap<>();
                    dataItem.put("gdp", ind.getGdp());
                    dataItem.put("poverty", ind.getPovertyRate());
                    PovertyCounty county = ind.getCounty();
                    String countyName = county != null ? county.getCountyName() : "未知县";
                    dataItem.put("county", countyName);
                    Province province = county != null ? county.getProvince() : null;
                    dataItem.put("province", province != null ? province.getProvinceName() : "未知省份");
                    dataItem.put("year", ind.getYear());
                    dataItem.put("income", ind.getRuralDisposableIncome());
                    dataItem.put("gdpPerCapita", ind.getGdpPerCapita());
                    dataItem.put("fiscalRevenue", ind.getFiscalRevenue());
                    dataItem.put("gdpYoy", ind.getGdpYoy());
                    return dataItem;
                })
                .collect(Collectors.toList());
        analysisData.put("gdpPovertyRelation", gdpPovertyData);
        
        List<Integer> years = buildYearRange(targetYear);
        analysisData.put("yearComparison", buildYearComparison(years));
        analysisData.put("trendSeries", buildTrendSeries(years));
        analysisData.put("countyComparison", buildCountyComparison(indicators));
        analysisData.put("heatmap", buildHeatmap(counties, indicators));
        analysisData.put("policyEffect", buildPolicyEffect(targetYear));
        analysisData.put("correlation", buildCorrelationPoints(indicators));
        
        return analysisData;
    }

    public Map<String, Object> getAnalysisDataByProvince(String province, Integer year) {
        return getAnalysisData(year);
    }

    private Map<String, Object> buildCountyComparison(List<EconomicIndicator> indicators) {
        List<EconomicIndicator> top = indicators.stream()
                .filter(ind -> ind.getGdp() != null && ind.getCounty() != null)
                .sorted(Comparator.comparing(EconomicIndicator::getGdp).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<String> names = top.stream()
                .map(ind -> ind.getCounty().getCountyName())
                .collect(Collectors.toList());
        List<Double> gdp = top.stream()
                .map(EconomicIndicator::getGdp)
                .map(this::round1)
                .collect(Collectors.toList());
        List<Double> income = top.stream()
                .map(EconomicIndicator::getRuralDisposableIncome)
                .map(value -> value != null ? (double) Math.round(value) : null)
                .collect(Collectors.toList());
        List<Double> fiscal = top.stream()
                .map(EconomicIndicator::getFiscalRevenue)
                .map(this::round1)
                .collect(Collectors.toList());

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("counties", names);
        comparison.put("gdp", gdp);
        comparison.put("income", income);
        comparison.put("fiscal", fiscal);
        return comparison;
    }

    private Map<String, Object> buildTrendSeries(List<Integer> years) {
        List<Double> poverty = new ArrayList<>();
        List<Double> income = new ArrayList<>();
        List<Double> gdp = new ArrayList<>();

        for (Integer year : years) {
            List<EconomicIndicator> yearIndicators = indicatorRepository.findByYear(year);
            poverty.add(round1(avg(yearIndicators.stream()
                    .map(EconomicIndicator::getPovertyRate)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()))));
            income.add((double) Math.round(avg(yearIndicators.stream()
                    .map(EconomicIndicator::getRuralDisposableIncome)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()))));
            gdp.add(round1(avg(yearIndicators.stream()
                    .map(EconomicIndicator::getGdp)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()))));
        }

        Map<String, Object> trend = new HashMap<>();
        trend.put("years", years);
        trend.put("povertyRate", poverty);
        trend.put("income", income);
        trend.put("gdp", gdp);
        return trend;
    }

    private Map<String, Object> buildYearComparison(List<Integer> years) {
        List<Double> avgPovertyRates = new ArrayList<>();
        List<Double> avgIncomes = new ArrayList<>();

        for (Integer year : years) {
            List<EconomicIndicator> yearIndicators = indicatorRepository.findByYear(year);
            avgPovertyRates.add(round1(avg(yearIndicators.stream()
                    .map(EconomicIndicator::getPovertyRate)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()))));
            avgIncomes.add((double) Math.round(avg(yearIndicators.stream()
                    .map(EconomicIndicator::getRuralDisposableIncome)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()))));
        }

        Map<String, Object> yearComparison = new HashMap<>();
        yearComparison.put("years", years);
        yearComparison.put("avgPovertyRate", avgPovertyRates);
        yearComparison.put("avgIncome", avgIncomes);
        return yearComparison;
    }

    private List<Map<String, Object>> buildHeatmap(List<PovertyCounty> counties,
                                                   List<EconomicIndicator> indicators) {
        Map<String, List<Double>> provinceRates = new HashMap<>();
        indicators.stream()
                .filter(ind -> ind.getCounty() != null && ind.getCounty().getProvince() != null)
                .forEach(ind -> {
                    String provinceName = ind.getCounty().getProvince().getProvinceName();
                    if (provinceName != null && ind.getPovertyRate() != null) {
                        provinceRates.computeIfAbsent(provinceName, k -> new ArrayList<>())
                                .add(ind.getPovertyRate());
                    }
                });

        Map<String, List<String>> provinceIndustries = new HashMap<>();
        counties.stream()
                .filter(c -> c.getProvince() != null && StringUtils.hasText(c.getMainIndustries()))
                .forEach(c -> provinceIndustries.computeIfAbsent(c.getProvince().getProvinceName(), k -> new ArrayList<>())
                        .add(c.getMainIndustries()));

        return provinceRates.entrySet().stream()
                .map(entry -> {
                    String province = entry.getKey();
                    double avgRate = round1(avg(entry.getValue()));
                    String topIndustry = mostFrequent(provinceIndustries.getOrDefault(province, List.of()));
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", province);
                    row.put("value", avgRate);
                    row.put("industry", topIndustry);
                    return row;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildPolicyEffect(int targetYear) {
        int baselineYear = Math.max(2018, targetYear - 3);
        List<EconomicIndicator> baselineIndicators = indicatorRepository.findByYear(baselineYear);
        List<EconomicIndicator> currentIndicators = indicatorRepository.findByYear(targetYear);

        Map<String, Object> effect = new HashMap<>();
        effect.put("baselineYear", baselineYear);
        effect.put("currentYear", targetYear);

        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(metric("贫困率(%)",
                round1(avg(baselineIndicators.stream().map(EconomicIndicator::getPovertyRate).filter(Objects::nonNull).collect(Collectors.toList()))),
                round1(avg(currentIndicators.stream().map(EconomicIndicator::getPovertyRate).filter(Objects::nonNull).collect(Collectors.toList())))));
        metrics.add(metric("人均收入(元)",
                (double) Math.round(avg(baselineIndicators.stream().map(EconomicIndicator::getRuralDisposableIncome).filter(Objects::nonNull).collect(Collectors.toList()))),
                (double) Math.round(avg(currentIndicators.stream().map(EconomicIndicator::getRuralDisposableIncome).filter(Objects::nonNull).collect(Collectors.toList())))));
        metrics.add(metric("GDP(亿元)",
                round1(avg(baselineIndicators.stream().map(EconomicIndicator::getGdp).filter(Objects::nonNull).collect(Collectors.toList()))),
                round1(avg(currentIndicators.stream().map(EconomicIndicator::getGdp).filter(Objects::nonNull).collect(Collectors.toList())))));
        metrics.add(metric("财政收入(亿元)",
                round1(avg(baselineIndicators.stream().map(EconomicIndicator::getFiscalRevenue).filter(Objects::nonNull).collect(Collectors.toList()))),
                round1(avg(currentIndicators.stream().map(EconomicIndicator::getFiscalRevenue).filter(Objects::nonNull).collect(Collectors.toList())))));

        effect.put("metrics", metrics);
        return effect;
    }

    private List<Map<String, Object>> buildCorrelationPoints(List<EconomicIndicator> indicators) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (EconomicIndicator indicator : indicators) {
            if (indicator.getPovertyRate() == null) {
                continue;
            }
            double poverty = indicator.getPovertyRate();
            double educationScore = normalize(indicator.getRuralDisposableIncome(), 6000, 20000);
            double industryScore = normalize(indicator.getGdpPerCapita(), 16000, 60000);
            double infrastructureScore = normalize(indicator.getFiscalRevenue(), 2.0, 25.0);

            points.add(correlationPoint("教育发展", educationScore, poverty));
            points.add(correlationPoint("产业活力", industryScore, poverty));
            points.add(correlationPoint("基础设施", infrastructureScore, poverty));

            if (points.size() > 300) {
                break;
            }
        }
        return points;
    }

    private Map<String, Object> correlationPoint(String factor, double score, double poverty) {
        Map<String, Object> point = new HashMap<>();
        point.put("factor", factor);
        point.put("score", round1(score * 100));
        point.put("povertyRate", round1(poverty));
        return point;
    }

    private Map<String, Object> metric(String name, Double before, Double after) {
        Map<String, Object> map = new HashMap<>();
        map.put("metric", name);
        map.put("before", before);
        map.put("after", after);
        map.put("change", before != null && after != null ? round1(after - before) : null);
        return map;
    }

    private List<Integer> buildYearRange(int targetYear) {
        int end = Math.max(targetYear, 2019);
        int start = Math.max(2018, end - 4);
        List<Integer> years = new ArrayList<>();
        for (int y = start; y <= end; y++) {
            years.add(y);
        }
        return years;
    }

    private double avg(List<Double> values) {
        return values == null || values.isEmpty()
                ? 0
                : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private String mostFrequent(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "优势产业融合发展";
        }
        Map<String, Long> counter = items.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return counter.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("优势产业融合发展");
    }

    private double normalize(Double value, double min, double max) {
        if (value == null) {
            return 0;
        }
        double clamped = Math.max(min, Math.min(max, value));
        return (clamped - min) / (max - min);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private Double round1(Double value) {
        return value == null ? null : round1(value.doubleValue());
    }
}