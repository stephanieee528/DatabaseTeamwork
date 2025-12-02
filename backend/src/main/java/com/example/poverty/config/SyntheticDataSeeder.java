package com.example.poverty.config;

import com.example.poverty.model.CountyProject;
import com.example.poverty.model.EconomicIndicator;
import com.example.poverty.model.PovertyCounty;
import com.example.poverty.model.Province;
import com.example.poverty.repository.CountyProjectRepository;
import com.example.poverty.repository.EconomicIndicatorRepository;
import com.example.poverty.repository.PovertyCountyRepository;
import com.example.poverty.repository.ProvinceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Generates a reproducible dataset so the UI always sees 832 counties backed by a real database file.
 */
@Component
@Profile("dev")
public class SyntheticDataSeeder implements CommandLineRunner {

    private static final LinkedHashMap<String, Integer> PROVINCE_QUOTAS = new LinkedHashMap<>();
    private static final Set<String> MOUNTAIN_PROVINCES = new HashSet<>();
    private static final List<Integer> YEARS = Arrays.asList(2018, 2019, 2020, 2021, 2022, 2023);
    private static final long TARGET_COUNTIES;
    private static final List<String> POVERTY_LEVELS = Arrays.asList("Ⅰ类重点县", "Ⅱ类巩固县", "Ⅲ类监测县");
    private static final List<String> PROJECT_CATEGORIES = Arrays.asList("产业扶贫", "教育扶智", "基础设施", "生态旅游", "数字乡村");
    private static final List<String> PROJECT_STATUSES = Arrays.asList("进行中", "已完成", "筹备阶段");
    private static final List<String> PROJECT_LEAD_UNITS = Arrays.asList("县乡村振兴局", "农业农村局", "教育局", "交通运输局", "水利局", "文旅局");

    static {
        PROVINCE_QUOTAS.put("云南省", 73);
        PROVINCE_QUOTAS.put("贵州省", 66);
        PROVINCE_QUOTAS.put("四川省", 45);
        PROVINCE_QUOTAS.put("甘肃省", 58);
        PROVINCE_QUOTAS.put("陕西省", 50);
        PROVINCE_QUOTAS.put("河北省", 45);
        PROVINCE_QUOTAS.put("山西省", 36);
        PROVINCE_QUOTAS.put("内蒙古自治区", 31);
        PROVINCE_QUOTAS.put("辽宁省", 15);
        PROVINCE_QUOTAS.put("吉林省", 8);
        PROVINCE_QUOTAS.put("黑龙江省", 14);
        PROVINCE_QUOTAS.put("安徽省", 20);
        PROVINCE_QUOTAS.put("江西省", 24);
        PROVINCE_QUOTAS.put("河南省", 38);
        PROVINCE_QUOTAS.put("湖北省", 28);
        PROVINCE_QUOTAS.put("湖南省", 40);
        PROVINCE_QUOTAS.put("广西壮族自治区", 54);
        PROVINCE_QUOTAS.put("海南省", 5);
        PROVINCE_QUOTAS.put("重庆市", 14);
        PROVINCE_QUOTAS.put("青海省", 42);
        PROVINCE_QUOTAS.put("宁夏回族自治区", 8);
        PROVINCE_QUOTAS.put("新疆维吾尔自治区", 35);
        PROVINCE_QUOTAS.put("西藏自治区", 74);

        MOUNTAIN_PROVINCES.addAll(Arrays.asList(
                "云南省", "贵州省", "四川省", "甘肃省", "陕西省",
                "青海省", "宁夏回族自治区", "新疆维吾尔自治区", "西藏自治区"
        ));

        TARGET_COUNTIES = PROVINCE_QUOTAS.values().stream().mapToLong(Integer::longValue).sum();
    }

    private final ProvinceRepository provinceRepository;
    private final PovertyCountyRepository countyRepository;
    private final EconomicIndicatorRepository indicatorRepository;
    private final CountyProjectRepository projectRepository;

    public SyntheticDataSeeder(ProvinceRepository provinceRepository,
                               PovertyCountyRepository countyRepository,
                               EconomicIndicatorRepository indicatorRepository,
                               CountyProjectRepository projectRepository) {
        this.provinceRepository = provinceRepository;
        this.countyRepository = countyRepository;
        this.indicatorRepository = indicatorRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public void run(String... args) {
        if (countyRepository.count() >= TARGET_COUNTIES) {
            return;
        }

        indicatorRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        countyRepository.deleteAllInBatch();
        provinceRepository.deleteAllInBatch();

        Map<String, Province> savedProvinces = new LinkedHashMap<>();
        PROVINCE_QUOTAS.forEach((provinceName, quota) -> {
            Province province = new Province();
            province.setProvinceName(provinceName);
            savedProvinces.put(provinceName, provinceRepository.save(province));
        });

        Random random = new Random(20240222L);
        List<EconomicIndicator> indicatorsToSave = new ArrayList<>();
        List<CountyProject> projectsToSave = new ArrayList<>();

        PROVINCE_QUOTAS.forEach((provinceName, quota) -> {
            Province province = savedProvinces.get(provinceName);
            boolean mountainous = MOUNTAIN_PROVINCES.contains(provinceName);

            for (int index = 1; index <= quota; index++) {
                PovertyCounty county = new PovertyCounty();
                county.setCountyName(provinceName + "示例县" + String.format("%03d", index));
                county.setProvince(province);
                county.setDelistingYear(randomDelistingYear(random));
                county.setPopulation(round1(mountainous ? randomBetween(random, 8, 22) : randomBetween(random, 20, 48)));
                county.setArea(round1(mountainous ? randomBetween(random, 1500, 4800) : randomBetween(random, 900, 2600)));
                county.setPovertyLevel(POVERTY_LEVELS.get(random.nextInt(POVERTY_LEVELS.size())));
                county.setPovertyAlleviationRate(round1(randomBetween(random, 86, 98)));
                county.setEconomicGrowthRate(round1(randomBetween(random, 4.0, 10.5)));
                county.setEmploymentRate(round1(randomBetween(random, 82, 96)));
                county.setCoveragePopulation((int) Math.round(randomBetween(random, 1.1, 4.8) * 10000));
                county.setMainIndustries(pickMainIndustry(mountainous, random));
                county = countyRepository.save(county);

                List<CountyProject> countyProjects = buildProjectsForCounty(county, random);
                double totalProjectInvestment = countyProjects.stream()
                        .map(CountyProject::getInvestment)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .sum();
                county.setTotalInvestment(round1(totalProjectInvestment > 0 ? totalProjectInvestment : randomBetween(random, 10, 55)));
                countyRepository.save(county);
                projectsToSave.addAll(countyProjects);

                double baseGdp = mountainous ? randomBetween(random, 12, 55) : randomBetween(random, 30, 120);
                double baseIncome = mountainous ? randomBetween(random, 7000, 9500) : randomBetween(random, 9000, 14000);
                double basePerCapita = mountainous ? randomBetween(random, 16000, 30000) : randomBetween(random, 28000, 52000);
                double baseFiscal = mountainous ? randomBetween(random, 2.5, 8.0) : randomBetween(random, 4.0, 18.0);
                double povertyRate = mountainous ? randomBetween(random, 7.0, 14.0) : randomBetween(random, 3.0, 8.0);

                double lastGdp = baseGdp;
                double lastIncome = baseIncome;
                double lastPerCapita = basePerCapita;
                double lastFiscal = baseFiscal;

                for (int year : YEARS) {
                    double gdpYoy = year == 2020 ? randomBetween(random, -0.03, 0.035) : randomBetween(random, 0.025, 0.095);
                    double incomeYoy = year == 2020 ? randomBetween(random, -0.02, 0.05) : randomBetween(random, 0.03, 0.11);
                    double fiscalYoy = year == 2020 ? randomBetween(random, -0.04, 0.04) : randomBetween(random, 0.02, 0.10);

                    double gdpValue = year == YEARS.get(0) ? round1(lastGdp) : round1(lastGdp * (1 + gdpYoy));
                    double incomeValue = year == YEARS.get(0) ? round0(lastIncome) : round0(lastIncome * (1 + incomeYoy));
                    double perCapitaValue = year == YEARS.get(0)
                            ? round0(lastPerCapita)
                            : round0(lastPerCapita * (1 + randomBetween(random, 0.015, 0.05)));
                    double fiscalValue = year == YEARS.get(0)
                            ? round2(lastFiscal)
                            : round2(lastFiscal * (1 + fiscalYoy));

                    lastGdp = gdpValue;
                    lastIncome = incomeValue;
                    lastPerCapita = perCapitaValue;
                    lastFiscal = fiscalValue;
                    povertyRate = Math.max(povertyRate - randomBetween(random, 0.4, 1.5), 0.5);

                    EconomicIndicator indicator = new EconomicIndicator();
                    indicator.setCounty(county);
                    indicator.setYear(year);
                    indicator.setGdp(gdpValue);
                    indicator.setGdpYoy(gdpYoy);
                    indicator.setGdpPerCapita((double) perCapitaValue);
                    indicator.setRuralDisposableIncome((double) incomeValue);
                    indicator.setRuralIncomeYoy(incomeYoy);
                    indicator.setFiscalRevenue(fiscalValue);
                    indicator.setFiscalRevenueYoy(fiscalYoy);
                    indicator.setPovertyRate(povertyRate);
                    indicatorsToSave.add(indicator);
                }
            }
        });

        indicatorRepository.saveAll(indicatorsToSave);
        projectRepository.saveAll(projectsToSave);
    }

    private static Integer randomDelistingYear(Random random) {
        Integer[] candidates = {2018, 2019, 2020, 2021, null, null};
        return candidates[random.nextInt(candidates.length)];
    }

    private static String pickMainIndustry(boolean mountainous, Random random) {
        String[] mountainIndustries = {"高原特色农业+乡村旅游", "清洁能源+生态畜牧", "中药材种植+康养服务", "水能开发+生态旅游"};
        String[] plainIndustries = {"现代农业+加工物流", "装备制造+新能源", "数字经济+文旅融合", "绿色食品+康养休闲"};
        String[] source = mountainous ? mountainIndustries : plainIndustries;
        return source[random.nextInt(source.length)];
    }

    private static List<CountyProject> buildProjectsForCounty(PovertyCounty county, Random random) {
        int projectCount = 2 + random.nextInt(4);
        List<CountyProject> projects = new ArrayList<>();
        for (int i = 1; i <= projectCount; i++) {
            String category = PROJECT_CATEGORIES.get(random.nextInt(PROJECT_CATEGORIES.size()));
            String status = PROJECT_STATUSES.get(random.nextInt(PROJECT_STATUSES.size()));
            int startYear = 2017 + random.nextInt(6);
            int duration = 1 + random.nextInt(3);
            int endYear = Math.min(startYear + duration, 2024);
            double investment = round1(randomBetween(random, 0.8, 5.8));
            int beneficiaries = (int) Math.round(randomBetween(random, 800, 6800));
            int progress = status.equals("已完成") ? 100 : Math.max(20, Math.min(98, (int) Math.round(randomBetween(random, 45, 95))));

            CountyProject project = new CountyProject();
            project.setCounty(county);
            project.setProjectName(county.getCountyName() + category + "项目" + String.format("%02d", i));
            project.setCategory(category);
            project.setInvestment(investment);
            project.setBeneficiaries(beneficiaries);
            project.setStatus(status);
            project.setProgress(progress);
            project.setStartYear(startYear);
            project.setEndYear(endYear);
            project.setLeadUnit(PROJECT_LEAD_UNITS.get(random.nextInt(PROJECT_LEAD_UNITS.size())));
            project.setDescription(String.format("围绕%s推进精准帮扶，打造县域示范样板", category));
            project.setImpactScore(round2(randomBetween(random, 0.6, 0.95)));
            projects.add(project);
        }
        return projects;
    }

    private static double randomBetween(Random random, double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    private static double round0(double value) {
        return Math.round(value);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

