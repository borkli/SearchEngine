package searchengine.model.error;

public class ApplicationError extends RuntimeException {

    public ApplicationError(String message) {
        super(message);
    }
}
