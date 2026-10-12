"""Read-only PostgreSQL adapters. Credentials are injected, never stored in source."""
import os
from contextlib import closing

SYSTEM_SQL = '''
SELECT id, numero_folio AS folio, descripcion, fecha_hechos AS "fechaHechos",
       unidad_academica_clave AS "unidadAcademicaClave", motivo, estatus,
       nombre_quejoso AS q_nombre, apellido1_quejoso AS q_a1, apellido2_quejoso AS q_a2,
       numero_identificacion_quejoso AS q_id, tipo_identificacion_quejoso AS q_tipo,
       correo_institucional AS q_correo,
       nombre_denunciado AS d_nombre, apellido1_denunciado AS d_a1,
       apellido2_denunciado AS d_a2
FROM public.quejas
'''
HISTORIC_SQL = '''
SELECT id, folio, descripcion, fecha_hechos AS "fechaHechos",
       unidad_academica_clave AS "unidadAcademicaClave", motivo, estatus, resultado,
       quejoso_nombre AS q_nombre, quejoso_apellido1 AS q_a1, quejoso_apellido2 AS q_a2,
       quejoso_numero_identificacion AS q_id, quejoso_tipo_identificacion AS q_tipo,
       denunciado_nombre AS d_nombre, denunciado_apellido1 AS d_a1,
       denunciado_apellido2 AS d_a2, denunciado_numero_identificacion AS d_id,
       denunciado_tipo_identificacion AS d_tipo
FROM public.quejas_historicas
'''


def adapt(row, origin):
    def person(prefix):
        return {'nombre': row.get(f'{prefix}_nombre'), 'apellido1': row.get(f'{prefix}_a1'),
                'apellido2': row.get(f'{prefix}_a2'), 'numeroIdentificacion': row.get(f'{prefix}_id'),
                'tipoIdentificacion': row.get(f'{prefix}_tipo'),
                'correo': row.get(f'{prefix}_correo')}
    return {'id': row['id'], 'folio': row['folio'], 'origen': origin,
            'descripcion': row.get('descripcion') or '',
            'fechaHechos': str(row['fechaHechos']) if row.get('fechaHechos') else None,
            'unidadAcademicaClave': row.get('unidadAcademicaClave'),
            'motivo': row.get('motivo'), 'estatus': row.get('estatus'),
            'resultado': row.get('resultado'), 'quejoso': person('q'), 'denunciado': person('d')}


def read_one(dsn, sql, origin):
    if not dsn:
        raise RuntimeError(f'Falta configurar DSN para {origin}')
    import psycopg
    from psycopg.rows import dict_row
    with closing(psycopg.connect(dsn, connect_timeout=5, row_factory=dict_row)) as conn:
        with conn.cursor() as cur:
            cur.execute('BEGIN READ ONLY')
            cur.execute('SET LOCAL statement_timeout = 10000')
            cur.execute(sql)
            return [adapt(r, origin) for r in cur.fetchall()]


def read_both():
    # Fail closed: never represent a failed DB as an empty search.
    system = read_one(os.getenv('SYSTEM_DATABASE_URL'), SYSTEM_SQL, 'SISTEMA')
    historical = read_one(os.getenv('HISTORIC_DATABASE_URL'), HISTORIC_SQL, 'HISTORICO')
    return system + historical
