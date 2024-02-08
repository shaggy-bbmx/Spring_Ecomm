package com.example.spring_react_loginPage.Repository;


import com.example.spring_react_loginPage.Model.ResetPasswordToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResetTokenRepo extends MongoRepository<ResetPasswordToken, String> {
    Optional<ResetPasswordToken> findByEmail(String email);

    void deleteByEmail(String email);
}
