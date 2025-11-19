package com.example.poverty.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "economic_indicator")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EconomicIndicator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "county_id")
    private PovertyCounty county;

    private Double gdp;
    private Double gdpYoy;
    private Double gdpPerCapita;
    private Double ruralDisposableIncome;
    private Double ruralIncomeYoy;
    private Double fiscalRevenue;
    private Double fiscalRevenueYoy;
    private Double povertyRate;

    // Explicit getters/setters in case Lombok is not active
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public Integer getYear() { return this.year; }
    public void setYear(Integer year) { this.year = year; }

    public PovertyCounty getCounty() { return this.county; }
    public void setCounty(PovertyCounty county) { this.county = county; }

    public Double getGdp() { return this.gdp; }
    public void setGdp(Double gdp) { this.gdp = gdp; }

    public Double getGdpYoy() { return this.gdpYoy; }
    public void setGdpYoy(Double gdpYoy) { this.gdpYoy = gdpYoy; }

    public Double getGdpPerCapita() { return this.gdpPerCapita; }
    public void setGdpPerCapita(Double gdpPerCapita) { this.gdpPerCapita = gdpPerCapita; }

    public Double getRuralDisposableIncome() { return this.ruralDisposableIncome; }
    public void setRuralDisposableIncome(Double ruralDisposableIncome) { this.ruralDisposableIncome = ruralDisposableIncome; }

    public Double getRuralIncomeYoy() { return this.ruralIncomeYoy; }
    public void setRuralIncomeYoy(Double ruralIncomeYoy) { this.ruralIncomeYoy = ruralIncomeYoy; }

    public Double getFiscalRevenue() { return this.fiscalRevenue; }
    public void setFiscalRevenue(Double fiscalRevenue) { this.fiscalRevenue = fiscalRevenue; }

    public Double getFiscalRevenueYoy() { return this.fiscalRevenueYoy; }
    public void setFiscalRevenueYoy(Double fiscalRevenueYoy) { this.fiscalRevenueYoy = fiscalRevenueYoy; }

    public Double getPovertyRate() {
        return this.povertyRate;
    }

    public void setPovertyRate(Double povertyRate) {
        this.povertyRate = povertyRate;
    }
}