# Vapor

Digital game store. Java 25, Javalin, PostgreSQL. Specification: `projeto-vapor.pdf`.

## Run

```bash
docker compose up --build
```

Open http://localhost:8080. The local administrator is `admin@example.com` with the password
`admin12345`. To change any value, copy `.env.example` to `.env` and edit it.

To run from IntelliJ: open `pom.xml` as a project, start only the database with
`docker compose up db` and run `vapor.Main`.

## Contribute and deploy

Open a pull request against `main`; `mvn package` runs the tests.
Deploy the image built from the `Dockerfile` with the variables of `.env.example`.
