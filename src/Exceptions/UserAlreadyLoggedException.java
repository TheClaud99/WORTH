package Exceptions;

public class UserAlreadyLoggedException extends Exception {

    public UserAlreadyLoggedException() {
        super("Sei già loggato");
    }
    public UserAlreadyLoggedException(String message) {
        super(message);
    }
}