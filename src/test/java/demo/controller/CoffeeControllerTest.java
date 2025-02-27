package demo.controller;

import demo.TestData;
import demo.model.Coffee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@WebFluxTest(CoffeeController.class)
public class CoffeeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveRedisOperations<String, Coffee> coffeeOps;

    @Test
    void createCoffee_ShouldReturnCoffee() {
        Coffee newCoffee = TestData.newCoffee();

        var valueOpsMock = mock(ReactiveValueOperations.class);
        when(coffeeOps.opsForValue()).thenReturn(valueOpsMock);
        when(valueOpsMock.set(any(), any())).thenReturn(Mono.just(true));

        webTestClient.post()
                .uri("/coffees")
                .bodyValue(newCoffee)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Coffee.class)
                .value(coffee -> {
                    assert coffee.id() != null;
                    assert coffee.name().equals(TestData.newCoffee().name());
                });

        Mockito.verify(coffeeOps.opsForValue()).set(any(), any());
    }

    @Test
    void allCoffees_ShouldReturnCoffees() {
        Coffee newCoffee = TestData.newCoffee();
        Coffee newCoffee1 = TestData.newCoffee();

        var valueOpsMock = mock(ReactiveValueOperations.class);
        when(coffeeOps.opsForValue()).thenReturn(valueOpsMock);

        when(coffeeOps.keys("*")).thenReturn(Flux.just(newCoffee.id(), newCoffee1.id()));

        when(valueOpsMock.get(anyString()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    if (key.equals(newCoffee.id())) {
                        return Mono.just(newCoffee);
                    } else if (key.equals(newCoffee1.id())) {
                        return Mono.just(newCoffee1);
                    }
                    return Mono.empty();
                });

        webTestClient.get()
                .uri("/coffees")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Coffee.class)
                .hasSize(2)
                .contains(newCoffee, newCoffee1);

        verify(coffeeOps).keys("*");
    }
}
