package net.mocknet.user_service.exception.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mocknet.user_service.config.MessageResolver;
import net.mocknet.user_service.exception.base.HttpGoneException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GoneHandler {

    private final MessageResolver resolver;

    @ExceptionHandler(HttpGoneException.class)
    public ProblemDetail handleGone(HttpGoneException ex, Locale locale) {
        log.debug("Ресурс больше недоступен: {}", ex.getMessageKey());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.GONE);
        problem.setTitle(resolver.msg("error.gone.title", locale));
        problem.setDetail(resolver.msg(ex, locale));
        return problem;
    }
}
