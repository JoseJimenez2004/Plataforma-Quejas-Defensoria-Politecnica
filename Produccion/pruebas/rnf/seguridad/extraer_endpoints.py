#!/usr/bin/env python3
"""Genera la matriz rol x endpoint a partir de los controllers de Spring.

Lee @RequestMapping de clase, @GetMapping/@PostMapping/... de método y el
@PreAuthorize (de método o, si no hay, de clase). Escribe un CSV:
servicio,metodo,ruta,roles_permitidos
donde roles_permitidos = PUBLICO | AUTENTICADO | ROL1|ROL2...

Uso: python3 extraer_endpoints.py <dir_backend> <salida.csv> [servicio ...]
"""
import csv
import os
import re
import sys

backend, salida = sys.argv[1], sys.argv[2]
servicios = sys.argv[3:] or [
    "auth-service", "queja-service", "revision-service", "admin-service",
    "catalogo-service", "notificaciones-service", "chatbot-service", "denunciado-service",
]

MAP = re.compile(r"@(Get|Post|Put|Delete|Patch|Request)Mapping\s*(?:\(([^)]*)\))?")
PRE = re.compile(r'@PreAuthorize\(\s*("(?:[^"\\]|\\.)*"|\w+)\s*\)')
CONST = re.compile(r"static\s+final\s+String\s+(\w+)\s*=\s*((?:\"[^\"]*\"\s*\+?\s*)+);", re.S)


def ruta(args):
    if not args:
        return ""
    m = re.search(r'\b(?:value|path)\s*=\s*\{?\s*"([^"]*)"', args)
    if m:
        return m.group(1)
    m = re.match(r'\s*\{?\s*"([^"]*)"', args)  # primer argumento posicional
    return m.group(1) if m else ""


def metodo_http(tipo, args):
    if tipo != "Request":
        return tipo.upper()
    m = re.search(r"RequestMethod\.(\w+)", args or "")
    return m.group(1) if m else "ANY"


def roles(expr, consts):
    if expr in consts:
        expr = consts[expr]
    found = re.findall(r"'([A-Z_]+)'", expr)
    if found:
        return "|".join(found)
    if "isAuthenticated" in expr:
        return "AUTENTICADO"
    return expr


def publicas(svc_dir):
    """Rutas que el SecurityConfig marca permitAll (prefijos)."""
    pats = []
    for dp, _, fs in os.walk(svc_dir):
        for f in fs:
            if f.endswith(".java") and ("Security" in f or "WebConfig" in f):
                src = open(os.path.join(dp, f), encoding="utf-8").read()
                for bloque in re.findall(r"requestMatchers\(([^;]*?)\)\s*\.permitAll", src, re.S):
                    pats += re.findall(r'"(/[^"]*)"', bloque)
    return pats


def es_publica(path, pats):
    for p in pats:
        base = p.replace("/**", "").replace("*", "")
        if p.endswith("/**") and path.startswith(base):
            return True
        if path == p:
            return True
    return False


filas = []
for svc in servicios:
    d = os.path.join(backend, svc, "src", "main", "java")
    pats = publicas(d)
    for dp, _, fs in os.walk(d):
        for f in fs:
            if not f.endswith("Controller.java"):
                continue
            src = open(os.path.join(dp, f), encoding="utf-8").read()
            consts = {k: "".join(re.findall(r'"([^"]*)"', v)) for k, v in CONST.findall(src)}
            cls = re.search(r"^public\s+class", src, re.M)
            if not cls:
                continue
            cabecera, cuerpo = src[: cls.start()], src[cls.start():]
            base = ""
            m = re.search(r"@RequestMapping\s*\(([^)]*)\)", cabecera)
            if m:
                base = ruta(m.group(1))
            pre_cls = PRE.search(cabecera)
            rol_cls = roles(pre_cls.group(1), consts) if pre_cls else None
            # cada mapping de método junto con las anotaciones que lo preceden
            for mm in MAP.finditer(cuerpo):
                ini = cuerpo.rfind(";", 0, mm.start())
                ini = max(ini, cuerpo.rfind("}", 0, mm.start()))
                anot = cuerpo[ini + 1: mm.end() + 400]
                anot = anot[: anot.find("(", anot.find("public") if "public" in anot else 0) + 1]
                p = base + ruta(mm.group(2))
                pre = PRE.search(cuerpo[ini + 1: mm.start()] + "\n" + cuerpo[mm.end(): mm.end() + 200].split("public")[0])
                if pre:
                    r = roles(pre.group(1), consts)
                elif rol_cls:
                    r = rol_cls
                elif es_publica(p, pats):
                    r = "PUBLICO"
                else:
                    r = "AUTENTICADO"
                if es_publica(p, pats) and not pre and not rol_cls:
                    r = "PUBLICO"
                filas.append([svc, metodo_http(mm.group(1), mm.group(2)), p or "/", r, f])

filas.sort()
with open(salida, "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["servicio", "metodo", "ruta", "roles_permitidos", "controller"])
    w.writerows(filas)
print(f"{len(filas)} endpoints -> {salida}")
