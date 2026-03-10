# Questionary

A self-study flashcard web application built with Spring Boot, Thymeleaf, and SQLite.

You are shown a question, draft your response mentally, reveal the official answer, and mark it as success or failure. Skipped questions cycle back. Progress is tracked per question until you reset or start fresh.

---

## Features

- **Quiz mode** — one question at a time with a draft textarea
- **Answer reveal** — show the official answer on demand, then mark success/fail
- **Skip** — move to the next unanswered question without marking
- **Progress bar** — tracks answered vs. remaining questions
- **Admin page** — import questions from a `.txt` file; reset or delete all
- **Persistent status** — success/fail status saved in SQLite; draft text is never stored

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+

### Run

```bash
mvn spring-boot:run
```

Open `http://localhost:8080/admin` to import questions, then `http://localhost:8080/quiz` to start.

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
?What is the capital of France?
Paris is the capital of France.
It has been the capital since medieval times.

?What is the chemical symbol for water?
H2O — two hydrogen atoms bonded to one oxygen atom.

?What does CPU stand for?
Central Processing Unit.
It is the primary component that executes instructions in a computer.
```

**Rules:**
- A line starting with `?` begins a new question (the `?` is stripped from the text)
- Lines below (until the next `?` line) are the official answer
- Blank lines are ignored and used as visual separators

---

## Pages

| URL | Description |
|-----|-------------|
| `/quiz` | Redirects to the first unanswered question |
| `/quiz/{id}` | Shows a specific question |
| `/quiz/{id}?showAnswer=true` | Shows the question with the official answer revealed |
| `/quiz/done` | Summary screen after all questions are reviewed |
| `/admin` | Import questions, view list, reset or delete |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2 |
| Templates | Thymeleaf |
| Persistence | Spring Data JPA + SQLite |
| Build | Maven |

---

## Project Structure

```
src/main/java/com/questionary/
├── QuestionaryApplication.java
├── entity/Question.java
├── repository/QuestionRepository.java
├── service/
│   ├── ImportService.java       # .txt parser
│   └── QuestionService.java
└── controller/
    ├── QuizController.java
    └── AdminController.java

src/main/resources/
├── application.properties
├── templates/
│   ├── quiz.html
│   ├── quiz-done.html
│   └── admin.html
└── static/css/style.css
```
