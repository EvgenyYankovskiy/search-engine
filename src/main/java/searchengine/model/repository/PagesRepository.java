package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface PagesRepository extends JpaRepository<Page, Integer> {
    Page findByPathAndSiteId(String path, int siteId);
    void deleteAllBySiteId(int id);
    long countBySiteId(int siteId);
    @Query(value = "SELECT pages.* FROM pages join search_indexes ON pages.id = search_indexes.page_id where lemma_id = ?;", nativeQuery = true)
    List<Page> findByLemmaIdJoinSearchIndexes(int lemmaId);
}
