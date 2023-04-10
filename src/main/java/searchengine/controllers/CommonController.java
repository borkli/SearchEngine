package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.ApiResponse;
import searchengine.model.error.ApplicationError;

public class CommonController {

    public ApiResponse okResponse() {
        return new ApiResponse(true);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception ex) {
        ApiResponse response = new ApiResponse(false);
        if (ex instanceof ApplicationError) {
            response.setError(ex.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        response.setError("Неизвестная ошибка");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
