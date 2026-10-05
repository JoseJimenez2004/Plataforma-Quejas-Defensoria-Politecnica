package ipn.escom.defensoria.ia.antecedentes;

import ipn.escom.defensoria.ia.dataset.Queja;

public record ResultadoAntecedente(Queja queja, double similitud, boolean historico) {
}
