package net.microgoose.mocknet.interview.mapper;

import lombok.RequiredArgsConstructor;
import net.microgoose.mocknet.interview.dto.interview_user.InterviewUserDto;
import net.microgoose.mocknet.interview.model.InterviewUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InterviewUserMapper {
    public InterviewUserDto toDto(InterviewUser user) {
        return InterviewUserDto.builder()
            .uuid(user.getId())
            .username(user.getUsername())
            .avatarUrl(user.getAvatarUrl())
            .build();
    }

    public List<InterviewUserDto> toDto(Set<InterviewUser> items) {
        return items.stream().map(this::toDto).collect(Collectors.toList());
    }
}
