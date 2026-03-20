# Questionary

A self-study flashcard web application built with Spring Boot, Thymeleaf, and SQLite.

You are shown a question, draft your response mentally, reveal the official answer, and mark it as success or failure. Skipped questions cycle back. Progress is tracked per question until you reset or start fresh.

---

## Features

- **Quiz mode** — one question at a time with a draft textarea
- **Answer reveal** — submit your draft, then reveal the official answer and mark success/fail
- **Failed-only mode** — toggle to cycle through only previously failed questions
- **Skip** — move to the next unanswered question without marking
- **Progress bar** — tracks answered vs. remaining questions
- **Admin page** — import questions from a `.txt` file; search/filter by text or status; edit or delete individual questions; reset or delete all
- **Persistent status** — success/fail status saved in SQLite; draft text is never stored
- **Multi-user** — each user has an isolated question set; login required
- **User management** — admins can create, edit, and delete user accounts

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+

### Run

```bash
mvn spring-boot:run
```

Open `http://localhost:8080` — you will be redirected to the login page.

**Default credentials:** `admin` / `admin` (created automatically on first run)

After logging in, go to `/admin` to import questions, then `/quiz` to start.

### Build a JAR

```bash
mvn package -DskipTests
java -jar target/questionary-0.0.1-SNAPSHOT.jar
```

The SQLite database file `questionary.db` is created automatically in the working directory.

---

## Question File Format

Questions are imported from plain `.txt` files:

```
#What is the capital of France?
Paris is the capital of France.
It has been the capital since medieval times.

@This line is a comment and will be ignored

#What is the output of this program?
#
#```java
#public static void main(String[] args) {
#    System.out.println(2 + 3);
#}
#```
The output is 5.

#What does CPU stand for?
Central Processing Unit.
It is the primary component that executes instructions in a computer.
```

**Rules:**
- A line starting with `#` begins or continues a question block (the `#` is stripped)
- **Multiple consecutive `#` lines combine into a single multi-line question** — useful for code snippets
- A bare `#` line (nothing after `#`) embeds a blank line within the question text
- A line starting with `@` is a comment and is ignored
- Non-`#`, non-blank lines after a question block are the official answer
- Blank file-lines are ignored in all states

---

## Pages

| URL | Description |
|-----|-------------|
| `/login` | Login page |
| `/quiz` | Redirects to the first unanswered (or failed in failed-only mode) question |
| `/quiz/{id}` | Shows a specific question |
| `/quiz/done` | Summary screen after all questions are reviewed |
| `/admin` | Import questions, search/filter list, edit/delete, reset or delete all |
| `/admin-users` | User management — create, edit, delete accounts (ADMIN only) |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2 |
| Security | Spring Security (form login, role-based access) |
| Templates | Thymeleaf |
| Persistence | Spring Data JPA + SQLite |
| Build | Maven |

---

## Project Structure

```
src/main/java/com/questionary/
├── QuestionaryApplication.java
├── config/
│   ├── DataInitializer.java         # Creates default admin on first run
│   └── WebConfig.java
├── entity/
│   ├── AppUser.java                 # User entity with ROLE_ADMIN/ROLE_USER constants
│   ├── Question.java
│   ├── QuestionStatus.java          # Enum: UNANSWERED, SUCCESS, FAILED
│   └── QuestionStatusConverter.java # JPA converter (UNANSWERED ↔ null in DB)
├── repository/
│   ├── AppUserRepository.java
│   └── QuestionRepository.java
├── security/
│   ├── AppUserDetails.java          # Record implementing UserDetails
│   ├── AppUserDetailsService.java
│   └── SecurityConfig.java
├── service/
│   ├── ImportService.java           # .txt parser
│   ├── QuestionService.java
│   └── UserService.java
└── controller/
    ├── AdminController.java
    ├── AppErrorController.java
    ├── QuizController.java
    └── UserManagementController.java

src/main/resources/
├── application.properties
├── templates/
│   ├── admin.html
│   ├── admin-users.html
│   ├── error.html
│   ├── login.html
│   ├── quiz.html
│   └── quiz-done.html
└── static/css/style.css
```
