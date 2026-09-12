package com.example.hms.dto;

import com.example.hms.domain.Person;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editable contact fields only; IDs, codes, and timestamps cannot be form-bound. */
public abstract class PersonForm {
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Phone is required")
    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    protected void copyContactFrom(Person person) {
        setFirstName(person.getFirstName());
        setLastName(person.getLastName());
        setPhone(person.getPhone());
        setEmail(person.getEmail());
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName == null || firstName.isBlank() ? null : firstName.strip(); }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName == null || lastName.isBlank() ? null : lastName.strip(); }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone == null || phone.isBlank() ? null : phone.strip(); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null || email.isBlank() ? null : email.strip(); }
}

