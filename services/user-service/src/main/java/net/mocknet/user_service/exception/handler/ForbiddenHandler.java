package net.mocknet.user_service.exception.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mocknet.user_service.config.MessageResolver;
import net.mocknet.user_service.exception.base.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ForbiddenHandler {

    private final MessageResolver resolver;

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, Locale locale) {
        log.warn("Доступ запрещён: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle(resolver.msg("error.forbidden.title", locale));
        problem.setDetail(resolver.msg("error.forbidden.detail", locale));
        return problem;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex, Locale locale) {
        log.warn("Доступ запрещён: {}", ex.getMessageKey());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle(resolver.msg("error.forbidden.title", locale));
        problem.setDetail(resolver.msg(ex, locale));
        return problem;
    }
}
