package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.ApiResponse;
import searchengine.dto.SearchResult;
import searchengine.model.*;
import searchengine.model.error.ApplicationError;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SitePageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.LemmaUtils;
import searchengine.utils.SnippetUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private SitePageRepository sitePageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    public ApiResponse search(SearchFilter filter) {
        if (filter.getQuery() == null || filter.getQuery().trim().isBlank()) {
            throw new ApplicationError("Поисковый запрос не может быть пустым");
        }
        String query = filter.getQuery().trim();
        ApiResponse response = new ApiResponse(true, 0, new ArrayList<>());
        Site site = searchSite(filter.getSite());
        HashMap<String, Integer> sourceLemmas = LemmaUtils.lemmatization(query, false);
        List<Lemma> existLemmas =
            site == null ?
                lemmaRepository.getByLemma(sourceLemmas.keySet()) :
                lemmaRepository.getByLemma(site.getId(), sourceLemmas.keySet());
        List<SitePage> matchPages = searchPages(existLemmas);
        if (matchPages.isEmpty()) {
            return response;
        }
        List<SearchResult> result = computeRelevance(
            matchPages, existLemmas, sourceLemmas.keySet()
        );
        return response
            .setCount(result.size())
            .setData(result);
    }

    private Site searchSite(String url) {
        Site site = null;
        if (url != null && !url.isBlank()) {
            url = url.endsWith("/") ?
                url.substring(0, url.length() - 1) : url;
            site = siteRepository.getByUrl(url.trim());
            if (site == null) {
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

    private List<SitePage> searchPages(List<Lemma> existLemmas) {
        List<SitePage> matchPages = new ArrayList<>();
        for (int i = 0; i < existLemmas.size(); i++) {
            Lemma lemma = existLemmas.get(i);
            if (i == 0) {
                matchPages = sitePageRepository.getByLemma(lemma.getId());
            } else {
                List<Long> ids = getIds(matchPages);
                matchPages = sitePageRepository.getByLemma(
                    lemma.getId(), ids
                );
            }
            if (matchPages.isEmpty()) {
                break;
            }
        }
        return matchPages;
    }

    private List<SearchResult> computeRelevance(List<SitePage> pages,
                                                List<Lemma> existLemmas,
                                                Set<String> sourceLemmas) {
        List<SearchResult> result = new ArrayList<>();
        List<Long> pageIds = getIds(pages);
        int totalRelevance = indexRepository.totalRelevance(pageIds);
        Map<Long, Double> relevanceByLemmas = indexRepository
            .relevanceByLemmas(pageIds, getIds(existLemmas))
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
            String snippet = SnippetUtils.generateSnippet(document, sourceLemmas);
            result.add(
                new SearchResult(
                    site.getUrl(), site.getName(),
                    page.getPath(), document.title(),
                    snippet, pageRelevance / totalRelevance
                )
            );
        }
        return result;
    }

    private <T> List<Long> getIds(List<T> objects) {
        return objects.stream().map(
            obj -> {
                if (obj instanceof SitePage) {
                    return ((SitePage) obj).getId();
                } else if (obj instanceof Lemma) {
                    return ((Lemma) obj).getId();
                }
                throw new ApplicationError("Неподдерживаемый тип");
            }
        ).toList();
    }
}
