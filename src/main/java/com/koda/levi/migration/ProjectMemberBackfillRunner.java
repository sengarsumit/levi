package com.koda.levi.migration;

import com.koda.levi.entity.Organization;
import com.koda.levi.entity.OrgUserStatus;
import com.koda.levi.entity.OrganizationUser;
import com.koda.levi.entity.Project;
import com.koda.levi.entity.ProjectMember;
import com.koda.levi.repository.OrganizationUserRepository;
import com.koda.levi.repository.ProjectMemberRepository;
import com.koda.levi.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time backfill for the project_members table introduced alongside project-level access
 * gating (ProjectAccessService). Without it, every project that already existed before this
 * deploy would instantly become invisible to every non-admin org member the moment it ships,
 * since project_members starts empty under ddl-auto: update. This preserves today's
 * "everyone in the org sees every project" behavior for pre-existing data by enrolling every
 * approved org member into every project already in that org; projects created after this
 * deploy start properly scoped via ProjectService.create's own auto-add-creator step instead.
 *
 * Guarded to run at most once: it no-ops the moment project_members has any rows at all, so a
 * restart (or a second instance) never re-runs it or duplicates membership.
 *
 * Runs outside any HTTP request, so TenantContext is never set and the tenantFilter Hibernate
 * filter is never enabled here — projectRepository.findAll() genuinely returns every project
 * across every organization, which is required to backfill the whole database in one pass.
 */
@Component
public class ProjectMemberBackfillRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProjectMemberBackfillRunner.class);

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final OrganizationUserRepository organizationUserRepository;

    public ProjectMemberBackfillRunner(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            OrganizationUserRepository organizationUserRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.organizationUserRepository = organizationUserRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (projectMemberRepository.count() > 0) {
            return;
        }

        List<Project> allProjects = projectRepository.findAll();
        if (allProjects.isEmpty()) {
            return;
        }

        int inserted = 0;
        for (Project project : allProjects) {
            Organization org = project.getOrganization();
            List<OrganizationUser> approvedMembers =
                    organizationUserRepository.findByOrganizationIdAndStatusFetchAll(org.getId(), OrgUserStatus.APPROVED);
            for (OrganizationUser ou : approvedMembers) {
                ProjectMember member = new ProjectMember();
                member.setProject(project);
                member.setOrganization(org);
                member.setUserId(ou.getUser().getId());
                projectMemberRepository.save(member);
                inserted++;
            }
        }

        logger.info("Project membership backfill: added {} membership row(s) across {} existing project(s)",
                inserted, allProjects.size());
    }
}
