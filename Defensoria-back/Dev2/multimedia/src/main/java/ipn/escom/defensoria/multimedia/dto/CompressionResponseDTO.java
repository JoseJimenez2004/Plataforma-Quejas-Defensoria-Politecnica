package ipn.escom.defensoria.multimedia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompressionResponseDTO {

    private String originalFile;

    private String compressedFile;

    private Long originalSize;

    private Long compressedSize;

    private Double compressionPercentage;

    private String downloadUrl;
}