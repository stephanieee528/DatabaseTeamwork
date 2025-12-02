package com.example.poverty.repository;

import com.example.poverty.model.AlertEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {
    List<AlertEvent> findByAcknowledgedByIsNull();

    @EntityGraph(attributePaths = {"rule", "county", "county.province"})
    List<AlertEvent> findAll();
}