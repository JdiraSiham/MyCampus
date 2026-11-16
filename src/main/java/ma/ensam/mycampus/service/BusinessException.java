package ma.ensam.mycampus.service;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}