package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

import java.util.Collection;
import java.util.List;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    @Query(
        value = "SELECT l FROM Lemma l " +
            "WHERE l.site.id = :siteId AND l.lemma IN (:lemmas) " +
            "ORDER BY l.frequency"
    )
    List<Lemma> getByLemma(Long siteId, Collection<String> lemmas);

    @Query(
        value = "SELECT l FROM Lemma l WHERE l.lemma IN (:lemmas) " +
            "ORDER BY l.frequency"
    )
    List<Lemma> getByLemma(Collection<String> lemmas);

    @Query(
        value = "SELECT COUNT(*) FROM lemma WHERE site_id = :siteId",
        nativeQuery = true
    )
    long countBy(Long siteId);

    @Modifying
    @Query(
        value = "UPDATE lemma l " +
            "JOIN `index` i ON i.lemma_id = l.id " +
            "SET l.frequency = IF(l.frequency > 0, l.frequency - 1, 0) " +
            "WHERE i.page_id = :pageId",
        nativeQuery = true
    )
    void updateByPage(Long pageId);

    @Modifying
    @Query(
        value = "DELETE FROM lemma WHERE site_id = :siteId",
        nativeQuery = true
    )
    void deleteBySiteId(Long siteId);
}