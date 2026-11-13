package customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The parts of a customer this consumer actually uses. Anything else the provider
 * returns (email, phone, lastModified, ...) is ignored, so it does not end up in the contract.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Customer(int id, String name, String status, Address address) {
}
