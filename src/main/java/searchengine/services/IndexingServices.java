package searchengine.services;

public interface IndexingServices {
    boolean startIndexing();
    boolean stopIndexing();
    boolean indexPage(String url);
}
