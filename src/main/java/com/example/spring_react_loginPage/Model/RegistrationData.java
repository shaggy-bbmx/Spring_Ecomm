package com.example.spring_react_loginPage.Model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;


@Getter
@Data
@Document(collection = "RegistrationData")
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationData {


    @NotBlank(message = "username should not be empty")
    private String username;

    @NotBlank(message = "Email should not be empty")
    private String email;

    @NotBlank(message = "password should not be empty")
    private String password;

    @NotBlank(message = "password should be confirmed")
    private String confirmPassword;
}

