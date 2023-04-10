package searchengine.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class ApiResponse {

    public ApiResponse(boolean result) {
        this.result = result;
    }

    private boolean result;
    private int count;
    private List<SearchResult> data;
    private String error;
}
