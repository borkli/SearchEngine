package searchengine.model;

import jakarta.persistence.Index;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Accessors(chain = true)
@Entity
@Table(
    name = "site_page",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"path", "site_id"}
    ),
    indexes = @Index(name = "sp_path_index", columnList = "path")
)
public class SitePage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    @ManyToOne(
        cascade = CascadeType.MERGE,
        fetch = FetchType.LAZY, optional = false
    )
    @JoinColumn(
        name = "site_id",
        referencedColumnName = "id",
        nullable = false
    )
    private Site site;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    @ColumnDefault("0")
    private int code;

    @Column(
        name = "content",
        columnDefinition = "mediumtext CHARACTER SET utf8mb4 " +
            "COLLATE utf8mb4_general_ci"
    )
    private String content;
}
