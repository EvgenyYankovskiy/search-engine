package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.SearchIndex;

public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {
    @Query(value = "SELECT lemma_rank FROM search_indexes WHERE page_id = ? and lemma_id = ?;", nativeQuery = true)
    Integer findByPageIdAndLemmaId(int pageId, int lemmaId);
}
