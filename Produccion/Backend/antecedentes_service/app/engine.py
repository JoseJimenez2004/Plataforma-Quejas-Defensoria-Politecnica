"""CPU-only, cached corpus index. Searches only encode the incoming narrative."""
import logging
import os
import threading
import time
from dataclasses import dataclass

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from .core import evaluate
from .db import read_both

LOG = logging.getLogger(__name__)
MODEL_NAME = os.getenv('EMBEDDING_MODEL', 'sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2')
_model = None
_model_lock = threading.Lock()
_index_lock = threading.RLock()
_index = None
_last_refresh_attempt = 0.0


def get_model():
    global _model
    if _model is None:
        with _model_lock:
            if _model is None:
                from sentence_transformers import SentenceTransformer
                import torch
                torch.set_num_threads(max(1, int(os.getenv('TORCH_NUM_THREADS', '2'))))
                _model = SentenceTransformer(MODEL_NAME, device='cpu', local_files_only=os.getenv('MODEL_OFFLINE', '1') == '1')
    return _model


@dataclass(frozen=True)
class Index:
    records: list
    embeddings: np.ndarray
    vectorizer: object
    matrix: object
    created: float


def build_index(records):
    records = [r for r in records if str(r.get('descripcion') or '').strip()]
    texts = [r['descripcion'] for r in records]
    if not records:
        return Index([], np.zeros((0, 0), dtype=np.float32), None, None, time.monotonic())
    model = get_model()
    batch = max(1, int(os.getenv('EMBEDDING_BATCH_SIZE', '16')))
    embeddings = np.asarray(model.encode(texts, normalize_embeddings=True,
                                          batch_size=batch, show_progress_bar=False), dtype=np.float32)
    vectorizer = None
    matrix = None
    try:
        vectorizer = TfidfVectorizer(lowercase=True, strip_accents='unicode', ngram_range=(1, 2),
                                     min_df=2 if len(records) >= 2 else 1,
                                     sublinear_tf=True, max_features=100000)
        matrix = vectorizer.fit_transform(texts)
    except ValueError as exc:
        if 'empty vocabulary' not in str(exc) and 'After pruning' not in str(exc):
            raise
        vectorizer = None
        matrix = None
    return Index(records, embeddings, vectorizer, matrix, time.monotonic())


def refresh_index(force=False):
    """Atomic swap: readers never observe a partially built index.

    A refresh failure retains the previous index; requests fail if it is stale.
    """
    global _index, _last_refresh_attempt
    with _index_lock:
        now = time.monotonic()
        ttl = max(5, int(os.getenv('INDEX_TTL_SECONDS', '300')))
        if not force and _index is not None and now - _index.created < ttl:
            return _index
        # Avoid hammering a failing DB on every request.
        if not force and now - _last_refresh_attempt < 10:
            if _index is None or now - _index.created > ttl + max(0, int(os.getenv('INDEX_MAX_STALE_SECONDS', '60'))):
                raise RuntimeError('Index refresh failed; no usable index')
            return _index
        _last_refresh_attempt = now
        try:
            updated = build_index(read_both())
        except Exception:
            LOG.exception('Index refresh failed')
            if _index is None or now - _index.created > ttl + max(0, int(os.getenv('INDEX_MAX_STALE_SECONDS', '60'))):
                raise
            return _index
        _index = updated
        LOG.info('Index refreshed: %d records', len(updated.records))
        return updated


def index_status():
    idx = _index
    if idx is None:
        return {'loaded': False, 'records': 0}
    return {'loaded': True, 'records': len(idx.records),
            'age_seconds': round(time.monotonic() - idx.created, 1)}


def search(query, records=None, top_k=10):
    """`records` is accepted for unit tests; production uses cached DB index."""
    description = str(query.get('descripcion') or '').strip()
    if not description:
        raise ValueError('La descripción no puede estar vacía')
    idx = build_index(records) if records is not None else refresh_index()
    if not idx.records:
        return {'por_hechos': [], 'por_involucrados': [], 'coincidencias_fuertes': []}
    q = np.asarray(get_model().encode([description], normalize_embeddings=True,
                                       show_progress_bar=False), dtype=np.float32)[0]
    semantic = np.clip(idx.embeddings @ q, -1, 1)
    lexical = np.zeros(len(idx.records), dtype=np.float32)
    if idx.vectorizer is not None:
        lexical = np.asarray((idx.vectorizer.transform([description]) @ idx.matrix.T).toarray()[0], dtype=np.float32)
    scores = np.clip(.85 * semantic + .15 * lexical, 0, 1)
    own_folio = query.get('folio')
    own_origin = query.get('origen', 'SISTEMA')
    if own_folio:
        valid = [i for i, r in enumerate(idx.records) if not (r['folio'] == own_folio and r['origen'] == own_origin)]
        records = [idx.records[i] for i in valid]
        return evaluate(query, records, scores[valid], semantic[valid], lexical[valid], top_k)
    return evaluate(query, idx.records, scores, semantic, lexical, top_k)
