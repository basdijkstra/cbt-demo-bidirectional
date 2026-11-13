package customer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Metadata;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.bjurr.wiremockpact.wiremockpactlib.api.WireMockPactApi;
import se.bjurr.wiremockpact.wiremockpactlib.api.WireMockPactConfig;
import se.bjurr.wiremockpact.wiremockpactlib.api.model.WireMockPactMetadata;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the CustomerClient against a WireMock simulation of the provider.
 * <p>
 * Every request WireMock serves during these tests is recorded and, after all tests have run,
 * written to a Pact file by wiremock-pact-lib. That Pact file is the consumer contract for
 * bidirectional contract testing: PactFlow compares it against the provider's OpenAPI spec.
 */
class CustomerClientTest {

    private static final String CONSUMER = "OSCustomerConsumerBDCT";
    private static final String PROVIDER = "SAPCustomerProviderBDCT";

    private static WireMockServer wireMockServer;
    private static WireMockPactApi wireMockPactApi;

    private CustomerClient customerClient;

    @BeforeAll
    static void startWireMockAndPactGenerator() {

        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        wireMockPactApi = WireMockPactApi.create(
                new WireMockPactConfig()
                        .setConsumerDefaultValue(CONSUMER)
                        .setProviderDefaultValue(PROVIDER)
                        .setPactJsonFolder("../pacts"));

        wireMockPactApi.clearAllSaved();
    }

    @BeforeEach
    void createClient() {
        customerClient = new CustomerClient(wireMockServer.baseUrl());
    }

    @Test
    void getCustomer_existingId_returnsCustomer() {

        // Only the fields this consumer uses are stubbed, so only those end up in the contract.
        wireMockServer.stubFor(get(urlEqualTo("/customers/12345"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 12345,
                                  "name": "Susan Jones",
                                  "status": "ACTIVE",
                                  "address": {
                                    "street": "Main Street",
                                    "houseNumber": 15,
                                    "city": "New York",
                                    "zipCode": "Y1A 2B3"
                                  }
                                }
                                """))
                .withMetadata(pactMetadata()));

        Customer customer = customerClient.getCustomer("12345");

        assertThat(customer.id()).isEqualTo(12345);
        assertThat(customer.name()).isEqualTo("Susan Jones");
        assertThat(customer.status()).isEqualTo("ACTIVE");
        assertThat(customer.address())
                .isEqualTo(new Address("Main Street", 15, "New York", "Y1A 2B3"));
    }

    @Test
    void getCustomer_nonExistentId_throwsCustomerNotFoundException() {

        wireMockServer.stubFor(get(urlEqualTo("/customers/99999"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse().withStatus(404))
                .withMetadata(pactMetadata()));

        assertThatThrownBy(() -> customerClient.getCustomer("99999"))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void getCustomer_nonIntegerId_throwsInvalidCustomerIdException() {

        wireMockServer.stubFor(get(urlEqualTo("/customers/abc"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse().withStatus(400))
                .withMetadata(pactMetadata()));

        assertThatThrownBy(() -> customerClient.getCustomer("abc"))
                .isInstanceOf(InvalidCustomerIdException.class);
    }

    @AfterAll
    static void writePactAndStopWireMock() {

        for (ServeEvent serveEvent : wireMockServer.getAllServeEvents()) {
            wireMockPactApi.addServeEvent(serveEvent);
        }

        wireMockPactApi.saveAll();
        wireMockServer.stop();
    }

    private static Metadata pactMetadata() {
        return new Metadata(Map.of(
                WireMockPactMetadata.METADATA_ATTR,
                new WireMockPactMetadata().setProvider(PROVIDER)));
    }
}
