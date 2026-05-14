package ipn.escom.defensoria.multimedia.controller;

import ipn.escom.defensoria.multimedia.dto.CompressionResponseDTO;
import ipn.escom.defensoria.multimedia.service.AudioCompressionService;
import ipn.escom.defensoria.multimedia.service.ImageCompressionService;
import ipn.escom.defensoria.multimedia.service.PdfCompressionService;
import ipn.escom.defensoria.multimedia.service.VideoCompressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/compression")
@RequiredArgsConstructor
public class CompressionController {

    private final ImageCompressionService imageService;
    private final VideoCompressionService videoService;
    private final AudioCompressionService audioService;
    private final PdfCompressionService pdfService;

    @PostMapping("/image")
    public ResponseEntity<CompressionResponseDTO> compressImage(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return ResponseEntity.ok(imageService.compress(file));
    }

    @PostMapping("/video")
    public ResponseEntity<CompressionResponseDTO> compressVideo(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return ResponseEntity.ok(videoService.compress(file));
    }

    @PostMapping("/audio")
    public ResponseEntity<CompressionResponseDTO> compressAudio(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return ResponseEntity.ok(audioService.compress(file));
    }

    @PostMapping("/pdf")
    public ResponseEntity<CompressionResponseDTO> compressPdf(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return ResponseEntity.ok(pdfService.compress(file));
    }
}