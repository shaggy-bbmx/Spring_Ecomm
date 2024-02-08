package com.example.spring_react_loginPage.Service;

import com.example.spring_react_loginPage.Config.Utility.JwtTokenUtil;
import com.example.spring_react_loginPage.Model.RegistrationData;
import com.example.spring_react_loginPage.Model.ResetPasswordToken;
import com.example.spring_react_loginPage.Model.User;
import com.example.spring_react_loginPage.Repository.ResetTokenRepo;
import com.example.spring_react_loginPage.Repository.UserRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.storage.GoogleStorageResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    @Autowired
    private final UserRepo userRepo;

    @Autowired
    private final ResetTokenRepo resetTokenRepo;

    @Autowired
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JavaMailSender javaMailSender;


    @Value("${Google.OAuth.userinfoEndpoint}")
    private String userinfoEndpoint;

    @Value("${spring.cloud.gcp.storage.bucket-name}")
    private String bucketName;


    public UserService(UserRepo userRepo, ResetTokenRepo resetTokenRepo, BCryptPasswordEncoder bCryptPasswordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.userRepo = userRepo;
        this.resetTokenRepo = resetTokenRepo;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }


    public User addUser(RegistrationData registrationData, MultipartFile image) throws IOException {
        User user = new User();
        user.setUsername(registrationData.getUsername());
        user.setEmail(registrationData.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(registrationData.getPassword()));

        Storage storage = StorageOptions.getDefaultInstance().getService();
        BlobId blobId = BlobId.of(bucketName, Objects.requireNonNull(image.getOriginalFilename()));
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(image.getContentType()).build();
        Blob blob = storage.create(blobInfo, image.getBytes());
        String imageUrl = blob.getMediaLink();
        System.out.println(blob.getMediaLink());
        System.out.println(imageUrl);

        user.setUrl(imageUrl);
        return userRepo.save(user);
    }


    public ResponseEntity<?> loginUser(User user, HttpServletResponse res, HttpServletRequest req) {
        Optional<User> userData = userRepo.findByUsername(user.getUsername());
        if (userData.isEmpty()) return ResponseEntity.status(401).body("Check username Again!!!");

        boolean isMatch = bCryptPasswordEncoder.matches(user.getPassword(), userData.get().getPassword());

        if (isMatch) {
            //generate token and pass it as a cookie
            String jwtToken = jwtTokenUtil.generateToken(userData.get());
            jwtTokenUtil.setTokenCookie(res, jwtToken);
            return ResponseEntity.status(200).body("Ok");
        } else {
            return ResponseEntity.status(401).body("Error");
        }
    }


    public boolean checkForLogin(HttpServletResponse res, HttpServletRequest req) {
        String token = jwtTokenUtil.getTokenFromCookie(req);
        return jwtTokenUtil.validateToken(token);
    }

    public ResponseEntity<?> getAllUsers() {
        User[] allUser = userRepo.findAll().toArray(new User[0]);
        return ResponseEntity.status(200).body(allUser);
    }

    public ResponseEntity<?> sendPasswordResetEmail(String email) {

        //...creating the reset Link
        String hostname = "localhost:3000";
        String pathName = "/resetPassword/";
        String token = UUID.randomUUID().toString();
        String url = "http://" + hostname + pathName + token + "/" + email;

        //....finding the email of the requester
        Optional<User> customer = userRepo.findByEmail(email);
        if (customer.isEmpty()) return ResponseEntity.status(401).body("Email id is not registered.");

        try {
            //...storing the token sent in the Link via mail in DB if email Id already
            //....exist in DB otherwise just updating the data
            Optional<ResetPasswordToken> temp = resetTokenRepo.findByEmail(email);
            if (temp.isPresent()) {
                temp.get().setToken(token);
                temp.get().setExpirationTime(LocalDateTime.now().plusMinutes(10));
                resetTokenRepo.save(temp.get());
            } else {
                ResetPasswordToken newTemp = new ResetPasswordToken();
                newTemp.setEmail(customer.get().getEmail());
                newTemp.setToken(token);
                newTemp.setExpirationTime(LocalDateTime.now().plusMinutes(10));
                resetTokenRepo.save(newTemp);

            }


            //.....sending the Link in mail
            javaMailSender.send(mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
                messageHelper.setTo(email);
                messageHelper.setSubject("Password Reset Token");
                messageHelper.setText(url);
            });
            return ResponseEntity.status(200).body("Check Your Email.");

        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }

    }

    public ResponseEntity<?> setNewPassword(String email, String token, String password) {
        //....find the token Data existing in DB for given email
        Optional<ResetPasswordToken> tokenData = resetTokenRepo.findByEmail(email);
        if (tokenData.isEmpty()) {
            return ResponseEntity.status(401).body("unauthorized request");
        }

        //...check if token is valid
        if (!Objects.equals(tokenData.get().getToken(), token)) {
            return ResponseEntity.status(401).body("Wrong Token");
        }

        //...check if token is expired
        LocalDateTime currentDateTime = LocalDateTime.now();
        int comparisonResult = currentDateTime.compareTo(tokenData.get().getExpirationTime());
        if (comparisonResult >= 0) {
            ResponseEntity.status(401).body("Token is Expired");
        }

        //...reset the password
        String encodedPassword = bCryptPasswordEncoder.encode(password);

        try {
            Optional<User> user = userRepo.findByEmail(email);
            if (user.isEmpty()) {
                return ResponseEntity.status(401).body("Email id is not valid");
            }
            user.get().setPassword(encodedPassword);
            userRepo.save(user.get());

            //...since everything is ok ...old token data should be deleted;
            resetTokenRepo.deleteByEmail(tokenData.get().getEmail());

            return ResponseEntity.status(200).body("Password successfully updated");
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }

    }

    public ResponseEntity<?> getUserInfo(String accessToken, HttpServletResponse res) throws JsonProcessingException {

        //...creating the end point to check token validity and making a GET request to it.
        String userinfoUrl = userinfoEndpoint + "?access_token=" + accessToken;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(userinfoUrl, String.class);

        //...destructuring the JSON response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response.getBody()).get("email");

        //...check if email id obtained form JSON response exist in our User Model
        Optional<User> user = userRepo.findByEmail(node.textValue());

        if (user.isEmpty()) {
            return ResponseEntity.status(403).body("You are not a registered user");
        }


        if (response.getStatusCode().is2xxSuccessful()) {
            String jwtToken = jwtTokenUtil.generateToken(node.textValue());
            jwtTokenUtil.setTokenCookie(res, jwtToken);
            return ResponseEntity.status(200).body("Ok");
        } else {
            return ResponseEntity.status(401).body("It's is a fake Token");
        }
    }
}
