package com.example.poverty.controller;

import com.example.poverty.model.EconomicIndicator;
import com.example.poverty.model.PovertyCounty;
import com.example.poverty.model.Province;
import com.example.poverty.repository.EconomicIndicatorRepository;
import com.example.poverty.repository.PovertyCountyRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/counties")
public class CountyController {
    private final PovertyCountyRepository countyRepo;
    private final EconomicIndicatorRepository indicatorRepo;

    public CountyController(PovertyCountyRepository countyRepo, EconomicIndicatorRepository indicatorRepo) {
        this.countyRepo = countyRepo; this.indicatorRepo = indicatorRepo;
    }

    @GetMapping
    public List<PovertyCounty> list(@RequestParam(required = false) Long provinceId) {
        if (provinceId == null) return countyRepo.findAll();
        return countyRepo.findByProvince_ProvinceId(provinceId);
    }

    @GetMapping("/{id}/indicators")
    public List<EconomicIndicator> getIndicators(@PathVariable Long id,
                                                 @RequestParam(required = false) Integer from,
                                                 @RequestParam(required = false) Integer to) {
        Optional<PovertyCounty> c = countyRepo.findById(id);
        if (!c.isPresent()) return List.of();
        int f = (from == null) ? 2015 : from;
        int t = (to == null) ? java.time.Year.now().getValue() : to;
        return indicatorRepo.findByCountyAndYearBetweenOrderByYearAsc(c.get(), f, t);
    }

    // 在 CountyController.java 中添加新的方法
    @GetMapping("/provinces")
    public List<Province> getProvinces() {
        // 这里需要添加 ProvinceRepository 来获取省份列表
        // 暂时返回空列表，实际项目中需要实现
        return List.of();
    }

    @GetMapping("/search")
    public List<PovertyCounty> searchCounties(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long provinceId) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 这里需要实现按关键词搜索县的功能
            // 暂时返回所有县
            return countyRepo.findAll();
        }
        return list(provinceId);
    }
}