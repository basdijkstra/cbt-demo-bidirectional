package customer;

public class InvalidCustomerIdException extends RuntimeException {

    public InvalidCustomerIdException(String id) {
        super(String.format("Customer ID %s was rejected by the provider", id));
    }
}
