# 📍 GeoMeasure Pro

Aplicación Android moderna para medición GPS de distancias y polígonos/áreas marcando puntos (A, B, C...), visualización de trayectoria en tiempo real y exportación de planos de alta definición.

---

## 📱 Cómo compilar e instalar el APK en tu teléfono (GitHub Actions)

Este repositorio viene configurado con un **flujo de trabajo automático de GitHub Actions** (`.github/workflows/build-apk.yml`).

### Opción A: Descargar directamente desde GitHub Releases (La más fácil)
1. Ve a la pestaña **Releases** en este repositorio de GitHub (o accede a `https://github.com/TU_USUARIO/TU_REPOSITORIO/releases`).
2. En la última versión (**latest**), haz clic en el archivo adjunto:
   - 📲 **`GeoMeasure-Pro.apk`**
3. Descárgalo directamente desde el navegador de tu celular.
4. Toca la descarga e instálala en tu dispositivo Android.

---

### Opción B: Descargar desde la pestaña "Actions"
1. Entra a la pestaña **Actions** en tu repositorio de GitHub.
2. Selecciona la ejecución más reciente del flujo de trabajo **"Build Android APK"**.
3. En la sección inferior **Artifacts**, haz clic en **`GeoMeasure-Pro-APK`** para descargar el archivo zip que contiene el APK.

---

### Opción C: Ejecutar la compilación manualmente en GitHub
1. En GitHub, ve a la pestaña **Actions**.
2. En el menú de la izquierda, selecciona **"Build Android APK"**.
3. Haz clic en el botón **"Run workflow"** -> **"Run workflow"**.
4. Espera 1-2 minutos a que termine la compilación y descarga el APK generado en la sección de Releases o Artifacts.

---

### ⚠️ Permiso de instalación en Android
Si es la primera vez que instalas una app descargada desde el navegador o gestor de archivos:
- Android te preguntará: *"Por tu seguridad, tu teléfono no tiene permitido instalar apps desconocidas de esta fuente"*.
- Toca en **Ajustes / Configuración** y activa la casilla **"Permitir desde esta fuente"**.
- Regresa y presiona **Instalar**.
