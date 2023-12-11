package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.jpa.HibernateQuery;
import org.springframework.data.jpa.provider.HibernateUtils;
import org.springframework.stereotype.Service;
import searchengine.config.Indexing;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PagesRepository;
import searchengine.model.repository.SitesRepository;
import searchengine.response.StatisticsResponse;
import searchengine.response.statistics.DetailedStatisticsItem;
import searchengine.response.statistics.StatisticsData;
import searchengine.response.statistics.TotalStatistics;
import searchengine.services.StatisticsService;

import javax.persistence.criteria.CriteriaBuilder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final PagesRepository pagesRepository;
    private final SitesRepository sitesRepository;
    private final LemmaRepository lemmaRepository;
    private final Indexing indexing;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sitesRepository.count());
        total.setIndexing(indexing.isIndexing());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            SiteConfig siteConf = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();

            item.setName(siteConf.getName());
            item.setUrl(siteConf.getUrl());

            searchengine.model.Site siteModel = sitesRepository.findByName(siteConf.getName());
            if (siteModel == null) {
                item.setPages(0);
                item.setLemmas(0);
                item.setStatus("No indexing");
                item.setError("");
                item.setStatusTime(LocalDateTime.now());
            } else {
                item.setPages(pagesRepository.countBySiteId(siteModel.getId()));
                item.setLemmas(lemmaRepository.countBySiteId(siteModel.getId()));
                item.setStatus(siteModel.getStatus().toString());
                item.setError(siteModel.getLastError());
                item.setStatusTime(siteModel.getStatusTime());
            }
            total.setPages(total.getPages() + item.getPages());
            total.setLemmas(total.getLemmas() + item.getLemmas());
            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
