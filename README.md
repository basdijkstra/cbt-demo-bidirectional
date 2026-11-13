# Bidirectional contract testing with PactFlow

A small demo of bidirectional contract testing (BDCT) between a Java consumer and an ASP.NET Core provider.
The consumer contract is generated from WireMock traffic instead of written with Pact, and the provider
contract is an OpenAPI spec.

```
consumer/   Java 21 client for the Customer API, tested against a WireMock simulation of the provider
provider/   ASP.NET Core minimal API (CustomerApi) and its OpenAPI spec (oas/openapi.yaml)
pacts/      Consumer contract generated from the WireMock traffic, published from here to PactFlow
```

| Role     | PactFlow name             | Contract |
|----------|---------------------------|----------|
| Consumer | `OSCustomerConsumerBDCT`  | `pacts/OSCustomerConsumerBDCT-SAPCustomerProviderBDCT.json` |
| Provider | `SAPCustomerProviderBDCT` | `provider/oas/openapi.yaml` |

## How it works

1. **Consumer.** `CustomerClientTest` tests `CustomerClient` against a WireMock server that simulates the provider.
   Every request WireMock serves is passed to [wiremock-pact-lib](https://github.com/tomasbjerre/wiremock-pact),
   which writes the requests and stubbed responses to a Pact file after the tests have run.
2. **Provider.** The provider publishes its OpenAPI spec, together with the results of whatever it does to show
   that the API actually matches that spec.
3. **PactFlow** compares the two contracts (the cross-contract check). No pact is replayed against the provider.

## The provider

Copied as-is from the consumer-driven demo.

`GET /customers/{id}`

| Situation                     | Response |
|-------------------------------|----------|
| Customer exists               | `200` with customer JSON |
| Customer ID not found         | `404` (problem details) |
| Customer ID is not an integer | `400` (problem details) |

Run it with `dotnet run --project provider/CustomerApi`; customer `12345` is seeded in memory.

`provider/CustomerApi.Tests` holds the Pact verification test from the consumer-driven demo. It is not part of
the bidirectional flow: with BDCT the provider does not verify pacts, PactFlow compares them with the OpenAPI spec.

## The consumer contract

`consumer/src/test/java/customer/CustomerClientTest.java` stubs three interactions, which become the contract:

- **200**: `GET /customers/12345` returns `id`, `name`, `status` and `address`. Only the fields the consumer uses
  are stubbed, so `email`, `phone` and `lastModified` are not in the contract and the provider is free to change them.
- **404**: `GET /customers/99999`, status code only.
- **400**: `GET /customers/abc`, status code only.

The contract contains example values, not matchers. That is enough for BDCT: PactFlow checks the values against
the schemas in the OpenAPI spec.

## Running

Requires Java 21+, Maven, and the .NET 10 SDK.

```bash
# Consumer: run the tests, which write pacts/OSCustomerConsumreBDCT-SAPCustomerProviderBDCT.json
cd consumer
mvn test

# Provider: run the API
cd ../provider
dotnet run --project CustomerApi
```

Publishing to PactFlow (for example with the [Pact CLI](https://docs.pact.io/implementation_guides/cli)) is not
part of this project:

```bash
# Consumer contract
pact-broker publish pacts/OSCustomerConsumerBDCT-SAPCustomerProviderBDCT.json \
  --consumer-app-version <version> --branch <branch>

# Provider contract
pactflow publish-provider-contract provider/oas/openapi.yaml \
  --provider SAPCustomerProviderBDCT --provider-app-version <version> --branch <branch> \
  --content-type application/yaml --verification-exit-code 0 \
  --verification-results <results file> --verification-results-content-type text/plain \
  --verifier <tool>
```

From `provider` folder:

```bash
pactflow publish-provider-contract oas\openapi.yaml --provider SAPCustomerProviderBDCT --provider-app-version 1.0.0 --branch main --content-type application/yaml --verification-success --verification-results oas\testresults.txt --verification-results-content-type text/plain --verifier bas
```