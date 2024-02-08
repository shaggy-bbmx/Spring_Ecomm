package com.example.spring_react_loginPage.Model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Data
@Document(collection = "resetTokens")
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordToken {

    @Id
    private String id;
    private String token;
    private String email;
    private LocalDateTime expirationTime;
}
