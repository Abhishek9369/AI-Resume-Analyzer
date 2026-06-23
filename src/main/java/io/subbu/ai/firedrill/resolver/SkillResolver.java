package io.subbu.ai.firedrill.resolver;

import io.subbu.ai.firedrill.entities.Skill;
import io.subbu.ai.firedrill.repos.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

/**
 * GraphQL resolver for Skill queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SkillResolver {

    private final SkillRepository skillRepository;

    @QueryMapping
    public Skill skill(@Argument UUID id) {
        log.info("Fetching skill: {}", id);
        return skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + id));
    }

    @QueryMapping
    public List<Skill> allSkills() {
        log.info("Fetching all skills");
        return skillRepository.findAll();
    }

    @QueryMapping
    public List<Skill> activeSkills() {
        log.info("Fetching active skills");
        return skillRepository.findByIsActive(true);
    }

    @QueryMapping
    public List<Skill> searchSkills(@Argument String name) {
        log.info("Searching skills by name: {}", name);
        if (name == null || name.trim().isEmpty()) {
            return skillRepository.findByIsActive(true);
        }
        return skillRepository.searchByName(name.trim());
    }

    @QueryMapping
    public List<Skill> skillsByCategory(@Argument String category) {
        log.info("Fetching skills by category: {}", category);
        return skillRepository.findByCategory(category);
    }

    @QueryMapping
    public List<String> skillCategories() {
        log.info("Fetching all skill categories");
        return skillRepository.findAllCategories();
    }

    @MutationMapping
    public Skill createSkill(
            @Argument String name,
            @Argument String category,
            @Argument String description) {
        
        log.info("Creating skill: {}", name);
        
        // Check if skill already exists
        skillRepository.findByNameIgnoreCase(name).ifPresent(s -> {
            throw new RuntimeException("Skill already exists: " + name);
        });
        
        Skill skill = Skill.builder()
                .name(name)
                .category(category)
                .description(description)
                .isActive(true)
                .build();

        return skillRepository.save(skill);
    }

    @MutationMapping
    public Skill updateSkill(
            @Argument UUID id,
            @Argument String name,
            @Argument String category,
            @Argument String description,
            @Argument Boolean isActive) {
        
        log.info("Updating skill: {}", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + id));

        if (name != null && !name.equals(skill.getName())) {
            // Check if new name conflicts with existing skill
            skillRepository.findByNameIgnoreCase(name).ifPresent(s -> {
                if (!s.getId().equals(id)) {
                    throw new RuntimeException("Skill name already exists: " + name);
                }
            });
            skill.setName(name);
        }
        if (category != null) skill.setCategory(category);
        if (description != null) skill.setDescription(description);
        if (isActive != null) skill.setIsActive(isActive);

        return skillRepository.save(skill);
    }

    @MutationMapping
    public Boolean deleteSkill(@Argument UUID id) {
        log.info("Deleting skill: {}", id);
        skillRepository.deleteById(id);
        return true;
    }
}
