package com.time.timecalc.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.time.timecalc.model.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
}