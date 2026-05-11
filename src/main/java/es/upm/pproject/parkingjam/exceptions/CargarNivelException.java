package es.upm.pproject.parkingjam.exceptions;

public class CargarNivelException extends RuntimeException {
    public CargarNivelException(String message, Throwable causa) {
        super(message,causa);
    }
}
