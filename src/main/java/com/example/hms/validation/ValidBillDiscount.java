package com.example.hms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BillDiscountValidator.class)
public @interface ValidBillDiscount {
    String message() default "Discount cannot exceed the subtotal.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
