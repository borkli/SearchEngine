package searchengine.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import searchengine.model.error.ApplicationError;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class LemmaUtils {

    private static final String HREF_TAG = "a[href]";
    private static final String EXCESS_TAGS = "<br>|<p>|&[a-z]+;";
    private static final String SPACE = "\\s+";
    private static final String INVALID_SYMBOLS_RUS = "[^а-яё\\s]";
    private static final String INVALID_SYMBOLS_ENG = "[^a-z\\s]";
    private static final Pattern SERVICE_PARTS_RUS = Pattern.compile("СОЮЗ|МЕЖД|ПРЕДЛ|ЧАСТ");
    private static final Pattern SERVICE_PARTS_ENG = Pattern.compile("PN|PREP|PART|ARTICLE");

    private static LuceneMorphology luceneMorphRus;
    private static LuceneMorphology luceneMorphEng;

    static {
        try {
            luceneMorphRus = new RussianLuceneMorphology();
            luceneMorphEng = new EnglishLuceneMorphology();
        } catch (Exception ex) {
            log.error("Create lemmatization", ex);
            throw new ApplicationError("Создание лемматизатора не удалось");
        }
    }

    public static HashMap<String, Integer> lemmatization(Document document) {
        String text = cleanHtmlBody(document);
        return lemmatization(text, false);
    }

    public static HashMap<String, Integer> lemmatization(String text, boolean addPosition) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        if (text.isBlank()) {
            return lemmas;
        }
        lemmatization(
            text, luceneMorphRus,
            lemmas, SERVICE_PARTS_RUS,
            INVALID_SYMBOLS_RUS, addPosition
        );
        lemmatization(
            text, luceneMorphEng,
            lemmas, SERVICE_PARTS_ENG,
            INVALID_SYMBOLS_ENG, addPosition
        );
        return lemmas;
    }

    private static void lemmatization(String text, LuceneMorphology luceneMorph,
                                      HashMap<String, Integer> lemmas,
                                      Pattern serviceParts, String invalid,
                                      boolean addPosition) {
        String[] words = textToArray(text, invalid);
        for (int i = 0; i < words.length; i++) {
            String wordFinal = words[i].trim();
            if (wordFinal.isBlank()) {
                continue;
            }
            List<String> info = luceneMorph.getMorphInfo(wordFinal);
            if (info.isEmpty() || serviceParts.matcher(info.get(0)).find()) {
                continue;
            }
            String lemma = luceneMorph.getNormalForms(wordFinal).get(0);
            if (!addPosition) {
                lemmas.compute(
                    lemma, (k, v) -> v == null ? 1 : ++v
                );
            } else {
                lemmas.put(lemma, i);
            }
        }
    }

    public static String getLemma(String word) {
        String rusWord = word.toLowerCase()
            .replaceAll(INVALID_SYMBOLS_RUS, "").trim();
        if (!rusWord.isBlank()) {
            return luceneMorphRus.getNormalForms(rusWord).get(0);
        } else {
            String engWord = word.toLowerCase()
                .replaceAll(INVALID_SYMBOLS_ENG, "").trim();
            return !engWord.isBlank() ?
                luceneMorphEng.getNormalForms(engWord).get(0) : null;
        }
    }

    public static String cleanHtmlBody(Document document) {
        document.body().select(HREF_TAG).remove();
        String body = document.body().html()
            .replaceAll(EXCESS_TAGS, " ");
        return Jsoup.clean(
            body, "", Safelist.none(),
            new Document.OutputSettings().prettyPrint(true)
        ).trim();
    }

    private static String[] textToArray(String text, String remove) {
        return text
            .toLowerCase()
            .replaceAll(remove, " ")
            .trim().split(SPACE);
    }
}
