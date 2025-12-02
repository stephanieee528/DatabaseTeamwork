package com.example.poverty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poverty_county")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PovertyCounty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long countyId;

    @Column(nullable = false, length = 100)
    private String countyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    private Province province;

    private Integer delistingYear;

    private Double population;
    private Double area;
    private String povertyLevel;
    private Double povertyAlleviationRate;
    private Double economicGrowthRate;
    private Double employmentRate;
    private Integer coveragePopulation;
    private Double totalInvestment;

    @Column(length = 255)
    private String mainIndustries;

    @Column(length = 255)
    private String educationSupport;

    // Explicit getters/setters in case Lombok isn't active
    public Long getCountyId() { return this.countyId; }
    public void setCountyId(Long countyId) { this.countyId = countyId; }

    public String getCountyName() { return this.countyName; }
    public void setCountyName(String countyName) { this.countyName = countyName; }

    public Province getProvince() { return this.province; }
    public void setProvince(Province province) { this.province = province; }

    public Integer getDelistingYear() { return this.delistingYear; }
    public void setDelistingYear(Integer delistingYear) { this.delistingYear = delistingYear; }

    public Double getPopulation() { return population; }
    public void setPopulation(Double population) { this.population = population; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public String getPovertyLevel() { return povertyLevel; }
    public void setPovertyLevel(String povertyLevel) { this.povertyLevel = povertyLevel; }

    public Double getPovertyAlleviationRate() { return povertyAlleviationRate; }
    public void setPovertyAlleviationRate(Double povertyAlleviationRate) { this.povertyAlleviationRate = povertyAlleviationRate; }

    public Double getEconomicGrowthRate() { return economicGrowthRate; }
    public void setEconomicGrowthRate(Double economicGrowthRate) { this.economicGrowthRate = economicGrowthRate; }

    public Double getEmploymentRate() { return employmentRate; }
    public void setEmploymentRate(Double employmentRate) { this.employmentRate = employmentRate; }

    public Integer getCoveragePopulation() { return coveragePopulation; }
    public void setCoveragePopulation(Integer coveragePopulation) { this.coveragePopulation = coveragePopulation; }

    public Double getTotalInvestment() { return totalInvestment; }
    public void setTotalInvestment(Double totalInvestment) { this.totalInvestment = totalInvestment; }

    public String getMainIndustries() { return mainIndustries; }
    public void setMainIndustries(String mainIndustries) { this.mainIndustries = mainIndustries; }

    public String getEducationSupport() { return educationSupport; }
    public void setEducationSupport(String educationSupport) { this.educationSupport = educationSupport; }
}