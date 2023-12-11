package searchengine.services.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Snippet {

    public static List<Integer> indexingLemmas(List<String> contentList, List<String> queryList, String content) {
        List<Integer> indexList = new ArrayList<>();
        for (String wordContent : contentList) {
            if (isQueryWord(wordContent, queryList)) {
                int index = getIndex(content, wordContent, indexList);
                indexList.add(index);
            }
        }
        return indexList;
    }

    private static boolean isQueryWord(String wordContent, List<String> queryList) {
        for (String queryWord : queryList) {
            if (wordContent.equals(queryWord)) {
                return true;
            }
        }
        return false;
    }

    private static int getIndex(String content, String word, List<Integer> indexList) {
        int index;
        index = content.indexOf(word);
        if (indexList.contains(index)) {
            index = content.indexOf(word, indexList.get(indexList.size() - 1) + 1);
        }
        return index;
    }

    public static String getSnippetText(String content, Integer startIndex, String phrase) {
        return snippetText(content, startIndex, phrase);
    }

    public static String getSnippetText(String content, Integer startIndex) {
        return snippetText(content, startIndex, null);
    }

    private static String snippetText(String content, Integer startIndex, String phrase) {
        int start = startIndex;
        int end = content.indexOf(" ", start);
        String word;
        if (phrase == null) {
            word = content.substring(start, end);
        } else {
            word = phrase;
        }

        int startSnippetPoint;
        int lastSnippetPoint;
        if (content.lastIndexOf(" ", start - 30) != -1) {
            startSnippetPoint = content.lastIndexOf(" ", start - 30);
        } else {
            startSnippetPoint = start;
        }

        if (content.indexOf(" ", end + 30) != -1) {
            lastSnippetPoint = content.indexOf(" ", end + 30);
        } else {
            lastSnippetPoint = content.indexOf(" ", end);
        }
        return content.substring(startSnippetPoint, lastSnippetPoint).trim().replaceAll(Pattern.quote(word), "<b>" + word + "</b>");
    }
}
