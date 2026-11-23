package io.github.ceydrew.trvalidation.examples.customer;

import io.github.ceydrew.trvalidation.jakarta.Tckn;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer registration and lookup, backed by an in-memory store. */
@RestController
@RequestMapping("/api/customers")
class CustomerController {

  private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

  private final Map<Long, Customer> customers = new ConcurrentHashMap<>();
  private final AtomicLong ids = new AtomicLong();

  /** Validates the body with {@code @Valid}; violations become a 400 problem response. */
  @PostMapping
  ResponseEntity<CustomerResponse> register(@Valid @RequestBody CustomerRequest request) {
    Customer customer = Customer.from(ids.incrementAndGet(), request);
    customers.put(customer.id(), customer);
    // value types print masked, so logging them is safe
    log.info("Registered customer {} with GSM {}", customer.id(), customer.gsm());
    return ResponseEntity.created(URI.create("/api/customers/" + customer.id()))
        .body(CustomerResponse.of(customer));
  }

  @GetMapping("/{id}")
  ResponseEntity<CustomerResponse> get(@PathVariable long id) {
    return ResponseEntity.of(Optional.ofNullable(customers.get(id)).map(CustomerResponse::of));
  }

  /**
   * A constraint directly on a request parameter: Spring MVC validates it without
   * {@code @Validated} on the controller.
   */
  @GetMapping
  ResponseEntity<CustomerResponse> findByIdentityNumber(
      @RequestParam @Tckn(allowYkn = true) String identityNumber) {
    String normalized = io.github.ceydrew.trvalidation.Tckn.normalize(identityNumber);
    return ResponseEntity.of(
        customers.values().stream()
            .filter(c -> c.identityNumber().equals(normalized))
            .findFirst()
            .map(CustomerResponse::of));
  }
}
