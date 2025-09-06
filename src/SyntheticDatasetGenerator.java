import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SyntheticDatasetGenerator {

    static class EdgeKey {
        final int u, v;
        EdgeKey(int u, int v) { this.u = u; this.v = v; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EdgeKey)) return false;
            EdgeKey k = (EdgeKey) o;
            return u == k.u && v == k.v;
        }
        @Override public int hashCode() { return Objects.hash(u, v); }
    }

    static class Edge {
        final int u, v;
        int distance;
        int time;
        Edge(int u, int v) { this.u = u; this.v = v; }
    }

    static class Point {
        int id;
        double x, y;
        Point(int id, double x, double y) { this.id = id; this.x = x; this.y = y; }
    }

    static class City {
        int id;
        Set<Integer> nodes = new HashSet<>();
        Set<EdgeKey> edges = new HashSet<>();
    }

    public static void main(String[] args) throws IOException {
        Path coordPath = Paths.get("USA-road-d.NY.co");
        Path distPath = Paths.get("USA-road-d.NY.gr");
        Path timePath = Paths.get("USA-road-t.NY.gr");

        Path nodeCityOutput = Paths.get("node-city.txt");
        Path edgeCityOutput = Paths.get("edge-city.txt");

        double baseWidth = 1.0;
        double rushWidthFactor = 1.5;
        String rushStart = "07:30";
        String rushEnd = "10:00";
        double congestedEdgeThresholdRatio = 1.5;
        double cityEdgeSelectionRate = 0.05;

        // Read inputs
        Map<Integer, long[]> coordsRaw = readCoordinates(coordPath);
        Map<EdgeKey, Edge> edges = readEdges(distPath, timePath);

        // Convert coordinates to double for clustering (scale to degrees)
        Map<Integer, Point> points = new HashMap<>();
        for (Map.Entry<Integer, long[]> e : coordsRaw.entrySet()) {
            int id = e.getKey();
            double x = e.getValue()[0] / 1e6;
            double y = e.getValue()[1] / 1e6;
            points.put(id, new Point(id, x, y));
        }

        // Identify congested edges and nodes
        Set<EdgeKey> congestedEdges = new HashSet<>();
        Set<Integer> congestedNodes = new HashSet<>();
        for (Edge edge : edges.values()) {
            if (edge.distance > 0) {
                double ratio = (double) edge.time / edge.distance;
                if (ratio >= congestedEdgeThresholdRatio) {
                    EdgeKey ek = new EdgeKey(edge.u, edge.v);
                    congestedEdges.add(ek);
                    congestedNodes.add(edge.u);
                    congestedNodes.add(edge.v);
                }
            }
        }

        // Extract congested nodes points for clustering
        List<Point> congestedPoints = new ArrayList<>();
        for (int nodeId : congestedNodes) {
            Point p = points.get(nodeId);
            if (p != null) congestedPoints.add(p);
        }

        // Run DBSCAN clustering
        double EPSILON = 0.0001; // coordinate distance threshold, adjust as needed
        int MIN_POINTS = 3;

        Map<Integer, Integer> clusterMap = runDBSCAN(congestedPoints, EPSILON, MIN_POINTS);

        Map<Integer, City> cities = new HashMap<>();
        AtomicInteger cityCounter = new AtomicInteger(1);
        for (Map.Entry<Integer, Integer> ent : clusterMap.entrySet()) {
            int nodeId = ent.getKey();
            int clusterId = ent.getValue();
            if (clusterId == -1) continue;
            City city = cities.computeIfAbsent(clusterId, k -> {
                City c = new City();
                c.id = cityCounter.getAndIncrement();
                return c;
            });
            city.nodes.add(nodeId);
        }

        // Assign edges whose both endpoints are in same city
        for (EdgeKey ek : congestedEdges) {
            Integer clusterU = clusterMap.getOrDefault(ek.u, -1);
            Integer clusterV = clusterMap.getOrDefault(ek.v, -1);
            if (clusterU != -1 && clusterU.equals(clusterV)) {
                City city = cities.get(clusterU);
                if (city != null) city.edges.add(ek);
            }
        }

        // Select edges for rush hour width increase
        Set<EdgeKey> rushEdges = new HashSet<>();
        Random rng = new Random(42);
        for (City city : cities.values()) {
            List<EdgeKey> cityEdgeList = new ArrayList<>(city.edges);
            Collections.shuffle(cityEdgeList, rng);
            int k = Math.max(1, (int) Math.floor(cityEdgeSelectionRate * cityEdgeList.size()));
            for (int i = 0; i < Math.min(k, cityEdgeList.size()); i++) rushEdges.add(cityEdgeList.get(i));
        }

        // Write node file (id, lon, lat, city_id or -1)
        try (BufferedWriter w = Files.newBufferedWriter(nodeCityOutput)) {
            w.write("node_id\tlongitude\tlatitude\tcity_id\n");
            for (int nodeId : coordsRaw.keySet()) {
                long[] raw = coordsRaw.get(nodeId);
                int cityId = clusterMap.getOrDefault(nodeId, -1);
                w.write(String.format(Locale.ROOT, "%d\t%d\t%d\t%d%n", nodeId, raw[0], raw[1], cityId));
            }
        }

        // Write edge file (source, dest, dist, time, baseWidth, rushWidth, city_id or -1)
        try (BufferedWriter w = Files.newBufferedWriter(edgeCityOutput)) {
            w.write("source\tdestination\tdistance\ttime\tbaseWidth\trushWidth\tcityId\n");
            for (Edge e : edges.values()) {
                int cityId = -1;
                EdgeKey ek = new EdgeKey(e.u, e.v);
                // If edge inside some city
                for (City city : cities.values()) {
                    if (city.edges.contains(ek)) {
                        cityId = city.id;
                        break;
                    }
                }
                double baseW = baseWidth;
                double rushW = baseWidth;
                if (rushEdges.contains(ek)) rushW = rushWidthFactor * baseWidth;
                w.write(String.format(Locale.ROOT,
                        "%d\t%d\t%d\t%d\t%.2f\t%.2f\t%d%n",
                        e.u, e.v, e.distance, e.time, baseW, rushW, cityId));
            }
        }

        System.out.println("Node file and edge file written:");
        System.out.println(" - " + nodeCityOutput.toAbsolutePath());
        System.out.println(" - " + edgeCityOutput.toAbsolutePath());
    }

    // DBSCAN methods same as previous code...

    static Map<Integer, Integer> runDBSCAN(List<Point> points, double eps, int minPts) {
        Map<Integer, Integer> clusterMap = new HashMap<>();
        int clusterId = 0;
        Set<Point> visited = new HashSet<>();

        for (Point p : points) {
            if (visited.contains(p)) continue;
            visited.add(p);
            List<Point> neighbors = regionQuery(points, p, eps);

            if (neighbors.size() < minPts) {
                clusterMap.put(p.id, -1);
            } else {
                clusterId++;
                expandCluster(points, clusterMap, p, neighbors, clusterId, eps, minPts, visited);
            }
        }
        return clusterMap;
    }

    static void expandCluster(List<Point> points, Map<Integer, Integer> clusterMap, Point p,
                              List<Point> neighbors, int clusterId, double eps, int minPts, Set<Point> visited) {
        clusterMap.put(p.id, clusterId);
        Queue<Point> seeds = new ArrayDeque<>(neighbors);

        while (!seeds.isEmpty()) {
            Point current = seeds.poll();
            if (!visited.contains(current)) {
                visited.add(current);
                List<Point> currentNeighbors = regionQuery(points, current, eps);
                if (currentNeighbors.size() >= minPts) {
                    seeds.addAll(currentNeighbors);
                }
            }
            if (!clusterMap.containsKey(current.id)) {
                clusterMap.put(current.id, clusterId);
            }
        }
    }

    static List<Point> regionQuery(List<Point> points, Point center, double eps) {
        List<Point> neighbors = new ArrayList<>();
        for (Point p : points) {
            if (distance(center, p) <= eps) {
                neighbors.add(p);
            }
        }
        return neighbors;
    }

    static double distance(Point a, Point b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // readCoordinates and readEdges as before
    static Map<Integer, long[]> readCoordinates(Path path) throws IOException {
        Map<Integer, long[]> map = new HashMap<>();
        if (!Files.exists(path)) return map;
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("c") || line.startsWith("p")) continue;
                if (!line.startsWith("v")) continue;
                String[] t = line.split("\\s+");
                if (t.length < 4) continue;
                int id = Integer.parseInt(t[1]);
                long x = Long.parseLong(t[2]);
                long y = Long.parseLong(t[3]);
                map.put(id, new long[]{x, y});
            }
        }
        return map;
    }

    static Map<EdgeKey, Edge> readEdges(Path distPath, Path timePath) throws IOException {
        Map<EdgeKey, Edge> map = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(distPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("c") || line.startsWith("p")) continue;
                if (!line.startsWith("a")) continue;
                String[] t = line.split("\\s+");
                if (t.length < 4) continue;
                int u = Integer.parseInt(t[1]);
                int v = Integer.parseInt(t[2]);
                int w = Integer.parseInt(t[3]);
                EdgeKey k = new EdgeKey(u, v);
                Edge e = map.get(k);
                if (e == null) {
                    e = new Edge(u, v);
                    map.put(k, e);
                }
                e.distance = w;
            }
        }
        try (BufferedReader br = Files.newBufferedReader(timePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("c") || line.startsWith("p")) continue;
                if (!line.startsWith("a")) continue;
                String[] t = line.split("\\s+");
                if (t.length < 4) continue;
                int u = Integer.parseInt(t[1]);
                int v = Integer.parseInt(t[2]);
                int w = Integer.parseInt(t[3]);
                EdgeKey k = new EdgeKey(u, v);
                Edge e = map.get(k);
                if (e == null) {
                    e = new Edge(u, v);
                    map.put(k, e);
                }
                e.time = w;
            }
        }
        return map;
    }
}
