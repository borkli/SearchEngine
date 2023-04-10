package searchengine.controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.ApiResponse;
import searchengine.dto.SearchFilter;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api/")
@AllArgsConstructor
public class ApiController extends CommonController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("startIndexing")
    public ApiResponse startIndexing() {
        indexingService.startIndexing();
        return okResponse();
    }

    @GetMapping("stopIndexing")
    public ApiResponse stopIndexing() {
        indexingService.stopIndexing();
        return okResponse();
    }

    @PostMapping("indexPage")
    public ApiResponse indexPage(@RequestParam String url) {
        indexingService.indexPage(url);
        return okResponse();
    }

    @GetMapping("search")
    public ApiResponse search(SearchFilter filter) {
        return searchService.search(filter);
    }
}
