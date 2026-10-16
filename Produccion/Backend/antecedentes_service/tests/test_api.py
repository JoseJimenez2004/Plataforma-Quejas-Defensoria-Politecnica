from fastapi.testclient import TestClient
from app import main


def test_legacy_contract_accepts_optional_people(monkeypatch):
    captured = {}
    def fake_search(query, top_k):
        captured.update(query)
        return {'por_hechos': [], 'por_involucrados': [], 'coincidencias_fuertes': []}
    monkeypatch.setattr(main, 'safe_search', fake_search)
    client = TestClient(main.app)
    response = client.post('/api/antecedentes/buscar', json={
        'texto': 'Relato de prueba', 'quejoso': {'nombre': 'Ana', 'apellido1': 'Ejemplo'},
        'denunciado': {'nombre': 'Luis', 'apellido1': 'Ejemplo'}})
    assert response.status_code == 200
    assert response.json() == {'antecedentes': []}
    assert captured['quejoso']['nombre'] == 'Ana'
    assert captured['denunciado']['nombre'] == 'Luis'


def test_v5_structured_contract(monkeypatch):
    monkeypatch.setattr(main, 'safe_search', lambda q, k: {
        'por_hechos': [], 'por_involucrados': [], 'coincidencias_fuertes': []})
    response = TestClient(main.app).post('/api/v5/antecedentes/buscar', json={
        'descripcion': 'Relato de prueba', 'quejoso': {'nombre': 'Ana'}})
    assert response.status_code == 200
    assert set(response.json()) == {'por_hechos', 'por_involucrados', 'coincidencias_fuertes'}
