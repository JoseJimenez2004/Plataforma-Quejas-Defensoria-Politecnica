"""V5: three independent evidence dimensions, never a duplicate verdict."""
import re
import unicodedata
from datetime import date
from typing import Any


def norm(v: Any) -> str:
    s = unicodedata.normalize('NFD', str(v or '').lower().strip())
    s = ''.join(c for c in s if unicodedata.category(c) != 'Mn')
    return re.sub(r'\s+', ' ', re.sub(r'[^a-z0-9\s]', ' ', s)).strip()


def person_id(p: dict | None) -> str:
    return norm((p or {}).get('numeroIdentificacion')).replace(' ', '')


def same_person(a: dict | None, b: dict | None) -> bool:
    x, y = person_id(a), person_id(b)
    return bool(x and y and x == y)


def full_name(p: dict | None) -> str:
    """Normalized full name; not a unique person identifier."""
    p = p or {}
    return norm(' '.join(str(p.get(k) or '') for k in ('nombre', 'apellido1', 'apellido2')))


def same_name(a: dict | None, b: dict | None) -> bool:
    """Tentative name coincidence only; never proof of identity."""
    x, y = full_name(a), full_name(b)
    return bool(x and y and len(x.split()) >= 2 and x == y)


def days_between(a, b):
    try:
        return abs((date.fromisoformat(str(a)[:10]) - date.fromisoformat(str(b)[:10])).days)
    except (ValueError, TypeError):
        return None


def level(score):
    return 'Alta' if score >= .70 else 'Moderada' if score >= .50 else 'Baja'


def identity_matches(query, candidate):
    mq = same_person(query.get('quejoso'), candidate.get('quejoso'))
    md = same_person(query.get('denunciado'), candidate.get('denunciado'))
    return mq, md


def evaluate(query, candidates, scores, semantic, lexical, top_k=10):
    """Scores align with candidates; strong matches cross top 100 by facts with identity."""
    order = sorted(range(len(candidates)), key=lambda i: scores[i], reverse=True)
    facts = []
    involved = []
    strong = []
    for rank, i in enumerate(order, 1):
        c = candidates[i]
        mq, md = identity_matches(query, c)
        nq = same_name(query.get('quejoso'), c.get('quejoso'))
        nd = same_name(query.get('denunciado'), c.get('denunciado'))
        # A name match is a review signal, not a verified identity.
        days = days_between(query.get('fechaHechos'), c.get('fechaHechos'))
        common = {'folio': c['folio'], 'origen': c['origen'], 'similitud_hechos': float(scores[i]),
                  'sim_semantica': float(semantic[i]), 'sim_lexica': float(lexical[i]),
                  'nivel_similitud': level(scores[i]), 'fechaHechos': c.get('fechaHechos'),
                  'unidadAcademicaClave': c.get('unidadAcademicaClave'), 'quejoso': c.get('quejoso'),
                  'denunciado': c.get('denunciado'), 'descripcion': c.get('descripcion'),
                  'motivo': c.get('motivo'), 'estatus': c.get('estatus'), 'resultado': c.get('resultado'),
                  'resumen': c.get('resumen', '')}
        if rank <= top_k:
            facts.append({'ranking_hechos': rank, **common})
        if mq or md or nq or nd:
            kinds = ' + '.join(x for x, yes in [
                ('Identificación del denunciado', md), ('Identificación del quejoso', mq),
                ('Nombre del denunciado (por verificar)', nd and not md),
                ('Nombre del quejoso (por verificar)', nq and not mq),
            ] if yes)
            involved.append({'folio': c['folio'], 'origen': c['origen'], 'mismo_quejoso': mq,
                             'mismo_denunciado': md, 'coincide_nombre_quejoso': nq,
                             'coincide_nombre_denunciado': nd, 'tipo_coincidencia': kinds,
                             'dias_diferencia': days, 'fechaHechos': c.get('fechaHechos')})
            if (mq or md) and rank <= 100 and scores[i] >= .50:
                reason = f'{kinds} | {level(scores[i])} similitud de hechos ({scores[i]*100:.1f}%)'
                if days is not None and days <= 180:
                    reason += ' | Hechos temporalmente cercanos'
                strong.append({**common, 'mismo_quejoso': mq, 'mismo_denunciado': md,
                               'coincide_nombre_quejoso': nq, 'coincide_nombre_denunciado': nd,
                               'tipo_coincidencia': kinds, 'dias_diferencia': days,
                               'razon_coincidencia_fuerte': reason})
    involved.sort(key=lambda x: (-(4*int(x['mismo_denunciado'])+3*int(x['mismo_quejoso'])+2*int(x['coincide_nombre_denunciado'])+int(x['coincide_nombre_quejoso'])),
                                 x['dias_diferencia'] if x['dias_diferencia'] is not None else float('inf')))
    for rank, item in enumerate(involved, 1):
        item['ranking_involucrados'] = rank
    for rank, item in enumerate(strong, 1):
        item['ranking_coincidencia'] = rank
    return {'por_hechos': facts, 'por_involucrados': involved, 'coincidencias_fuertes': strong}
