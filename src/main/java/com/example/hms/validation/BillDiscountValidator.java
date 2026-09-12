package com.example.hms.validation;

import com.example.hms.dto.CreateBillForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class BillDiscountValidator implements ConstraintValidator<ValidBillDiscount, CreateBillForm> {
    @Override
    public boolean isValid(CreateBillForm form, ConstraintValidatorContext context) {
        if (form == null) { return true; }
        BigDecimal[] values = {form.getConsultationFee(), form.getServiceCharge(), form.getMedicineCharge(), form.getOtherCharge(), form.getDiscount()};
        for (BigDecimal value : values) {
            // Let the field constraints explain missing, negative, or excessive amounts first.
            if (value == null || value.signum() < 0 || value.scale() > 2 || value.precision() - value.scale() > 11) { return true; }
        }
        if (values[4].compareTo(values[0].add(values[1]).add(values[2]).add(values[3])) <= 0) { return true; }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("discount").addConstraintViolation();
        return false;
    }
}
