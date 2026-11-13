package billsplitter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight JSON utility for REST API communication.
 * Requires ZERO third-party libraries, keeping the academic project clean and self-contained.
 */
public class SimpleJson {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String recordToJson(BillRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escape(r.getId())).append("\",");
        sb.append("\"title\":\"").append(escape(r.getTitle())).append("\",");
        sb.append("\"totalAmount\":").append(String.format(Locale.US, "%.2f", r.getTotalAmount())).append(",");
        sb.append("\"taxPercent\":").append(String.format(Locale.US, "%.2f", r.getTaxPercent())).append(",");
        sb.append("\"finalTotal\":").append(String.format(Locale.US, "%.2f", r.getFinalTotal())).append(",");
        sb.append("\"perPersonShare\":").append(String.format(Locale.US, "%.2f", r.getPerPersonShare())).append(",");
        sb.append("\"timestamp\":\"").append(escape(r.getTimestamp())).append("\",");
        sb.append("\"memberCount\":").append(r.getMemberCount()).append(",");
        sb.append("\"members\":[");
        String[] members = r.getMembers();
        for (int i = 0; i < members.length; i++) {
            sb.append("\"").append(escape(members[i])).append("\"");
            if (i < members.length - 1) sb.append(",");
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    public static String recordsToJson(List<BillRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < records.size(); i++) {
            sb.append(recordToJson(records.get(i)));
            if (i < records.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Extracts a string field from a simple JSON object string.
     */
    public static String getString(String json, String key, String defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return defaultValue;
    }

    /**
     * Extracts a double number field from a simple JSON object string.
     */
    public static double getDouble(String json, String key, double defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Extracts an array of strings from a simple JSON object string.
     */
    public static List<String> getStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        Pattern arrayPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher arrayMatcher = arrayPattern.matcher(json);
        if (arrayMatcher.find()) {
            String itemsContent = arrayMatcher.group(1);
            Pattern itemPattern = Pattern.compile("\"([^\"]*)\"");
            Matcher itemMatcher = itemPattern.matcher(itemsContent);
            while (itemMatcher.find()) {
                String val = itemMatcher.group(1).trim();
                if (!val.isEmpty()) {
                    result.add(val);
                }
            }
        }
        return result;
    }
}
