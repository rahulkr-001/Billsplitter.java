package billsplitter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

// Multithreading using Runnable
class PaymentReminderThread implements Runnable {
    private String[] members;
    private double share;

    public PaymentReminderThread(String[] members, double share) {
        this.members = members;
        this.share = share;
    }

    @Override
    public void run() {
        try {
            // Simulates waiting 2 seconds before sending automated reminders
            Thread.sleep(2000);
            System.out.println("\n--- [Background Reminder Thread] ---");
            for (String member : members) {
                System.out.printf("Reminder sent to %s: Please pay Rs. %.2f\n", member, share);
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            System.out.println("Reminder thread interrupted.");
        }
    }
}

// Implements Exportable interface
class ReceiptManager implements Exportable {
    private String receiptContent;

    public ReceiptManager(String receiptContent) {
        this.receiptContent = receiptContent;
    }

    // Synchronized method for thread-safe file writing
    @Override
    public synchronized void saveReceiptToFile() {
        File file = new File("split_receipt.txt"); // Module 1: File class
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(receiptContent + "\n---------------------------------\n");
            System.out.println("Receipt saved successfully to: " + file.getName());
        } catch (IOException e) {
            System.out.println("File Error: " + e.getMessage());
        }
    }

    public String getReceiptContent() {
        return receiptContent;
    }
}

public class Main {

    /**
     * Preserved original Interactive Console CLI Mode.
     * Accessible via command line argument (--cli) or interactive console menu.
     */
    public static void runConsoleCli(Scanner sc) {
        // try-catch-finally block
        try {
            System.out.println("\n=== SMART BILL SPLITTER (CLI MODE) ===");
            System.out.print("Enter Expense Title (e.g., Dinner, Cab): ");
            String title = sc.nextLine();

            System.out.print("Enter Total Bill Amount (Rs.): ");
            double amount = sc.nextDouble();

            System.out.print("Number of friends splitting: ");
            int count = sc.nextInt();
            sc.nextLine(); // consume newline

            String[] friends = new String[count];
            for (int i = 0; i < count; i++) {
                System.out.print("Enter name of friend " + (i + 1) + ": ");
                friends[i] = sc.nextLine();
            }

            System.out.print("Add GST/Service Tax? (1 = No, 2 = Yes): ");
            int choice = sc.nextInt();

            // Polymorphism (Parent reference holding child object)
            Bill bill;
            double tax = 0.0;
            if (choice == 2) {
                System.out.print("Enter Tax Percentage (e.g., 5 or 18): ");
                tax = sc.nextDouble();
                bill = new TaxedBill(title, amount, friends, tax);
            } else {
                bill = new EqualBill(title, amount, friends);
            }

            double perPerson = bill.calculateSharePerPerson();
            double finalTotal = (bill instanceof TaxedBill) ? ((TaxedBill) bill).getTotalWithTax() : amount;

            // StringBuffer to format receipt
            StringBuffer sb = new StringBuffer();
            sb.append("Expense: ").append(bill.getTitle()).append("\n");
            sb.append("Split Between: ").append(count).append(" people\n");
            if (tax > 0) {
                sb.append(String.format("Subtotal: Rs. %.2f (Tax: %.1f%%)\n", amount, tax));
                sb.append(String.format("Total Payable: Rs. %.2f\n", finalTotal));
            } else {
                sb.append(String.format("Total Amount: Rs. %.2f\n", amount));
            }
            sb.append(String.format("Each Person Pays: Rs. %.2f\n", perPerson));
            sb.append("Members: ");
            for (String f : friends) {
                sb.append(f).append(" ");
            }

            System.out.println("\n" + sb.toString());

            // Save to file via Interface
            Exportable exporter = new ReceiptManager(sb.toString());
            exporter.saveReceiptToFile();

            // Feature 2: Also save to history so web dashboard can see calculations made in CLI
            BillRecord record = new BillRecord(title, amount, tax, finalTotal, perPerson, friends);
            BillHistoryManager.addRecord(record);

            // Start Background Reminder Thread
            Thread reminderThread = new Thread(new PaymentReminderThread(friends, perPerson));
            reminderThread.start();
            reminderThread.join(); // Wait for thread to finish cleanly

        } catch (InvalidBillException e) {
            System.out.println("\n[Validation Error]: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("\n[Input Error]: Please enter valid numbers.");
        }
    }

    public static void main(String[] args) {
        // Check for direct CLI argument
        if (args.length > 0 && (args[0].equalsIgnoreCase("--cli") || args[0].equalsIgnoreCase("-c"))) {
            Scanner sc = new Scanner(System.in);
            try {
                runConsoleCli(sc);
            } finally {
                sc.close();
                System.out.println("\nSession closed.");
            }
            return;
        }

        // Start Built-in Web Server for Modern UI/UX
        try {
            int port = WebServer.startServer(8080);
            String url = "http://localhost:" + port + "/";

            System.out.println("==============================================================");
            System.out.println("                    SMART BILL SPLITTER                       ");
            System.out.println("==============================================================");
            System.out.println(" [✓] Modern Web Dashboard running at: " + url);
            System.out.println(" [✓] Opening default browser automatically...");
            System.out.println("--------------------------------------------------------------");
            System.out.println(" Console Commands:");
            System.out.println("   Type [1] + Enter: Re-open Web Dashboard in browser");
            System.out.println("   Type [2] + Enter: Run Classic Console CLI Mode");
            System.out.println("   Type [Q] + Enter: Stop Server and Exit");
            System.out.println("==============================================================");

            // Open browser
            WebServer.openBrowser(url);

            // If args specified --web, just keep running
            if (args.length > 0 && (args[0].equalsIgnoreCase("--web") || args[0].equalsIgnoreCase("-w"))) {
                System.out.println("Running in dedicated web server mode. Press Ctrl+C to stop.");
                return;
            }

            // Interactive console listener
            Scanner sc = new Scanner(System.in);
            while (sc.hasNextLine()) {
                String cmd = sc.nextLine().trim();
                if (cmd.equalsIgnoreCase("q") || cmd.equalsIgnoreCase("exit")) {
                    System.out.println("Shutting down Smart Bill Splitter server...");
                    WebServer.stopServer();
                    break;
                } else if (cmd.equals("1")) {
                    System.out.println("Opening browser to " + url + "...");
                    WebServer.openBrowser(url);
                } else if (cmd.equals("2")) {
                    runConsoleCli(sc);
                    System.out.println("\nReturning to Web Dashboard listener (" + url + ")");
                } else if (!cmd.isEmpty()) {
                    System.out.println("Unknown command '" + cmd + "'. Use [1] for Web, [2] for CLI, or [Q] to quit.");
                }
            }

        } catch (IOException e) {
            System.err.println("Could not start Web Dashboard: " + e.getMessage());
            System.out.println("Falling back to Classic CLI mode...");
            Scanner sc = new Scanner(System.in);
            try {
                runConsoleCli(sc);
            } finally {
                sc.close();
            }
        }
    }
}
