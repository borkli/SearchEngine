package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SitePage;

import java.util.List;

@Repository
@Transactional
public interface SitePageRepository extends JpaRepository<SitePage, Long> {

    @Query(
        value = "SELECT sp.id FROM site_page sp " +
            "WHERE sp.path = :path AND site_id = :siteId",
        nativeQuery = true
    )
    Long getIdByPath(String path, Long siteId);

    @Query(
        value = "SELECT sp FROM SitePage sp " +
            "WHERE sp.path = :path AND site.id = :siteId"
    )
    SitePage getByPath(String path, Long siteId);

    @Query(
        value = "SELECT sp FROM SitePage sp " +
            "JOIN Index i ON sp.id = i.page.id " +
            "WHERE i.lemma.id = :lemmaId"
    )
    List<SitePage> getByLemma(Long lemmaId);

    @Query(
        value = "SELECT sp FROM SitePage sp " +
            "JOIN Index i ON sp.id = i.page.id " +
            "WHERE i.lemma.id = :lemmaId AND sp.id IN (:pageIds)"
    )
    List<SitePage> getByLemma(Long lemmaId, List<Long> pageIds);

    @Query(
        value = "SELECT COUNT(*) FROM site_page WHERE site_id = :siteId",
        nativeQuery = true
    )
    long countBy(Long siteId);

    @Modifying
    @Query(
        value = "INSERT INTO site_page(site_id, path) VALUES(:siteId, :path) " +
            "ON DUPLICATE KEY UPDATE id = id",
        nativeQuery = true
    )
    int insert(Long siteId, String path);

    @Modifying
    @Query(
        value = "INSERT INTO site_page(site_id, path, code, content) " +
            "VALUES(:siteId, :path, :code, :content) " +
            "ON DUPLICATE KEY UPDATE site_id = site_id, " +
            "path = path, code = code, content = content",
        nativeQuery = true
    )
    void insert(Long siteId, String path, int code, String content);

    @Modifying
    @Query(
        value = "UPDATE site_page SET code = :code, content = :content " +
            "WHERE site_id = :siteId AND path = :path",
        nativeQuery = true
    )
    int update(int code, String content, Long siteId, String path);

    @Modifying
    @Query(
        value = "DELETE FROM site_page WHERE site_id = :siteId",
        nativeQuery = true
    )
    void deleteBySiteId(Long siteId);
}
