package billsplitter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Feature 2: Bill Record Model
 * Represents a saved bill calculation record for history tracking.
 */
public class BillRecord {
    private String id;
    private String title;
    private double totalAmount;
    private double taxPercent;
    private double finalTotal;
    private double perPersonShare;
    private String[] members;
    private String timestamp;

    public BillRecord(String id, String title, double totalAmount, double taxPercent,
                      double finalTotal, double perPersonShare, String[] members, String timestamp) {
        this.id = (id == null || id.isEmpty()) ? generateId() : id;
        this.title = (title == null || title.trim().isEmpty()) ? "General Expense" : title.trim();
        this.totalAmount = totalAmount;
        this.taxPercent = taxPercent;
        this.finalTotal = finalTotal;
        this.perPersonShare = perPersonShare;
        this.members = members != null ? members : new String[0];
        this.timestamp = (timestamp == null || timestamp.isEmpty()) ? getCurrentTimestamp() : timestamp;
    }

    public BillRecord(String title, double totalAmount, double taxPercent,
                      double finalTotal, double perPersonShare, String[] members) {
        this(null, title, totalAmount, taxPercent, finalTotal, perPersonShare, members, null);
    }

    private static String generateId() {
        return "BILL-" + System.currentTimeMillis() % 100000;
    }

    private static String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public double getFinalTotal() {
        return finalTotal;
    }

    public double getPerPersonShare() {
        return perPersonShare;
    }

    public String[] getMembers() {
        return members;
    }

    public int getMemberCount() {
        return members != null ? members.length : 0;
    }

    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Serializes record to a single pipe-delimited line for bill_history.txt persistence.
     */
    public String toStorageString() {
        String membersJoined = String.join(",", members);
        // Replace potential pipes in title to preserve format
        String safeTitle = title.replace("|", "/");
        return String.format("%s|%s|%s|%.2f|%.2f|%.2f|%.2f|%s",
                id, timestamp, safeTitle, totalAmount, taxPercent, finalTotal, perPersonShare, membersJoined);
    }

    /**
     * Deserializes record from a pipe-delimited storage line.
     */
    public static BillRecord fromStorageString(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length < 8) {
            return null;
        }
        try {
            String id = parts[0].trim();
            String timestamp = parts[1].trim();
            String title = parts[2].trim();
            double totalAmount = Double.parseDouble(parts[3].trim());
            double taxPercent = Double.parseDouble(parts[4].trim());
            double finalTotal = Double.parseDouble(parts[5].trim());
            double perPersonShare = Double.parseDouble(parts[6].trim());
            String membersStr = parts[7].trim();
            String[] members = membersStr.isEmpty() ? new String[0] : membersStr.split(",");
            for (int i = 0; i < members.length; i++) {
                members[i] = members[i].trim();
            }
            return new BillRecord(id, title, totalAmount, taxPercent, finalTotal, perPersonShare, members, timestamp);
        } catch (Exception e) {
            return null;
        }
    }
}
