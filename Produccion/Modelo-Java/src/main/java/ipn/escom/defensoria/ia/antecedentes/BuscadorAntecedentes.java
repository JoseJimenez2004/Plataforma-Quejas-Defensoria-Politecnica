package ipn.escom.defensoria.ia.antecedentes;

import ipn.escom.defensoria.ia.dataset.Queja;
import ipn.escom.defensoria.ia.normalizacion.Normalizador;
import ipn.escom.defensoria.ia.tfidf.IndiceTfidf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BuscadorAntecedentes {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final IndiceTfidf indice = new IndiceTfidf();
    private final List<Queja> quejas = new ArrayList<>();
    private final Set<String> folios = new HashSet<>();

    public List<Queja> agregar(Collection<Queja> nuevas) {
        List<Queja> aceptadas = new ArrayList<>();
        lock.writeLock().lock();
        try {
            for (Queja q : nuevas) {
                String texto = q == null ? null : q.textoParaVectorizar();
                if (texto == null || texto.isBlank() || (q.folio != null && !folios.add(q.folio))) {
                    continue;
                }
                indice.agregar(Normalizador.tokenizar(texto));
                quejas.add(q);
                aceptadas.add(q);
            }
            if (!aceptadas.isEmpty()) {
                indice.consolidar();
            }
        } finally {
            lock.writeLock().unlock();
        }
        return aceptadas;
    }

    public List<ResultadoAntecedente> buscar(String consulta, double umbral, int topK, boolean incluirTexto) {
        lock.readLock().lock();
        try {
            double[] sim = indice.similitudes(Normalizador.tokenizar(consulta));
            PriorityQueue<Integer> heap = new PriorityQueue<>(topK + 1, Comparator.comparingDouble(d -> sim[d]));
            for (int d = 0; d < sim.length; d++) {
                if (sim[d] <= 0 || sim[d] < umbral) {
                    continue;
                }
                heap.offer(d);
                if (heap.size() > topK) {
                    heap.poll();
                }
            }
            List<ResultadoAntecedente> resultados = new ArrayList<>(heap.size());
            while (!heap.isEmpty()) {
                int d = heap.poll();
                resultados.add(ResultadoAntecedente.de(quejas.get(d), sim[d], incluirTexto));
            }
            Collections.reverse(resultados);
            return resultados;
        } finally {
            lock.readLock().unlock();
        }
    }

    public int tamano() {
        lock.readLock().lock();
        try {
            return indice.tamano();
        } finally {
            lock.readLock().unlock();
        }
    }

    public int tamanoVocabulario() {
        lock.readLock().lock();
        try {
            return indice.tamanoVocabulario();
        } finally {
            lock.readLock().unlock();
        }
    }
}
