package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;

import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<Index, Long> {

    @Query(
        value = "SELECT i.page.id, SUM(i.rank) FROM Index i " +
            "WHERE i.page.id IN (:pageIds) AND i.lemma.id IN (:lemmaIds) " +
            "GROUP BY i.page.id"
    )
    List<Object[]> relevanceByLemmas(List<Long> pageIds, List<Long> lemmaIds);

    @Query(
        value = "SELECT SUM(i.rank) FROM Index i " +
            "WHERE i.page.id IN (:pageIds)"
    )
    int totalRelevance(List<Long> pageIds);

    @Modifying
    @Query(
        value = "DELETE FROM `index` WHERE page_id = :pageId",
        nativeQuery = true
    )
    void deleteByPageId(Long pageId);

    @Modifying
    @Query(
        value = "DELETE i FROM `index` i " +
            "JOIN site_page p ON i.page_id = p.id " +
            "WHERE p.site_id = :siteId",
        nativeQuery = true
    )
    void deleteBySiteId(Long siteId);
}
