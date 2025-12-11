# PROJECT_SPEC.md: Koog Code Reviewer (ACP Edition)

## 1. Project Overview
We are building a **Server-side AI Agent** using the **JetBrains Koog Framework**. This agent will function as an automated code reviewer. It acts as a CLI tool that communicates with IntelliJ IDEA via the **Agent Client Protocol (ACP)** over Standard Input/Output (stdio).

**Goal:** The user can ask the agent to "Review this file" in the IntelliJ AI Chat. The agent will read the file from the disk, perform basic static analysis (mocked or simple regex for V1), and return structured feedback.

## 2. Technical Stack
*   **Language:** Kotlin (JVM)
*   **Build System:** Gradle (Kotlin DSL)
*   **Framework:** JetBrains Koog (latest version)
*   **Communication:** Agent Client Protocol (ACP) via `stdio`
*   **Serialization:** `kotlinx.serialization` (JSON)

## 3. System Architecture
```
graph TD
    IntelliJ[IntelliJ AI Chat] -- JSON-RPC over Stdio --> Wrapper[Gradle Wrapper Script]
    Wrapper -- Invokes --> Main[Main.kt / ACPServer]
    Main -- Routes --> Agent[Koog Agent Logic]
    Agent -- Calls --> Tools[Agent Tools]
    Tools -- Reads --> FS[File System]
```


## 4. Implementation Plan (Checklist)

### Phase 1: Project Initialization
- [ ] Initialize a new Kotlin/JVM project using Gradle.
- [ ] Configure `build.gradle.kts` with the `application` plugin.
- [ ] Add dependencies:
    -   `org.jetbrains.koog:koog-core` (and any ACP-specific modules if separate).
    -   `org.jetbrains.kotlinx:kotlinx-serialization-json`.
    -   `ch.qos.logback:logback-classic` (for logging, ensuring logs don't pollute stdio).

### Phase 2: The Core Agent Infrastructure
- [ ] Create `src/main/kotlin/Main.kt`.
- [ ] Implement the entry point that parses args.
- [ ] If arg is `"acp"`, start the `ACPServer`.
- [ ] Define a basic "Hello World" agent to verify the pipeline.
- [ ] Configure the `installDist` task to generate the executable.

### Phase 3: Tool Implementation
- [ ] **Tool 1: `ReadFileTool`**
    -   **Function:** `readFile(path: String): String`
    -   **Description:** Reads text content from a local file path relative to the project root.
    -   **Error Handling:** Return a clean error message string if file not found.
- [ ] **Tool 2: `SimpleLinterTool`**
    -   **Function:** `scanCode(content: String): String`
    -   **Description:** Analyzes a string of code for specific bad patterns (e.g., `println` usage, `TODO` comments, or hardcoded API keys).
    -   **Output:** A formatted string listing warnings found.

### Phase 4: Agent Logic & Prompts
- [ ] **System Prompt:** "You are a senior code reviewer. You must use the `ReadFileTool` to inspect files before answering. When you find issues using `SimpleLinterTool`, explain *why* they are bad practice."
- [ ] **Wiring:** Register the tools with the Koog Agent instance in `Main.kt`.

### Phase 5: Deployment & Integration
- [ ] Run `./gradlew installDist`.
- [ ] Create/Update `acp.json` in the user's home or project configuration to point to the generated script.
- [ ] Verify connection in IntelliJ AI Chat.

---

## 5. Detailed Specifications

### 5.1 Gradle Configuration (`build.gradle.kts`)
Ensure the application plugin is configured correctly so the script is executable.
```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
    application
}

application {
    mainClass.set("com.example.codereview.MainKt")
}

tasks.installDist {
    // Ensure the output is predictable for the acp.json path
}
```


### 5.2 Tool Definitions (Koog DSL)
The tools should be defined using the idiomatic Koog syntax.

**Mock Example for `SimpleLinterTool`:**
```kotlin
val simpleLinterTool = tool(
    name = "SimpleLinter",
    description = "Scans code for basic style violations like TODOs or print statements."
) {
    val code by argument<String>("code", "The source code content to scan")
    
    execute {
        val warnings = mutableListOf<String>()
        if (code.contains("TODO")) warnings.add("- Found TODO comment")
        if (code.contains("println")) warnings.add("- Found print statement (use Logger instead)")
        
        if (warnings.isEmpty()) "No issues found." else warnings.joinToString("\n")
    }
}
```


### 5.3 ACP Configuration (`acp.json`)
The final artifact must be mapped here.
*   **Command:** Absolute path to `./build/install/koog-reviewer/bin/koog-reviewer`
*   **Args:** `["acp"]`

## 6. Definition of Done
The project is considered complete when:
1.  The project builds successfully with `./gradlew installDist`.
2.  The agent appears in the IntelliJ AI Assistant "Agent" dropdown.
3.  The user can type "Check src/Main.kt for issues" in the chat.
4.  The agent correctly reads the file, identifies a "TODO" (if present), and replies with the warning.