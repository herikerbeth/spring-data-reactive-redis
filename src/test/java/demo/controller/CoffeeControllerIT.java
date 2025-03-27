package demo.controller;

import com.redis.testcontainers.RedisContainer;
import demo.TestData;
import demo.model.Coffee;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment =
SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CoffeeControllerIT {

    @LocalServerPort
    private Integer port;

    static RedisContainer redis = new RedisContainer(
            "redis:latest"
    );

    @BeforeAll
    static void beforeAll() {
        redis.start();
    }

    @AfterAll
    static void afterAll() {
        redis.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
    }

    @Autowired
    private ReactiveRedisOperations<String, Coffee> coffeeOps;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
        coffeeOps.keys("*")
                .flatMap(coffeeOps.opsForValue()::delete)
                .then()
                .subscribe();
    }

    @Test
    void shouldReturnCoffeeWhenCreating() {
        Coffee coffee = TestData.newCoffee();

        given()
                .contentType("application/json")
                .body(coffee)
                .when()
                .post("/coffees")
                .then()
                .statusCode(200)
                .body("name", equalTo(coffee.name()));
    }

    @Test
    void shouldReturnAllCoffees() {
        Coffee coffee = TestData.newCoffee();

        coffeeOps.opsForValue()
                .set(coffee.id(), coffee)
                .then()
                .subscribe();

        given()
                .contentType("application/json")
                .when()
                .get("/coffees")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo(coffee.name()));
    }
}
