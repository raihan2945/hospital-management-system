package com.example.hms.exception;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView notFound(ResourceNotFoundException exception) {
        return ErrorPages.create(404, "Record not found", exception.getMessage());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ModelAndView missingPage() { return ErrorPages.standard(404); }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class, BindException.class, ConstraintViolationException.class})
    public ModelAndView invalidParameter() { return ErrorPages.standard(400); }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ModelAndView methodValidation(HandlerMethodValidationException exception) {
        return ErrorPages.standard(exception.isForReturnValue() ? 500 : 400);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ModelAndView invalidRequest(InvalidRequestException exception) {
        return ErrorPages.create(400, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView conflict() {
        return ErrorPages.create(409, "Unable to save this change",
                "The record conflicts with existing data or is still in use. Check for an existing appointment or invoice, and reload the record before trying again.");
    }

    @ExceptionHandler({AppointmentStateException.class, ReferencedRecordException.class, BillingStateException.class})
    public ModelAndView recordState(RuntimeException exception) {
        return ErrorPages.create(409, "Unable to change this record", exception.getMessage());
    }

    @ExceptionHandler({BillingValidationException.class, AppointmentValidationException.class})
    public ModelAndView businessValidation(RuntimeException exception) {
        return ErrorPages.create(400, "Unable to complete this request", exception.getMessage());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class, QueryTimeoutException.class})
    public ModelAndView concurrentChange() { return ErrorPages.standard(409); }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ModelAndView unavailable() { return ErrorPages.standard(503); }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView unsupportedMethod(HttpRequestMethodNotSupportedException exception, HttpServletResponse response) {
        if (exception.getSupportedMethods() != null) { response.setHeader("Allow", String.join(", ", exception.getSupportedMethods())); }
        return ErrorPages.standard(405);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ModelAndView unsupportedContent() { return ErrorPages.standard(415); }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ModelAndView unsupportedResponse() { return ErrorPages.standard(406); }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView responseStatus(ResponseStatusException exception) {
        return ErrorPages.standard(exception.getStatusCode().value());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception exception) {
        String reference = UUID.randomUUID().toString();
        log.error("Unexpected application error [{}]", reference, exception);
        ModelAndView view = ErrorPages.standard(500);
        view.addObject("reference", reference);
        return view;
    }
}
