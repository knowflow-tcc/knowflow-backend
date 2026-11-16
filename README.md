# KnowFlow - Backend

API REST do sistema KnowFlow (TCC - UCSal): gestão do conhecimento sobre problemas,
causas e soluções, com busca por índice hash, categorias em árvore, histórico,
painel de indicadores e relatórios em PDF.

Java 17, Spring Boot 3, Spring Security (BCrypt), Spring Data JPA e PostgreSQL.
Camadas: Controller → Service → Repository → Model, com DTOs.

## Pré-requisitos
JDK 17, Maven e PostgreSQL com um banco chamado `fluxoconhecimento`
(guia em `docs/guias/01-instalar-postgresql.md`).

## Como rodar
1. Clone os dois repositórios **na mesma pasta**, o frontend com o nome `frontend`:
   ```
   git clone https://github.com/knowflow-tcc/knowflow-backend.git
   git clone https://github.com/knowflow-tcc/knowflow-frontend.git frontend
   ```
2. Dentro de `knowflow-backend`:
   - normal: `mvn spring-boot:run`
   - com dados de demonstração (senha `Knowflow2026`):
     `mvn spring-boot:run "-Dspring-boot.run.arguments=--knowflow.demo=true"`
   - testes automatizados: `mvn test`
3. Acesse http://localhost:8080 (Swagger em http://localhost:8080/swagger-ui.html).

As senhas do `application.properties` (banco `postgres` e admin inicial `Admin1234`)
são só para desenvolvimento local.

## Documentação
- `docs/banco/` - script do banco e backup
- `docs/postman/` - coleção do Postman com os casos de teste
- `docs/rastreabilidade.md` - requisitos x código
- `docs/guias/` - instalação, Postman/Swagger e GitHub
