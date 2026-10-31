package com.shopflow.starter.web;

import com.shopflow.kernel.error.BusinessException;
import com.shopflow.kernel.error.ErrorKind;
import com.shopflow.starter.correlation.Correlation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Transforme toutes les erreurs en réponses Problem Details (RFC 9457) de même forme dans tous les services :
 * {@code type}, {@code title}, {@code status}, {@code detail}, {@code instance}, plus {@code code} (stable,
 * traduit par le frontend), {@code correlationId} (pour retrouver la trace dans les logs) et, si besoin,
 * {@code errors[]} (champs invalides).
 * <p>
 * Hérite du gestionnaire standard de Spring, qui couvre déjà les erreurs techniques courantes (405, 415…).
 */
@Slf4j
@RestControllerAdvice
public class ProblemDetailsExceptionHandler extends ResponseEntityExceptionHandler {

    static final String TYPE_BASE = "https://shopflow.dev/errors/";
    static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    static final String MISSING_HEADER = "MISSING_HEADER";
    static final String INVALID_PARAMETER = "INVALID_PARAMETER";
    static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final String CODE = "code";
    private static final String CORRELATION_ID = "correlationId";
    private static final String ERRORS = "errors";

    /** Erreur métier : statut selon sa nature, avec son code et, le cas échéant, le champ en cause. */
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> handleBusinessException(final BusinessException exception, final WebRequest request) {
        HttpStatus status = statusOf(exception.getKind());
        ProblemDetail problem = problem(status, exception.getCode(), exception.getMessage());
        if (exception.getField() != null) {
            problem.setProperty(ERRORS, List.of(fieldError(exception.getField(), exception.getCode(),
                    exception.getMessage())));
        }
        return handleExceptionInternal(exception, problem, new HttpHeaders(), status, request);
    }

    /** Erreur imprévue : 500 avec un message générique ; le détail technique ne va que dans les logs. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpectedException(final Exception exception, final WebRequest request) {
        log.error("Unexpected error while handling {}", path(request), exception);
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR,
                "An unexpected error occurred. Please quote the correlationId when contacting support.");
        return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR,
                request);
    }

    /** Champs invalides (Bean Validation, {@code @Valid}) : la liste des champs dans {@code errors[]}. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException exception,
                                                                  final HttpHeaders headers,
                                                                  final HttpStatusCode status,
                                                                  final WebRequest request) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_FAILED,
                "The request contains " + fieldErrors.size() + " invalid field(s).");
        problem.setProperty(ERRORS, fieldErrors.stream()
                .map(error -> fieldError(error.getField(), toUpperSnakeCase(error.getCode()), error.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** En-tête obligatoire absent (ex. {@code X-Seller-Id}). */
    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(final ServletRequestBindingException exception,
                                                                          final HttpHeaders headers,
                                                                          final HttpStatusCode status,
                                                                          final WebRequest request) {
        if (exception instanceof MissingRequestHeaderException missingHeader) {
            ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, MISSING_HEADER,
                    "Required header '" + missingHeader.getHeaderName() + "' is missing.");
            return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
        }
        return super.handleServletRequestBindingException(exception, headers, status, request);
    }

    /** Paramètre mal formé (ex. un identifiant qui n'est pas un UUID). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(final TypeMismatchException exception,
                                                        final HttpHeaders headers,
                                                        final HttpStatusCode status,
                                                        final WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, INVALID_PARAMETER,
                "Parameter '" + exception.getPropertyName() + "' has an invalid value: '" + exception.getValue() + "'.");
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Corps JSON illisible. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(final HttpMessageNotReadableException exception,
                                                                  final HttpHeaders headers,
                                                                  final HttpStatusCode status,
                                                                  final WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, MALFORMED_REQUEST, "The request body is not valid JSON.");
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Passage obligé de toutes les réponses d'erreur : complète chaque Problem Details (code par défaut, type,
     * titre, instance, correlationId), y compris ceux construits par Spring pour les erreurs techniques.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(final Exception exception, final Object body,
                                                             final HttpHeaders headers,
                                                             final HttpStatusCode status,
                                                             final WebRequest request) {
        Object problemBody = body;
        // Erreurs techniques de Spring : leur Problem Details n'est construit qu'ici, à partir de l'exception
        if (problemBody == null && exception instanceof ErrorResponse errorResponse) {
            problemBody = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }
        if (problemBody instanceof ProblemDetail problem) {
            complete(problem, status, request);
        }
        return super.handleExceptionInternal(exception, problemBody, headers, status, request);
    }

    private static void complete(final ProblemDetail problem, final HttpStatusCode status, final WebRequest request) {
        Map<String, Object> properties = problem.getProperties();
        String code = properties != null && properties.get(CODE) instanceof String existing
                ? existing
                : defaultCode(status);
        problem.setProperty(CODE, code);
        problem.setType(URI.create(TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(toTitle(code));
        if (problem.getInstance() == null) {
            problem.setInstance(URI.create(path(request)));
        }
        problem.setProperty(CORRELATION_ID, Correlation.current().orElse(null));
    }

    private static ProblemDetail problem(final HttpStatus status, final String code, final String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        return problem;
    }

    private static Map<String, String> fieldError(final String field, final String code, final String message) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("field", field);
        error.put(CODE, code);
        error.put("message", message);
        return error;
    }

    private static HttpStatus statusOf(final ErrorKind kind) {
        return switch (kind) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
    }

    /** Code d'une erreur technique gérée par Spring : le nom du statut HTTP (ex. {@code METHOD_NOT_ALLOWED}). */
    private static String defaultCode(final HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.name() : "HTTP_" + status.value();
    }

    /** {@code PRODUCT_NOT_FOUND} → « Product not found ». */
    private static String toTitle(final String code) {
        String words = code.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /** {@code NotBlank} → {@code NOT_BLANK}. */
    private static String toUpperSnakeCase(final String constraint) {
        return constraint == null ? "INVALID" : constraint.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }

    private static String path(final WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : "";
    }
}
