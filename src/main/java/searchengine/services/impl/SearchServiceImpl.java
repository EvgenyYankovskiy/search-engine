package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PagesRepository;
import searchengine.model.repository.SearchIndexRepository;
import searchengine.model.repository.SitesRepository;
import searchengine.morphology.Morphology;
import searchengine.response.SearchResponse;
import searchengine.response.search.DataInfo;
import searchengine.services.SearchService;
import searchengine.services.utils.Snippet;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final SitesRepository sitesRepository;
    private final SearchIndexRepository searchIndexRepository;
    private final PagesRepository pagesRepository;
    private List<DataInfo> dataInfoList = new ArrayList<>();

    @Override
    public SearchResponse getResponse() {
        return new SearchResponse(true, "", dataInfoList.size(), dataInfoList);
    }

    @Override
    public boolean search(String query, String url, int offset, int limit) {
        if (query.isEmpty()) {
            return false;
        }
        dataInfoList.clear();

        if (url.isEmpty()) {
            List<Site> siteList = sitesRepository.findAll();
            for (Site site : siteList) {
                searchBySite(site, query);
            }
        } else {
            Site site = sitesRepository.findByUrl(url);
            searchBySite(site, query);
        }

        if (!dataInfoList.isEmpty()) {
            getRelativeRelevance(dataInfoList);
            Collections.sort(dataInfoList);
            dataInfoList = limitList(dataInfoList, limit, offset);
        }
        return true;
    }

    private void searchBySite(Site site, String query) {
        Morphology morphology = Morphology.getInstance();
        Set<String> lemmaSet = morphology.getLemmaSet(query);

        int siteId = site.getId();

        long maxFreq = lemmaRepository.findMaxFreqBySiteId(siteId);
        List<Lemma> lemmaList = new ArrayList<>();
        for (String lemma : lemmaSet) {
            Lemma repositoryLemma = lemmaRepository.findLemmaByMaxFreq(lemma, siteId, maxFreq);
            if (repositoryLemma != null) {
                lemmaList.add(repositoryLemma);
            }
        }

        List<Page> pageList = getPageList(lemmaList);
        List<Thread> threads = new ArrayList<>();
        for (Page page : pageList) {
            Thread thread = new Thread(() -> {
                DataInfo dataInfo = new DataInfo();

                String body = getBody(page.getContent());
                String title = getTitle(page.getContent());
                String snippet = getSnippet(body, query, morphology);
                if (snippet.isEmpty()) {
                    snippet = title;
                }

                dataInfo.setSite(site.getUrl().endsWith("/") ? site.getUrl().substring(0, site.getUrl().length() - 1) : site.getUrl());
                dataInfo.setUri(page.getPath());
                dataInfo.setTitle(title);
                dataInfo.setSiteName(site.getName());
                dataInfo.setSnippet(snippet);
                dataInfo.setRelevance(getAbsolutRelevance(page, lemmaList));
                dataInfoList.add(dataInfo);
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getSnippet(String content, String query, Morphology morphology) {
        StringBuilder snippet = new StringBuilder();

        String[] queryArray = query.trim().split("[\\p{Punct}\\s]+");
        String lowerContent = content.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        if (queryArray.length > 1 && lowerContent.contains(lowerQuery)) {
            int startIndexPhrase = lowerContent.indexOf(lowerQuery);
            return Snippet.getSnippetText(lowerContent, startIndexPhrase, lowerQuery);
        }

        List<String> contentList = morphology.getLemmasText(content);
        List<String> queryList = morphology.getLemmaSet(query).stream().toList();
        List<Integer> indexList = Snippet.indexingLemmas(contentList, queryList, content);

        LinkedHashSet<String> snippetSet = new LinkedHashSet<>();
        for (Integer index : indexList) {
            if (index != -1) {
                String pieceSnippet = Snippet.getSnippetText(content, index);
                snippetSet.add(pieceSnippet);
            }
        }
        for (int i = 0; i < snippetSet.size(); i++) {
            snippet.append(snippetSet.toArray()[i]).append("...");
            if (snippet.length() > 250) {
                break;
            }
        }
        return snippet.toString();
    }

    private float getAbsolutRelevance(Page page, List<Lemma> lemmaList) {
        float relevance = 0;
        for (Lemma lemma : lemmaList) {
            Integer searchIndex = searchIndexRepository.findByPageIdAndLemmaId(page.getId(), lemma.getId());
            if (searchIndex != null) {
                relevance = relevance + searchIndex;
            }
        }
        return relevance;
    }

    private void getRelativeRelevance(List<DataInfo> dataInfoList) {
        float maxRelevance = dataInfoList.stream().map(DataInfo::getRelevance).max(Float::compare).get();
        for (DataInfo dataInfo : dataInfoList) {
            dataInfo.setRelevance(dataInfo.getRelevance() / maxRelevance);
        }
    }

    private List<Page> getPageList(List<Lemma> lemmaList) {
        List<Page> pageList = new ArrayList<>();
        Collections.sort(lemmaList);

        for (Lemma lemma : lemmaList) {
            List<Page> pageListForLemma = pagesRepository.findByLemmaIdJoinSearchIndexes(lemma.getId());
            pageList.addAll(pageListForLemma);
        }
        return pageList;
    }

    private String getBody(String html) {
        Document document = Jsoup.parse(html);
        return document.text();
    }

    private String getTitle(String html) {
        Document document = Jsoup.parse(html);
        return document.title();
    }

    private List<DataInfo> limitList(List<DataInfo> dataInfoList, int limit, int offset) {
        int count;
        if (limit < dataInfoList.size()) {
            count = limit;
        } else {
            count = dataInfoList.size();
        }
        return dataInfoList.subList(offset, count);
    }
}
