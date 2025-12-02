package com.example.poverty.repository;

import com.example.poverty.model.CountyProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CountyProjectRepository extends JpaRepository<CountyProject, Long> {
    List<CountyProject> findByCounty_CountyId(Long countyId);
}



