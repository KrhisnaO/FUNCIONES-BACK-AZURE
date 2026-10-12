# FUNCIONES-BACK-AZURE
 
Funciones serverless en Java 21 (Azure Functions) del Sistema de Gestión de Usuarios y Roles. Exponen operaciones CRUD mediante API REST y consultas mediante GraphQL sobre una base de datos Oracle Autonomous, e implementan una arquitectura basada en eventos con Azure Event Grid.
 
## Funciones
 
| Función | Trigger | Ruta |
| --- | --- | --- |
| UsuariosFunction | HTTP (GET, POST, PUT, DELETE) | /api/usuarios |
| RolesFunction | HTTP (GET, POST, PUT, DELETE) | /api/roles |
| UsuariosGraphQLFunction | HTTP POST (GraphQL) | /api/graphql/usuarios |
| RolesGraphQLFunction | HTTP POST (GraphQL) | /api/graphql/roles |
| GeneradorEventosFunction | HTTP POST | /api/GeneradorEventosFunction |
| ProcessEventGridEvent | Event Grid | - |
 
## Eventos
 
| Evento | Se publica cuando | Acción del consumidor (ProcessEventGridEvent) |
| --- | --- | --- |
| Usuarios.UsuarioCreado | Se crea un usuario | Asigna el rol por defecto (ID 1) si el usuario no tiene rol |
| Roles.RolEliminado | Se elimina un rol | Quita ese rol a los usuarios que lo tenían |
 
## Ejemplos de uso
 
Crear usuario:
 
```json
POST /api/usuarios
{ "nombre": "Ana Pérez", "correo": "ana@correo.cl" }
```
 
Eliminar rol:
 
```json
DELETE /api/roles
{ "id_rol": "2" }
```
 
Consulta GraphQL:
 
```json
POST /api/graphql/usuarios
{ "query": "{ usuarios { id nombre email } }" }
```
 
## Tecnologías
 
- Java 21, Maven
- Azure Functions (azure-functions-java-library)
- Oracle Autonomous Database (ojdbc11 + wallet en `src/main/resources/wallet`)
- graphql-java
- Azure Event Grid (azure-messaging-eventgrid)
## Base de datos
 
El script de creación de tablas está en `sql/script_bd.sql`.
 
## Compilar y desplegar
 
```bash
mvn clean package -DskipTests
mvn azure-functions:deploy
```

