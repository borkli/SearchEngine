package searchengine.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SearchFilter {

    private String query;

    private String site;

    private int offset;

    private int limit;
}
