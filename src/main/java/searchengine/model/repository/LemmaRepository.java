package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.Lemma;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findByLemmaAndSiteId(String lemma, int siteId);
    long countBySiteId (int siteId);
    @Query(value = "SELECT max(lemmas.frequency) FROM lemmas WHERE site_id = ?;", nativeQuery = true)
    long findMaxFreqBySiteId(int siteId);
    @Query(value = "SELECT * FROM lemmas WHERE lemma = ? and site_id = ? and frequency < ? * 0.2;", nativeQuery = true)
    Lemma findLemmaByMaxFreq(String lemma, int siteId, long maxFreq);
}
