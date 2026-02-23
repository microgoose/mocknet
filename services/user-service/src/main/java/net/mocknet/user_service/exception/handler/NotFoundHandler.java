package net.mocknet.user_service.exception.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mocknet.user_service.config.MessageResolver;
import net.mocknet.user_service.exception.base.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class NotFoundHandler {

    private final MessageResolver resolver;

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex, Locale locale) {
        log.warn("Ресурс не найден: {}", ex.getMessageKey());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle(resolver.msg("error.not-found.title", locale));
        problem.setDetail(resolver.msg(ex, locale));
        return problem;
    }
}
