import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class GuiLauncher {
    private JFrame frame;
    private JTextField sourceField;
    private JTextField destinationField;
    private JTextField departureField;
    private JTextField intervalField;
    private JTextField budgetField;
    private JTextArea outputArea;
    private MapPanel mapPanel;
    private JLabel statusLabel;
    private List<Integer> currentPath = Collections.emptyList();
    private List<Integer> currentWideEdges = Collections.emptyList();

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Headless environment detected. The Swing GUI cannot be displayed. Please run with a display (set DISPLAY or use a desktop session).");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            GuiLauncher launcher = new GuiLauncher();
            launcher.start();
        });
    }

    private void start() {
        BidirectionalAstar.configureDefaults();
        boolean loaded = BidirectionalAstar.loadGraphFromDisk(null, null);
        if (!loaded) {
            JOptionPane.showMessageDialog(null,
                    "Failed to load graph files. Ensure the configured graph directory points to your dataset.",
                    "Graph load error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        System.out.println("[GUI] Graph loaded. Nodes=" + Graph.get_nodes().size());
        initUi();
    }

    private void initUi() {
        frame = new JFrame("Wide-Path Demo");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setPreferredSize(new Dimension(1200, 800));

        JPanel form = new JPanel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        sourceField = new JTextField("0", 10);
        destinationField = new JTextField("0", 10);
        departureField = new JTextField("450", 10);
        intervalField = new JTextField("360", 10);
        budgetField = new JTextField("45", 10);

        int row = 0;
        row = addRow(form, c, row, "Source node ID:", sourceField);
        row = addRow(form, c, row, "Destination node ID:", destinationField);
        row = addRow(form, c, row, "Departure time (minutes from 00:00):", departureField);
        row = addRow(form, c, row, "Interval duration (minutes):", intervalField);
        row = addRow(form, c, row, "Budget (minutes):", budgetField);

        JButton runButton = new JButton("Run Query");
        runButton.addActionListener(this::runQuery);
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(runButton, c);

        outputArea = new JTextArea(16, 48);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));

        statusLabel = new JLabel("Graph loaded. Ready.");

        mapPanel = new MapPanel();

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, form, outputScroll);
        topSplit.setResizeWeight(0.45);
        topSplit.setBorder(BorderFactory.createEmptyBorder());

        frame.add(topSplit, BorderLayout.NORTH);
        frame.add(mapPanel, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private int addRow(JPanel panel, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        panel.add(field, c);
        return row + 1;
    }

    private void runQuery(ActionEvent event) {
        statusLabel.setText("Running query...");
        outputArea.setText("");
        int source, dest;
        double departure, interval, budget;
        try {
            source = Integer.parseInt(sourceField.getText().trim());
            dest = Integer.parseInt(destinationField.getText().trim());
            departure = Double.parseDouble(departureField.getText().trim());
            interval = Double.parseDouble(intervalField.getText().trim());
            budget = Double.parseDouble(budgetField.getText().trim());
        } catch (NumberFormatException nfe) {
            statusLabel.setText("Invalid input: " + nfe.getMessage());
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            Result result;
            long elapsedMs;

            @Override
            protected Void doInBackground() {
                long start = System.currentTimeMillis();
                try {
                    BidirectionalAstar.setIntervalDuration(interval);
                    System.out.println("[GUI] Running query " + source + " -> " + dest + " dep=" + departure + " interval=" + interval + " budget=" + budget);
                    result = BidirectionalAstar.runSingleQuery(source, dest, departure, interval, budget);
                } catch (InterruptedException | ExecutionException e) {
                    result = null;
                    outputArea.setText("Error: " + e.getMessage());
                    System.err.println("[GUI] Query error: " + e.getMessage());
                } finally {
                    elapsedMs = System.currentTimeMillis() - start;
                    System.out.println("[GUI] Query finished in " + elapsedMs + " ms");
                }
                return null;
            }

            @Override
            protected void done() {
                if (result == null) {
                    currentPath = Collections.emptyList();
                    currentWideEdges = Collections.emptyList();
                    mapPanel.setPath(currentPath, currentWideEdges);
                    statusLabel.setText("Query failed.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Source -> Destination: ").append(source).append(" -> ").append(dest).append("\n");
                sb.append("Departure (input / result): ").append(departure).append(" / ").append(result.get_departureTime()).append("\n");
                sb.append("Score: ").append(result.get_score()).append("\n");
                sb.append("Travel time (approx): ").append(String.format("%.2f", result.get_travel_time())).append(" mins\n");
                sb.append("Right turns: ").append(result.get_right_turns()).append("\n");
                sb.append("Sharp turns: ").append(result.get_sharp_turns()).append("\n");

                List<Integer> path = result.get_pathNodes();
                List<Integer> wideEdges = result.get_wideEdgeIndices();
                currentPath = path != null ? path : Collections.emptyList();
                currentWideEdges = wideEdges != null ? wideEdges : Collections.emptyList();
                mapPanel.setPath(currentPath, currentWideEdges);

                if (path != null && path.size() > 1) {
                    sb.append("Path edges (wide edges marked *):\n");
                    for (int i = 0; i < path.size() - 1; i++) {
                        int u = path.get(i);
                        int v = path.get(i + 1);
                        boolean isWide = wideEdges != null && wideEdges.contains(i);
                        sb.append(u).append(" -> ").append(v);
                        if (isWide) sb.append("  *wide");
                        sb.append("\n");
                    }
                } else {
                    sb.append("No path computed.\n");
                }

                sb.append("Elapsed: ").append(elapsedMs / 1000.0).append(" seconds\n");
                outputArea.setText(sb.toString());
                statusLabel.setText("Done.");
            }
        };
        worker.execute();
    }

    private static class MapPanel extends JPanel {
        private List<Integer> path = Collections.emptyList();
        private List<Integer> wideEdgeIndices = Collections.emptyList();

        MapPanel() {
            setPreferredSize(new Dimension(520, 640));
            setBackground(Color.white);
            setBorder(BorderFactory.createTitledBorder("Map (nodes & directed edges)"));
        }

        void setPath(List<Integer> path, List<Integer> wideEdgeIndices) {
            this.path = path != null ? new ArrayList<>(path) : Collections.emptyList();
            this.wideEdgeIndices = wideEdgeIndices != null ? new ArrayList<>(wideEdgeIndices) : Collections.emptyList();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Map<Integer, Node> nodes = Graph.get_nodes();
            if (nodes == null || nodes.isEmpty()) {
                drawCenteredText(g, "No nodes loaded");
                return;
            }

            double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
            double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
            for (Node n : nodes.values()) {
                double lat = n.get_latitude();
                double lon = n.get_longitude();
                if (lat < minLat) minLat = lat;
                if (lat > maxLat) maxLat = lat;
                if (lon < minLon) minLon = lon;
                if (lon > maxLon) maxLon = lon;
            }
            if (!Double.isFinite(minLat) || !Double.isFinite(minLon)) {
                drawCenteredText(g, "Invalid coordinates");
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padding = 30;
            double xRange = Math.max(maxLon - minLon, 1e-6);
            double yRange = Math.max(maxLat - minLat, 1e-6);

            // Helper to project lon/lat to panel coords
            for (Map.Entry<Integer, Node> entry : nodes.entrySet()) {
                Node from = entry.getValue();
                for (Edge edge : from.get_outgoing_edges().values()) {
                    Node to = nodes.get(edge.get_destination());
                    if (to == null) continue;
                    Point p1 = project(from, w, h, padding, minLon, minLat, xRange, yRange, maxLat);
                    Point p2 = project(to, w, h, padding, minLon, minLat, xRange, yRange, maxLat);
                    g2.setColor(new Color(0x2D7DD2));
                    g2.setStroke(new BasicStroke(1.4f));
                    drawArrow(g2, p1.x, p1.y, p2.x, p2.y);
                }
            }

            g2.setColor(Color.DARK_GRAY);
            for (Node n : nodes.values()) {
                Point p = project(n, w, h, padding, minLon, minLat, xRange, yRange, maxLat);
                g2.fillOval(p.x - 3, p.y - 3, 6, 6);
            }

            if (path != null && path.size() > 1) {
                for (int i = 0; i < path.size() - 1; i++) {
                    int u = path.get(i);
                    int v = path.get(i + 1);
                    Node from = nodes.get(u);
                    Node to = nodes.get(v);
                    if (from == null || to == null) continue;
                    Point p1 = project(from, w, h, padding, minLon, minLat, xRange, yRange, maxLat);
                    Point p2 = project(to, w, h, padding, minLon, minLat, xRange, yRange, maxLat);
                    boolean wide = wideEdgeIndices != null && wideEdgeIndices.contains(i);
                    g2.setColor(wide ? new Color(0xE4572E) : new Color(0x0A8754));
                    g2.setStroke(new BasicStroke(wide ? 3.0f : 2.0f));
                    drawArrow(g2, p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        private void drawCenteredText(Graphics g, String text) {
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() + fm.getAscent()) / 2;
            g.drawString(text, x, y);
        }

        private Point project(Node n, int w, int h, int padding,
                              double minLon, double minLat, double xRange, double yRange, double maxLat) {
            double xNorm = (n.get_longitude() - minLon) / xRange;
            double yNorm = (maxLat - n.get_latitude()) / yRange; // invert so north is up
            int x = (int) (padding + xNorm * (w - 2 * padding));
            int y = (int) (padding + yNorm * (h - 2 * padding));
            return new Point(x, y);
        }

        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
            g2.drawLine(x1, y1, x2, y2);
            double dx = x2 - x1;
            double dy = y2 - y1;
            double angle = Math.atan2(dy, dx);
            int len = 10;
            int wing = 4;
            int xArrow1 = (int) (x2 - len * Math.cos(angle) + wing * Math.sin(angle));
            int yArrow1 = (int) (y2 - len * Math.sin(angle) - wing * Math.cos(angle));
            int xArrow2 = (int) (x2 - len * Math.cos(angle) - wing * Math.sin(angle));
            int yArrow2 = (int) (y2 - len * Math.sin(angle) + wing * Math.cos(angle));
            Polygon head = new Polygon();
            head.addPoint(x2, y2);
            head.addPoint(xArrow1, yArrow1);
            head.addPoint(xArrow2, yArrow2);
            g2.fillPolygon(head);
        }
    }
}
