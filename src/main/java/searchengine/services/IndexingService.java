package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.model.error.ApplicationError;
import searchengine.repository.IndexRepository;
import searchengine.repository.JdbcRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SitePageRepository;
import searchengine.repository.SiteRepository;
import searchengine.task.PageRecursiveTask;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@AllArgsConstructor
public class IndexingService {

    private final int PROCESSORS = Runtime.getRuntime().availableProcessors();

    private final Executor executor = Executors.newFixedThreadPool(PROCESSORS);
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final SitePageRepository sitePageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final JdbcRepository jdbcRepository;
    private List<PageRecursiveTask> tasks;

    public void startIndexing() {
        if (!tasks.isEmpty()) {
            throw new ApplicationError("Индексация уже запущена");
        }

        List<String> actualUrls = new ArrayList<>();
        List<SiteConfig> sitesList = getSites();
        for (SiteConfig siteConfig : sitesList) {
            checkSiteConfig(siteConfig);
            actualUrls.add(siteConfig.getUrl());
            executor.execute(
                () -> parsePages(siteConfig)
            );
        }
        executor.execute(
            () -> deleteNotActualSites(actualUrls)
        );
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

    private void parsePages(SiteConfig siteConfig) {
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
            indexRepository.deleteBySiteId(List.of(site.getId()));
            lemmaRepository.deleteBySiteId(List.of(site.getId()));
            sitePageRepository.deleteBySiteId(List.of(site.getId()));
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

    private void deleteNotActualSites(List<String> actualUrls) {
        List<Long> ids = siteRepository.getNotActualSites(actualUrls);
        if (ids.isEmpty()) {
            return;
        }
        indexRepository.deleteBySiteId(ids);
        lemmaRepository.deleteBySiteId(ids);
        sitePageRepository.deleteBySiteId(ids);
        siteRepository.delete(ids);
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
        if (siteConfig.getName() == null || siteConfig.getName().isBlank()) {
            throw new ApplicationError("Название сайта не может быть пустым");
        }
        String url = siteConfig.getUrl();
        if (url == null || url.isBlank()) {
            throw new ApplicationError("URL не может быть пустым");
        }
        siteConfig.setUrl(
            url.endsWith("/") ? url.substring(0, url.length() - 1) : url
        );
    }
}
