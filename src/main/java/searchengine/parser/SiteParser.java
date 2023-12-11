package searchengine.parser;

import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.config.StopFlag;
import searchengine.model.*;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PagesRepository;
import searchengine.model.repository.SearchIndexRepository;
import searchengine.model.repository.SitesRepository;
import searchengine.morphology.Morphology;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Component
public class SiteParser extends RecursiveAction {
    @Setter
    private String url;
    @Setter
    private Site site;
    @Autowired
    private final PagesRepository pagesRepository;
    @Autowired
    private final SitesRepository sitesRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final SearchIndexRepository searchIndexRepository;
    private static StopFlag stopFlag;

    public SiteParser(PagesRepository pagesRepository, SitesRepository sitesRepository, LemmaRepository lemmaRepository, SearchIndexRepository searchIndexRepository, StopFlag stopFlag) {
        this.pagesRepository = pagesRepository;
        this.sitesRepository = sitesRepository;
        this.lemmaRepository = lemmaRepository;
        this.searchIndexRepository = searchIndexRepository;
        this.stopFlag = stopFlag;
    }

    @Override
    protected void compute() {
        try {
            sleep();
            Document document = jsoupConnect(url).parse();
            int statusCode = jsoupConnect(url).statusCode();

            if (statusCode < 400 && !stopFlag.isStopFlag()) {
                String path = getPath(url);
                if (checkRepositoryPage(path, site.getId())) {
                    refreshTimeSite(site);
                    Page page = savePage(site, path, document.html(), statusCode);
                    saveLemmaAndSearchIndex(page, document.html());
                    parse(document);
                }
            }
        } catch (IOException e) {
            errorSite(site, e.getMessage());
        } finally {
            ForkJoinPool.commonPool().shutdown();
        }
    }

    private void parse(Document document) {
        Elements urls = document.select("a");

        List<SiteParser> taskList = new ArrayList<>();

        for (Element element : urls) {
            String href = element.absUrl("href");
            if (checkLink(href, document) && !stopFlag.isStopFlag()) {
                String path = getPath(href).trim();
                if (checkRepositoryPage(path, site.getId())) {
                    SiteParser task = new SiteParser(pagesRepository, sitesRepository, lemmaRepository, searchIndexRepository, stopFlag);
                    task.setUrl(href);
                    task.setSite(site);

                    taskList.add(task);
                    task.fork();
                }
            }
        }

        for (SiteParser task : taskList) {
            task.join();
        }
    }

    protected Connection.Response jsoupConnect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .proxy("192.168.5.10", 3128)
                .timeout(10000000)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .execute();
    }

    protected void sleep() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected boolean checkLink(String link, Document document) {
        String invalidUrl = ".*\\.(js|css|jpg|pdf|jpeg|gif|zip|tar|jar|gz|svg|ppt|pptx|php|png)($|\\?.*)";
        return link.startsWith(document.baseUri())
                && !link.equals(document.baseUri())
                && !link.matches(invalidUrl)
                && !link.contains("#");
    }

    protected boolean checkRepositoryPage(String path, int siteId) {
        Page page = pagesRepository.findByPathAndSiteId(path, siteId);
        return page == null;
    }

    protected void refreshTimeSite(Site site) {
        site.setStatusTime(LocalDateTime.now());
        sitesRepository.save(site);
    }

    protected void errorSite(Site site, String error) {
        site.setStatus(Status.FAILED);
        site.setLastError(error);
        sitesRepository.save(site);
    }

    protected Page savePage(Site site, String path, String content, int code) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(path);
        page.setContent(content);
        page.setCode(code);

        pagesRepository.save(page);
        return page;
    }

    protected String getPath(String url) {
        String path = url.substring(site.getUrl().length());
        return path.isEmpty() ? "/" : path.startsWith("/") ? path : "/" + path;
    }

    protected void saveLemmaAndSearchIndex(Page page, String htmlPage) throws IOException {
        Morphology morphology = Morphology.getInstance();
        String cleanHtmlPage = morphology.cleanHtml(htmlPage);
        HashMap<String, Integer> lemmas = morphology.russianLemmas(cleanHtmlPage);

        lemmas.forEach((LemmaKey, rankValue) -> {
            Lemma lemma = lemmaRepository.findByLemmaAndSiteId(LemmaKey, site.getId());
            if (lemma != null) {
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.save(lemma);

                SearchIndex searchIndex = new SearchIndex();
                searchIndex.setLemma(lemma);
                searchIndex.setPage(page);
                searchIndex.setRank(rankValue);

                searchIndexRepository.save(searchIndex);
            } else {
                Lemma newLemma = new Lemma();
                newLemma.setLemma(LemmaKey);
                newLemma.setFrequency(1);
                newLemma.setSite(site);
                lemmaRepository.save(newLemma);

                SearchIndex searchIndex = new SearchIndex();
                searchIndex.setLemma(newLemma);
                searchIndex.setPage(page);
                searchIndex.setRank(rankValue);

                searchIndexRepository.save(searchIndex);
            }
        });
    }

}
