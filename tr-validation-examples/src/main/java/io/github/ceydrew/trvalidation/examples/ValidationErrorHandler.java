package io.github.ceydrew.trvalidation.examples;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Turns validation failures into RFC 9457 problem responses listing each field and its message.
 * Messages follow the request's {@code Accept-Language} (English or Turkish).
 *
 * <p>The rejected values are deliberately left out: echoing an invalid TCKN or IBAN back would leak
 * personal data into responses and client logs.
 */
@RestControllerAdvice
class ValidationErrorHandler {

  /** One invalid field or parameter. */
  record FieldViolation(String field, String message) {}

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail invalidBody(MethodArgumentNotValidException e) {
    return problem(
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
            .toList());
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ProblemDetail invalidParameters(HandlerMethodValidationException e) {
    return problem(
        e.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                new FieldViolation(
                                    result.getMethodParameter().getParameterName(),
                                    error.getDefaultMessage())))
            .toList());
  }

  private static ProblemDetail problem(List<FieldViolation> violations) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Validation failed");
    problem.setProperty(
        "errors",
        violations.stream()
            .sorted(
                Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
            .toList());
    return problem;
  }
}
