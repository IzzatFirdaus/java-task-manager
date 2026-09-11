# Task Manager CLI (Java)

A lightweight console-based application built using Java demonstrating core Object-Oriented Programming (OOP) concepts, class encapsulation, and collection frameworks.

## Features
- **Object-Oriented Architecture:** Models domain entities (`Task`) and business logic controllers (`TaskManager`) independently.
- **Java Collections Framework:** Manages dynamic task storage using `ArrayList`.
- **Clean Structure:** Organizes code using standard package declarations.
- **HTTP API Mode:** Built-in REST API server with vanilla JavaScript frontend (zero runtime dependencies).

## Project Structure
```text
java-task-manager/
├── bin/                           # Compiled class files
├── data/                          # Runtime data storage
│   └── tasks.json                 # Task persistence file
├── lib/                           # External libraries (JUnit 5 for testing)
├── public/                        # Web frontend (HTML/CSS/JS)
│   ├── index.html                 # SPA dashboard
│   ├── styles.css                 # Modern CSS styling
│   └── app.js                     # Vanilla ES6 API client
├── src/
│   ├── App.java                   # Entry point (supports --web flag)
│   ├── api/                       # HTTP API layer
│   │   └── TaskHttpServer.java    # REST server implementation
│   ├── cli/                       # Command-line interface
│   │   ├── Command.java
│   │   └── CommandLineInterface.java
│   ├── exception/                 # Custom exception types
│   ├── model/                     # Domain models
│   │   ├── Priority.java
│   │   └── Task.java
│   ├── persistence/               # Data persistence layer
│   │   ├── FileTaskRepository.java
│   │   └── TaskRepository.java
│   ├── service/                   # Business logic
│   │   └── TaskManager.java
│   └── util/                      # Utility classes
│       ├── ConsolePrinter.java
│       ├── IdGenerator.java
│       ├── InputReader.java
│       └── JsonUtils.java
├── .gitignore
└── README.md
```

## Prerequisites

* **JDK:** Java Development Kit (JDK 17 or higher installed and added to PATH).

## How to Build & Run

### 1. Clone the Repository

```bash
git clone https://github.com/IzzatFirdaus/java-task-manager.git
cd java-task-manager
```

### 2. Compile Java Source Files

Compile all source files from the `src` directory into the output `bin` folder:

```powershell
# Compile all Java files recursively
javac -d bin $(Get-ChildItem -Recurse -Filter *.java src | Resolve-Path -Relative)
```

Or compile individual files:

```powershell
javac -d bin src/App.java src/model/Task.java src/service/TaskManager.java
```

### 3. Run the Application

#### CLI Mode (Default)

Execute the compiled entry point from the `bin` directory:

```powershell
java -cp bin App
```

This starts an interactive REPL where you can manage tasks using commands like:
- `add <title>` - Create a new task
- `list` - List all pending tasks
- `complete <id>` - Mark a task as complete
- `delete <id>` - Delete a task
- `exit` - Save and quit

#### Web Mode

Start the HTTP API server with a web dashboard:

```powershell
java -cp bin App --web
```

This starts a REST API server on `http://localhost:8080` and serves a web dashboard at `http://localhost:8080/`.

For verbose logging (useful for debugging):

```powershell
java -cp bin App --web --verbose
```

**Web API Endpoints:**

| Method   | Endpoint                     | Description                    |
|----------|------------------------------|--------------------------------|
| GET      | `/api/tasks`                 | List all tasks                 |
| POST     | `/api/tasks`                 | Create a new task              |
| GET      | `/api/tasks/{id}`            | Get task by ID                 |
| PUT      | `/api/tasks/{id}`            | Update a task                  |
| POST     | `/api/tasks/{id}/complete`   | Mark task as complete          |
| POST     | `/api/tasks/{id}/uncomplete` | Mark task as pending           |
| DELETE   | `/api/tasks/{id}`            | Delete a task                  |

**API Request/Response Examples:**

Create a task:
```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/tasks -Method POST -Body '{"title":"My Task","description":"Task details","priority":"HIGH"}' -ContentType "application/json"
```

List tasks:
```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/tasks
```

## Testing

Run the test suite using JUnit 5 console launcher:

```powershell
java -jar lib/junit-platform-console-standalone-*.jar --class-path bin --select-package service
```

## Architecture

See `ARCHITECTURE.md` for detailed technical design documentation.