package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.response.IndexingResponse;
import searchengine.response.SearchResponse;
import searchengine.response.StatisticsResponse;
import searchengine.services.IndexingServices;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;
import searchengine.services.impl.IndexingServiceImpl;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingServices indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingServiceImpl indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok().body(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        if (indexingService.startIndexing()) {
            return ResponseEntity.ok().body(new IndexingResponse(true, ""));
        }
        return ResponseEntity.badRequest().body(new IndexingResponse(false, "Индексация уже запущенна"));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return ResponseEntity.ok().body(new IndexingResponse(true, ""));
        }
        return ResponseEntity.badRequest().body(new IndexingResponse(false, "Индексация не запущена"));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        if (indexingService.indexPage(url)) {
            return ResponseEntity.ok().body(new IndexingResponse(true, ""));
        }
        return ResponseEntity.badRequest().body(new IndexingResponse(false,
                "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(defaultValue = "") String query, @RequestParam(defaultValue = "") String site,
                                                 @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
        if (searchService.search(query, site, offset, limit)) {
            return ResponseEntity.ok().body(searchService.getResponse());
        }
        return ResponseEntity.badRequest().body(new SearchResponse(false, "Задан пустой поисковый запрос", null, null));
    }
}
