package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

@Repository
public interface SitesRepository extends JpaRepository<Site, Integer> {
    Site findByName(String name);
    Site findByUrl(String url);
}
