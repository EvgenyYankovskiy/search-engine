package searchengine.morphology;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class Morphology {
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private LuceneMorphology luceneMorphology;

    public Morphology(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public static Morphology getInstance() {
        LuceneMorphology morphology = null;
        try {
            morphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Morphology(morphology);
    }

    public HashMap<String, Integer> russianLemmas(String text) {
        HashMap<String, Integer> lemmas = new HashMap<>();

        String[] words = wordsArray(text);

        for (String word : words) {
            if (word.isBlank() || word.length() <= 2) {
                continue;
            }

            List<String> morphInfo = luceneMorphology.getMorphInfo(word);
            if (isParticles(morphInfo)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String wordFormNoun = normalForms.get(0);
            if (lemmas.containsKey(wordFormNoun)) {
                lemmas.put(wordFormNoun, lemmas.get(wordFormNoun) + 1);
            } else {
                lemmas.put(wordFormNoun, 1);
            }
        }
        return lemmas;
    }

    public Set<String> getLemmaSet(String text) {
        String[] textArray = wordsArray(text);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            List<String> wordMorphInfo = luceneMorphology.getMorphInfo(word);
            if (!word.isEmpty() && isCorrectWordForm(word) && !isParticles(wordMorphInfo)) {
                lemmaSet.add(luceneMorphology.getNormalForms(word).get(0));
            }
        }
        return lemmaSet;
    }

    private String[] wordsArray(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isParticles(List<String> morphsInfo) {
        for (String morphInfo : morphsInfo) {
            if (morphInfo.contains("ПРЕДЛ")
                    || morphInfo.contains("СОЮЗ")
                    || morphInfo.contains("МЕЖД")
                    || morphInfo.contains("ЧАСТ")
                    || morphInfo.contains("МС")) {
                return true;
            }
        }
        return false;
    }

    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) {
                return false;
            }
        }
        return true;
    }

    public List<String> getLemmasText(String content) {
        String[] words = wordsArray(content);
        List<String> lemmas = new ArrayList<>();
        for (String word : words) {
            if (word.isBlank() || word.length() <= 2) {
                continue;
            }

            List<String> morphInfo = luceneMorphology.getMorphInfo(word);
            if (isParticles(morphInfo)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            lemmas.add(normalForms.get(0));
        }
        return lemmas;
    }

    public String cleanHtml(String htmlPage) {
        return htmlPage.replaceAll("<[^>]*>", "");
    }
}
