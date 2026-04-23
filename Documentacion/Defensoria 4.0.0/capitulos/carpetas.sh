#!/bin/bash

# Crear carpeta raíz del proyecto
mkdir -p "PROYECTO_TT"
cd "PROYECTO_TT"

# Lista de Actores según tus diagramas
actores=("QUEJOSO" "RECEPCIONISTA" "ANALISTA_PRIMER_CONTACTO" "SUBDEFENSORIA" "DEFENSOR" "ADMINISTRADOR")

# Diccionarios de Casos de Uso por Actor (Extraídos de tus imágenes)
declare -A cu_quejoso=( [01]="Levantar_una_queja" [02]="Crear_Cuenta" [03]="Solicitar_folio" [05]="Iniciar_sesion" [06]="Recuperar_contrasenia" [09]="Levantar_queja_desde_perfil" [10]="Consultar_tablero_de_quejas" [11]="Revisar_sus_quejas" [12]="Modificar_queja" [13]="Eliminar_queja" [14]="Adjuntar_evidencia_extra" [15]="Revisar_linea_de_tiempo" [16]="Revisar_notificaciones" [17]="Consultar_propuesta_conciliacion" [18]="Aceptar_propuesta" [19]="Rechazar_propuesta" [20]="Configurar_perfil" [21]="Agregar_datos_contacto" [22]="Agregar_datos_tutor" )

declare -A cu_recep=( [19]="Iniciar_sesion_admin" [20]="Consultar_bandeja_quejas" [21]="Validar_requisitos" [22]="Registrar_correspondencia_fisica" [23]="Asignar_turno" [24]="Buscar_antecedentes" [25]="Agregar_agenda_citas" [26]="Registrar_queja_fisica" )

declare -A cu_analista=( [27]="Abrir_queja" [30]="Validar_queja" [31]="Rechazar_queja" [33]="Buscar_antecedencia" [35]="Canalizar_abogado" [36]="Acceder_menu_opciones" [37]="Acceder_bandeja_analisis" [38]="Acceder_dashboard_expedientes" [39]="Analizar_queja" [40]="Acceder_expediente" [41]="Determinar_competencia" [43]="Generar_remision" )

declare -A cu_subdef=( [51]="Acceder_menu_opciones" [52]="Acceder_bandeja_acuerdos" [53]="Acceder_notificaciones" [54]="Acceder_expedientes" [55]="Solicitar_actos_investigacion" [56]="Gestionar_plazos_respuesta" [57]="Enviar_oficio_UA" [58]="Determinar_conclusion" [60]="Elaborar_acuerdo" )

declare -A cu_defensor=( [63]="Acceder_menu_opciones" [64]="Consultar_panel_quejas_acuerdo" [65]="Firmar_acuerdos_digitalmente" [66]="Consultar_tablero_gestion_global" [67]="Consultar_estadisticas_generales" [68]="Cambiar_status_finalizado" [69]="Enviar_a_archivo" )

declare -A cu_admin=( [70]="Alta_Baja_personal" [71]="Asignar_roles_permisos" [72]="Gestionar_catalogo_dependencias" [73]="Gestionar_copias_seguridad" [74]="Configurar_plantillas_documentos" )

for actor in "${actores[@]}"; do
    # Crear subcarpetas estándar
    base="INFO/$actor"
    mkdir -p "$base/CASOS_DE_USO"
    mkdir -p "$base/MOCKUPS"
    mkdir -p "$base/Diagrama_de_Secuencia"
    mkdir -p "$base/Diagrama_de_Actividades"
    mkdir -p "$base/Base_de_Datos"
    mkdir -p "$base/CAT_Mensajes"
    mkdir -p "$base/CAT_ERRORES"
    mkdir -p "$base/Documentacion"

    # Crear archivos base
    touch "$base/MOCKUPS/mockups.tex"
    touch "$base/Diagrama_de_Secuencia/diagramasecuencia.tex"
    touch "$base/Base_de_Datos/basedatos.tex"
    touch "$base/CAT_Mensajes/catmensajes.tex"
    touch "$base/CAT_ERRORES/caterrores.tex"
    touch "$base/Documentacion/documentacion.tex"

    # Generar archivos de Casos de Uso y Actividades según el actor
    case $actor in
        "QUEJOSO") for i in "${!cu_quejoso[@]}"; do touch "$base/CASOS_DE_USO/CU${i}_${cu_quejoso[$i]}.tex"; touch "$base/Diagrama_de_Actividades/DA${i}_${cu_quejoso[$i]}.tex"; done ;;
        "RECEPCIONISTA") for i in "${!cu_recep[@]}"; do touch "$base/CASOS_DE_USO/CU${i}_${cu_recep[$i]}.tex"; touch "$base/Diagrama_de_Actividades/DA${i}_${cu_recep[$i]}.tex"; done ;;
        "ANALISTA_PRIMER_CONTACTO") for i in "${!cu_analista[@]}"; do touch "$base/CASOS_DE_USO/CU${i}_${cu_analista[$i]}.tex"; touch "$base/Diagrama_de_Actividades/DA${i}_${cu_analista[$i]}.tex"; done ;;
        "SUBDEFENSORIA") for i in "${!cu_subdef[@]}"; do touch "$base/CASOS_DE_USO/CU${i}_${cu_subdef[$i]}.tex"; touch "$base/Diagrama_de_Actividades/DA${i}_${cu_subdef[$i]}.tex"; done ;;
        "DEFENSOR") for i in "${!cu_defensor[@]}"; do touch "$base/CASOS_DE_USO/CU${i}_${cu_defensor[$i]}.tex"; touch "$base/Diagrama_de_Actividades/DA${i}_${cu_defensor[$i]}.tex"; done ;;
        "ADMINISTRADOR") for i in "${!cu_admin[@]}"; do touch "$base/CASOS_DE_USO/CU${i}_${cu_admin[$i]}.tex"; touch "$base/Diagrama_de_Actividades/DA${i}_${cu_admin[$i]}.tex"; done ;;
    esac
done

echo "¡Estructura completada para los 80+ archivos!"
