package billsplitter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * Built-in HTTP Server serving the modern HTML5/CSS3/JavaScript frontend
 * and providing REST API endpoints that integrate seamlessly with the existing Java classes.
 * Uses only standard JDK com.sun.net.httpserver.HttpServer (Zero external dependencies).
 */
public class WebServer {
    private static HttpServer server;
    private static int activePort = 8080;

    public static int startServer(int preferredPort) throws IOException {
        int port = preferredPort;
        int maxAttempts = 10;
        HttpServer createdServer = null;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                createdServer = HttpServer.create(new InetSocketAddress(port), 0);
                activePort = port;
                break;
            } catch (IOException e) {
                port++;
            }
        }

        if (createdServer == null) {
            throw new IOException("Failed to bind HTTP server to any port in range " + preferredPort + "-" + (preferredPort + maxAttempts));
        }

        server = createdServer;

        // Register Handlers
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/split", new SplitApiHandler());
        server.createContext("/api/history", new HistoryApiHandler());
        server.createContext("/api/receipt", new ReceiptApiHandler());
        server.createContext("/api/remind", new RemindApiHandler());

        server.setExecutor(null); // Default executor
        server.start();
        return activePort;
    }

    public static int getActivePort() {
        return activePort;
    }

    public static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    public static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Windows fallback
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            }
        } catch (Exception ignored) {
            // Headless or restricted environments gracefully ignore browser opening
        }
    }

    /**
     * Serves static web frontend assets (HTML, CSS, JS)
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Security check against directory traversal
            if (path.contains("..")) {
                sendResponse(exchange, 403, "text/plain", "Forbidden");
                return;
            }

            byte[] fileBytes = loadWebAsset(path);
            if (fileBytes == null) {
                sendResponse(exchange, 404, "text/plain", "404 Not Found: " + path);
                return;
            }

            String contentType = getMimeType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }

        private byte[] loadWebAsset(String relativePath) {
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            // Try candidate file locations
            String[] searchPaths = new String[]{
                    "billsplitter/web/" + relativePath,
                    "web/" + relativePath,
                    relativePath
            };

            for (String searchPath : searchPaths) {
                File file = new File(searchPath);
                if (file.exists() && file.isFile()) {
                    try {
                        return Files.readAllBytes(file.toPath());
                    } catch (IOException ignored) {}
                }
            }

            // Fallback to classpath
            try (InputStream is = getClass().getResourceAsStream("/billsplitter/web/" + relativePath)) {
                if (is != null) {
                    return is.readAllBytes();
                }
            } catch (IOException ignored) {}

            return null;
        }

        private String getMimeType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json; charset=UTF-8";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".txt")) return "text/plain; charset=UTF-8";
            return "application/octet-stream";
        }
    }

    /**
     * REST API Endpoint for Bill Splitting Calculation
     */
    static class SplitApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            try {
                String title = SimpleJson.getString(body, "title", "Expense");
                double amount = SimpleJson.getDouble(body, "amount", 0.0);
                double tax = SimpleJson.getDouble(body, "tax", 0.0);
                List<String> membersList = SimpleJson.getStringArray(body, "members");

                if (title == null || title.trim().isEmpty()) {
                    title = "Expense";
                }
                title = title.trim();

                // Validate input
                if (amount <= 0) {
                    throw new InvalidBillException("Bill amount must be greater than zero!");
                }
                if (membersList.isEmpty()) {
                    throw new InvalidBillException("At least one person is required to split a bill!");
                }

                String[] members = membersList.toArray(new String[0]);

                // M-2: Polymorphism (Parent reference holding child object)
                Bill bill;
                if (tax > 0) {
                    bill = new TaxedBill(title, amount, members, tax);
                } else {
                    bill = new EqualBill(title, amount, members);
                }

                double perPerson = bill.calculateSharePerPerson();
                double finalTotal = (bill instanceof TaxedBill) ? ((TaxedBill) bill).getTotalWithTax() : amount;

                // M-1: StringBuffer to format receipt
                StringBuffer sb = new StringBuffer();
                sb.append("Expense: ").append(bill.getTitle()).append("\n");
                sb.append("Split Between: ").append(members.length).append(" people\n");
                if (tax > 0) {
                    sb.append(String.format(Locale.US, "Subtotal: Rs. %.2f (GST/Tax: %.1f%%)\n", amount, tax));
                    sb.append(String.format(Locale.US, "Total with Tax: Rs. %.2f\n", finalTotal));
                } else {
                    sb.append(String.format(Locale.US, "Total Amount: Rs. %.2f\n", amount));
                }
                sb.append(String.format(Locale.US, "Each Person Pays: Rs. %.2f\n", perPerson));
                sb.append("Members: ");
                for (String f : members) {
                    sb.append(f).append(" ");
                }

                // Save to file via Exportable Interface
                Exportable exporter = new ReceiptManager(sb.toString());
                exporter.saveReceiptToFile();

                // Feature 2: Save to History
                BillRecord record = new BillRecord(title, amount, tax, finalTotal, perPerson, members);
                BillHistoryManager.addRecord(record);

                // M-3: Multithreading - Run Payment Reminder Thread in background
                new Thread(new PaymentReminderThread(members, perPerson)).start();

                // Build JSON response
                StringBuilder jsonResponse = new StringBuilder();
                jsonResponse.append("{");
                jsonResponse.append("\"success\":true,");
                jsonResponse.append("\"id\":\"").append(SimpleJson.escape(record.getId())).append("\",");
                jsonResponse.append("\"title\":\"").append(SimpleJson.escape(title)).append("\",");
                jsonResponse.append("\"totalAmount\":").append(String.format(Locale.US, "%.2f", amount)).append(",");
                jsonResponse.append("\"taxPercent\":").append(String.format(Locale.US, "%.2f", tax)).append(",");
                jsonResponse.append("\"finalTotal\":").append(String.format(Locale.US, "%.2f", finalTotal)).append(",");
                jsonResponse.append("\"perPersonShare\":").append(String.format(Locale.US, "%.2f", perPerson)).append(",");
                jsonResponse.append("\"memberCount\":").append(members.length).append(",");
                jsonResponse.append("\"timestamp\":\"").append(SimpleJson.escape(record.getTimestamp())).append("\",");
                jsonResponse.append("\"receipt\":\"").append(SimpleJson.escape(sb.toString())).append("\",");
                jsonResponse.append("\"members\":[");
                for (int i = 0; i < members.length; i++) {
                    jsonResponse.append("\"").append(SimpleJson.escape(members[i])).append("\"");
                    if (i < members.length - 1) jsonResponse.append(",");
                }
                jsonResponse.append("]");
                jsonResponse.append("}");

                sendResponse(exchange, 200, "application/json", jsonResponse.toString());

            } catch (InvalidBillException e) {
                String err = "{\"success\":false,\"error\":\"" + SimpleJson.escape(e.getMessage()) + "\"}";
                sendResponse(exchange, 400, "application/json", err);
            } catch (Exception e) {
                String err = "{\"success\":false,\"error\":\"Please enter valid numbers and inputs.\"}";
                sendResponse(exchange, 400, "application/json", err);
            }
        }
    }

    /**
     * REST API Endpoint for History Management (Feature 2)
     */
    static class HistoryApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<BillRecord> history = BillHistoryManager.getHistory();
                String json = "{\"success\":true,\"count\":" + history.size() + ",\"history\":" + SimpleJson.recordsToJson(history) + "}";
                sendResponse(exchange, 200, "application/json", json);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String path = exchange.getRequestURI().getPath();
                if (path != null && path.endsWith("/clear")) {
                    BillHistoryManager.clearHistory();
                    sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"History cleared\"}");
                } else {
                    sendResponse(exchange, 400, "application/json", "{\"error\":\"Invalid history action\"}");
                }
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}");
            }
        }
    }

    /**
     * Endpoint to view saved receipt file contents
     */
    static class ReceiptApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            File file = new File("split_receipt.txt");
            if (!file.exists()) {
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"content\":\"No receipts generated yet.\"}");
                return;
            }
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"content\":\"" + SimpleJson.escape(content) + "\"}");
            } catch (IOException e) {
                sendResponse(exchange, 500, "application/json", "{\"success\":false,\"error\":\"" + SimpleJson.escape(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Endpoint to trigger payment reminder thread on demand
     */
    static class RemindApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            String body = readRequestBody(exchange);
            List<String> membersList = SimpleJson.getStringArray(body, "members");
            double share = SimpleJson.getDouble(body, "share", 0.0);

            if (membersList.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"success\":false,\"error\":\"No members provided\"}");
                return;
            }

            // Start background thread
            new Thread(new PaymentReminderThread(membersList.toArray(new String[0]), share)).start();
            sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Payment reminders dispatched in background thread.\"}");
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int read;
            while ((read = is.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
