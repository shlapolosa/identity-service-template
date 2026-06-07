package com.template.identity.web;

import com.template.identity.core.application.BaseRegistrationUseCase.RegistrationCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration request body. Maps onto the base {@link RegistrationCommand}
 * (the base orchestrator works against the command, not this DTO).
 */
public class RegistrationRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String firstName;
    private String lastName;
    private String phoneNumber;

    public RegistrationCommand toCommand() {
        RegistrationCommand command = new RegistrationCommand();
        command.setEmail(email);
        command.setPassword(password);
        command.setFirstName(firstName);
        command.setLastName(lastName);
        command.setPhoneNumber(phoneNumber);
        return command;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
