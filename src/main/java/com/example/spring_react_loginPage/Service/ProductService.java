package com.example.spring_react_loginPage.Service;


import com.example.spring_react_loginPage.Model.Product;
import com.example.spring_react_loginPage.Repository.ProductRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private final ProductRepo productRepo;

    public ProductService(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }


    public ResponseEntity<?> saveProduct(Product product) {
        try {
            productRepo.save(product);
            return ResponseEntity.status(200).body("OK");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Fail to save product");
        }
    }

    public ResponseEntity<?> getProductInfo(int size, int page, int lb, int hb) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        List<Product> temporaryList = productRepo.findByRatingBetween(lb, hb, pageable);
        Product[] products = temporaryList.toArray(Product[]::new);
        return ResponseEntity.status(200).body(products);
    }

    public ResponseEntity<?> getProductCount(int lb, int hb) {
        try {
            Number count = productRepo.findCountByRatingBetween(lb, hb);
            return ResponseEntity.status(200).body(count);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
