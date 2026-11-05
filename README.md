🐦 Zazili — Jugar es aprender

Proyecto desarrollado para Hack4Edu 2026 — Modalidad Innovadora

Zazili es una aplicación Android nativa diseñada para niñas y niños con TDAH y dificultades de atención. En lugar de exigir que el niño se adapte al ritmo de la tecnología, Zazili adapta la tecnología al sistema sensorial y cognitivo del niño.

🎯 El Problema

Las plataformas educativas tradicionales asumen que el niño puede quedarse quieto, leer instrucciones largas y sostener la motivación por cuarenta minutos. Para un niño con TDAH, el error suele castigarse con temporizadores agotados o tachas rojas, generando frustración, evitación y daños a la autoestima.

✨ La Solución

Zazili invierte la lógica. Es un entorno seguro con sesiones cortas, instrucciones de una línea, movimiento, pausas y logros visibles.

El error es información: No hay castigos, solo pistas y segundas oportunidades.

El descanso es aprendizaje: Zazi (el compañero virtual) se cansa y pide dormir. Enseña autorregulación limitando el tiempo de pantalla orgánicamente.

🚀 Características Principales

🧠 Diagnóstico Adaptativo: Evalúa 4 vectores (lógica, lectura, velocidad, memoria) al inicio para calibrar la dificultad y los relojes de los minijuegos.

🗣️ Motor de Voz Neural Local (ONNX): La voz de Zazi corre 100% offline en el dispositivo usando un modelo Piper VITS. Garantiza consistencia sensorial (siempre suena igual, sin voces robóticas) sin depender de internet.

💬 IA Conversacional Segura: Chat integrado impulsado por Gemini 3.1 Flash Lite. Cuenta con límites pedagógicos estrictos (respuestas de 3 oraciones máximo), filtros de seguridad para evitar consejos médicos, y memoria corta para proteger la privacidad.

🧩 UX Neurodivergente: Modo Zen (bajo contraste), texto biónico para anclar lectura, y botones de altavoz ubicuos.

📊 El Nido (Panel Adulto): Progreso e hitos accesibles mediante PIN parental, diseñados para acompañar, no para vigilar.

🛠️ Arquitectura y Tecnologías

UI: Jetpack Compose (100% declarativo).

Lenguaje: Kotlin + Corrutinas.

Persistencia Local: DataStore (Preferencias y Perfiles) y Room. Toda la información sensible del niño vive estrictamente en su dispositivo.

Audio AI: Lector ONNX nativo integrado mediante JNI.

LLM: Google Generative AI SDK (gemini-3.1-flash-lite).

⚙️ Instrucciones de Instalación (Para Jueces y Desarrolladores)

Por motivos de seguridad, la clave de la API de Google Gemini no está incluida en este repositorio. Para compilar y ejecutar el proyecto con la funcionalidad de chat activa, entonces hay dos métodos de instalación el Try-Hard y el sencillo:

A. Try-Hard:
Clona el repositorio:

git clone https://github.com/tu-usuario/zazili.git


Abre el proyecto en Android Studio.

En la raíz del proyecto, abre (o crea si no existe) el archivo local.properties.

Añade las siguientes líneas con tu propia clave de Google AI Studio:

GEMINI_API_KEY=tu_clave_api_aqui
GEMINI_MODEL_NAME=gemini-3.1-flash-lite


(Nota: No uses comillas alrededor del valor en este archivo).

Sincroniza Gradle (Sync Project with Gradle Files).

Ejecuta la aplicación en un emulador o dispositivo físico (API 28+).

B. El sencillo XD:

Aunque también para facilidades del jurado hemos incluido link para 'Zazili_JugarEsAprender_V.1.0.1.apk' en cualquier teléfono Android en la carpeta 'installer_package_apk' desde drive
Y ademas en la carpeta ´documentacion´ dentro del repositorio vienen todos los archivos con información como video demos, Metodología y UX y un Resumen/Abstract. 

Link al drive: https://drive.google.com/drive/folders/1rx7eBD77yvfaLscpBjnTkVh-ZT2JfIKE?usp=sharing

👥 Equipo ZaziliTeam (Hack4Edu 2026)

Benjamín Monroy Hernández

Fátima Annett Solano

Karen Castañeda Beltrán

"Si un solo niño termina la tarea sintiendo que 'sí pudo', ya ganamos."
