package imagestash.j;

@SuppressWarnings("serial")
public class AddressFormatException extends RuntimeException {
    public AddressFormatException() {
    }

    public AddressFormatException(String message) {
        super(message);
    }

    public AddressFormatException(Throwable cause) {
        super(cause);
    }

    public AddressFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
