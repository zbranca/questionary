# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Questionary** is a Spring Boot + Thymeleaf + SQLite web application for self-study practice. Users are shown a question one at a time, draft a mental response, reveal the official answer, and mark the question as succeeded or failed. An admin page manages the question database via `.txt` file imports.

---

## Tech Stack

- **Java 21**, **Spring Boot 3.2.4** — web + Thymeleaf + Spring Data JPA
- **SQLite** — `sqlite-jdbc 3.45.2.0` + `hibernate-community-dialects`
- **Thymeleaf** — server-side templates (no JavaScript framework); cache disabled for hot-reload in dev
- **Maven** — build tool; packages as **WAR** (deployable to external Tomcat or runnable as fat-JAR)

---

## Project Structure

```
src/main/java/com/questionary/
├── QuestionaryApplication.java       # Entry point
├── entity/
│   ├── Question.java                 # JPA entity
│   ├── QuestionStatus.java           # Enum: UNANSWERED, SUCCESS, FAILED
│   └── QuestionStatusConverter.java  # JPA converter (UNANSWERED ↔ null)
├── repository/QuestionRepository.java
├── service/
│   ├── ImportService.java            # .txt file parser
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
- `status` field: `QuestionStatus` enum — `UNANSWERED`, `SUCCESS`, `FAILED`
- `QuestionStatusConverter` maps `UNANSWERED` ↔ `null` in the DB column (so legacy null rows work)
- `sortOrder` preserves import sequence for deterministic "next question" ordering
- User-typed draft is never persisted — only `status` is saved

### Quiz URL Contract
| Method | URL | Action |
|--------|-----|--------|
| GET | `/quiz` | Redirect to first unanswered (or first failed in failed-only mode) |
| GET | `/quiz/{id}` | Show question (answer hidden) |
| POST | `/quiz/{id}/reveal` | Post draft text → render question with answer revealed |
| POST | `/quiz/{id}/skip` | Skip → next unanswered/failed (excluding current) |
| POST | `/quiz/{id}/mark?status=SUCCESS\|FAILED` | Persist status → next |
| GET | `/quiz/done` | Summary screen |

"Show Answer" submits a form via POST, sending the draft textarea content to `/quiz/{id}/reveal`.

### Failed-Only Mode
A session flag `failedOnlyMode` restricts quiz navigation to questions with `FAILED` status. Toggled via `POST /admin/toggle-failed-mode`; state shown in both the admin and quiz pages.

### Admin URL Contract
| Method | URL | Action |
|--------|-----|--------|
| GET | `/admin` | Question list + stats + upload form (supports `?q=` text search and `?statusFilter=` enum filter) |
| POST | `/admin/import` | Upload `.txt`, parse and append to DB |
| POST | `/admin/toggle-failed-mode` | Toggle session-based failed-only quiz mode |
| POST | `/admin/question/{id}/update` | Edit question text, answer text, and status |
| POST | `/admin/question/{id}/delete` | Delete single question |
| POST | `/admin/reset-statuses` | Set all statuses → UNANSWERED |
| POST | `/admin/delete-all` | Delete all questions |

All mutations follow the POST-Redirect-GET pattern with `RedirectAttributes` flash messages.

### SQLite Config
```properties
spring.datasource.url=jdbc:sqlite:questionary.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.datasource.hikari.maximum-pool-size=1   # prevent SQLITE_BUSY
spring.jpa.hibernate.ddl-auto=update           # schema auto-migrates on startup
```
DB file is created in the working directory where the JAR/WAR is launched. For an external Tomcat, override with:
```bash
export JAVA_OPTS="-Dspring.datasource.url=jdbc:sqlite:/absolute/path/questionary.db"
```

### Import File Format
```
#Question text here
Answer line 1
Answer line 2

#Multi-line question — line 1
#Line 2 of the same question
#
#```java
#int x = 5;
#```
Answer to the multi-line question

#Next question
Answer

@This is a comment line and is ignored
```
- Lines starting with `#` begin or continue a question block (the `#` is stripped)
- **Multiple consecutive `#` lines combine into a single multi-line question**
- A bare `#` line (nothing after `#`) embeds a blank line within the question text
- Lines starting with `@` are comment lines and are skipped
- Non-`#`, non-blank lines after a question block accumulate as the answer
- Blank file-lines are ignored in all states; a new `#` line after answer lines starts a fresh block

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
