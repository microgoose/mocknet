package net.mocknet.user_service.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.mocknet.user_service.config.MessageResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class UnprocessableEntityHandler {

    private final MessageResolver resolver;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, Locale locale) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : "Недопустимое значение",
                (first, second) -> first
            ));

        log.debug("Ошибка валидации тела запроса: {}", errors);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setTitle(resolver.msg("error.validation.title", locale));
        problem.setDetail(resolver.msg("error.validation.body.detail", locale));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, Locale locale) {
        Map<String, String> errors = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> {
                    String path = violation.getPropertyPath().toString();
                    int dotIndex = path.lastIndexOf('.');
                    return dotIndex >= 0 ? path.substring(dotIndex + 1) : path;
                },
                ConstraintViolation::getMessage,
                (first, second) -> first
            ));

        log.debug("Ошибка валидации параметров: {}", errors);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setTitle(resolver.msg("error.validation.title", locale));
        problem.setDetail(resolver.msg("error.validation.params.detail", locale));
        problem.setProperty("errors", errors);
        return problem;
    }
}
