package com.example.hms.controller;

import com.example.hms.domain.*;
import com.example.hms.domain.enums.*;
import com.example.hms.dto.*;
import com.example.hms.exception.*;
import com.example.hms.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/billing")
public class BillingController {
    private final BillingService service;
    public BillingController(BillingService service) { this.service = service; }
    @ModelAttribute("activeSection")
    public String activeSection() { return "billing"; }
    @ModelAttribute("statuses")
    public PaymentStatus[] statuses() { return PaymentStatus.values(); }
    @ModelAttribute("methods")
    public PaymentMethod[] methods() { return PaymentMethod.values(); }
    @InitBinder("form")
    public void formBinding(WebDataBinder binder) {
        binder.setAllowedFields("patientId", "appointmentId", "consultationFee", "serviceCharge", "medicineCharge", "otherCharge", "discount");
    }
    @InitBinder("paymentForm")
    public void paymentBinding(WebDataBinder binder) {
        binder.setAllowedFields("amount", "paymentMethod", "transactionReference", "version");
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long patientId, @RequestParam(required = false) Long appointmentId,
                       @RequestParam(required = false) PaymentStatus status, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("records", service.search(patientId, appointmentId, status, page));
        model.addAttribute("patientId", patientId);
        model.addAttribute("appointmentId", appointmentId);
        model.addAttribute("status", status);
        model.addAttribute("patients", service.patientOptions());
        return "billing/list";
    }
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long patientId,
                          @RequestParam(required = false) Long appointmentId, Model model) {
        model.addAttribute("form", service.newForm(patientId, appointmentId));
        return formPage(model);
    }
    @PostMapping
    public String create(@Valid @ModelAttribute("form") CreateBillForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                Bill bill = service.createBill(form);
                redirect.addFlashAttribute("success", "Invoice created successfully.");
                return "redirect:/billing/" + bill.getId();
            } catch (BillingValidationException exception) {
                errors.rejectValue(exception.getField(), "billing.invalid", exception.getMessage());
            }
        }
        return formPage(model);
    }
    @GetMapping("/{id}")
    public String invoice(@PathVariable Long id, Model model) {
        model.addAttribute("record", service.getBill(id));
        model.addAttribute("payments", service.paymentHistory(id));
        return "billing/invoice";
    }
    @GetMapping("/{id}/payment")
    public String paymentForm(@PathVariable Long id, Model model) {
        Bill bill = service.getBill(id);
        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BillingStateException("This invoice is fully paid. No further payment is due.");
        }
        PaymentForm form = new PaymentForm();
        form.setAmount(bill.getDueAmount());
        form.setVersion(bill.getVersion());
        model.addAttribute("paymentForm", form);
        model.addAttribute("record", bill);
        return "billing/payment";
    }
    @PostMapping("/{id}/payment")
    public String recordPayment(@PathVariable Long id, @Valid @ModelAttribute("paymentForm") PaymentForm form,
                                BindingResult errors, Model model, RedirectAttributes redirect) {
        service.getBill(id);
        if (!errors.hasErrors()) {
            try {
                service.recordPayment(id, form);
                redirect.addFlashAttribute("success", "Payment recorded successfully.");
                return "redirect:/billing/" + id;
            } catch (BillingValidationException exception) {
                errors.rejectValue(exception.getField(), "billing.invalid", exception.getMessage());
            }
        }
        model.addAttribute("record", service.getBill(id));
        return "billing/payment";
    }
    private String formPage(Model model) {
        model.addAttribute("patients", service.patientOptions());
        model.addAttribute("appointments", service.appointmentOptions());
        return "billing/form";
    }
}
