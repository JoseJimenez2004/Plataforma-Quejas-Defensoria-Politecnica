import json
import joblib
import pandas as pd
import numpy as np
import os

# Importamos las definiciones originales para que joblib reconozca los objetos
# Sustituye 'modelos' por el nombre de tu archivo .py donde definiste SpacyVectorizer y VectorizadorCombinado
from modelos import (
    SpacyVectorizer, 
    SentenceBertVectorizer, 
    VectorizadorCombinado, 
    CAMPOS_JURIDICOS
)

# Lista exacta de tus archivos .pkl en la carpeta 'modelos/'
modelos_archivos = [
    "modelos/spacy_avg__SVM_RBF__20260429_215334.pkl",
    "modelos/spacy_avg__RegLog__20260429_212440.pkl",
    "modelos/spacy_avg__RandomForest__20260429_212440.pkl",
    "modelos/sentence_bert__SVM_RBF__20260429_212440.pkl",
    "modelos/sentence_bert__RegLog__20260429_215334.pkl",
    "modelos/sentence_bert__RandomForest__20260429_215334.pkl"
]

def ejecutar_pruebas_cruzadas(ruta_pruebas="pruebas2.json"):
    if not os.path.exists(ruta_pruebas):
        print(f"Error: No se encontró {ruta_pruebas}")
        return

    with open(ruta_pruebas, "r", encoding="utf-8") as f:
        datos_test = json.load(f)

    print(f"Cargadas {len(datos_test)} quejas de prueba.")
    
    # Vector vacío (todo False) para forzar clasificación por texto únicamente
    vector_vacio = np.zeros(len(CAMPOS_JURIDICOS), dtype=np.float32)
    
    resumen_final = []

    for ruta_pkl in modelos_archivos:
        nombre_modelo = os.path.basename(ruta_pkl).split("__2026")[0]
        print(f"Evaluando modelo: {nombre_modelo}...")
        
        try:
            pipe = joblib.load(ruta_pkl)
            resultados_json = []
            aciertos = 0

            for caso in datos_test:
                texto = caso["descripcion_hechos"]
                clase_real = caso["clase"]
                
                # Creamos el DataFrame con los features en False
                df_input = pd.DataFrame({
                    "texto": [texto], 
                    "features_juridicos": [vector_vacio]
                })
                
                # Inferencia
                prediccion = pipe.predict(df_input)[0]
                correcto = (prediccion == clase_real)
                if correcto: aciertos += 1
                
                resultados_json.append({
                    "Queja": texto[:100] + "...",
                    "Real": clase_real,
                    "Prediccion": prediccion,
                    "Correcto": "SÍ" if correcto else "NO"
                })

            # Guardar el JSON individual de este modelo
            nombre_salida = f"resultado_{nombre_modelo}.json"
            with open(nombre_salida, "w", encoding="utf-8") as f_out:
                json.dump(resultados_json, f_out, indent=2, ensure_ascii=False)
            
            resumen_final.append({
                "Modelo": nombre_modelo,
                "Aciertos": aciertos,
                "Total": len(datos_test),
                "Exactitud": f"{(aciertos/len(datos_test)):.2%}"
            })

        except Exception as e:
            print(f"  -> Error con {nombre_modelo}: {e}")

    # Imprimir Tabla Comparativa en Texto
    print("\n" + "="*80)
    print(f"{'RESUMEN DE PRUEBAS (MODO SOLO TEXTO)':^80}")
    print("="*80)
    df_resumen = pd.DataFrame(resumen_final)
    print(df_resumen.to_string(index=False))
    print("="*80)

if __name__ == "__main__":
    ejecutar_pruebas_cruzadas()