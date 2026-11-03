# Vapor

Digital game store. Java 25, Javalin, PostgreSQL. Specification: `projeto-vapor.pdf`.

## Run

```bash
docker compose up --build
```

Open http://localhost:8080. Local accounts (all with simple demo passwords):

| Role          | E-mail                  | Password    |
|---------------|-------------------------|-------------|
| Administrator | `admin@example.com`     | `admin12345`|
| Publisher     | `publisher@example.com` | `demo12345` |
| Player        | `player@example.com`    | `demo12345` |

The demo games are loaded on the first start when `DEMO_DATA=true`. To change any value, copy
`.env.example` to `.env` and edit it.

To run from IntelliJ: open `pom.xml` as a project, start only the database with
`docker compose up db` and run `vapor.Main`.

## Contribute and deploy

Open a pull request against `main`; `mvn package` builds the jar.
Deploy the image built from the `Dockerfile` with the variables of `.env.example`.
