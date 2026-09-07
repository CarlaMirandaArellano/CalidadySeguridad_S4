# CalidadySeguridad_S4 — Backend + CI/CD (Jenkins + SonarQube)

Este proyecto es el backend de Spring Boot de la actividad de la Semana 4 (CDY2203), junto con la infraestructura de Docker necesaria para correr **Jenkins** y **SonarQube** y hacer análisis estático de código sobre el proyecto.

## Qué incluye este repositorio

- `src/` → código fuente del backend (Spring Boot 3.4.4, Java 17), incluyendo los módulos `invoice`, `care` y `medication` (facturas).
- `docker-compose.yaml` → levanta 3 servicios: la base de datos MySQL, Jenkins y SonarQube.
- `mysql/Dockerfile` → imagen personalizada de MySQL usada por el `docker-compose.yaml`.
- `pom.xml` / `mvnw` → proyecto Maven con su wrapper (no es necesario tener Maven instalado).

## Requisitos previos

- **Docker Desktop** instalado y corriendo.
- **Git**.
- Un puerto libre en tu computador para Jenkins (8081), SonarQube (9000) y MySQL (3306). Si alguno está ocupado, cámbialo en `docker-compose.yaml` (por ejemplo `"8082:8080"` en vez de `"8081:8080"`).

No necesitas tener Java ni Maven instalados: todo corre dentro de los contenedores / con el wrapper del proyecto.

## Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/CarlaMirandaArellano/CalidadySeguridad_S4.git
cd CalidadySeguridad_S4/backend
```

## Paso 2 — Levantar la infraestructura (MySQL + Jenkins + SonarQube)

Desde la carpeta `backend` (donde están el `docker-compose.yaml` y la carpeta `mysql/`):

```bash
docker compose up -d
```

Esto crea 3 contenedores:

- `mysql-cdy2203-s4` → base de datos (puerto 3306).
- `jenkins-cdy2203-s4` → Jenkins (puerto 8081 en tu navegador).
- `sonarqube-cdy2203-s4` → SonarQube (puerto 9000 en tu navegador).

Espera 1-2 minutos a que todo termine de iniciar (SonarQube tarda un poco más porque levanta Elasticsearch internamente). Puedes revisar el estado con:

```bash
docker compose ps
```

Todos los servicios deben aparecer como `Up`.

## Paso 3 — Configurar Jenkins (solo la primera vez)

1. Abre `http://localhost:8081` en tu navegador.
2. Jenkins te pide una contraseña inicial. Obténla con:
   ```bash
   docker exec jenkins-cdy2203-s4 cat /var/jenkins_home/secrets/initialAdminPassword
   ```
3. Elige **"Install suggested plugins"**.
4. Crea tu usuario administrador cuando te lo pida.
5. Ve a **"Manage Jenkins" → "Plugins" → "Available plugins"**, busca **"SonarQube Scanner"** e instálalo.

## Paso 4 — Configurar SonarQube (solo la primera vez)

1. Abre `http://localhost:9000` en tu navegador (si el login te marca error con `admin`/`admin`, prueba en una ventana de incógnito).
2. Entra con usuario `admin` y contraseña `admin`.
3. Te va a pedir cambiar la contraseña — defínela y guárdala en un lugar seguro.
4. Genera un token: ícono de tu perfil (arriba a la derecha) → **"My Account" → "Security"** → genera un **"Global Analysis Token"**. Cópialo, porque SonarQube solo lo muestra una vez.

## Paso 5 — Conectar Jenkins con SonarQube

1. En Jenkins, ve a **"Manage Jenkins" → "Credentials"** → agrega una credencial tipo **"Secret text"** con el token generado en el paso anterior. Ponle un ID reconocible, por ejemplo `sonarqube-token`.
2. Ve a **"Manage Jenkins" → "Tools"** (Global Tool Configuration) → en la sección **"SonarQube Scanner installations"**, agrega una instalación (puedes dejar que la instale automáticamente).
3. Ve a **"Manage Jenkins" → "System"** → sección **"SonarQube servers"** → agrega un servidor:
   - Name: `SonarQube` (o el nombre que prefieras)
   - Server URL: `http://sonarqube:9000` (así, con el nombre del servicio de Docker, **no** `localhost`, porque Jenkins y SonarQube se hablan entre contenedores)
   - Server authentication token: selecciona la credencial creada en el paso 1.

## Paso 6 — Crear el Job en Jenkins

1. Desde el dashboard de Jenkins, **"New Item"** → nombre del job (por ejemplo `backend-cdy2203-s4`) → tipo **"Freestyle project"**.
2. En **"Source Code Management"**, selecciona **"Git"** y pon la URL de este mismo repositorio:
   `https://github.com/CarlaMirandaArellano/CalidadySeguridad_S4.git`
3. En **"Build Environment"**, marca **"Prepare SonarQube Scanner environment"**.
4. En **"Build Steps"**, agrega dos pasos:
   - **"Execute shell"** con:
     ```bash
     cd backend
     ./mvnw clean install
     ```
   - **"Execute SonarQube Scanner"** con estas "Analysis properties":
     ```
     sonar.projectKey=backend-cdy2203-s4
     sonar.projectName=backend-cdy2203-s4
     sonar.sources=backend/src
     sonar.java.binaries=backend/target/classes
     ```
5. Guarda.

## Paso 7 — Correr el análisis

1. En el job, haz clic en **"Build Now"**.
2. Revisa el **"Console Output"** del build — debe terminar con `BUILD SUCCESS` y `Finished: SUCCESS`.
3. Entra a `http://localhost:9000`, abre el proyecto `backend-cdy2203-s4` y revisa los resultados en la pestaña **"Overall Code"** (todo el proyecto) y **"New Code"** (solo lo nuevo desde el último análisis).

## Notas / troubleshooting

- **Cada persona genera su propio token de SonarQube** — no se comparte, porque cada instalación de SonarQube (la tuya, la mía) es independiente y no comparte usuarios ni tokens.
- Si usas Mac y el proyecto está dentro de la carpeta **Desktop** o **Documents**, puede que la Terminal no tenga permiso para leer esas carpetas (macOS las protege). Si te aparece un error de `Operation not permitted`, dale permiso a la Terminal en **Configuración del Sistema → Privacidad y Seguridad → Acceso total al disco**, o mueve el proyecto a una carpeta fuera de Desktop/Documents (por ejemplo, directo en tu carpeta de usuario).
- Si `git add` o `git commit` te da un error de `index.lock`, es que un proceso de Git quedó "pegado". Cierra VSCode y cualquier otra terminal abierta con el proyecto, y si el error persiste, borra el archivo manualmente:
  ```bash
  rm .git/index.lock
  ```
- El Quality Gate de SonarQube puede salir en rojo por falta de cobertura de tests (0%) — es una limitación conocida del proyecto (no hay tests automatizados escritos todavía), no un error de configuración.
