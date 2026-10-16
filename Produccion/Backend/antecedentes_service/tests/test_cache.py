import numpy as np
from app import engine


class DummyModel:
    def __init__(self):
        self.calls = []

    def encode(self, texts, **kwargs):
        self.calls.append(list(texts))
        return np.array([[1.0, 0.0] for _ in texts], dtype=np.float32)


def test_index_cached_and_query_only_encoded_after_first_request(monkeypatch):
    model = DummyModel()
    rows = [dict(folio='F1', origen='SISTEMA', descripcion='Una queja de prueba',
                 quejoso={}, denunciado={})]
    monkeypatch.setattr(engine, 'get_model', lambda: model)
    monkeypatch.setattr(engine, 'read_both', lambda: rows)
    monkeypatch.setattr(engine, '_index', None)
    monkeypatch.setattr(engine, '_last_refresh_attempt', 0.0)
    monkeypatch.setenv('INDEX_TTL_SECONDS', '300')
    query = {'descripcion': 'Una queja nueva', 'quejoso': {}, 'denunciado': {}}
    first = engine.search(query)
    second = engine.search(query)
    assert len(first['por_hechos']) == len(second['por_hechos']) == 1
    assert len(model.calls) == 3  # corpus once; each query once
    assert model.calls[0] == ['Una queja de prueba']


def test_refresh_failure_is_not_silent_empty_result(monkeypatch):
    monkeypatch.setattr(engine, '_index', None)
    monkeypatch.setattr(engine, '_last_refresh_attempt', 0.0)
    monkeypatch.setattr(engine, 'read_both', lambda: (_ for _ in ()).throw(RuntimeError('db unavailable')))
    try:
        engine.refresh_index(force=True)
    except RuntimeError as exc:
        assert str(exc) == 'db unavailable'
    else:
        raise AssertionError('Expected refresh failure')
