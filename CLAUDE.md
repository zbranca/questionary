# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Questionary** is a Spring Boot + Thymeleaf + SQLite web application for self-study practice. Users are shown a question one at a time, draft a mental response, reveal the official answer, and mark the question as succeeded or failed. An admin page manages the question database via `.txt` file imports.

---

## Tech Stack

- **Java 21**, **Spring Boot 3.2.4** — web + Thymeleaf + Spring Data JPA
- **SQLite** — `sqlite-jdbc 3.45.2.0` + `hibernate-community-dialects`
- **Thymeleaf** — server-side templates (no JavaScript framework); cache disabled for hot-reload in dev
- **Maven** — build tool; packages as **JAR** (runnable with embedded Tomcat)

---

## Project Structure

```
src/main/java/com/questionary/
├── QuestionaryApplication.java       # Entry point; standard Spring Boot app
├── config/
│   ├── DataInitializer.java          # Creates default admin on first run
│   └── WebConfig.java                # MVC config (e.g. resource handlers)
├── entity/
│   ├── AppUser.java                  # JPA entity; ROLE_ADMIN / ROLE_USER constants
│   ├── Chapter.java                  # JPA entity; groups questions into chapters
│   ├── Question.java                 # JPA entity (with chapter FK)
│   ├── QuestionStatus.java           # Enum: UNANSWERED, SUCCESS, FAILED
│   └── QuestionStatusConverter.java  # JPA converter (UNANSWERED ↔ null)
├── repository/
│   ├── AppUserRepository.java
│   ├── ChapterRepository.java        # Find chapters by user; find-or-create for imports
│   └── QuestionRepository.java       # Includes chapter-filtered query variants
├── security/
│   ├── AppUserDetails.java           # Record wrapping AppUser; implements UserDetails
│   ├── AppUserDetailsService.java    # Loads user by username for Spring Security
│   └── SecurityConfig.java          # Form login, route access rules, password encoder
├── service/
│   ├── ImportService.java            # .txt file parser
│   ├── QuestionService.java          # Business logic
│   └── UserService.java             # User CRUD and admin-count guard
└── controller/
    ├── AdminController.java          # Admin import/manage
    ├── AppErrorController.java       # Custom error page
    ├── QuizController.java           # Quiz flow
    └── UserManagementController.java # Admin user management (ADMIN only)

src/main/resources/
├── application.properties
├── templates/
│   ├── admin.html                    # Import + question list
│   ├── admin-users.html              # User list + create/edit/delete (ADMIN only)
│   ├── error.html                    # Custom error page
│   ├── login.html                    # Spring Security login form
│   ├── quiz.html                     # Question + answer reveal
│   └── quiz-done.html                # All-done screen
└── static/css/style.css
```

---

## Key Architecture Decisions

### Spring Security + Multi-user
- `SecurityConfig` configures form login at `/login`, logout, and per-role access rules
- `ADMIN` role: full access including `/admin/**` and `/admin-users/**`
- `USER` role: quiz and admin question management, no user management
- `AppUserDetails` is a Java **record** wrapping `AppUser` — use `principal.user()` (not `getUser()`)
- `DataInitializer` creates a default `admin/admin` account on first run if no users exist
- `AppUser` exposes `ROLE_ADMIN` and `ROLE_USER` constants — use these instead of hardcoded strings

### User Management URL Contract
| Method | URL | Action |
|--------|-----|--------|
| GET | `/admin-users` | List all users (ADMIN only) |
| POST | `/admin-users/create` | Create new user |
| POST | `/admin-users/{id}/edit` | Edit username, password, or role |
| POST | `/admin-users/{id}/delete` | Delete user (cannot delete self or last admin) |

### Entity: `Chapter`
- `name` field: chapter name (e.g., "Java Basics", "Advanced Patterns")
- `sortOrder`: preserves import sequence for stable chapter ordering
- `user` FK: chapters are isolated per user
- Chapters are created automatically during import when `@@Chapter Name` is encountered
- Find-or-create by name during import prevents duplicates if the same chapter is re-imported
- Displayed in admin question table; filterable by chapter in quiz

### Entity: `Question`
- `status` field: `QuestionStatus` enum — `UNANSWERED`, `SUCCESS`, `FAILED`
- `QuestionStatusConverter` maps `UNANSWERED` ↔ `null` in the DB column (so legacy null rows work)
- `sortOrder` preserves import sequence for deterministic "next question" ordering
- `chapter` FK: optional reference to a `Chapter` entity (nullable, so legacy questions with no chapter work)
- `chapterName` (@Transient): scratch field used only during import parsing; never persisted to DB
- User-typed draft is never persisted — only `status` is saved
- Questions are isolated per user — all queries are scoped by `AppUser`
- Chapter name is displayed in the quiz question card and in the admin questions table

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
| GET | `/admin` | Question list + stats + upload form (supports `?q=` text search, `?statusFilter=` enum filter, `?chapterFilter=` chapter name filter) |
| POST | `/admin/import` | Upload `.txt`, parse and append to DB; creates chapters as needed |
| POST | `/admin/set-chapter-filter` | Store selected chapter IDs in session for quiz filtering |
| POST | `/admin/toggle-failed-mode` | Toggle session-based failed-only quiz mode |
| POST | `/admin/question/{id}/update` | Edit question text, answer text, and status (chapter is read-only, assigned at import) |
| POST | `/admin/question/{id}/delete` | Delete single question |
| POST | `/admin/reset-statuses` | Set all statuses → UNANSWERED |
| POST | `/admin/delete-all` | Delete all questions |

All mutations follow the POST-Redirect-GET pattern with `RedirectAttributes` flash messages.

### Chapter Filter Session
A session map `selectedChapterIds` (type `Set<Long>`) holds the IDs of chapters selected for quiz viewing. When populated, the quiz only shows questions from those chapters. When empty (default), all chapters are shown. Managed via `POST /admin/set-chapter-filter`.

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
@@Chapter Name
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
@This line is now regular content (part of the answer)

@@Another Chapter
#New question
Its answer
```
- Lines starting with `@@` begin a new chapter (the `@@` prefix is stripped; name is trimmed)
  - All following questions belong to that chapter until the next `@@` or end of file
  - Blank lines between a chapter header and the first `#` are ignored
  - Chapter names are automatically deduplicated: re-importing a file with the same chapter name reuses the existing `Chapter` entity
- Lines starting with `#` begin or continue a question block (the `#` is stripped)
  - **Multiple consecutive `#` lines combine into a single multi-line question**
  - A bare `#` line (nothing after `#`) embeds a blank line within the question text
- Lines starting with `@` (single `@`, not `@@`) are now treated as regular answer content (no longer comments)
- Non-`#`, non-blank, non-`@@` lines after a question block accumulate as the answer
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
