package com.time.timecalc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;

import com.time.timecalc.dto.ProjectRequest;
import com.time.timecalc.dto.ProjectResponse;
import com.time.timecalc.model.Project;
import com.time.timecalc.model.enums.ProjectStatus;
import com.time.timecalc.model.enums.RiskLevel;
import com.time.timecalc.repository.CodeSnapshotRepository;
import com.time.timecalc.repository.EstimationReportRepository;
import com.time.timecalc.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @Mock
    EstimationReportRepository estimationReportRepository;

    @Mock
    CodeSnapshotRepository codeSnapshotRepository;

    @InjectMocks
    ProjectService projectService;

    @Test
    void createProject_defaultsBranchToMain_andTrims() {
        ProjectRequest req = new ProjectRequest();
        req.setName("N");
        req.setRepoUrl("url");
        req.setBranchName("  "); // empty -> main
        req.setToken("t");

        when(projectRepository.existsByRepoUrlAndBranchName("url", "main")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ProjectResponse res = projectService.createProject(req);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project saved = captor.getValue();
        assertThat(saved.getBranchName()).isEqualTo("main");
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(saved.getCurrentRiskLevel()).isEqualTo(RiskLevel.LOW);

        assertThat(res.getBranchName()).isEqualTo("main");
        assertThat(res.getRepoUrl()).isEqualTo("url");
        assertThat(res.getName()).isEqualTo("N");
    }

    @Test
    void createProject_throwsIfRepoAndBranchExists() {
        ProjectRequest req = new ProjectRequest();
        req.setName("N");
        req.setRepoUrl("url");
        req.setBranchName("dev");
        req.setToken("t");

        when(projectRepository.existsByRepoUrlAndBranchName("url", "dev")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("уже добавлен");
    }

    @Test
    void getProjectById_throwsIfMissing() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void getAllProjects_mapsToResponses() {
        Project p1 = Project.builder().id(UUID.randomUUID()).name("A").repoUrl("u1").branchName("main").build();
        Project p2 = Project.builder().id(UUID.randomUUID()).name("B").repoUrl("u2").branchName("dev").build();
        when(projectRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProjectResponse> res = projectService.getAllProjects();

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getName()).isEqualTo("A");
        assertThat(res.get(1).getRepoUrl()).isEqualTo("u2");
    }

    @Test
    void setActualDuration_setsCompleted_andPersists() {
        UUID id = UUID.randomUUID();
        Project p = Project.builder()
                .id(id)
                .name("N")
                .repoUrl("u")
                .branchName("main")
                .status(ProjectStatus.ACTIVE)
                .currentRiskLevel(RiskLevel.LOW)
                .build();

        when(projectRepository.findById(id)).thenReturn(Optional.of(p));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse res = projectService.setActualDuration(id, 6.0);

        assertThat(res.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(res.getActualDurationMonths()).isEqualTo(6.0);
        verify(projectRepository).save(eq(p));
        assertThat(p.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(p.getActualDurationMonths()).isEqualTo(6.0);
    }

    @Test
    void deleteProject_deletesRelatedDataAndProject() {
        UUID id = UUID.randomUUID();

        projectService.deleteProject(id);

        InOrder order = inOrder(estimationReportRepository, codeSnapshotRepository, projectRepository);
        order.verify(estimationReportRepository).deleteAllBySnapshotProjectId(id);
        order.verify(codeSnapshotRepository).deleteAllByProjectId(id);
        order.verify(projectRepository).deleteById(id);
    }
}

