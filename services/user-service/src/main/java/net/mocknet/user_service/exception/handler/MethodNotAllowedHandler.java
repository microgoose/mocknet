package net.mocknet.user_service.exception.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mocknet.user_service.config.MessageResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class MethodNotAllowedHandler {

    private final MessageResolver resolver;

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, Locale locale) {
        log.debug("Недопустимый HTTP-метод: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
        problem.setTitle(resolver.msg("error.method-not-allowed.title", locale));
        problem.setDetail(resolver.msg("error.method-not-allowed.detail", locale));
        return problem;
    }
}
