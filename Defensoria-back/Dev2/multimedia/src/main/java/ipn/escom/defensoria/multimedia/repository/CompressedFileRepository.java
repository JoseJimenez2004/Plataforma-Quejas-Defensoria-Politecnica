package ipn.escom.defensoria.multimedia.repository;

import ipn.escom.defensoria.multimedia.entity.CompressedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompressedFileRepository extends JpaRepository<CompressedFile, Long> {
}