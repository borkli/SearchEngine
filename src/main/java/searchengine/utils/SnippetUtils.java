package searchengine.utils;

import org.jsoup.nodes.Document;

import java.util.Set;
import java.util.StringJoiner;

public class SnippetUtils {

    private static final int WORDS_COUNT = 5;
    private static final String ELLIPSIS = "...";

    public static String generateSnippet(Document document, Set<String> sourceLemmas) {
        StringJoiner joiner = new StringJoiner(" ");
        document.select("title").remove();
        String cleanText = LemmaUtils.cleanHtmlBody(document);
        if (cleanText.isBlank()) {
            return "";
        }
        int limit = 0;
        String[] words = cleanText.toLowerCase().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String lemma = LemmaUtils.getLemma(word);
            if (lemma == null) {
                continue;
            }
            if (sourceLemmas.contains(lemma)) {
                int start = Math.max(0, i - WORDS_COUNT);
                int end = Math.min(words.length, i + WORDS_COUNT);
                for (int j = start; j < end; j++) {
                    String wordByIndex = words[j];
                    if (wordByIndex.equals(word) || sourceLemmas.contains(wordByIndex)) {
                        wordByIndex = "<b>" + wordByIndex + "</b>";
                    }
                    joiner.add(wordByIndex);
                }
                joiner.add(ELLIPSIS);
                if (++limit >= sourceLemmas.size()) {
                    break;
                }
                if (start == 0 && end == words.length) {
                    break;
                }
            }
        }
        return joiner.toString();
    }
}
