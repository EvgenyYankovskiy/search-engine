package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Indexing;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.config.StopFlag;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PagesRepository;
import searchengine.model.repository.SearchIndexRepository;
import searchengine.model.repository.SitesRepository;
import searchengine.parser.PageParser;
import searchengine.parser.SiteParser;
import searchengine.services.IndexingServices;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingServices {
    private final SitesRepository sitesRepository;
    private final PagesRepository pagesRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;
    private final SitesList sitesList;
    private final StopFlag stopFlag;
    private final Indexing indexing;
    private final List<ForkJoinPool> forkJoinPools = new ArrayList<>();

    @Override
    public boolean startIndexing() {
        AtomicBoolean statusIndexing = isIndexing(new AtomicBoolean(false));
        stopFlag.setStopFlag(false);
        indexing.setIndexing(true);

        if (statusIndexing.get()) {
            return false;
        }

        List<SiteConfig> siteList = sitesList.getSites();

        siteList.forEach(s -> {
            Site site = sitesRepository.findByUrl(s.getUrl());
            if (site != null) {
                deleteSite(site);
            }
            Site newSite = createSite(s.getUrl(), s.getName());
            sitesRepository.save(newSite);
            new Thread(() -> startSiteParser(newSite)).start();
        });
        return true;
    }

    @Override
    public boolean stopIndexing() {
        AtomicBoolean statusIndexing = isIndexing(new AtomicBoolean(false));
        indexing.setIndexing(false);

        if (!statusIndexing.get()) {
            return false;
        }

        stopFlag.setStopFlag(true);
        forkJoinPools.forEach(ForkJoinPool::shutdownNow);

        pagesRepository.flush();
        sitesRepository.flush();
        lemmaRepository.flush();
        searchIndexRepository.flush();

        sitesRepository.findAll().forEach(site -> {
            if (site.getStatus().equals(Status.INDEXING)) {
                site.setLastError("Индексация остановлена пользователем");
                site.setStatus(Status.FAILED);
                sitesRepository.save(site);
            }
        });
        return true;
    }

    @Override
    public boolean indexPage(String url) {
        Site siteModel;
        SiteConfig siteConf = containsSiteList(url);
        if (siteConf == null) {
            return false;
        } else {
            siteModel = getSiteModel(url, siteConf);
        }

        PageParser pageParser = new PageParser(pagesRepository, sitesRepository, lemmaRepository, searchIndexRepository, stopFlag);
        pageParser.setSite(siteModel);
        pageParser.setUrl(url);
        pageParser.onePageParse();
        return true;
    }

    private Site getSiteModel(String url, SiteConfig siteConf) {
        Site siteModel;
        siteModel = containsSite(url);
        if (siteModel == null) {
            siteModel = createSite(siteConf.getUrl(), siteConf.getName());
        } else {
            pagesRepository.delete(pagesRepository.findByPathAndSiteId(url, siteModel.getId()));
        }
        return siteModel;
    }

    public AtomicBoolean isIndexing(AtomicBoolean indexing) {
        Iterable<Site> siteList = sitesRepository.findAll();
        for (Site site : siteList) {
            indexing.set(site.getStatus().equals(Status.INDEXING));
        }
        return indexing;
    }

    private Site createSite(String url, String name) {
        Site site = new Site();
        site.setUrl(url);
        site.setName(name);
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        return site;
    }

    private void startSiteParser(Site site) {
        SiteParser siteParser = new SiteParser(pagesRepository, sitesRepository, lemmaRepository, searchIndexRepository, stopFlag);
        siteParser.setUrl(site.getUrl());
        siteParser.setSite(site);

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPools.add(forkJoinPool);
        forkJoinPool.invoke(siteParser);

        site.setStatus(Status.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        sitesRepository.save(site);
    }


    public void deleteSite(Site site) {
        pagesRepository.deleteAllBySiteId(site.getId());
        sitesRepository.delete(site);
    }

    private Site containsSite(String url) {
        Iterable<Site> siteList = sitesRepository.findAll();
        for (Site site : siteList) {
            if (url.startsWith(site.getUrl())) {
                return site;
            }
        }
        return null;
    }

    private SiteConfig containsSiteList(String url) {
        List<SiteConfig> siteList = sitesList.getSites();
        for (SiteConfig site : siteList) {
            if (url.startsWith(site.getUrl())) {
                return site;
            }
        }
        return null;
    }
}
