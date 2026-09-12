package com.example.hms.controller;

import com.example.hms.domain.Appointment;
import com.example.hms.domain.enums.AppointmentStatus;
import com.example.hms.dto.AppointmentForm;
import com.example.hms.exception.AppointmentStateException;
import com.example.hms.exception.AppointmentValidationException;
import com.example.hms.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentService service;

    public AppointmentController(AppointmentService service) { this.service = service; }

    @ModelAttribute("activeSection")
    public String activeSection() { return "appointments"; }

    @ModelAttribute("statuses")
    public AppointmentStatus[] statuses() { return AppointmentStatus.values(); }

    @InitBinder("form")
    public void formBinding(WebDataBinder binder) {
        binder.setAllowedFields("patientId", "doctorId", "appointmentDate", "appointmentTime", "reason", "notes", "version");
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(required = false) Long patientId, @RequestParam(required = false) Long doctorId,
                       @RequestParam(required = false) AppointmentStatus status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("records", service.search(date, patientId, doctorId, status, page));
        model.addAttribute("date", date);
        model.addAttribute("patientId", patientId);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("status", status);
        choices(model);
        return "appointments/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long patientId,
                          @RequestParam(required = false) Long doctorId, Model model) {
        AppointmentForm form = new AppointmentForm();
        form.setPatientId(patientId);
        form.setDoctorId(doctorId);
        model.addAttribute("form", form);
        return formPage(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AppointmentForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                Appointment appointment = service.createAppointment(form);
                redirect.addFlashAttribute("success", "Appointment scheduled successfully.");
                return "redirect:/appointments/" + appointment.getId();
            } catch (AppointmentValidationException exception) {
                errors.rejectValue(exception.getField(), "appointment.invalid", exception.getMessage());
            }
        }
        return formPage(model, null);
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("record", service.getAppointment(id));
        return "appointments/details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Appointment appointment = service.getAppointment(id);
        appointment.requireEditable();
        model.addAttribute("form", AppointmentForm.from(appointment));
        return formPage(model, id);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") AppointmentForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        service.getAppointment(id).requireEditable();
        if (!errors.hasErrors()) {
            try {
                service.updateAppointment(id, form);
                redirect.addFlashAttribute("success", "Appointment updated successfully.");
                return "redirect:/appointments/" + id;
            } catch (AppointmentValidationException exception) {
                errors.rejectValue(exception.getField(), "appointment.invalid", exception.getMessage());
            }
        }
        return formPage(model, id);
    }

    @GetMapping("/{id}/confirm")
    public String confirmPage(@PathVariable Long id, Model model) { return transitionPage(id, "confirm", model); }

    @GetMapping("/{id}/cancel")
    public String cancelPage(@PathVariable Long id, Model model) { return transitionPage(id, "cancel", model); }

    @GetMapping("/{id}/complete")
    public String completePage(@PathVariable Long id, Model model) { return transitionPage(id, "complete", model); }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, @RequestParam Long version, RedirectAttributes redirect) {
        service.confirmAppointment(id, version);
        return changed(id, "Appointment confirmed.", redirect);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam Long version, RedirectAttributes redirect) {
        service.cancelAppointment(id, version);
        return changed(id, "Appointment cancelled. The time slot is available for another booking.", redirect);
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, @RequestParam Long version, RedirectAttributes redirect) {
        service.completeAppointment(id, version);
        return changed(id, "Appointment completed.", redirect);
    }

    private String transitionPage(Long id, String action, Model model) {
        Appointment appointment = service.getAppointment(id);
        appointment.requireEditable();
        if (action.equals("confirm") && appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new AppointmentStateException("Only scheduled appointments can be confirmed.");
        }
        model.addAttribute("record", appointment);
        model.addAttribute("action", action);
        model.addAttribute("title", switch (action) {
            case "cancel" -> "Cancel appointment";
            case "complete" -> "Complete appointment";
            default -> "Confirm appointment";
        });
        return "appointments/transition";
    }

    private String changed(Long id, String message, RedirectAttributes redirect) {
        redirect.addFlashAttribute("success", message);
        return "redirect:/appointments/" + id;
    }

    private String formPage(Model model, Long id) {
        model.addAttribute("id", id);
        model.addAttribute("title", id == null ? "Schedule appointment" : "Edit appointment");
        choices(model);
        return "appointments/form";
    }

    private void choices(Model model) {
        model.addAttribute("patients", service.patientOptions());
        model.addAttribute("doctors", service.doctorOptions());
    }
}
