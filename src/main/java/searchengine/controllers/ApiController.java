package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.ApiResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SearchFilter;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api/")
public class ApiController extends CommonController {

    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private IndexingService indexingService;
    @Autowired
    private SearchService searchService;

    @GetMapping("statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("startIndexing")
    public ResponseEntity<?> startIndexing() {
        indexingService.startIndexing();
        return okResponse();
    }

    @GetMapping("stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        indexingService.stopIndexing();
        return okResponse();
    }

    @PostMapping("indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        indexingService.indexPage(url);
        return okResponse();
    }

    @GetMapping("search")
    public ResponseEntity<ApiResponse> search(SearchFilter filter) {
        return new ResponseEntity<>(searchService.search(filter), HttpStatus.OK);
    }
}
