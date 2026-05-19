# 📱 Hubly (RankUp) — Plataforma Multimódulo para Gestión de Eventos

Hubly es una aplicación móvil nativa para Android diseñada para revolucionar la organización y participación en eventos comunitarios y competitivos. Mediante una arquitectura robusta y modular, la plataforma permite a los usuarios crear eventos públicos o privados, comunicarse en tiempo real, controlar el aforo de manera transaccional y gestionar rankings dinámicos basados en la gamificación.

---

## 🚀 Características Principales

* **Gestión Integral de Eventos:** Creación de eventos personalizados filtrados por categorías estéticas automáticas (**Deportes, Social, E-Sports, Educación**).
* **Seguridad Avanzada:** Soporte para eventos privados con control de acceso restringido mediante contraseñas cifradas y validaciones automáticas.
* **Sistema de Rankings Reactivo:** Módulo de puntuación competitiva en tiempo real con lógicas avanzadas para la gestión de empates y actualizaciones masivas en cascada.
* **Chat en Tiempo Real:** Canal de comunicación integrado e instantáneo por cada evento para dinamizar la interacción de la comunidad.
* **Panel de Perfil Adaptativo:** Centro de control de reputación que expone métricas clave, historial de eventos, rating del usuario y badges/logros conseguidos.

---

## 🛠️ Stack Tecnológico & Infraestructura

La ingeniería del software de Hubly se ha cimentado sobre las tecnologías más modernas y demandadas del ecosistema de desarrollo nativo Android:

* **Lenguaje:** [Kotlin](https://kotlinlang.org/) (v2.0.21) con soporte para el compilador de alto rendimiento K2.
* **Interfaz de Usuario:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (BOM 2024.09.00) bajo los estándares de diseño de **Material Design 3**.
* **Arquitectura:** Patrón **MVVM (Model-View-ViewModel)** estricto con separación absoluta de responsabilidades y flujo reactivo de datos.
* **Inyección de Dependencias:** [Dagger Hilt](https://developer.android.com/technologies/dependency-injection/hilt-android) (v2.51.1) acoplado con KAPT para optimizar el ciclo de vida de los componentes.
* **Backend as a Service (BaaS):** Ecosistema de [Google Firebase](https://firebase.google.com/) embebido:
    * *Cloud Firestore (v26.1.0):* Base de datos NoSQL en la nube con sincronización instantánea orientada a documentos.
    * *Firebase Authentication (v24.0.1):* Gestión del ciclo de vida del usuario integrado con la API moderna de **Google Identity Manager / Credentials**.
* **Persistencia Local:** [Room KTX](https://developer.android.com/training/data-storage/room) (v2.8.4) para el almacenamiento seguro y caché local sincronizada con Corrutinas.
* **Librerías Core:** [Coil Compose](https://coil-kt.github.io/coil/) (v2.6.0) para el procesamiento asíncrono y renderizado de imágenes de red.

---

## ⚙️ Especificaciones Técnicas de Compilación

El proyecto se encuentra automatizado a nivel de Gradle empleando la API moderna de **Version Catalog** (`libs.versions.toml`):

* **Compile SDK:** 36 (Android 16.0 "Baklava")
* **Target SDK:** 36
* **Min SDK:** 26 (Android 8.0 Oreo)
* **Java JVM Target:** 11
* **Android Gradle Plugin (AGP):** 8.13.2

---

## 📦 Instalación y Configuración Local

Si deseas clonar el proyecto y ejecutarlo en tu entorno local de Android Studio, sigue estos pasos:

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/sergiorojo66/Hubly.git
