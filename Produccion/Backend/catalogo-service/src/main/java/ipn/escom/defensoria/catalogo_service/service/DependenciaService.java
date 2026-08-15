package ipn.escom.defensoria.catalogo_service.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.catalogo_service.dto.DependenciaRequest;
import ipn.escom.defensoria.catalogo_service.dto.ImportacionResumenModel;
import ipn.escom.defensoria.catalogo_service.entity.Dependencia;
import ipn.escom.defensoria.catalogo_service.repository.DependenciaRepository;

@Service
public class DependenciaService {

    @Autowired
    private DependenciaRepository dependenciaRepository;

    public List<Dependencia> listarActivas() {
        return dependenciaRepository.findByActivoTrueOrderByNombreAsc();
    }

    public List<Dependencia> listarActivasPorTipo(String tipo) {
        return dependenciaRepository.findByActivoTrueAndTipoOrderByNombreAsc(tipo);
    }

    public Optional<Dependencia> buscarPorClave(String clave) {
        return dependenciaRepository.findByClave(clave);
    }

    /** Lista TODAS las dependencias (activas e inactivas) -- usado por el panel de
     * administración, a diferencia de {@link #listarActivas} que es para el público. */
    public List<Dependencia> listarTodas() {
        return dependenciaRepository.findAll();
    }

    public Dependencia crear(DependenciaRequest datos) {
        if (esVacio(datos.getClave()) || esVacio(datos.getNombre()) || esVacio(datos.getTipo())) {
            throw new RuntimeException("Clave, nombre y tipo son obligatorios.");
        }
        if (dependenciaRepository.findByClave(datos.getClave()).isPresent()) {
            throw new RuntimeException("Ya existe una dependencia con esa clave.");
        }

        Dependencia dependencia = new Dependencia();
        aplicarDatos(dependencia, datos);
        dependencia.setActivo(true);
        return dependenciaRepository.save(dependencia);
    }

    public Dependencia editar(String clave, DependenciaRequest datos) {
        Dependencia dependencia = dependenciaRepository.findByClave(clave)
                .orElseThrow(() -> new RuntimeException("No se encontró una dependencia con esa clave."));
        aplicarDatos(dependencia, datos);
        return dependenciaRepository.save(dependencia);
    }

    private void aplicarDatos(Dependencia dependencia, DependenciaRequest datos) {
        if (!esVacio(datos.getClave())) {
            dependencia.setClave(datos.getClave());
        }
        if (!esVacio(datos.getNombre())) {
            dependencia.setNombre(datos.getNombre());
        }
        if (!esVacio(datos.getTipo())) {
            dependencia.setTipo(datos.getTipo());
        }
        dependencia.setClavePadre(datos.getClavePadre());
        dependencia.setAbreviatura(datos.getAbreviatura());
        dependencia.setCategoria(datos.getCategoria());
        dependencia.setNivel(datos.getNivel() != null ? datos.getNivel() : 1);
        dependencia.setCorreoContacto(datos.getCorreoContacto());
        dependencia.setNombreTitular(datos.getNombreTitular());
    }

    /**
     * "Importar Directorio desde Excel (SIA/IPN)" -- columnas esperadas (en este orden, con
     * encabezado en la fila 1): Clave, Nombre, Abreviatura, Tipo, Correo de Contacto,
     * Nombre del Titular. Actualiza por clave si ya existe, crea si no.
     */
    public ImportacionResumenModel importarDesdeExcel(MultipartFile archivo) {
        int creadas = 0;
        int actualizadas = 0;
        List<String> errores = new ArrayList<>();

        try (InputStream is = archivo.getInputStream(); Workbook libro = WorkbookFactory.create(is)) {
            Sheet hoja = libro.getSheetAt(0);

            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null || esFilaVacia(fila)) {
                    continue;
                }
                try {
                    String clave = textoCelda(fila, 0);
                    if (esVacio(clave)) {
                        errores.add("Fila " + (i + 1) + ": falta la clave, se omitió.");
                        continue;
                    }

                    DependenciaRequest datos = new DependenciaRequest();
                    datos.setClave(clave);
                    datos.setNombre(textoCelda(fila, 1));
                    datos.setAbreviatura(textoCelda(fila, 2));
                    datos.setTipo(esVacio(textoCelda(fila, 3)) ? "UNIDAD_ACADEMICA" : textoCelda(fila, 3));
                    datos.setCorreoContacto(textoCelda(fila, 4));
                    datos.setNombreTitular(textoCelda(fila, 5));
                    datos.setNivel(1);

                    if (dependenciaRepository.findByClave(clave).isPresent()) {
                        editar(clave, datos);
                        actualizadas++;
                    } else {
                        crear(datos);
                        creadas++;
                    }
                } catch (Exception exFila) {
                    errores.add("Fila " + (i + 1) + ": " + exFila.getMessage());
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo leer el archivo de Excel: " + ex.getMessage());
        }

        return new ImportacionResumenModel(creadas, actualizadas, errores);
    }

    private boolean esFilaVacia(Row fila) {
        return esVacio(textoCelda(fila, 0)) && esVacio(textoCelda(fila, 1));
    }

    private static final DataFormatter FORMATEADOR = new DataFormatter();

    private String textoCelda(Row fila, int indice) {
        Cell celda = fila.getCell(indice);
        if (celda == null) {
            return "";
        }
        return FORMATEADOR.formatCellValue(celda).trim();
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
