package searchengine.utils;

import org.jsoup.nodes.Document;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class SnippetUtils {

    private static final int INITIAL_WORDS_COUNT = 30;
    private static final String ELLIPSIS = "... ";

    private static int listSize;
    private static int wordsSize;
    private static Set<Integer> usedIndexes;

    public static String generateSnippet(Document document, Set<String> lemmas) {
        String[] words = createWordsArray(document);
        if (lemmas.isEmpty() || words.length == 0) {
            return "";
        }
        wordsSize = words.length;
        listSize = lemmas.size();
        usedIndexes = new HashSet<>();

        Set<Integer> lemmaIndexes = findLemmaIndexes(words, lemmas);
        collectIndexes(lemmaIndexes);
        return collectSnippets(words, lemmaIndexes);
    }

    private static String[] createWordsArray(Document document) {
        String text = LemmaUtils.cleanHtmlBody(document);
        if (text.isBlank()) {
            return new String[0];
        }
        return text.toLowerCase().split("\\s+");
    }

    private static Set<Integer> findLemmaIndexes(String[] words, Set<String> lemmas) {
        Set<Integer> lemmaIndexes = new HashSet<>();
        for (int i = 0; i < wordsSize; i++) {
            String word = words[i];
            String lemma = LemmaUtils.getLemma(word);
            if (lemma == null) {
                continue;
            }
            if (lemmas.contains(lemma)) {
                lemmaIndexes.add(i);
                if (lemmas.size() == 1) {
                    break;
                }
            }
        }
        return lemmaIndexes;
    }

    private static void collectIndexes(Set<Integer> lemmaIndexes) {
        for (Integer lemmaIndex : lemmaIndexes) {
            int start = start(lemmaIndex);
            int end = end(lemmaIndex);
            if (insideRange(start, end, lemmaIndex)) {
                return;
            }
            usedIndexes.add(lemmaIndex);
        }
    }

    private static String collectSnippets(String[] words, Set<Integer> lemmaIndexes) {
        StringJoiner joiner = new StringJoiner(" ");
        listSize = usedIndexes.size();
        for (Integer usedIndex : usedIndexes) {
            int start = start(usedIndex);
            int end = end(usedIndex);
            for (int j = start; j < end; j++) {
                String wordByIndex = words[j];
                if (lemmaIndexes.contains(j)) {
                    wordByIndex = "<b>" + wordByIndex + "</b>";
                }
                joiner.add(wordByIndex);
            }
            joiner.add(ELLIPSIS);
        }
        return joiner.toString();
    }

    private static int start(int index) {
        return Math.max(0, index - wordsCount());
    }

    private static int end(int index) {
        return Math.min(wordsSize, index + wordsCount());
    }

    private static int wordsCount() {
        return INITIAL_WORDS_COUNT / listSize;
    }

    private static boolean insideRange(int start, int end, int index) {
        if (usedIndexes.isEmpty()) {
            return false;
        }
        for (Integer lemmaIndex : usedIndexes) {
            int lemmaStart = start(lemmaIndex);
            int lemmaEnd = end(lemmaIndex);
            boolean inside =
                (lemmaStart < start && start < lemmaEnd) ||
                    (lemmaStart < index && index < lemmaEnd) ||
                    (lemmaStart < end && end < lemmaEnd);
            if (inside) {
                return true;
            }
        }
        return false;
    }
}
