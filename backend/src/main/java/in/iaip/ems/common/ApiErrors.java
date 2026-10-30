package in.iaip.ems.common;

import java.util.LinkedHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ProblemDetail forbidden(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN, "You do not have permission for this action.");
  }

  @ExceptionHandler(ApiException.class)
  ProblemDetail business(ApiException e) {
    return ProblemDetail.forStatusAndDetail(e.status, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(MethodArgumentNotValidException e) {
    var fields = new LinkedHashMap<String, String>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Please correct the highlighted fields.");
    problem.setProperty("errors", fields);
    return problem;
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    HandlerMethodValidationException.class
  })
  ProblemDetail malformed(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Invalid request value or format.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail duplicate(DataIntegrityViolationException e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "An email or employee code already exists, or a referenced record is invalid.");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception e) {
    // Deliberately do not log request bodies or database exception text containing personal data.
    System.err.println("Request failed: " + e.getClass().getSimpleName());
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the request. Please try again.");
  }
}
