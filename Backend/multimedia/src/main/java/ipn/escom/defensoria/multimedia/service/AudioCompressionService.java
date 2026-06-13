package ipn.escom.defensoria.multimedia.service;

import ipn.escom.defensoria.multimedia.dto.CompressionResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AudioCompressionService implements CompressionService {

    private static final String UPLOAD_DIR = "uploads/audio/";
    private static final String COMPRESSED_DIR = "compressed/audio/";

    @Override
    public CompressionResponseDTO compress(MultipartFile multipartFile) throws Exception {

        Files.createDirectories(Paths.get(UPLOAD_DIR));
        Files.createDirectories(Paths.get(COMPRESSED_DIR));

        String originalName = multipartFile.getOriginalFilename();

        Path originalPath = Paths.get(UPLOAD_DIR + originalName);

        Files.write(originalPath, multipartFile.getBytes());

        String compressedName = "compressed_" + originalName;

        Path compressedPath = Paths.get(COMPRESSED_DIR + compressedName);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i",
                originalPath.toString(),
                "-b:a",
                "128k",
                compressedPath.toString()
        );

        processBuilder.inheritIO();

        Process process = processBuilder.start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Error compressing audio");
        }

        long originalSize = Files.size(originalPath);
        long compressedSize = Files.size(compressedPath);

        double percentage =
                ((double) (originalSize - compressedSize) / originalSize) * 100;

        return CompressionResponseDTO.builder()
                .originalFile(originalName)
                .compressedFile(compressedName)
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .compressionPercentage(percentage)
                .downloadUrl("/api/compression/download/" + compressedName)
                .build();
    }
}