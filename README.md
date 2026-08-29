# Task Manager CLI (Java)

A lightweight console-based application built using Java demonstrating core Object-Oriented Programming (OOP) concepts, class encapsulation, and collection frameworks.

## Features
- **Object-Oriented Architecture:** Models domain entities (`Task`) and business logic controllers (`TaskManager`) independently.
- **Java Collections Framework:** Manages dynamic task storage using `ArrayList`.
- **Clean Structure:** Organizes code using standard package declarations.

## Project Structure
```text
java-task-manager/
├── bin/
├── src/
│   ├── App.java
│   ├── model/
│   │   └── Task.java
│   └── service/
│       └── TaskManager.java
├── .gitignore
└── README.md
```

## Prerequisites

* **JDK:** Java Development Kit (JDK 17 or higher installed and added to PATH).

## How to Build & Run

### 1. Clone the Repository

```bash
git clone [https://github.com/IzzatFirdaus/java-task-manager.git](https://github.com/IzzatFirdaus/java-task-manager.git)
cd java-task-manager
```

### 2. Compile Java Source Files

Compile all source files from the `src` directory into the output `bin` folder:

```powershell
javac -d bin src/App.java src/model/Task.java src/service/TaskManager.java
```

### 3. Run the Application

Execute the compiled entry point from the `bin` directory:

```powershell
java -cp bin App
```