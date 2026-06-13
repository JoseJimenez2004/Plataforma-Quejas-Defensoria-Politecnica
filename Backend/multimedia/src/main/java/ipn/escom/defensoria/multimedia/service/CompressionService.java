package ipn.escom.defensoria.multimedia.service;

import ipn.escom.defensoria.multimedia.dto.CompressionResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface CompressionService {

    CompressionResponseDTO compress(MultipartFile file) throws Exception;
}