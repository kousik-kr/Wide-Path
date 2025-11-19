import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class ApiServer {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) { }
        }

        bootstrapGraph();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/nodes", ApiServer::handleNodes);
        server.createContext("/api/network/meta", ApiServer::handleNetworkMeta);
        server.createContext("/api/queries/run", ApiServer::handleRunQuery);
        server.createContext("/api/metrics/live", ApiServer::handleMetrics);
        server.setExecutor(ForkJoinPool.commonPool());
        System.out.println("API server started on port " + port);
        server.start();
    }

    private static void bootstrapGraph() {
        // Minimal defaults so repeated runs don't fail because static fields are unset
        BidirectionalAstar.THRESHOLD = 10;
        BidirectionalAstar.SHARP_THRESHOLD = 60;
        BidirectionalAstar.WIDENESS_THRESHOLD = 12.8;
        BidirectionalAstar.TIME_LIMIT = 5;
        BidirectionalAstar.pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        // If graph already populated skip
        if (!Graph.get_nodes().isEmpty()) {
            return;
        }

        // Provide a tiny in-memory network so the API is immediately usable even
        // without the full dataset on disk.
        Graph.set_vertex_count(4);
        Graph.updateArrivalTimeSeries(new String[]{"0", "720", "1440"});
        Graph.updateWidthTimeSeries(new String[]{"0", "720", "1440"});

        Graph.add_node(0, new Node(40.7128, -74.0060));
        Graph.add_node(1, new Node(40.7138, -74.0010));
        Graph.add_node(2, new Node(40.7160, -74.0040));
        Graph.add_node(3, new Node(40.7140, -74.0100));

        addDemoEdge(0, 1, 0.5, false, 8.0, 8.0, 6.0);
        addDemoEdge(1, 2, 0.6, false, 10.0, 10.0, 5.5);
        addDemoEdge(2, 3, 0.7, false, 9.0, 9.0, 6.5);
        addDemoEdge(0, 3, 0.9, false, 12.0, 12.0, 8.0);
    }

    private static void addDemoEdge(int src, int dest, double distance, boolean clearway, double baseWidth, double rushWidth, double travelMinutes) {
        Edge edge = new Edge(src, dest, distance, clearway, baseWidth, rushWidth);
        edge.add_time_property(0, new Properties(travelMinutes));
        edge.add_time_property(720, new Properties(travelMinutes));
        edge.add_time_property(1440, new Properties(travelMinutes));
        if (!clearway) {
            edge.setWidth(baseWidth);
        } else {
            edge.add_wideness_property(0, new Properties(baseWidth));
            edge.add_wideness_property(720, new Properties(rushWidth));
            edge.add_wideness_property(1440, new Properties(baseWidth));
        }
        Graph.get_node(src).insert_outgoing_edge(edge);
        Graph.get_node(dest).insert_incoming_edge(edge);
    }

    private static void handleNodes(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        URI uri = exchange.getRequestURI();
        String query = Optional.ofNullable(uri.getQuery()).orElse("");
        String search = "";
        for (String part : query.split("&")) {
            if (part.startsWith("search=")) {
                search = part.substring("search=".length()).toLowerCase(Locale.ROOT);
                break;
            }
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map.Entry<Integer, Node> entry : Graph.get_nodes().entrySet()) {
            int id = entry.getKey();
            Node node = entry.getValue();
            if (search.length() > 1) {
                String idStr = Integer.toString(id);
                if (!idStr.contains(search)) continue;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", id);
            payload.put("latitude", node.get_latitude());
            payload.put("longitude", node.get_longitude());
            payload.put("degree", node.get_outgoing_edges().size());
            nodes.add(payload);
        }

        writeJson(exchange, toJsonArray(nodes));
    }

    private static void handleNetworkMeta(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        DoubleSummaryStatistics latStats = Graph.get_nodes().values().stream()
                .collect(Collectors.summarizingDouble(Node::get_latitude));
        DoubleSummaryStatistics lonStats = Graph.get_nodes().values().stream()
                .collect(Collectors.summarizingDouble(Node::get_longitude));

        String body = String.format(Locale.ROOT,
                "{\"vertexCount\":%d,\"bounds\":[%.6f,%.6f,%.6f,%.6f]}",
                Graph.get_vertex_count(),
                lonStats.getMin(), latStats.getMin(), lonStats.getMax(), latStats.getMax());
        writeJson(exchange, body);
    }

    private static void handleRunQuery(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String body = readBody(exchange.getRequestBody());
        Map<String, Double> payload = parseNumericJson(body);

        int source = payload.getOrDefault("source", 0.0).intValue();
        int destination = payload.getOrDefault("destination", 0.0).intValue();
        double departure = payload.getOrDefault("startDepartureMinutes", 0.0);
        double budget = payload.getOrDefault("budgetMinutes", 60.0);

        Query query = new Query(source, destination, departure, departure + budget, budget);
        Result result = null;
        long start = System.currentTimeMillis();
        try {
            BidirectionalDriver driver = new BidirectionalDriver(query, budget);
            result = driver.driver();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            Graph.reset();
        }
        long elapsed = System.currentTimeMillis() - start;
        Runtime runtime = Runtime.getRuntime();
        double memoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);

        if (result == null) {
            result = new Result(departure, 0.0, 0);
        }

        Node sourceNode = Graph.get_node(source);
        Node destinationNode = Graph.get_node(destination);
        double[][] coordinates = new double[][]{
                {sourceNode != null ? sourceNode.get_longitude() : 0.0, sourceNode != null ? sourceNode.get_latitude() : 0.0},
                {destinationNode != null ? destinationNode.get_longitude() : 0.0, destinationNode != null ? destinationNode.get_latitude() : 0.0}
        };
        double widthValue = Graph.get_node(source) != null && !Graph.get_node(source).get_outgoing_edges().isEmpty()
                ? Graph.get_node(source).get_outgoing_edges().values().iterator().next().get_width(departure)
                : 6.0;

        StringBuilder coordBuilder = new StringBuilder();
        coordBuilder.append("[");
        for (int i = 0; i < coordinates.length; i++) {
            coordBuilder.append(String.format(Locale.ROOT, "[%.6f,%.6f]", coordinates[i][0], coordinates[i][1]));
            if (i < coordinates.length - 1) coordBuilder.append(',');
        }
        coordBuilder.append("]");

        String responseJson = String.format(Locale.ROOT,
                "{\"result\":{\"departureTime\":%.2f,\"score\":%.2f,\"rightTurns\":%d},\"geometry\":{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":%s},\"properties\":{\"widthMeters\":[%.2f,%.2f],\"clearway\":[false,false],\"timeSeries\":[%.2f,%.2f],\"width\":%.2f}},\"diagnostics\":{\"elapsedSeconds\":%.3f,\"memoryMb\":%.3f}}",
                result.get_departureTime(), result.get_score(), result.get_right_turns(),
                coordBuilder.toString(),
                widthValue, widthValue,
                departure, departure + budget,
                widthValue,
                elapsed / 1000.0, memoryMb);

        writeJson(exchange, responseJson);
    }

    private static void handleMetrics(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        double memoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        String json = String.format(Locale.ROOT, "{\"memoryMb\":%.2f,\"timestamp\":%d}", memoryMb, System.currentTimeMillis());
        writeJson(exchange, json);
    }

    private static Map<String, Double> parseNumericJson(String body) {
        Map<String, Double> values = new HashMap<>();
        String sanitized = body.trim();
        if (sanitized.startsWith("{") && sanitized.endsWith("}")) {
            sanitized = sanitized.substring(1, sanitized.length() - 1);
        }
        for (String part : sanitized.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].replaceAll("[\\\"{} ]", "");
            String val = kv[1].replaceAll("[\\\"{} ]", "");
            try {
                values.put(key, Double.parseDouble(val));
            } catch (NumberFormatException ignored) { }
        }
        return values;
    }

    private static String readBody(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static void writeJson(HttpExchange exchange, String body) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String toJsonArray(List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            sb.append('{');
            int idx = 0;
            for (Map.Entry<String, Object> ent : item.entrySet()) {
                sb.append('\"').append(ent.getKey()).append('\"').append(':');
                Object value = ent.getValue();
                if (value instanceof String) {
                    sb.append('\"').append(value).append('\"');
                } else {
                    sb.append(String.format(Locale.ROOT, "%s", value));
                }
                if (idx < item.size() - 1) sb.append(',');
                idx++;
            }
            sb.append('}');
            if (i < items.size() - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }
}
