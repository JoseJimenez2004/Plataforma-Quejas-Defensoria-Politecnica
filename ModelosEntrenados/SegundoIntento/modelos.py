import json, os, copy, argparse
import numpy as np
import pandas as pd
import joblib
from datetime import datetime

from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import (
    accuracy_score, f1_score, precision_score,
    recall_score, classification_report
)
from sklearn.base import BaseEstimator, TransformerMixin
from sklearn.pipeline import Pipeline

import spacy
from sentence_transformers import SentenceTransformer
from sklearn.svm import SVC
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.naive_bayes import GaussianNB

# ─────────────────────────────────────────────
#  1. CONFIGURACIÓN Y CONSTANTES
# ─────────────────────────────────────────────

CAMPOS_JURIDICOS = [
    "es_conflicto_laboral_art4_I",
    "es_evaluacion_academica_pura_art4_II",
    "hay_vulneracion_notoria_de_derechos_en_evaluacion",
    "es_resolucion_disciplinaria_art4_III",
    "es_afectacion_colectiva_art4_IV",
    "involucra_oic_art4_V",
    "hay_acto_u_omision_de_autoridad_institucional",
]

# ─────────────────────────────────────────────
#  2. VECTORIZADORES DE TEXTO (BASE)
# ─────────────────────────────────────────────

class SpacyVectorizer(BaseEstimator, TransformerMixin):
    def fit(self, X, y=None):
        return self

    def transform(self, textos):
        # Carga perezosa para evitar errores de serialización
        nlp = spacy.load("es_core_news_lg")
        vecs = []
        for t in textos:
            doc = nlp(t)
            tokens = [tok.vector for tok in doc if tok.has_vector and not tok.is_stop]
            vecs.append(np.mean(tokens, axis=0) if tokens else np.zeros(300))
        return np.array(vecs)

class SentenceBertVectorizer(BaseEstimator, TransformerMixin):
    def fit(self, X, y=None):
        return self

    def transform(self, textos):
        # Carga perezosa para evitar bloqueos en multiprocesamiento
        model = SentenceTransformer("paraphrase-multilingual-mpnet-base-v2")
        return model.encode(list(textos), show_progress_bar=False)

# ─────────────────────────────────────────────
#  3. COMPONENTE JURÍDICO Y COMBINACIÓN
# ─────────────────────────────────────────────

def extraer_features_juridicos(df: pd.DataFrame) -> np.ndarray:
    matriz = []
    for _, row in df.iterrows():
        fila = []
        for campo in CAMPOS_JURIDICOS:
            valor = row.get(campo, False)
            if isinstance(valor, str):
                valor = valor.lower() == "true"
            fila.append(int(bool(valor)))
        matriz.append(fila)
    return np.array(matriz, dtype=np.float32)

class VectorizadorCombinado(BaseEstimator, TransformerMixin):
    def __init__(self, vec_clase, peso_juridico: float = 3.0):
        self.vec_clase = vec_clase
        self.peso_juridico = peso_juridico
        self.vectorizador_ = vec_clase()

    def fit(self, X, y=None):
        textos = X["texto"].tolist()
        self.vectorizador_.fit(textos, y)
        return self

    def transform(self, X):
        textos = X["texto"].tolist()
        # Recuperar matriz de features (debe venir empaquetada en el DataFrame)
        features_juridicos = np.stack(X["features_juridicos"].values)
        
        embeddings = self.vectorizador_.transform(textos)
        features_amplificados = features_juridicos * self.peso_juridico
        
        return np.hstack([embeddings, features_amplificados])

# ─────────────────────────────────────────────
#  4. EXPERIMENTOS Y EVALUACIÓN
# ─────────────────────────────────────────────

VECTORIZADORES = {
    "spacy_avg": SpacyVectorizer,
    "sentence_bert": SentenceBertVectorizer,
}

CLASIFICADORES = {
    "SVM_RBF": SVC(kernel="rbf", C=1.5, probability=True, random_state=42),
    "RegLog": LogisticRegression(max_iter=1000, random_state=42),
    "RandomForest": RandomForestClassifier(n_estimators=200, random_state=42),
}

def construir_pipeline_combinado(vec_clase, clf, peso_juridico=3.0) -> Pipeline:
    return Pipeline([
        ("vectorizador", VectorizadorCombinado(vec_clase, peso_juridico)),
        ("clasificador", copy.deepcopy(clf)),
    ])

def evaluar_pipeline_combinado(pipe, df, y) -> dict:
    X_train, X_test, y_train, y_test = train_test_split(
        df, y, test_size=0.2, random_state=42, stratify=y
    )
    pipe.fit(X_train, y_train)
    y_pred = pipe.predict(X_test)
    
    # Nota: n_jobs=1 recomendado si usas SentenceBERT para evitar colisiones de memoria
    cv_scores = cross_val_score(pipe, df, y, cv=3, scoring="f1_weighted")

    return {
        "accuracy": round(accuracy_score(y_test, y_pred), 4),
        "f1": round(f1_score(y_test, y_pred, average="weighted"), 4),
        "cv_f1_mean": round(cv_scores.mean(), 4),
        "cv_f1_std": round(cv_scores.std(), 4),
        "report": classification_report(y_test, y_pred),
    }

# ─────────────────────────────────────────────
#  4b. PERSISTENCIA DE MÉTRICAS DEL MEJOR MODELO
# ─────────────────────────────────────────────

RUTA_METRICAS_MEJOR = "mejor_modelo_metricas.json"

def guardar_metricas_mejor_modelo(resultados: dict, timestamp: str):
    """
    Selecciona el modelo con mejor F1 del experimento actual,
    genera un JSON enriquecido con metadatos para evaluaciones futuras
    y lo persiste en disco. Sobreescribe sólo si el nuevo modelo supera
    al que ya estaba guardado.
    """
    if not resultados:
        print("Sin resultados que guardar.")
        return

    mejor_clave = max(resultados, key=lambda k: resultados[k]["f1"])
    mejor = resultados[mejor_clave]

    nuevo_registro = {
        "clave_experimento":   mejor_clave,
        "timestamp":           timestamp,
        "vectorizador":        mejor["vectorizador"],
        "clasificador":        mejor["clasificador"],
        "ruta_pkl":            mejor["ruta_pkl"],
        "metricas": {
            "accuracy":    mejor["accuracy"],
            "f1_weighted": mejor["f1"],
            "cv_f1_mean":  mejor["cv_f1_mean"],
            "cv_f1_std":   mejor["cv_f1_std"],
        },
        "campos_juridicos_usados": CAMPOS_JURIDICOS,
        "descripcion": (
            f"Mejor modelo del experimento {timestamp}. "
            f"Vectorizador: {mejor['vectorizador']}, "
            f"Clasificador: {mejor['clasificador']}."
        ),
    }

    # ── Comparar contra el mejor guardado previamente ──────────────────
    f1_previo = -1.0
    if os.path.exists(RUTA_METRICAS_MEJOR):
        with open(RUTA_METRICAS_MEJOR, "r", encoding="utf-8") as f:
            previo = json.load(f)
        f1_previo = previo.get("metricas", {}).get("f1_weighted", -1.0)

    if nuevo_registro["metricas"]["f1_weighted"] > f1_previo:
        with open(RUTA_METRICAS_MEJOR, "w", encoding="utf-8") as f:
            json.dump(nuevo_registro, f, indent=2, ensure_ascii=False)
        print(
            f"\n✔ Mejor modelo actualizado → F1: {nuevo_registro['metricas']['f1_weighted']} "
            f"(anterior: {f1_previo if f1_previo >= 0 else 'ninguno'})"
        )
        print(f"  Guardado en: {RUTA_METRICAS_MEJOR}")
    else:
        print(
            f"\n⚠ El modelo actual (F1={nuevo_registro['metricas']['f1_weighted']}) "
            f"no supera al guardado (F1={f1_previo}). No se sobreescribe."
        )

    return nuevo_registro


def cargar_metricas_mejor_modelo() -> dict:
    """
    Lee el JSON del mejor modelo guardado.
    Útil para pruebas futuras, reportes o comparaciones.
    """
    if not os.path.exists(RUTA_METRICAS_MEJOR):
        raise FileNotFoundError(
            f"No se encontró '{RUTA_METRICAS_MEJOR}'. "
            "Ejecuta comparar_todos() primero."
        )
    with open(RUTA_METRICAS_MEJOR, "r", encoding="utf-8") as f:
        return json.load(f)
    
def comparar_todos(ruta_json: str):
    with open(ruta_json, "r", encoding="utf-8") as f:
        data = json.load(f)

    df_raw = pd.DataFrame(data)
    etiquetas = df_raw["clase"].tolist()

    # Pre-procesamiento de entrada
    features_juridicos = extraer_features_juridicos(df_raw)
    df_input = pd.DataFrame({
        "texto": df_raw["descripcion_hechos"],
        "features_juridicos": list(features_juridicos),
    })

    os.makedirs("modelos", exist_ok=True)
    resultados = {}
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    for vec_nombre, vec_clase in VECTORIZADORES.items():
        print(f"\nVectorizador: {vec_nombre}")
        for clf_nombre, clf in CLASIFICADORES.items():
            print(f"  Entrenando {clf_nombre}...", end=" ", flush=True)
            try:
                pipe = construir_pipeline_combinado(vec_clase, clf)
                metricas = evaluar_pipeline_combinado(pipe, df_input, etiquetas)
                
                nombre = f"{vec_nombre}__{clf_nombre}__{timestamp}"
                ruta_pkl = f"modelos/{nombre}.pkl"
                joblib.dump(pipe, ruta_pkl)

                resultados[nombre] = {
                    "vectorizador": vec_nombre,
                    "clasificador": clf_nombre,
                    "ruta_pkl": ruta_pkl,
                    **{k: v for k, v in metricas.items() if k != "report"}
                }
                print(f"OK! F1: {metricas['f1']}")
            except Exception as e:
                print(f"Error: {e}")

    with open("resultados_comparacion.json", "w", encoding="utf-8") as f:
        json.dump(resultados, f, indent=2)
    
    guardar_metricas_mejor_modelo(resultados, timestamp)

# ─────────────────────────────────────────────
#  5. USO DEL MODELO (PREDICCIÓN)
# ─────────────────────────────────────────────

def predecir(texto: str, features_juridicos: dict = None):
    # Cargar el mejor basado en resultados_comparacion.json
    with open("resultados_comparacion.json", "r") as f:
        res = json.load(f)
    mejor_clave = max(res, key=lambda k: res[k]["f1"])
    pipe = joblib.load(res[mejor_clave]["ruta_pkl"])

    if features_juridicos is None:
        features_juridicos = {campo: False for campo in CAMPOS_JURIDICOS}

    f_vec = np.array([int(bool(features_juridicos.get(c, False))) for c in CAMPOS_JURIDICOS], dtype=np.float32)
    df_pred = pd.DataFrame({"texto": [texto], "features_juridicos": [f_vec]})
    
    return pipe.predict(df_pred)[0]

if __name__ == "__main__":
    # Para entrenar: python script.py
    # Para predecir: descomentar abajo
    comparar_todos("casos2.json")
    # print(predecir("Me negaron la inscripción por mi religión"))