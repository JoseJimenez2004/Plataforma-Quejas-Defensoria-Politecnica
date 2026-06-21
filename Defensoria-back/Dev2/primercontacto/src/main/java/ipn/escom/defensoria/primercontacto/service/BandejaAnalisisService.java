package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.BandejaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.FiltroExpedienteDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BandejaAnalisisService {

    private final PlataformaCentralClientService plataformaCentralClientService;

    public BandejaAnalisisService(
            PlataformaCentralClientService plataformaCentralClientService
    ) {
        this.plataformaCentralClientService = plataformaCentralClientService;
    }

    public List<BandejaAnalisisDTO> obtenerBandeja(String token) {
        return plataformaCentralClientService.obtenerBandejaAnalisis(token);
    }

    public BandejaAnalisisDTO buscarPorFolio(
            String folio,
            String token
    ) {

        return plataformaCentralClientService.obtenerBandejaAnalisis(token)
                .stream()
                .filter(q -> q.getFolio().equalsIgnoreCase(folio))
                .findFirst()
                .orElse(null);
    }

    public List<BandejaAnalisisDTO> filtrar(
            FiltroExpedienteDTO filtro,
            String token
    ) {

        return plataformaCentralClientService.obtenerBandejaAnalisis(token)
                .stream()
                .filter(q -> filtro.getFolio() == null ||
                        q.getFolio().toLowerCase()
                                .contains(filtro.getFolio().toLowerCase()))
                .filter(q -> filtro.getPrioridad() == null ||
                        q.getPrioridad().equalsIgnoreCase(filtro.getPrioridad()))
                .filter(q -> filtro.getEstatus() == null ||
                        q.getEstatus().equalsIgnoreCase(filtro.getEstatus()))
                .filter(q -> filtro.getUnidadAcademica() == null ||
                        q.getUnidadAcademica().equalsIgnoreCase(
                                filtro.getUnidadAcademica()))
                .collect(Collectors.toList());
    }

    public List<BandejaAnalisisDTO> obtenerPorPrioridad(
            String prioridad,
            String token
    ) {

        return plataformaCentralClientService.obtenerBandejaAnalisis(token)
                .stream()
                .filter(q ->
                        q.getPrioridad().equalsIgnoreCase(prioridad))
                .collect(Collectors.toList());
    }

    public List<BandejaAnalisisDTO> obtenerPorEstatus(
            String estatus,
            String token
    ) {

        return plataformaCentralClientService.obtenerBandejaAnalisis(token)
                .stream()
                .filter(q ->
                        q.getEstatus().equalsIgnoreCase(estatus))
                .collect(Collectors.toList());
    }
}