# Questionary — CLAUDE.md

## Project Overview

**Questionary** is a Spring Boot + Thymeleaf + SQLite web application for self-study practice. Users are shown a question one at a time, draft a mental response, reveal the official answer, and mark the question as succeeded or failed. An admin page manages the question database via `.txt` file imports.

---

## Tech Stack

- **Spring Boot 3.2.4** — web + Thymeleaf + Spring Data JPA
- **SQLite** — `sqlite-jdbc 3.45.2.0` + `hibernate-community-dialects`
- **Thymeleaf** — server-side templates (no JavaScript framework)
- **Maven** — build tool

---

## Project Structure

```
src/main/java/com/questionary/
├── QuestionaryApplication.java       # Entry point
├── entity/Question.java              # JPA entity
├── repository/QuestionRepository.java
├── service/
│   ├── ImportService.java            # .txt file parser (no Spring deps)
│   └── QuestionService.java          # Business logic
└── controller/
    ├── QuizController.java           # Quiz flow
    └── AdminController.java          # Admin import/manage

src/main/resources/
├── application.properties
├── templates/
│   ├── quiz.html                     # Question + answer reveal
│   ├── quiz-done.html                # All-done screen
│   └── admin.html                    # Import + question list
└── static/css/style.css
```

---

## Key Architecture Decisions

### Entity: `Question`
- `status` field: `null` = unanswered, `"SUCCESS"`, `"FAILED"` (plain String, not enum)
- `sortOrder` preserves import sequence for deterministic "next question" ordering
- User-typed draft is never persisted — only `status` is saved

### Quiz URL Contract
| Method | URL | Action |
|--------|-----|--------|
| GET | `/quiz` | Redirect to first unanswered question |
| GET | `/quiz/{id}` | Show question (answer hidden) |
| GET | `/quiz/{id}?showAnswer=true` | Show question with answer revealed |
| POST | `/quiz/{id}/skip` | Skip → next unanswered (excluding current) |
| POST | `/quiz/{id}/mark?status=SUCCESS\|FAILED` | Persist status → next |
| GET | `/quiz/done` | Summary screen |

"Show Answer" is a plain `<a>` link — no POST — so browser history works cleanly.

### Admin URL Contract
| Method | URL | Action |
|--------|-----|--------|
| GET | `/admin` | Question list + stats + upload form |
| POST | `/admin/import` | Upload `.txt`, parse and append to DB |
| POST | `/admin/reset-statuses` | Set all status → null |
| POST | `/admin/delete-all` | Delete all questions |

All mutations follow the POST-Redirect-GET pattern with `RedirectAttributes` flash messages.

### SQLite Config
```properties
spring.datasource.url=jdbc:sqlite:questionary.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.datasource.hikari.maximum-pool-size=1   # prevent SQLITE_BUSY
```
DB file is created in the working directory where the JAR is launched.

### Import File Format
```
?Question text here
Answer line 1
Answer line 2

?Next question
Answer
```
- Lines starting with `?` begin a new question block (the `?` is stripped)
- Subsequent non-blank lines accumulate as the answer
- Blank lines are ignored; only a new `?` line closes a block

---

## Running Locally

```bash
mvn spring-boot:run
```
App starts at `http://localhost:8080`. The DB file `questionary.db` is created on first run.

---

## Build

```bash
mvn package -DskipTests
java -jar target/questionary-0.0.1-SNAPSHOT.jar
```
