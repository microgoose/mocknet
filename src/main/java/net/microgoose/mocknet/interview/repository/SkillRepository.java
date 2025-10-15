package net.microgoose.mocknet.interview.repository;

import net.microgoose.mocknet.interview.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    boolean existsByName(String name);
}
