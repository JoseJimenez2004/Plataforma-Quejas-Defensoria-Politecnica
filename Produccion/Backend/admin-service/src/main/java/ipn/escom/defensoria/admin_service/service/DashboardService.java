package ipn.escom.defensoria.admin_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ipn.escom.defensoria.admin_service.model.DashboardResumenModel;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    @Autowired
    private PersonalAdministrativoService personalService;

    @Autowired
    private PlantillaService plantillaService;

    @Autowired
    private RespaldoService respaldoService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${catalogo.service.url}")
    private String catalogoServiceUrl;

    public DashboardResumenModel obtenerResumen() {
        long totalPersonal = personalService.contarActivos();
        long totalPlantillas = plantillaService.contarActivas();
        String ultimoRespaldo = respaldoService.ultimoRespaldoTexto();
        long totalDependencias = obtenerTotalDependencias();

        return new DashboardResumenModel(totalPersonal, totalDependencias, ultimoRespaldo, totalPlantillas);
    }

    private long obtenerTotalDependencias() {
        try {
            Object[] dependencias = restTemplate.getForObject(
                    catalogoServiceUrl + "/api/catalogos/dependencias", Object[].class);
            return dependencias == null ? 0 : dependencias.length;
        } catch (Exception ex) {
            // El dashboard no debe romperse solo porque catalogo-service esté caído --
            // simplemente mostramos 0 y lo dejamos registrado en el log.
            log.warn("No se pudo obtener el total de dependencias desde catalogo-service: {}", ex.getMessage());
            return 0;
        }
    }
}
