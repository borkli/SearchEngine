package searchengine.dto.statistics;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TotalStatistics {
    private int sites;
    private long pages;
    private long lemmas;
    private boolean indexing = false;
}
