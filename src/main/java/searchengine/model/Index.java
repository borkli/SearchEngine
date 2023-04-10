package searchengine.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(
    name = "`index`",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"page_id", "lemma_id"})
    }
)
public class Index {

    public Index(SitePage page, Lemma lemma, double rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(cascade = CascadeType.MERGE, optional = false)
    @JoinColumn(
        name = "page_id",
        referencedColumnName = "id",
        nullable = false
    )
    private SitePage page;

    @ManyToOne(cascade = CascadeType.MERGE, optional = false)
    @JoinColumn(
        name = "lemma_id",
        referencedColumnName = "id",
        nullable = false
    )
    private Lemma lemma;

    @Column(name = "index_rank", nullable = false)
    private double rank;
}
