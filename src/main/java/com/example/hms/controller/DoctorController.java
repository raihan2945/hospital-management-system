package com.example.hms.controller;

import com.example.hms.domain.Doctor;
import com.example.hms.dto.DoctorForm;
import com.example.hms.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/doctors")
public class DoctorController {
    private final DoctorService service;

    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @ModelAttribute("activeSection")
    public String activeSection() { return "doctors"; }

    @ModelAttribute("suggestedSpecializations")
    public List<String> suggestedSpecializations() {
        return List.of("General Medicine", "Cardiology", "Neurology", "Pediatrics",
                "Dermatology", "Orthopedics", "ENT", "Gynecology");
    }

    @InitBinder("form")
    public void formBinding(WebDataBinder binder) {
        binder.setAllowedFields("firstName", "lastName", "phone", "email",
                "specialization", "qualification", "consultationFee", "available");
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "") String specialization,
                       @RequestParam(required = false) Boolean available,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("records", service.search(keyword, specialization, available, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("specialization", specialization);
        model.addAttribute("available", available);
        model.addAttribute("specializations", service.specializations());
        return "doctors/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new DoctorForm());
        return formPage(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") DoctorForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return formPage(model, null);
        }
        Doctor record = service.createDoctor(form);
        redirect.addFlashAttribute("success", "Doctor registered successfully.");
        return "redirect:/doctors/" + record.getId();
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("record", service.getDoctor(id));
        return "doctors/details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", DoctorForm.from(service.getDoctor(id)));
        return formPage(model, id);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") DoctorForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        service.getDoctor(id);
        if (errors.hasErrors()) {
            return formPage(model, id);
        }
        service.updateDoctor(id, form);
        redirect.addFlashAttribute("success", "Doctor updated successfully.");
        return "redirect:/doctors/" + id;
    }

    @GetMapping("/{id}/delete")
    public String confirmDelete(@PathVariable Long id, Model model) {
        Doctor record = service.getDoctor(id);
        model.addAttribute("record", record);
        model.addAttribute("code", record.getDoctorCode());
        model.addAttribute("recordType", "doctor");
        model.addAttribute("directory", "/doctors");
        return "directory/delete";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        service.deleteDoctor(id);
        redirect.addFlashAttribute("success", "Doctor deleted successfully.");
        return "redirect:/doctors";
    }

    private String formPage(Model model, Long id) {
        model.addAttribute("id", id);
        model.addAttribute("title", id == null ? "Register doctor" : "Edit doctor");
        return "doctors/form";
    }
}

