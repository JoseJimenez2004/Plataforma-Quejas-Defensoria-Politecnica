package ipn.escom.defensoria.antecedentes_service;

import ipn.escom.defensoria.ia.dataset.Queja;
import java.util.List;

public record EntrenamientoBatchRequest(List<Queja> quejas) {
}
