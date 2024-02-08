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
@Document(collection = "products")
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    private String id;

    @NotBlank(message = "title should not be empty")
    private String title;
    private Number price;
    @NotBlank(message = "image should not be empty")
    private String image;
    private Number rating;

}
