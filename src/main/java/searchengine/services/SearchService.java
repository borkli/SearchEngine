package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import searchengine.dto.ApiResponse;
import searchengine.dto.SearchFilter;
import searchengine.dto.SearchResult;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.model.SitePage;
import searchengine.model.SiteStatus;
import searchengine.model.error.ApplicationError;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SitePageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.LemmaUtils;
import searchengine.utils.SnippetUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class SearchService {

    private final int DEFAULT_OFFSET = 0;
    private final int DEFAULT_LIMIT = 20;

    private final SiteRepository siteRepository;
    private final SitePageRepository sitePageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public ApiResponse search(SearchFilter filter) {
        if (filter.getQuery() == null || filter.getQuery().trim().isBlank()) {
            throw new ApplicationError("Поисковый запрос не может быть пустым");
        }
        String query = filter.getQuery().trim();
        ApiResponse response = new ApiResponse(true);
        Site site = searchSite(filter.getSite());
        HashMap<String, Integer> sourceLemmas = LemmaUtils.lemmatization(query, false);
        List<Lemma> existLemmas =
            site == null ?
                lemmaRepository.getByLemma(sourceLemmas.keySet()) :
                lemmaRepository.getByLemma(site.getId(), sourceLemmas.keySet());
        if (existLemmas.isEmpty()) {
            return response
                .setCount(0)
                .setData(new ArrayList<>());
        }
        List<SitePage> matchPages = searchPages(existLemmas, filter, response);
        if (matchPages.isEmpty()) {
            return response;
        }
        List<SearchResult> result = collectResultByRelevance(
            matchPages, existLemmas, sourceLemmas.keySet()
        );
        return response
            .setData(result);
    }

    private Site searchSite(String url) {
        Site site = null;
        if (url != null && !url.isBlank()) {
            url = url.endsWith("/") ?
                url.substring(0, url.length() - 1) : url;
            site = siteRepository.getByUrl(url.trim());
            if (site == null) {
                log.error("Site not found: " + url);
                throw new ApplicationError("Сайт не найден");
            }
            if (site.getStatus() != SiteStatus.INDEXED) {
                throw new ApplicationError(
                    "Сайт не проиндексирован: " + site.getStatus()
                );
            }
        }
        return site;
    }

    private List<SitePage> searchPages(List<Lemma> existLemmas,
                                       SearchFilter filter,
                                       ApiResponse response) {
        List<SitePage> matchPages = new ArrayList<>();
        List<Long> idsByLemma = sitePageRepository.getIdsByLemma(
            existLemmas.get(0).getId(), Sort.by("id").ascending()
        );
        if (idsByLemma.isEmpty()) {
            return matchPages;
        }
        for (int i = 0; i < existLemmas.size(); i++) {
            Lemma lemma = existLemmas.get(i);
            if (i != existLemmas.size() - 1) {
                if (i == 0) {
                    continue;
                }
                idsByLemma = sitePageRepository.getIdsByLemma(lemma.getId(), idsByLemma);
            } else {
                response.setCount(
                    sitePageRepository.countByLemma(lemma.getId(), idsByLemma)
                );
                matchPages = sitePageRepository.getByLemma(
                    lemma.getId(), idsByLemma, getLimit(filter)
                );
            }
            if (idsByLemma.isEmpty()) {
                break;
            }
        }
        return matchPages;
    }

    private PageRequest getLimit(SearchFilter filter) {
        int offset = filter.getOffset() != null ?
            filter.getOffset() : DEFAULT_OFFSET;
        int limit = filter.getLimit() != null ?
            filter.getLimit() : DEFAULT_LIMIT;
        return PageRequest.of(
            offset != 0 ? limit / offset : 0, limit
        );
    }

    private List<SearchResult> collectResultByRelevance(List<SitePage> pages,
                                                        List<Lemma> existLemmas,
                                                        Set<String> sourceLemmas) {
        List<SearchResult> result = new ArrayList<>();
        List<Long> pageIds = pages.stream()
            .map(SitePage::getId).toList();
        int totalRelevance = indexRepository.totalRelevance(pageIds);
        Map<Long, Double> relevanceByLemmas = indexRepository
            .relevanceByLemmas(
                pageIds, existLemmas.stream().map(Lemma::getId).toList()
            )
            .stream()
            .collect(Collectors.toMap(
                o -> (Long) o[0], o -> (Double) o[1]
            ));
        for (SitePage page : pages) {
            Double pageRelevance = relevanceByLemmas.get(page.getId());
            if (pageRelevance == null || pageRelevance == 0) {
                continue;
            }
            Site site = page.getSite();
            Document document = Jsoup.parse(page.getContent());
            String title = document
                .select("title")
                .remove().text();
            String snippet = SnippetUtils.generateSnippet(document, sourceLemmas);
            result.add(
                new SearchResult(
                    site.getUrl(), site.getName(),
                    page.getPath(), title,
                    snippet, pageRelevance / totalRelevance
                )
            );
        }
        return result;
    }
}
