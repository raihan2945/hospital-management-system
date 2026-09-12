package com.example.hms.controller;

import com.example.hms.domain.Patient;
import com.example.hms.domain.enums.Gender;
import com.example.hms.dto.PatientForm;
import com.example.hms.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/patients")
public class PatientController {
    private final PatientService service;
    private final java.time.Clock clock;

    public PatientController(PatientService service, java.time.Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @ModelAttribute("hospitalToday")
    public java.time.LocalDate hospitalToday() { return java.time.LocalDate.now(clock); }

    @ModelAttribute("activeSection")
    public String activeSection() { return "patients"; }

    @ModelAttribute("genders")
    public Gender[] genders() { return Gender.values(); }

    @ModelAttribute("bloodGroups")
    public List<String> bloodGroups() { return List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"); }

    @InitBinder("form")
    public void formBinding(WebDataBinder binder) {
        binder.setAllowedFields("firstName", "lastName", "phone", "email",
                "gender", "dateOfBirth", "address", "bloodGroup", "emergencyContact");
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) Gender gender,
                       @RequestParam(defaultValue = "") String bloodGroup,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("records", service.search(keyword, gender, bloodGroup, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("gender", gender);
        model.addAttribute("bloodGroup", bloodGroup);
        return "patients/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new PatientForm());
        return formPage(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") PatientForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return formPage(model, null);
        }
        Patient record = service.createPatient(form);
        redirect.addFlashAttribute("success", "Patient registered successfully.");
        return "redirect:/patients/" + record.getId();
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("record", service.getPatient(id));
        return "patients/details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", PatientForm.from(service.getPatient(id)));
        return formPage(model, id);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") PatientForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        service.getPatient(id);
        if (errors.hasErrors()) {
            return formPage(model, id);
        }
        service.updatePatient(id, form);
        redirect.addFlashAttribute("success", "Patient updated successfully.");
        return "redirect:/patients/" + id;
    }

    @GetMapping("/{id}/delete")
    public String confirmDelete(@PathVariable Long id, Model model) {
        Patient record = service.getPatient(id);
        model.addAttribute("record", record);
        model.addAttribute("code", record.getPatientCode());
        model.addAttribute("recordType", "patient");
        model.addAttribute("directory", "/patients");
        return "directory/delete";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.deletePatient(id);
        redirect.addFlashAttribute("success", "Patient deleted successfully.");
        return "redirect:/patients";
    }

    private String formPage(Model model, Long id) {
        model.addAttribute("id", id);
        model.addAttribute("title", id == null ? "Register patient" : "Edit patient");
        return "patients/form";
    }
}
