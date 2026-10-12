from app.core import evaluate, same_person
from app.db import adapt


def test_id_only():
    assert same_person({'numeroIdentificacion':'A-123'}, {'numeroIdentificacion':'a-123'})
    assert not same_person({'nombre':'Ana'}, {'nombre':'Ana'})


def test_independent_dimensions():
    query = {'fechaHechos':'2026-01-01', 'quejoso': {'numeroIdentificacion':'ABC'}, 'denunciado':{}}
    records = [{'folio':'F1','origen':'SISTEMA','quejoso':{'numeroIdentificacion':'ABC'},'denunciado':{},'fechaHechos':'2026-01-02'},
               {'folio':'F2','origen':'HISTORICO','quejoso':{},'denunciado':{},'fechaHechos':None}]
    r = evaluate(query, records, [.8,.9], [.8,.9], [.8,.9], 2)
    assert [x['folio'] for x in r['por_hechos']] == ['F2','F1']
    assert len(r['por_involucrados']) == 1
    assert len(r['coincidencias_fuertes']) == 1
    assert r['coincidencias_fuertes'][0]['folio'] == 'F1'


def test_empty():
    assert evaluate({}, [], [], [], [])['coincidencias_fuertes'] == []


def test_adapt():
    r = adapt({'id':1,'folio':'F1','q_nombre':'A','d_nombre':'B'}, 'SISTEMA')
    assert r['quejoso']['nombre'] == 'A'


def test_name_coincidence_is_tentative_not_verified_identity():
    query = {'quejoso': {'nombre': 'María', 'apellido1': 'López', 'apellido2': 'García'},
             'denunciado': {'nombre': 'Juan', 'apellido1': 'Pérez'}}
    records = [{'folio': 'F1', 'origen': 'SISTEMA',
                'quejoso': {'nombre': 'MARIA', 'apellido1': 'LOPEZ', 'apellido2': 'GARCIA'},
                'denunciado': {'nombre': 'Juan', 'apellido1': 'Pérez'}}]
    r = evaluate(query, records, [.9], [.9], [.9])
    assert len(r['por_involucrados']) == 1
    assert r['por_involucrados'][0]['coincide_nombre_quejoso']
    assert r['por_involucrados'][0]['coincide_nombre_denunciado']
    assert not r['por_involucrados'][0]['mismo_quejoso']
    assert not r['coincidencias_fuertes']


def test_same_id_can_produce_strong_coincidence():
    query = {'quejoso': {'numeroIdentificacion': 'A123'}}
    records = [{'folio': 'F1', 'origen': 'SISTEMA',
                'quejoso': {'numeroIdentificacion': 'A-123'}, 'denunciado': {}}]
    r = evaluate(query, records, [.9], [.9], [.9])
    assert r['coincidencias_fuertes'][0]['mismo_quejoso']
