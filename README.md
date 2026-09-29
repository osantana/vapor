# Vapor

Digital game store. Java 25, Javalin, PostgreSQL. Specification: `projeto-vapor.pdf`.

## Run

```bash
cp .env.example .env    # then change the passwords and the secret key
docker compose up --build
```

Open http://localhost:8080 and log in as administrator with `ADMIN_EMAIL` and `ADMIN_PASSWORD`.

To run from IntelliJ: open `pom.xml` as a project, start only the database with
`docker compose up db` and run `vapor.Main`.

## Contribute and deploy

Open a pull request against `main`; `mvn package` runs the tests.
Deploy the image built from the `Dockerfile` with the variables of `.env.example`.
