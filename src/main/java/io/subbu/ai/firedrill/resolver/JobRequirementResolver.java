package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.JobRequirement;
import io.subbu.ai.firedrill.entities.Skill;
import io.subbu.ai.firedrill.repos.JobRequirementRepository;
import io.subbu.ai.firedrill.repos.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * GraphQL resolver for JobRequirement queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class JobRequirementResolver {

    private final JobRequirementRepository jobRepository;
    private final SkillRepository skillRepository;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public JobRequirement jobRequirement(@Argument UUID id) {
        log.info("Fetching job requirement: {}", id);
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job requirement not found: " + id));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public List<JobRequirement> allJobRequirements() {
        log.info("Fetching all job requirements");
        return jobRepository.findAll();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER')")
    public List<JobRequirement> activeJobRequirements() {
        log.info("Fetching active job requirements");
        return jobRepository.findByIsActive(true);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public JobRequirement createJobRequirement(
            @Argument String title,
            @Argument String requiredSkills,
            @Argument List<UUID> skillIds,
            @Argument Integer minExperience,
            @Argument Integer maxExperience,
            @Argument String requiredEducation,
            @Argument String domain,
            @Argument String description) {
        
        log.info("Creating job requirement: {}", title);
        
        JobRequirement job = JobRequirement.builder()
                .title(title)
                .requiredSkills(requiredSkills)
                .minExperience(minExperience)
                .maxExperience(maxExperience)
                .minExperienceYears(minExperience)
                .maxExperienceYears(maxExperience)
                .requiredEducation(requiredEducation)
                .domain(domain)
                .domainRequirements(domain)
                .description(description)
                .isActive(true)
                .build();

        // Add skills if provided
        if (skillIds != null && !skillIds.isEmpty()) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(skillIds));
            job.setSkills(skills);
            // If requiredSkills text is empty, derive it from the selected skill names
            // so the AI matching engine has proper text criteria to work with
            if (job.getRequiredSkills() == null || job.getRequiredSkills().isBlank()) {
                String derivedSkills = skills.stream()
                        .map(Skill::getName)
                        .sorted()
                        .collect(java.util.stream.Collectors.joining(", "));
                job.setRequiredSkills(derivedSkills);
            }
        }

        return jobRepository.save(job);
    }

    @MutationMapping
    public JobRequirement updateJobRequirement(
            @Argument UUID id,
            @Argument String title,
            @Argument String requiredSkills,
            @Argument List<UUID> skillIds,
            @Argument Integer minExperience,
            @Argument Integer maxExperience,
            @Argument String requiredEducation,
            @Argument String domain,
            @Argument String description,
            @Argument Boolean isActive) {
        
        log.info("Updating job requirement: {}", id);
        JobRequirement job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job requirement not found: " + id));

        if (title != null) job.setTitle(title);
        if (requiredSkills != null) job.setRequiredSkills(requiredSkills);
        if (minExperience != null) {
            job.setMinExperience(minExperience);
            job.setMinExperienceYears(minExperience);
        }
        if (maxExperience != null) {
            job.setMaxExperience(maxExperience);
            job.setMaxExperienceYears(maxExperience);
        }
        if (requiredEducation != null) job.setRequiredEducation(requiredEducation);
        if (domain != null) {
            job.setDomain(domain);
            job.setDomainRequirements(domain);
        }
        if (description != null) job.setDescription(description);
        if (isActive != null) job.setIsActive(isActive);

        // Update skills if provided
        if (skillIds != null) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(skillIds));
            job.setSkills(skills);
            // Re-derive requiredSkills text from badge skills when they change
            if (!skills.isEmpty() && (job.getRequiredSkills() == null || job.getRequiredSkills().isBlank())) {
                String derivedSkills = skills.stream()
                        .map(Skill::getName)
                        .sorted()
                        .collect(java.util.stream.Collectors.joining(", "));
                job.setRequiredSkills(derivedSkills);
            }
        }

        return jobRepository.save(job);
    }

    @MutationMapping
    public Boolean deleteJobRequirement(@Argument UUID id) {
        log.info("Deleting job requirement: {}", id);
        jobRepository.deleteById(id);
        return true;
    }
}
