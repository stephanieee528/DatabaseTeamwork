package com.example.poverty.controller;

import com.example.poverty.model.CountyProject;
import com.example.poverty.model.EconomicIndicator;
import com.example.poverty.model.PovertyCounty;
import com.example.poverty.model.Province;
import com.example.poverty.repository.CountyProjectRepository;
import com.example.poverty.repository.EconomicIndicatorRepository;
import com.example.poverty.repository.PovertyCountyRepository;
import com.example.poverty.repository.ProvinceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/counties")
public class CountyController {
    private final PovertyCountyRepository countyRepo;
    private final EconomicIndicatorRepository indicatorRepo;
    private final ProvinceRepository provinceRepo;
    private final CountyProjectRepository projectRepo;

    public CountyController(PovertyCountyRepository countyRepo,
                            EconomicIndicatorRepository indicatorRepo,
                            ProvinceRepository provinceRepo,
                            CountyProjectRepository projectRepo) {
        this.countyRepo = countyRepo;
        this.indicatorRepo = indicatorRepo;
        this.provinceRepo = provinceRepo;
        this.projectRepo = projectRepo;
    }

    @GetMapping
    public List<PovertyCounty> list(@RequestParam(required = false) Long provinceId,
                                    @RequestParam(required = false) String keyword) {
        if (StringUtils.hasText(keyword)) {
            return provinceId == null
                    ? countyRepo.findByCountyNameContainingIgnoreCase(keyword.trim())
                    : countyRepo.findByCountyNameContainingIgnoreCaseAndProvince_ProvinceId(keyword.trim(), provinceId);
        }
        if (provinceId == null) {
            return countyRepo.findAll();
        }
        return countyRepo.findByProvince_ProvinceId(provinceId);
    }

    @GetMapping("/{id}/indicators")
    public List<EconomicIndicator> getIndicators(@PathVariable Long id,
                                                 @RequestParam(required = false) Integer from,
                                                 @RequestParam(required = false) Integer to) {
        Optional<PovertyCounty> county = countyRepo.findById(id);
        if (county.isEmpty()) {
            return List.of();
        }
        int f = (from == null) ? 2015 : from;
        int t = (to == null) ? java.time.Year.now().getValue() : to;
        return indicatorRepo.findByCountyAndYearBetweenOrderByYearAsc(county.get(), f, t);
    }

    @GetMapping("/provinces")
    public List<Province> getProvinces() {
        return provinceRepo.findAll();
    }

    @GetMapping("/search")
    public List<PovertyCounty> searchCounties(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long provinceId) {
        return list(provinceId, keyword);
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<?> getCountyDetail(@PathVariable Long id) {
        PovertyCounty county = countyRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "县不存在"));
        List<EconomicIndicator> indicators = indicatorRepo.findByCounty_OrderByYearAsc(county);
        EconomicIndicator latest = indicators.isEmpty() ? null : indicators.get(indicators.size() - 1);
        List<CountyProject> projects = projectRepo.findByCounty_CountyId(id);

        Map<String, Object> detail = new HashMap<>();
        detail.put("county", buildCountySummary(county, latest, projects));
        detail.put("kpis", buildKpi(latest, county));
        detail.put("projectOverview", buildProjectOverview(projects, county));
        detail.put("projects", projects);
        detail.put("indicators", indicators);
        detail.put("indicatorTrend", indicators.stream().map(this::mapIndicator).collect(Collectors.toList()));
        return ResponseEntity.ok(detail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PovertyCounty> updateCounty(@PathVariable Long id,
                                                      @RequestBody CountyUpdateRequest request) {
        PovertyCounty county = countyRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "县不存在"));
        if (StringUtils.hasText(request.getCountyName())) {
            county.setCountyName(request.getCountyName().trim());
        }
        if (request.getProvinceId() != null) {
            Province province = provinceRepo.findById(request.getProvinceId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "省份不存在"));
            county.setProvince(province);
        }
        if (request.getPopulation() != null) {
            county.setPopulation(request.getPopulation());
        }
        if (request.getArea() != null) {
            county.setArea(request.getArea());
        }
        if (StringUtils.hasText(request.getPovertyLevel())) {
            county.setPovertyLevel(request.getPovertyLevel().trim());
        }
        if (request.getPovertyAlleviationRate() != null) {
            county.setPovertyAlleviationRate(request.getPovertyAlleviationRate());
        }
        if (request.getEconomicGrowthRate() != null) {
            county.setEconomicGrowthRate(request.getEconomicGrowthRate());
        }
        if (request.getEmploymentRate() != null) {
            county.setEmploymentRate(request.getEmploymentRate());
        }
        if (request.getCoveragePopulation() != null) {
            county.setCoveragePopulation(request.getCoveragePopulation());
        }
        if (request.getTotalInvestment() != null) {
            county.setTotalInvestment(request.getTotalInvestment());
        }
        if (StringUtils.hasText(request.getMainIndustries())) {
            county.setMainIndustries(request.getMainIndustries().trim());
        }
        if (StringUtils.hasText(request.getEducationSupport())) {
            county.setEducationSupport(request.getEducationSupport().trim());
        }
        countyRepo.save(county);
        return ResponseEntity.ok(county);
    }

    @GetMapping("/{id}/projects")
    public List<CountyProject> listProjects(@PathVariable Long id) {
        assertCountyExists(id);
        return projectRepo.findByCounty_CountyId(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/projects")
    public ResponseEntity<CountyProject> createProject(@PathVariable Long id,
                                                       @RequestBody ProjectRequest request) {
        PovertyCounty county = countyRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "县不存在"));
        validateProjectRequest(request);
        CountyProject project = new CountyProject();
        applyProjectRequest(project, request);
        project.setCounty(county);
        projectRepo.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/projects/{projectId}")
    public ResponseEntity<CountyProject> updateProject(@PathVariable Long id,
                                                       @PathVariable Long projectId,
                                                       @RequestBody ProjectRequest request) {
        CountyProject project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "项目不存在"));
        if (!Objects.equals(project.getCounty().getCountyId(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "项目不属于当前县");
        }
        validateProjectRequest(request);
        applyProjectRequest(project, request);
        projectRepo.save(project);
        return ResponseEntity.ok(project);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, @PathVariable Long projectId) {
        CountyProject project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "项目不存在"));
        if (!Objects.equals(project.getCounty().getCountyId(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "项目不属于当前县");
        }
        projectRepo.delete(project);
        return ResponseEntity.noContent().build();
    }

    private void validateProjectRequest(ProjectRequest request) {
        if (!StringUtils.hasText(request.getProjectName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "项目名称必填");
        }
        if (request.getProgress() != null && (request.getProgress() < 0 || request.getProgress() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "项目进度需在0-100之间");
        }
    }

    private void applyProjectRequest(CountyProject project, ProjectRequest request) {
        project.setProjectName(request.getProjectName());
        project.setCategory(request.getCategory());
        project.setInvestment(request.getInvestment());
        project.setBeneficiaries(request.getBeneficiaries());
        project.setStatus(request.getStatus());
        project.setProgress(request.getProgress());
        project.setStartYear(request.getStartYear());
        project.setEndYear(request.getEndYear());
        project.setLeadUnit(request.getLeadUnit());
        project.setDescription(request.getDescription());
        project.setImpactScore(request.getImpactScore());
    }

    private Map<String, Object> buildCountySummary(PovertyCounty county,
                                                   EconomicIndicator latest,
                                                   List<CountyProject> projects) {
        Map<String, Object> summary = new HashMap<>();
        Province province = county.getProvince();
        summary.put("countyId", county.getCountyId());
        summary.put("countyName", county.getCountyName());
        summary.put("province", province != null ? province.getProvinceName() : null);
        summary.put("provinceId", province != null ? province.getProvinceId() : null);
        summary.put("povertyLevel", StringUtils.hasText(county.getPovertyLevel())
                ? county.getPovertyLevel()
                : deriveLevel(county));
        summary.put("population", derivePopulation(county));
        summary.put("area", deriveArea(county));
        summary.put("delistingYear", county.getDelistingYear());
        summary.put("delistingDate", county.getDelistingYear() != null ? county.getDelistingYear() + "-12-31" : "持续推进中");
        summary.put("delistedHouseholds", deriveDelistedHouseholds(county));
        summary.put("delistedPopulation", deriveDelistedPopulation(county));
        summary.put("mainIndustries", StringUtils.hasText(county.getMainIndustries())
                ? county.getMainIndustries()
                : pickIndustryFallback(county));
        summary.put("educationSupport", StringUtils.hasText(county.getEducationSupport())
                ? county.getEducationSupport()
                : defaultEducationSupport(county));
        summary.put("supportProjects", projects.size());
        summary.put("industrialProjects",
                projects.stream()
                        .filter(p -> p.getCategory() != null && p.getCategory().contains("产业"))
                        .count());
        summary.put("coveragePopulation", county.getCoveragePopulation() != null
                ? county.getCoveragePopulation()
                : deriveCoveragePopulation(county));
        summary.put("totalInvestment", county.getTotalInvestment() != null
                ? county.getTotalInvestment()
                : round1(projects.stream()
                        .map(CountyProject::getInvestment)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .sum()));

        if (latest != null) {
            summary.put("gdp", latest.getGdp());
            summary.put("incomePerCapita", latest.getRuralDisposableIncome());
            summary.put("gdpPerCapita", latest.getGdpPerCapita());
            summary.put("povertyRate", latest.getPovertyRate());
            summary.put("fiscalRevenue", latest.getFiscalRevenue());
            summary.put("gdpYoy", latest.getGdpYoy());
        }
        return summary;
    }

    private Map<String, Object> buildKpi(EconomicIndicator latest, PovertyCounty county) {
        Map<String, Object> card = new HashMap<>();
        card.put("povertyAlleviationRate", county.getPovertyAlleviationRate() != null
                ? county.getPovertyAlleviationRate()
                : deriveDefaultKpi(county, 88, 98));
        card.put("economicGrowthRate", county.getEconomicGrowthRate() != null
                ? county.getEconomicGrowthRate()
                : deriveDefaultKpi(county, 4.5, 9.5));
        card.put("employmentRate", county.getEmploymentRate() != null
                ? county.getEmploymentRate()
                : deriveDefaultKpi(county, 82, 95));
        if (latest != null) {
            card.put("povertyRate", latest.getPovertyRate());
        }
        return card;
    }

    private Map<String, Object> buildProjectOverview(List<CountyProject> projects, PovertyCounty county) {
        Map<String, Object> overview = new HashMap<>();
        double totalInvest = projects.stream()
                .map(CountyProject::getInvestment)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        int beneficiaries = projects.stream()
                .map(CountyProject::getBeneficiaries)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        overview.put("projectCount", projects.size());
        overview.put("totalInvestment", round1(totalInvest));
        overview.put("totalBeneficiaries", beneficiaries);
        overview.put("coveragePopulation", county.getCoveragePopulation() != null
                ? county.getCoveragePopulation()
                : deriveCoveragePopulation(county));
        return overview;
    }

    private Map<String, Object> mapIndicator(EconomicIndicator indicator) {
        Map<String, Object> row = new HashMap<>();
        row.put("year", indicator.getYear());
        row.put("gdp", indicator.getGdp());
        row.put("povertyRate", indicator.getPovertyRate());
        row.put("ruralDisposableIncome", indicator.getRuralDisposableIncome());
        row.put("gdpPerCapita", indicator.getGdpPerCapita());
        row.put("gdpYoy", indicator.getGdpYoy());
        row.put("fiscalRevenue", indicator.getFiscalRevenue());
        return row;
    }

    private void assertCountyExists(Long id) {
        if (!countyRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "县不存在");
        }
    }

    private double derivePopulation(PovertyCounty county) {
        if (county.getPopulation() != null) {
            return county.getPopulation();
        }
        long seed = seed(county);
        double population = 12 + (seed % 9) * 1.6;
        return round1(population);
    }

    private double deriveArea(PovertyCounty county) {
        if (county.getArea() != null) {
            return county.getArea();
        }
        long seed = seed(county);
        return 1200 + (int) (seed % 7) * 180;
    }

    private long deriveDelistedHouseholds(PovertyCounty county) {
        long seed = seed(county);
        return 2500 + seed * 95;
    }

    private long deriveDelistedPopulation(PovertyCounty county) {
        long seed = seed(county);
        return 9000 + seed * 240;
    }

    private int deriveCoveragePopulation(PovertyCounty county) {
        long seed = seed(county);
        return (int) (12000 + seed * 180);
    }

    private double deriveDefaultKpi(PovertyCounty county, double min, double max) {
        long seed = seed(county);
        double ratio = (seed % 100) / 100.0;
        return round1(min + (max - min) * ratio);
    }

    private String pickIndustryFallback(PovertyCounty county) {
        String[] industries = new String[]{
                "特色农业+乡村旅游", "清洁能源+牧业加工", "茶叶加工+康养服务", "数字乡村+中药材"
        };
        long seed = seed(county);
        return industries[(int) (seed % industries.length)];
    }

    private String defaultEducationSupport(PovertyCounty county) {
        long seed = seed(county);
        return "建成乡村寄宿制学校" + (2 + (seed % 3)) + "所，开通远程课堂";
    }

    private String deriveLevel(PovertyCounty county) {
        String[] levels = {"Ⅰ类重点县", "Ⅱ类巩固县", "Ⅲ类监测县"};
        long seed = seed(county);
        return levels[(int) (seed % levels.length)];
    }

    private long seed(PovertyCounty county) {
        return county.getCountyId() != null ? county.getCountyId() : 1L;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public static class CountyUpdateRequest {
        private String countyName;
        private Long provinceId;
        private Double population;
        private Double area;
        private String povertyLevel;
        private Double povertyAlleviationRate;
        private Double economicGrowthRate;
        private Double employmentRate;
        private Integer coveragePopulation;
        private Double totalInvestment;
        private String mainIndustries;
        private String educationSupport;

        public String getCountyName() {
            return countyName;
        }

        public void setCountyName(String countyName) {
            this.countyName = countyName;
        }

        public Long getProvinceId() {
            return provinceId;
        }

        public void setProvinceId(Long provinceId) {
            this.provinceId = provinceId;
        }

        public Double getPopulation() {
            return population;
        }

        public void setPopulation(Double population) {
            this.population = population;
        }

        public Double getArea() {
            return area;
        }

        public void setArea(Double area) {
            this.area = area;
        }

        public String getPovertyLevel() {
            return povertyLevel;
        }

        public void setPovertyLevel(String povertyLevel) {
            this.povertyLevel = povertyLevel;
        }

        public Double getPovertyAlleviationRate() {
            return povertyAlleviationRate;
        }

        public void setPovertyAlleviationRate(Double povertyAlleviationRate) {
            this.povertyAlleviationRate = povertyAlleviationRate;
        }

        public Double getEconomicGrowthRate() {
            return economicGrowthRate;
        }

        public void setEconomicGrowthRate(Double economicGrowthRate) {
            this.economicGrowthRate = economicGrowthRate;
        }

        public Double getEmploymentRate() {
            return employmentRate;
        }

        public void setEmploymentRate(Double employmentRate) {
            this.employmentRate = employmentRate;
        }

        public Integer getCoveragePopulation() {
            return coveragePopulation;
        }

        public void setCoveragePopulation(Integer coveragePopulation) {
            this.coveragePopulation = coveragePopulation;
        }

        public Double getTotalInvestment() {
            return totalInvestment;
        }

        public void setTotalInvestment(Double totalInvestment) {
            this.totalInvestment = totalInvestment;
        }

        public String getMainIndustries() {
            return mainIndustries;
        }

        public void setMainIndustries(String mainIndustries) {
            this.mainIndustries = mainIndustries;
        }

        public String getEducationSupport() {
            return educationSupport;
        }

        public void setEducationSupport(String educationSupport) {
            this.educationSupport = educationSupport;
        }
    }

    public static class ProjectRequest {
        private String projectName;
        private String category;
        private Double investment;
        private Integer beneficiaries;
        private String status;
        private Integer progress;
        private Integer startYear;
        private Integer endYear;
        private String leadUnit;
        private String description;
        private Double impactScore;

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Double getInvestment() {
            return investment;
        }

        public void setInvestment(Double investment) {
            this.investment = investment;
        }

        public Integer getBeneficiaries() {
            return beneficiaries;
        }

        public void setBeneficiaries(Integer beneficiaries) {
            this.beneficiaries = beneficiaries;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getProgress() {
            return progress;
        }

        public void setProgress(Integer progress) {
            this.progress = progress;
        }

        public Integer getStartYear() {
            return startYear;
        }

        public void setStartYear(Integer startYear) {
            this.startYear = startYear;
        }

        public Integer getEndYear() {
            return endYear;
        }

        public void setEndYear(Integer endYear) {
            this.endYear = endYear;
        }

        public String getLeadUnit() {
            return leadUnit;
        }

        public void setLeadUnit(String leadUnit) {
            this.leadUnit = leadUnit;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getImpactScore() {
            return impactScore;
        }

        public void setImpactScore(Double impactScore) {
            this.impactScore = impactScore;
        }
    }
}