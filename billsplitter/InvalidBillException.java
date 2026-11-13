package billsplitter;

// Creating own exception subclass
public class InvalidBillException extends Exception {
    public InvalidBillException(String message) {
        super(message);
    }
}