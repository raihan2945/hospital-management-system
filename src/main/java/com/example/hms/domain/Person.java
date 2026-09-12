package com.example.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Common name and contact state inherited by patients and doctors. */
@MappedSuperclass
public abstract class Person extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String phone;

    @Email
    @Size(max = 254)
    @Column(length = 254)
    private String email;

    protected Person() {
        // Required by JPA.
    }

    protected Person(String firstName, String lastName, String phone, String email) {
        rename(firstName, lastName);
        updateContact(phone, email);
    }

    public void rename(String firstName, String lastName) {
        String validFirstName = requiredText(firstName, "First name", 100);
        String validLastName = requiredText(lastName, "Last name", 100);
        this.firstName = validFirstName;
        this.lastName = validLastName;
    }

    public void updateContact(String phone, String email) {
        String validPhone = requiredText(phone, "Phone", 30);
        String normalizedEmail = email == null || email.isBlank() ? null : email.strip();
        if (normalizedEmail != null && normalizedEmail.length() > 254) {
            throw new IllegalArgumentException("Email must not exceed 254 characters");
        }
        this.phone = validPhone;
        this.email = normalizedEmail;
    }

    protected static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    protected static String optionalText(String value, String field, int maxLength) {
        return value == null || value.isBlank() ? null : requiredText(value, field, maxLength);
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }
}
