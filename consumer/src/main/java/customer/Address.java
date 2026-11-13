package customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Address(String street, int houseNumber, String city, String zipCode) {
}
