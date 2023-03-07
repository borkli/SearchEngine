package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SitePageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsService {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private SitePageRepository sitePageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;

    public StatisticsResponse getStatistics() {
        List<Site> sites = siteRepository.findAll();
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.size());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (Site site : sites) {
            long pageCountBy = sitePageRepository.countBy(site.getId());
            long lemmaCountBy = lemmaRepository.countBy(site.getId());
            total.setPages(total.getPages() + pageCountBy)
                .setLemmas(total.getLemmas() + lemmaCountBy);
            if (!total.isIndexing() && site.getStatus() == SiteStatus.INDEXING) {
                total.setIndexing(true);
            }
            detailed.add(
                new DetailedStatisticsItem()
                    .setName(site.getName())
                    .setUrl(site.getUrl())
                    .setStatus(site.getStatus().name())
                    .setStatusTime(site.getStatusTime())
                    .setError(
                        site.getLastError() != null ? site.getLastError() : ""
                    )
                    .setPages(pageCountBy)
                    .setLemmas(lemmaCountBy)
            );
        }

        StatisticsData data = new StatisticsData();
        data.setTotal(total)
            .setDetailed(detailed);
        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
