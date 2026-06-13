package ipn.escom.defensoria.multimedia.entity;

import ipn.escom.defensoria.multimedia.enums.FileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "compressed_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompressedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String compressedName;

    @Column(nullable = false)
    private String originalPath;

    @Column(nullable = false)
    private String compressedPath;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    private Long originalSize;

    private Long compressedSize;

    private Double compressionPercentage;

    private LocalDateTime createdAt;
}