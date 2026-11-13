package customer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Client for the SAP Customer Provider API (GET /customers/{id}).
 */
public class CustomerClient {

    private final String baseUrl;
    // HTTP/1.1 only: otherwise the JDK client adds h2c upgrade headers to every request,
    // which would then show up in the generated contract.
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomerClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Customer getCustomer(String id) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/customers/%s", baseUrl, id)))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }

        return switch (response.statusCode()) {
            case 200 -> parse(response.body());
            case 400 -> throw new InvalidCustomerIdException(id);
            case 404 -> throw new CustomerNotFoundException(id);
            default -> throw new IllegalStateException(
                    String.format("Unexpected response status %d for customer %s", response.statusCode(), id));
        };
    }

    private Customer parse(String body) {
        try {
            return objectMapper.readValue(body, Customer.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
