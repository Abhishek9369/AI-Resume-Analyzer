package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.repos.CandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

/**
 * GraphQL resolver for Candidate queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CandidateResolver {

    private final CandidateRepository candidateRepository;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER', 'HR')")
    public Candidate candidate(@Argument UUID id) {
        log.info("Fetching candidate: {}", id);
        return candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found: " + id));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER', 'HR')")
    public List<Candidate> allCandidates() {
        log.info("Fetching all candidates");
        return candidateRepository.findAll();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER', 'HR')")
    public List<Candidate> searchCandidatesByName(@Argument String name) {
        log.info("Searching candidates by name: {}", name);
        return candidateRepository.searchByName(name);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER', 'HIRING_MANAGER', 'HR')")
    public List<Candidate> searchCandidatesBySkill(@Argument String skill) {
        log.info("Searching candidates by skill: {}", skill);
        return candidateRepository.findBySkillsContaining(skill);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER')")
    public Candidate updateCandidate(
            @Argument UUID id,
            @Argument String name,
            @Argument String email,
            @Argument String mobile,
            @Argument String skills,
            @Argument Integer experience,
            @Argument String education,
            @Argument String currentCompany) {
        
        log.info("Updating candidate: {}", id);
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found: " + id));

        if (name != null) candidate.setName(name);
        if (email != null) candidate.setEmail(email);
        if (mobile != null) candidate.setMobile(mobile);
        if (skills != null) candidate.setSkills(skills);
        if (experience != null) candidate.setExperience(experience);
        if (education != null) candidate.setEducation(education);
        if (currentCompany != null) candidate.setCurrentCompany(currentCompany);

        return candidateRepository.save(candidate);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteCandidate(@Argument UUID id) {
        log.info("Deleting candidate: {}", id);
        candidateRepository.deleteById(id);
        return true;
    }
}
