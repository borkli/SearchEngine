package searchengine.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@AllArgsConstructor
public class JdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insertLemmaBatch(List<Lemma> lemmas) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO lemma (site_id, lemma, frequency) " +
                "VALUES (?, ?, ?) AS new(s, l, f) " +
                "ON DUPLICATE KEY UPDATE frequency = frequency + new.f",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Lemma lemma = lemmas.get(i);
                    int index = 0;
                    ps.setLong(++index, lemma.getSite().getId());
                    ps.setString(++index, lemma.getLemma());
                    ps.setInt(++index, lemma.getFrequency());
                }

                @Override
                public int getBatchSize() {
                    return lemmas.size();
                }
            }
        );
    }

    public void insertIndexBatch(List<Index> indices) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO `index` (lemma_id, page_id, index_rank) " +
                "VALUES (?, ?, ?) AS new(l, p, r) " +
                "ON DUPLICATE KEY UPDATE index_rank = index_rank + new.r",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Index indexPage = indices.get(i);
                    int index = 0;
                    ps.setLong(++index, indexPage.getLemma().getId());
                    ps.setLong(++index, indexPage.getPage().getId());
                    ps.setDouble(++index, indexPage.getRank());
                }

                @Override
                public int getBatchSize() {
                    return indices.size();
                }
            }
        );
    }
}
