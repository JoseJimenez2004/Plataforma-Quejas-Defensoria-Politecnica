package ipn.escom.defensoria.revision_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ipn.escom.defensoria.revision_service.model.IdentificacionOficialModel;

@Service
public class IdentificacionOficialService {

    private static final String OFICIAL = "Identificación oficial";
    private static final String INSTITUCIONAL = "Identificación institucional (IPN)";
    private static final String REPRESENTACION = "Representación (en su caso)";

    /**
     * Documentos con los que el quejoso acredita su identidad y, en su caso, la representación,
     * conforme al Artículo 13 publicado en la Gaceta Politécnica y al apartado de Control y
     * Gestión Administrativa de Documentos del MP-DDP. Se expone como catálogo para que la
     * recepcionista seleccione cuál exhibió.
     */
    private static final List<IdentificacionOficialModel> IDENTIFICACIONES = List.of(
            new IdentificacionOficialModel("ine", "Credencial para votar (INE)", OFICIAL, "Vigente, con fotografía"),
            new IdentificacionOficialModel("pasaporte", "Pasaporte", OFICIAL, "Vigente"),
            new IdentificacionOficialModel("cedula", "Cédula profesional", OFICIAL, "Con fotografía"),
            new IdentificacionOficialModel("cartilla", "Cartilla del Servicio Militar Nacional", OFICIAL, "Liberada"),
            new IdentificacionOficialModel("licencia", "Licencia de conducir", OFICIAL, "Vigente"),
            new IdentificacionOficialModel("credencial_ipn", "Credencial del IPN", INSTITUCIONAL,
                    "Alumno o personal, vigente; coteja la boleta o el número de empleado"),
            new IdentificacionOficialModel("acta_tutor", "Acta de nacimiento o documento de tutela + identificación del padre, madre o tutor",
                    REPRESENTACION, "Menores de edad (CECyT)"),
            new IdentificacionOficialModel("poder_notarial", "Poder notarial + identificaciones", REPRESENTACION,
                    "Representante legal / apoderado"),
            new IdentificacionOficialModel("carta_poder", "Carta poder simple firmada ante dos testigos + identificaciones",
                    REPRESENTACION, "Identificaciones de todas las partes")
    );

    public List<IdentificacionOficialModel> listar() {
        return IDENTIFICACIONES;
    }
}
