package searchengine.parser;

import lombok.Setter;
import org.jsoup.nodes.Document;
import searchengine.config.StopFlag;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PagesRepository;
import searchengine.model.repository.SearchIndexRepository;
import searchengine.model.repository.SitesRepository;

import java.io.IOException;

@Setter
public class PageParser extends SiteParser {
    private Site site;
    private String url;

    public PageParser(PagesRepository pagesRepository, SitesRepository sitesRepository, LemmaRepository lemmaRepository, SearchIndexRepository searchIndexRepository, StopFlag stopFlag) {
        super(pagesRepository, sitesRepository, lemmaRepository, searchIndexRepository, stopFlag);
    }


    public void onePageParse() {
        try {
            sleep();
            Document document = jsoupConnect(url).parse();
            int statusCode = jsoupConnect(url).statusCode();

            if (statusCode < 400) {
                String path = getPath(url);
                Page page = savePage(site, path, document.html(), statusCode);
                saveLemmaAndSearchIndex(page, document.html());
            }
        } catch (IOException e) {
            errorSite(site, e.getMessage());
        }
    }
}
