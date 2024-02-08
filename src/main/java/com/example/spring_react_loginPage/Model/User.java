package com.example.spring_react_loginPage.Model;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Data
@Document(collection = "users")
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    private String id;

    @NotBlank(message = "username should not be empty")
    private String username;

    @NotBlank(message = "Email should not be empty")
    private String email;

    @NotBlank(message = "password should not be empty")
    private String password;

    private String url;
}
