package ipn.escom.defensoria.revision_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ipn.escom.defensoria.revision_service.model.IdentificacionOficialModel;

@Service
public class IdentificacionOficialService {

    /**
     * Listado de identificaciones oficiales válidas de acuerdo al artículo 48 del
     * Reglamento de la Ley General de Población (vigente para trámites ante
     * instituciones públicas). Se expone como catálogo para que el recepcionista
     * seleccione cuál presentó el quejoso. El listado se puede mover a una tabla
     * de catalogo-service cuando se requiera mantenimiento dinámico.
     */
    private static final List<IdentificacionOficialModel> IDENTIFICACIONES = List.of(
            new IdentificacionOficialModel("ine", "Credencial para votar (INE)"),
            new IdentificacionOficialModel("pasaporte", "Pasaporte vigente"),
            new IdentificacionOficialModel("cedula", "Cédula profesional"),
            new IdentificacionOficialModel("licencia", "Licencia de conducir"),
            new IdentificacionOficialModel("cartilla", "Cartilla del servicio militar nacional"),
            new IdentificacionOficialModel("afiliacion", "Credencial de afiliación al IMSS/ISSSTE"),
            new IdentificacionOficialModel("acta", "Acta de nacimiento (con foto o CURP, según aplique)")
    );

    public List<IdentificacionOficialModel> listar() {
        return IDENTIFICACIONES;
    }
}
