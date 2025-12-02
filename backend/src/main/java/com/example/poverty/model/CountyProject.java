package com.example.poverty.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "county_project")
public class CountyProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "county_id")
    @JsonIgnore
    private PovertyCounty county;

    @Column(nullable = false, length = 150)
    private String projectName;

    @Column(length = 50)
    private String category;

    private Double investment;

    private Integer beneficiaries;

    @Column(length = 30)
    private String status;

    private Integer progress;

    private Integer startYear;

    private Integer endYear;

    @Column(length = 120)
    private String leadUnit;

    @Column(length = 500)
    private String description;

    private Double impactScore;

    public CountyProject() {
    }

    public CountyProject(Long projectId,
                         PovertyCounty county,
                         String projectName,
                         String category,
                         Double investment,
                         Integer beneficiaries,
                         String status,
                         Integer progress,
                         Integer startYear,
                         Integer endYear,
                         String leadUnit,
                         String description,
                         Double impactScore) {
        this.projectId = projectId;
        this.county = county;
        this.projectName = projectName;
        this.category = category;
        this.investment = investment;
        this.beneficiaries = beneficiaries;
        this.status = status;
        this.progress = progress;
        this.startYear = startYear;
        this.endYear = endYear;
        this.leadUnit = leadUnit;
        this.description = description;
        this.impactScore = impactScore;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public PovertyCounty getCounty() {
        return county;
    }

    public void setCounty(PovertyCounty county) {
        this.county = county;
    }

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

