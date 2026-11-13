# Smart Bill Splitter

> *"Split bills. Share fairly."*

Smart Bill Splitter is a lightweight expense-sharing application designed to simplify group bill calculations and shared expense management. It enables users to divide expenses among multiple participants, calculate tax-inclusive contributions, generate digital receipts, and maintain a history of previous transactions.

Built with Java and standard web technologies, the application provides a modern, responsive web dashboard powered by an embedded Java HTTP server, alongside a fully functional command-line interface.

---

## Features

- **Equal Bill Splitting:** Divide the total expense equally among multiple participants with precise decimal calculations.
- **Tax-Inclusive Calculations:** Apply GST or service tax percentages before calculating individual contributions.
- **Dynamic Participant Management:** Add and remove participants dynamically with validation for empty and duplicate names.
- **Individual Contribution Breakdown:** View each participant's payable amount through clearly organized result cards.
- **Digital Receipt Generation:** Generate formatted expense receipts and save them locally in `split_receipt.txt`.
- **Calculation History:** Maintain persistent records of previous calculations in `bill_history.txt`, with options to review and reload previous bills.
- **Background Payment Reminders:** Simulate asynchronous payment notifications using Java multithreading.
- **Dual Interface:** Access the application through a locally hosted web dashboard or an interactive command-line interface.
- **Responsive Design:** Enjoy a consistent interface across desktop, tablet, and mobile devices.
- **Zero External Dependencies:** Uses standard Java SE libraries without requiring Maven, Gradle, or third-party JAR files.

---

## Technology Stack & Implementation

### Technologies Used

| Technology | Implementation |
|---|---|
| **Java (JDK 11+)** | Core business logic and backend development |
| **Java HTTP Server** | Embedded server using `com.sun.net.httpserver.HttpServer` |
| **HTML5** | Dashboard structure and semantic elements |
| **CSS3** | Responsive layouts, styling, and animations |
| **Vanilla JavaScript** | Dynamic interactions and API communication |
| **JSON** | Frontend-backend data exchange |
| **Java I/O** | Local receipt and expense history storage |
| **JVM** | Java application execution environment |

### Java Concepts & Core Implementation

The application incorporates the following Java and Object-Oriented Programming concepts:

- **Classes and Objects:** Modular application components and data models.
- **Encapsulation:** Controlled access to application data.
- **Abstraction:** Common bill functionality through an abstract `Bill` class.
- **Inheritance:** Specialized `EqualBill` and `TaxedBill` implementations.
- **Polymorphism:** Runtime selection of bill calculation behavior.
- **Interfaces:** `Exportable` contract for receipt generation.
- **Arrays & StringBuffer:** Participant management and receipt formatting.
- **Exception Handling:** Custom `InvalidBillException` for input validation.
- **Multithreading:** Background processing through `Runnable` and `Thread`.
- **Synchronization:** Safe coordination of shared file operations.
- **File Handling:** Persistent receipt and calculation history storage.

### Application Architecture

The application follows a lightweight architecture that separates frontend presentation, backend processing, and data persistence.

```text
Web Dashboard (HTML, CSS, JavaScript)
                 |
              HTTP / JSON
                 |
       Embedded Java HTTP Server
                 |
          Business Logic
       (Bill, EqualBill, TaxedBill)
                 |
       Management Components
    (Receipt & History Managers)
                 |
          Local File Storage

       Background Processing
        (PaymentReminderThread)

       Alternative: Console CLI
```

**Architecture Highlights:**
- Object-oriented and modular Java backend.
- Embedded HTTP server without external frameworks.
- Client-server communication using HTTP and JSON.
- Local file-based persistence without an external database.
- Independent background task execution.
- No third-party Java dependencies required.
---

## Project Structure

```text
smart-bill-splitter/
│
├── billsplitter/
│   ├── Bill.java
│   ├── BillHistoryManager.java
│   ├── BillRecord.java
│   ├── Exportable.java
│   ├── InvalidBillException.java
│   ├── Main.java
│   ├── PaymentReminderThread.java
│   ├── ReceiptManager.java
│   ├── SimpleJson.java
│   ├── WebServer.java
│   │
│   └── web/
│       ├── index.html
│       ├── style.css
│       └── app.js
│
├── web/
│   ├── index.html
│   ├── style.css
│   └── app.js
│
├── run.bat
├── run.sh
├── .gitignore
├── LICENSE
└── README.md
```

---

## ⚙️ Prerequisites

Before running the application, ensure you have:

- **Java Development Kit:** JDK 11 or later.
- **Web Browser:** A modern browser such as Google Chrome, Microsoft Edge, Firefox, or Safari.
- **Terminal:** Command Prompt, PowerShell, or a Unix-based terminal.

No additional build tools or third-party Java libraries are required.

---

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/rahulkr-001/Billsplitter.java.git
```

### 2. Navigate to the Project Directory

```bash
cd smart-bill-splitter
```

### 3. Compile the Application

```bash
javac billsplitter/*.java
```

### 4. Launch the Application

**Web Dashboard**

```bash
java billsplitter.Main
```

The application starts the embedded HTTP server and makes the web dashboard available at:

`http://localhost:8080/`

**Console Interface**

To launch the command-line interface directly:

```bash
java billsplitter.Main --cli
```

### Alternative: Launch Scripts

The project also includes platform-specific launch scripts.

| Operating System | Command |
|---|---|
| Windows | `run.bat` |
| macOS / Linux | `chmod +x run.sh && ./run.sh` |

---

## 📖 Usage Guide

### Web Dashboard

1. Launch the application using the Java command.
2. Open `http://localhost:8080/` in your browser.
3. Enter the total bill amount and an optional expense description.
4. Enable GST or service tax if applicable.
5. Add participants using the participant input field.
6. Review individual contribution amounts.
7. Calculate and split the bill.
8. Generate and export the digital receipt.
9. Copy the bill summary or access previous calculations through the History section.

### Console Interface

1. Launch the application using `java billsplitter.Main --cli`.
2. Follow the interactive terminal prompts.
3. Enter the expense details and participant information.
4. Specify the applicable tax percentage.
5. View the calculated individual contributions.
6. Access the generated receipt and background reminder output.

---

## 💾 Data Storage

Smart Bill Splitter uses local file-based storage to maintain application records.

| File | Purpose |
|---|---|
| `split_receipt.txt` | Stores generated bill receipts |
| `bill_history.txt` | Maintains previous bill calculations |

No external database is required.

---

## 🔒 Privacy

- Expense records are stored locally.
- No external database or cloud account is required.
- The application does not require users to create an account.
- Generated receipt and history files remain within the local application environment.

---
