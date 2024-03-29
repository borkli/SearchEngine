package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<Site, Long> {

    @Query(
        value = "SELECT s FROM Site s WHERE s.url = :url"
    )
    Site getByUrl(String url);

    @Query(
        value = "SELECT s.id FROM Site s WHERE s.url NOT IN (:urls)"
    )
    List<Long> getNotActualSites(List<String> urls);

    @Modifying
    @Query(
        value = "UPDATE site SET status = :status WHERE id = :id",
        nativeQuery = true
    )
    void updateStatus(String status, Long id);

    @Modifying
    @Query(
        value = "UPDATE site SET status = :status, last_error = :error " +
            "WHERE id = :siteId",
        nativeQuery = true
    )
    void updateFailedStatus(String status, String error, Long siteId);


    @Modifying
    @Query(
        value = "UPDATE site SET status_time = :dateTime WHERE id = :id",
        nativeQuery = true
    )
    void updateStatusTime(LocalDateTime dateTime, Long id);



    @Modifying
    @Query(
        value = "UPDATE site SET last_error = :error WHERE id = :id",
        nativeQuery = true
    )
    void updateLastError(String error, Long id);

    @Modifying
    @Query(
        value = "DELETE FROM site WHERE id IN (:ids)",
        nativeQuery = true
    )
    void delete(List<Long> ids);
}
