package ipn.escom.defensoria.admin_service.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.admin_service.model.RespaldoResumenModel;

/**
 * Respaldos de la base de datos compartida (defensoria_db) vía pg_dump/psql (ProcessBuilder).
 * Requiere que la imagen del contenedor tenga instalado postgresql-client (ver Dockerfile
 * propio de admin-service) y que "respaldos.directorio" esté montado como volumen para que
 * los archivos sobrevivan a que se reconstruya el contenedor.
 */
@Service
public class RespaldoService {

    private static final Logger log = LoggerFactory.getLogger(RespaldoService.class);
    private static final DateTimeFormatter FORMATO_NOMBRE =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    @Value("${respaldos.directorio}")
    private String directorio;

    @Value("${db.host}")
    private String dbHost;

    @Value("${db.puerto}")
    private String dbPuerto;

    @Value("${db.nombre}")
    private String dbNombre;

    @Value("${db.usuario}")
    private String dbUsuario;

    @Value("${db.password}")
    private String dbPassword;

    public RespaldoResumenModel ejecutarRespaldoManual() {
        crearDirectorioSiNoExiste();
        String nombreArchivo = "defensoria_db_" + FORMATO_NOMBRE.format(Instant.now()) + ".sql";
        Path destino = Path.of(directorio, nombreArchivo);

        List<String> comando = List.of(
                "pg_dump",
                "-h", dbHost,
                "-p", dbPuerto,
                "-U", dbUsuario,
                "-F", "p",
                "-f", destino.toString(),
                dbNombre);

        ejecutar(comando, "pg_dump");
        log.info("Respaldo manual generado: {}", destino);
        return aResumen(destino.toFile());
    }

    public void restaurar(String nombreArchivo) {
        Path origen = Path.of(directorio, nombreArchivo);
        if (!Files.exists(origen)) {
            throw new RuntimeException("No se encontró el archivo de respaldo indicado.");
        }

        List<String> comando = List.of(
                "psql",
                "-h", dbHost,
                "-p", dbPuerto,
                "-U", dbUsuario,
                "-d", dbNombre,
                "-f", origen.toString());

        ejecutar(comando, "psql (restauración)");
        log.warn("Base de datos restaurada desde el respaldo: {}", nombreArchivo);
    }

    public List<RespaldoResumenModel> listar() {
        crearDirectorioSiNoExiste();
        File carpeta = new File(directorio);
        File[] archivos = carpeta.listFiles((dir, nombre) -> nombre.endsWith(".sql"));
        if (archivos == null) {
            return List.of();
        }
        return List.of(archivos).stream()
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(this::aResumen)
                .toList();
    }

    public Resource obtenerArchivo(String nombreArchivo) {
        Path ruta = Path.of(directorio, nombreArchivo);
        if (!Files.exists(ruta)) {
            throw new RuntimeException("No se encontró ese archivo de respaldo.");
        }
        return new FileSystemResource(ruta);
    }

    /** Último respaldo disponible (manual o automático) -- para la tarjeta "Seguridad" del
     * dashboard y el texto "Último respaldo automático realizado". */
    public String ultimoRespaldoTexto() {
        List<RespaldoResumenModel> respaldos = listar();
        if (respaldos.isEmpty()) {
            return "Sin respaldos todavía";
        }
        return respaldos.get(0).getFecha();
    }

    /** Respaldo automático diario a las 4:00 AM -- coincide con lo mostrado en el mockup
     * ("Último respaldo automático realizado: Hoy, 04:00 AM"). */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 4 * * *")
    public void respaldoAutomaticoDiario() {
        try {
            ejecutarRespaldoManual();
        } catch (Exception ex) {
            log.error("Falló el respaldo automático programado", ex);
        }
    }

    private void ejecutar(List<String> comando, String nombreProceso) {
        try {
            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.environment().put("PGPASSWORD", dbPassword);
            pb.redirectErrorStream(true);
            Process proceso = pb.start();

            String salida = new String(proceso.getInputStream().readAllBytes());
            boolean termino = proceso.waitFor(120, TimeUnit.SECONDS);

            if (!termino) {
                proceso.destroyForcibly();
                throw new RuntimeException(nombreProceso + " tardó demasiado y fue cancelado.");
            }
            if (proceso.exitValue() != 0) {
                log.error("{} terminó con error (código {}): {}", nombreProceso, proceso.exitValue(), salida);
                throw new RuntimeException(nombreProceso + " falló. Revisa los logs del servidor.");
            }
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("No se pudo ejecutar " + nombreProceso + ": " + ex.getMessage());
        }
    }

    private void crearDirectorioSiNoExiste() {
        File carpeta = new File(directorio);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
    }

    private RespaldoResumenModel aResumen(File archivo) {
        String fecha = Instant.ofEpochMilli(archivo.lastModified())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return new RespaldoResumenModel(archivo.getName(), archivo.length(), fecha);
    }
}
