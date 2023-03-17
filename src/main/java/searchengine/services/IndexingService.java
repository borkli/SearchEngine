package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.model.error.ApplicationError;
import searchengine.repository.*;
import searchengine.task.PageRecursiveTask;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
public class IndexingService {

    private final int PROCESSORS = Runtime.getRuntime().availableProcessors();
    private final Executor executor = Executors.newFixedThreadPool(PROCESSORS);
    private List<PageRecursiveTask> tasks = new ArrayList<>();

    @Autowired
    private SitesList sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private SitePageRepository sitePageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private JdbcRepository jdbcRepository;

    public void startIndexing() {
        if (!tasks.isEmpty()) {
            throw new ApplicationError("Индексация уже запущена");
        }

        List<SiteConfig> sitesList = getSites();
        for (SiteConfig siteConfig : sitesList) {
            checkSiteConfig(siteConfig);
            executor.execute(
                () -> parsePages(siteConfig)
            );
        }
    }

    public void parsePages(SiteConfig siteConfig) {
        Site site = updateSite(siteConfig, true);
        PageRecursiveTask task = new PageRecursiveTask(
            site, siteConfig.getUrl(),
            siteRepository, sitePageRepository,
            lemmaRepository, jdbcRepository
        );
        tasks.add(task);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.submit(task);
        if (task.join()) {
            siteRepository.updateStatus(SiteStatus.INDEXED.name(), site.getId());
        }
        tasks.clear();
    }

    private Site updateSite(SiteConfig siteConfig, boolean delete) {
        String url = siteConfig.getUrl();
        Site site = siteRepository.getByUrl(url);
        if (site == null) {
            site = new Site(
                siteConfig.getUrl(), siteConfig.getName(),
                SiteStatus.INDEXING, LocalDateTime.now(), ""
            );
            return siteRepository.saveAndFlush(site);
        } else if (delete) {
            indexRepository.deleteBySiteId(site.getId());
            lemmaRepository.deleteBySiteId(site.getId());
            sitePageRepository.deleteBySiteId(site.getId());
            site.setName(siteConfig.getName())
                .setStatus(SiteStatus.INDEXING)
                .setStatusTime(LocalDateTime.now())
                .setLastError("");
            return siteRepository.saveAndFlush(site);
        }
        if (site.getId() == null) {
            log.error("Site not found: " + url);
            throw new ApplicationError("Сайт не найден");
        }
        return site;
    }

    public void stopIndexing() {
        if (tasks.isEmpty()) {
            throw new ApplicationError("Индексация не запущена");
        }
        for (PageRecursiveTask task : tasks) {
            if (!task.isCancelled()) {
                task.stopIndexing();
            }
        }
    }

    public void indexPage(String url) {
        boolean outsideUrl = true;
        for (SiteConfig siteConfig : getSites()) {
            String parentUrl = siteConfig.getUrl().trim();
            if (parentUrl.isBlank()) {
                continue;
            }
            if (url.startsWith(parentUrl)) {
                outsideUrl = false;
                executor.execute(
                    () -> {
                        Site site = updateSite(siteConfig, false);
                        new PageRecursiveTask(
                            site, url, siteRepository,
                            sitePageRepository, lemmaRepository,
                            jdbcRepository, indexRepository,
                            url.equals(parentUrl), parentUrl
                        ).indexPage();
                    }
                );
                break;
            }
        }
        if (outsideUrl) {
            throw new ApplicationError("Неизвестный URL");
        }
    }

    private List<SiteConfig> getSites() {
        List<SiteConfig> sitesList = sites.getSites();
        if (CollectionUtils.isEmpty(sitesList)) {
            log.error("SiteConfig list is empty");
            throw new ApplicationError("Список сайтов не может быть пустым");
        }
        return sitesList;
    }

    private void checkSiteConfig(SiteConfig siteConfig) {
        if (siteConfig.getUrl().isBlank()) {
            throw new ApplicationError("URL не может быть пустым");
        }
        if (siteConfig.getName().isBlank()) {
            throw new ApplicationError("Название сайта не может быть пустым");
        }
    }
}
