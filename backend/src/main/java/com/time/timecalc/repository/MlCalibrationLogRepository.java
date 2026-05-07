package com.time.timecalc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.time.timecalc.model.MlCalibrationLog;

@Repository
public interface MlCalibrationLogRepository extends JpaRepository<MlCalibrationLog, Long> {
    
    Optional<MlCalibrationLog> findFirstByOrderByPerformedAtDesc();
}