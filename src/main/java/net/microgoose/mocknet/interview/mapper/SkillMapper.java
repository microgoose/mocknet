package net.microgoose.mocknet.interview.mapper;

import lombok.RequiredArgsConstructor;
import net.microgoose.mocknet.interview.dto.skill.SkillDto;
import net.microgoose.mocknet.interview.model.Skill;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SkillMapper {

    public SkillDto toDto(Skill skill) {
        return SkillDto.builder()
            .uuid(skill.getId())
            .name(skill.getName())
            .build();
    }

    public Set<SkillDto> toDto(Set<Skill> items) {
        return items.stream().map(this::toDto).collect(Collectors.toSet());
    }
}
