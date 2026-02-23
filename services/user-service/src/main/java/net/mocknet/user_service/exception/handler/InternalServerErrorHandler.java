package net.mocknet.user_service.exception.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mocknet.user_service.config.MessageResolver;
import net.mocknet.user_service.exception.base.InternalException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Slf4j
@Order
@RestControllerAdvice
@RequiredArgsConstructor
public class InternalServerErrorHandler {

    private final MessageResolver resolver;

    @ExceptionHandler(InternalException.class)
    public ProblemDetail handleInternal(InternalException ex, Locale locale) {
        log.error("Внутренняя ошибка", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle(resolver.msg("error.internal.title", locale));
        problem.setDetail(resolver.msg("error.internal.detail", locale));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, Locale locale) {
        log.error("Непредвиденная ошибка", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle(resolver.msg("error.internal.title", locale));
        problem.setDetail(resolver.msg("error.internal.detail", locale));
        return problem;
    }
}
