package billsplitter;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Feature 2: Bill Splitting History Manager
 * Thread-safe manager that maintains in-memory and file-based calculation history.
 * Follows core Java I/O conventions (Module 1: File class, Module 3: Synchronized methods).
 */
public class BillHistoryManager {
    private static final String HISTORY_FILE = "bill_history.txt";
    private static final List<BillRecord> records = new ArrayList<>();
    private static boolean isInitialized = false;

    static {
        loadHistoryFromFile();
    }

    /**
     * Loads existing history records from bill_history.txt if the file exists.
     */
    public static synchronized void loadHistoryFromFile() {
        records.clear();
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            isInitialized = true;
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                BillRecord record = BillRecord.fromStorageString(line);
                if (record != null) {
                    records.add(record);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load bill history: " + e.getMessage());
        }
        isInitialized = true;
    }

    /**
     * Adds a new bill calculation record to both memory and the history file.
     */
    public static synchronized void addRecord(BillRecord record) {
        if (!isInitialized) {
            loadHistoryFromFile();
        }
        records.add(record);

        // Append to bill_history.txt
        File file = new File(HISTORY_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(record.toStorageString());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to save bill record to history file: " + e.getMessage());
        }
    }

    /**
     * Returns an unmodifiable list of history records, sorted newest first.
     */
    public static synchronized List<BillRecord> getHistory() {
        if (!isInitialized) {
            loadHistoryFromFile();
        }
        List<BillRecord> copy = new ArrayList<>(records);
        Collections.reverse(copy); // Newest calculations first
        return Collections.unmodifiableList(copy);
    }

    /**
     * Clears all history in memory and empties the bill_history.txt file.
     */
    public static synchronized void clearHistory() {
        records.clear();
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (FileWriter writer = new FileWriter(file, false)) {
                // Truncate file
                writer.write("");
            } catch (IOException e) {
                System.err.println("Failed to clear history file: " + e.getMessage());
            }
        }
    }

    public static synchronized int getHistoryCount() {
        return records.size();
    }
}
