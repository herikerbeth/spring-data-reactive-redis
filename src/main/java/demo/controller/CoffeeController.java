package demo.controller;

import demo.model.Coffee;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/coffees")
public class CoffeeController {

    private final ReactiveRedisOperations<String, Coffee> coffeeOps;

    public CoffeeController(ReactiveRedisOperations<String, Coffee> coffeeOps) {
        this.coffeeOps = coffeeOps;
    }

    @PostMapping
    public Mono<Coffee> createCoffee(@RequestBody Coffee coffee) {
        Coffee newCoffee = new Coffee(UUID.randomUUID().toString(), coffee.name());
        return coffeeOps.opsForValue()
                .set(newCoffee.id(), newCoffee)
                .thenReturn(newCoffee);
    }

    @GetMapping
    public Flux<Coffee> allCoffees() {
        return coffeeOps.keys("*")
                .flatMap(coffeeOps.opsForValue()::get);
    }
}
