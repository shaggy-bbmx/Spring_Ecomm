package com.example.spring_react_loginPage.Repository;


import com.example.spring_react_loginPage.Model.Product;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends MongoRepository<Product, String> {

    @Query(value = "{ 'rating' : { $gte: ?0, $lte: ?1 } }", count = true)
    Number findCountByRatingBetween(int lb, int hb);


    List<Product> findByRatingBetween(int lb, int hb, PageRequest pageable);
}
