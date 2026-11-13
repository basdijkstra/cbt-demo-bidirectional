package customer;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String id) {
        super(String.format("Customer with ID %s was not found", id));
    }
}
