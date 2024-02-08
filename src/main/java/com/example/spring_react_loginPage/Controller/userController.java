package com.example.spring_react_loginPage.Controller;


import com.example.spring_react_loginPage.Model.Product;
import com.example.spring_react_loginPage.Model.RegistrationData;
import com.example.spring_react_loginPage.Model.User;
import com.example.spring_react_loginPage.Service.ProductService;
import com.example.spring_react_loginPage.Service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST})
@RequestMapping("/api")
public class userController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    private final ObjectMapper objectMapper;

    public userController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerController(@RequestParam String username,
                                                @RequestParam String email,
                                                @RequestParam String password,
                                                @RequestParam String confirmPassword,
                                                @RequestParam MultipartFile image) {

        //....check if 2 passwords matches
        if (!Objects.equals(password, confirmPassword)) {
            return ResponseEntity.status(401).body("Password don't match");
        }

        //...creating a Dto object
        RegistrationData registrationData = new RegistrationData();
        registrationData.setUsername(username);
        registrationData.setEmail(email);
        registrationData.setPassword(password);
        registrationData.setConfirmPassword(confirmPassword);


        //....save the user and upload image to Google Cloud
        try {

            User registeredUser = userService.addUser(registrationData, image);
            return ResponseEntity.status(200).body(registeredUser);
        } catch (IOException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }

    }

    @PostMapping("/login")
    public ResponseEntity<?> loginController(@RequestBody User user,
                                             HttpServletResponse res, HttpServletRequest req) {
        try {
            return userService.loginUser(user, res, req);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/checkLogin")
    public ResponseEntity<?> checkLoginController(HttpServletResponse res, HttpServletRequest req) {
        try {
            boolean isUser = userService.checkForLogin(res, req);
            if (isUser) return ResponseEntity.status(200).body(true);
            else return ResponseEntity.status(200).body(false);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<?> getAllUsers(HttpServletResponse res, HttpServletRequest req) {
        try {
            boolean isUser = userService.checkForLogin(res, req);
            if (!isUser) return ResponseEntity.status(401).body("");

            return userService.getAllUsers();
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletResponse res, HttpServletRequest req) {
        try {
            Cookie cookie = new Cookie("jwtToken", "");
            cookie.setMaxAge(0); // Convert milliseconds to seconds
            cookie.setSecure(true); // Enable for HTTPS
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            res.addCookie(cookie);
            return ResponseEntity.status(200).body("Logout Done");
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<?> sendPasswordResetEmail(@RequestBody JsonNode requestBody) {
        String email = requestBody.get("email").asText();
        System.out.println(email);
        return userService.sendPasswordResetEmail(email);
    }


    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPasswordController(@RequestBody JsonNode requestBody) {
        String email = requestBody.get("email").asText();
        String token = requestBody.get("token").asText();
        String password = requestBody.get("password").asText();

        return userService.setNewPassword(email, token, password);
    }

    @PostMapping("/token/handle")
    public ResponseEntity<?> handleToken(@RequestBody JsonNode requestBody, HttpServletResponse response) {
        String accessToken = requestBody.get("accessToken").asText();
        try {
            return userService.getUserInfo(accessToken, response);
        } catch (Exception e) {
            return ResponseEntity.status(402).body(e.getMessage());
        }

    }

    @PostMapping("/product/save")
    public ResponseEntity<?> productSaveController(@Valid @RequestBody Product product) {
        return productService.saveProduct(product);
    }

    @GetMapping("/product/info")
    public ResponseEntity<?> productInfoController(@RequestParam(name = "size", defaultValue = "3") int size,
                                                   @RequestParam(name = "page", defaultValue = "1") int page,
                                                   @RequestParam(name = "lb", defaultValue = "0") int lb,
                                                   @RequestParam(name = "hb", defaultValue = "5") int hb,
                                                   HttpServletRequest request, HttpServletResponse response) {
        boolean isAuthenticated = userService.checkForLogin(response, request);
        if (!isAuthenticated) return ResponseEntity.status(401).body("Please login again!!!");
        return productService.getProductInfo(size, page, lb - 1, hb + 1);
    }

    @GetMapping("/product/count")
    public ResponseEntity<?> productCountController(
            @RequestParam(name = "lb", defaultValue = "0") int lb,
            @RequestParam(name = "hb", defaultValue = "5") int hb,
            HttpServletResponse response, HttpServletRequest request) {
        boolean isAuthenticated = userService.checkForLogin(response, request);
        if (!isAuthenticated) return ResponseEntity.status(401).body("Please login again!!!");
        return productService.getProductCount(lb, hb);
    }

}

