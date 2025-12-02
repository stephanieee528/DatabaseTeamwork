package com.example.poverty.repository;

import com.example.poverty.model.PovertyCounty;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PovertyCountyRepository extends JpaRepository<PovertyCounty, Long> {
    List<PovertyCounty> findByProvince_ProvinceId(Long provinceId);
    List<PovertyCounty> findByCountyNameContainingIgnoreCase(String keyword);
    List<PovertyCounty> findByCountyNameContainingIgnoreCaseAndProvince_ProvinceId(String keyword, Long provinceId);
}