package searchengine.services;

import searchengine.response.SearchResponse;

import java.io.IOException;

public interface SearchService {
    boolean search(String query, String url, int offset, int limit);
    SearchResponse getResponse();
}
