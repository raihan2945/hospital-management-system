package com.example.hms.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.dao.OptimisticLockingFailureException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException exception, Model model) {
        model.addAttribute("title", "Record not found");
        model.addAttribute("message", exception.getMessage());
        return "error/message";
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidParameter(Model model) {
        model.addAttribute("title", "Invalid request");
        model.addAttribute("message", "Check the record number, page number, and selected filters, then try again.");
        return "error/message";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(Model model) {
        model.addAttribute("title", "Unable to save this change");
        model.addAttribute("message", "The record conflicts with existing data or is still in use. Return to the directory and try again.");
        return "error/message";
    }

    @ExceptionHandler({AppointmentStateException.class, ReferencedRecordException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String appointmentState(RuntimeException exception, Model model) {
        model.addAttribute("title", "Unable to change this record");
        model.addAttribute("message", exception.getMessage());
        return "error/message";
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String staleAppointment(Model model) {
        model.addAttribute("title", "Appointment changed");
        model.addAttribute("message", "Reload the appointment before trying again.");
        return "error/message";
    }
}
