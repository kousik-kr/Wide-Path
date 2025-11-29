import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import managers.MetricsCollector;
import managers.QueryHistoryManager;
import managers.QueryLogger;
import managers.ThemeManager;
import managers.GoogleDriveDatasetLoader;
import models.QueryResult;
import ui.components.StatusBar;
import ui.panels.AdvancedMapPanel;
import ui.panels.MetricsDashboard;
import ui.panels.QueryHistoryPanel;
import ui.panels.QueryInputPanel;

/**
 * World-Class Wide-Path GUI Application - Main Entry Point
 * 
 * Architecture:
 * - Modular design with separate packages for UI, managers, and models
 * - Thread-safe concurrent operations
 * - Modern Material Design inspired interface
 * - Comprehensive metrics and history tracking
 * 
 * @author Wide-Path Team
 * @version 2.0
 */
public class GuiLauncher {
    // Core Application Components
    private final JFrame frame;
    private final ExecutorService executorService;
    private final QueryHistoryManager historyManager;
    private final MetricsCollector metricsCollector;
    private final ThemeManager themeManager;
    private final QueryLogger queryLogger;
    
    // UI Panels
    private QueryInputPanel inputPanel;
    private JTextPane outputPane;
    private AdvancedMapPanel mapPanel;
    private MetricsDashboard metricsDashboard;
    private QueryHistoryPanel historyPanel;
    private StatusBar statusBar;
    private JProgressBar progressBar;
    
    // State Management
    private volatile boolean isQueryRunning = false;
    private List<Integer> currentPath = Collections.emptyList();
    private List<Integer> currentWideEdges = Collections.emptyList();

    public static void main(String[] args) {
        // Set system look and feel properties for better rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Headless environment detected. GUI cannot be displayed.");
            return;
        }
        
        // Set modern Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                GuiLauncher launcher = new GuiLauncher();
                launcher.start();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "Failed to launch application: " + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public GuiLauncher() {
        this.frame = new JFrame("Wide-Path Pro - Advanced Pathfinding Analysis");
        this.executorService = Executors.newFixedThreadPool(4);
        this.historyManager = new QueryHistoryManager();
        this.metricsCollector = new MetricsCollector();
        this.themeManager = new ThemeManager();
        this.queryLogger = new QueryLogger();
        
        // Configure frame
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
        
        setupKeyboardShortcuts();
    }

    private void start() {
        // Try to load dataset from Google Drive if not available locally
        try {
            GoogleDriveDatasetLoader driveLoader = new GoogleDriveDatasetLoader();
            
            // Check if dataset is already cached
            if (!driveLoader.isDatasetCached(null)) {
                int choice = JOptionPane.showConfirmDialog(null,
                    "Dataset not found locally.\n" +
                    "Would you like to download it from Google Drive?\n\n" +
                    "Note: This may take several minutes depending on dataset size.",
                    "Download Dataset",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                
                if (choice == JOptionPane.YES_OPTION) {
                    // Show progress dialog
                    JDialog progressDialog = new JDialog(frame, "Downloading Dataset", true);
                    JProgressBar progressBar = new JProgressBar();
                    progressBar.setIndeterminate(true);
                    progressBar.setString("Downloading dataset files from Google Drive...");
                    progressBar.setStringPainted(true);
                    JPanel panel = new JPanel(new BorderLayout(10, 10));
                    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                    panel.add(new JLabel("Please wait while the dataset is being downloaded."), BorderLayout.NORTH);
                    panel.add(progressBar, BorderLayout.CENTER);
                    progressDialog.add(panel);
                    progressDialog.setSize(450, 150);
                    progressDialog.setLocationRelativeTo(null);
                    
                    // Download in background thread
                    SwingWorker<String, Void> downloadWorker = new SwingWorker<String, Void>() {
                        @Override
                        protected String doInBackground() throws Exception {
                            return driveLoader.ensureDatasetAvailable();
                        }
                        
                        @Override
                        protected void done() {
                            progressDialog.dispose();
                            try {
                                String datasetPath = get();
                                BidirectionalAstar.setConfiguredGraphDataDir(datasetPath);
                                loadGraphAndInitUI();
                            } catch (Exception e) {
                                // Check if this is a configuration error
                                String errorMsg = e.getMessage();
                                if (errorMsg != null && errorMsg.contains("File ID not configured")) {
                                    int fallbackChoice = JOptionPane.showConfirmDialog(null,
                                        "Google Drive file IDs are not configured yet.\n\n" +
                                        "Would you like to:\n" +
                                        "• YES - Use demo graph (small test dataset)\n" +
                                        "• NO - Exit and configure file IDs later\n\n" +
                                        "To configure: See GOOGLE_DRIVE_QUICKSTART.md",
                                        "Configuration Required",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.INFORMATION_MESSAGE);
                                    
                                    if (fallbackChoice == JOptionPane.YES_OPTION) {
                                        // Use demo graph
                                        loadGraphAndInitUI();
                                    } else {
                                        System.exit(0);
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(null,
                                        "Failed to download dataset: " + errorMsg + "\n\n" +
                                        "Please check:\n" +
                                        "1. Internet connection is available\n" +
                                        "2. Google Drive files are publicly accessible\n\n" +
                                        "Loading demo graph instead...",
                                        "Download Error",
                                        JOptionPane.WARNING_MESSAGE);
                                    loadGraphAndInitUI();
                                }
                            }
                        }
                    };
                    
                    downloadWorker.execute();
                    progressDialog.setVisible(true);
                    return; // Exit start() - loadGraphAndInitUI() will be called after download
                } else {
                    // User declined download, offer demo graph
                    int demoChoice = JOptionPane.showConfirmDialog(null,
                        "Would you like to use the demo graph instead?\n\n" +
                        "The demo graph is a small test dataset for trying out the application.",
                        "Use Demo Graph",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                    
                    if (demoChoice != JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                    // Fall through to load demo graph
                }
            } else {
                // Dataset is cached, use it
                String datasetPath = driveLoader.ensureDatasetAvailable();
                BidirectionalAstar.setConfiguredGraphDataDir(datasetPath);
            }
        } catch (Exception e) {
            System.err.println("[GUI] Google Drive loader error: " + e.getMessage());
            // Fall back to default behavior
        }
        
        // Load graph
        loadGraphAndInitUI();
    }
    
    private void loadGraphAndInitUI() {
        BidirectionalAstar.configureDefaults();
        boolean loaded = BidirectionalAstar.loadGraphFromDisk(null, null);
        if (!loaded) {
            JOptionPane.showMessageDialog(null,
                    "Failed to load graph files. Ensure the configured graph directory points to your dataset.",
                    "Graph Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int nodeCount = Graph.get_nodes().size();
        System.out.println("[GUI] Graph loaded. Nodes=" + nodeCount);
        
        initializeUI(nodeCount);
        frame.setVisible(true);
    }

    private void setupKeyboardShortcuts() {
        KeyStroke runShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        
        Action runAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isQueryRunning && inputPanel != null) {
                    // Trigger run button programmatically
                    System.out.println("[GUI] Run query triggered via Ctrl+Enter");
                }
            }
        };
        
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(runShortcut, "runQuery");
        frame.getRootPane().getActionMap().put("runQuery", runAction);
    }

    private void shutdown() {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Are you sure you want to exit Wide-Path Pro?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            statusBar.dispose();
            metricsDashboard.dispose();
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
            frame.dispose();
            System.exit(0);
        }
    }

    private void initializeUI(int maxNodeId) {
        frame.setLayout(new BorderLayout(0, 0));
        frame.setPreferredSize(new Dimension(1600, 900));
        
        // Create menu bar
        createMenuBar();
        frame.setJMenuBar(createMenuBar());
        
        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create input panel (left side)
        inputPanel = new QueryInputPanel(maxNodeId, this::executeQuery);
        
        // Create tabbed pane for output and visualization
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        // Results tab
        JPanel resultsPanel = createResultsPanel();
        tabbedPane.addTab("📊 Results", null, resultsPanel, "Query results and statistics");
        
        // Visualization tab
        mapPanel = new AdvancedMapPanel();
        tabbedPane.addTab("🗺️ Visualization", null, mapPanel, "Path visualization and graph explorer");
        
        // Metrics tab
        metricsDashboard = new MetricsDashboard(metricsCollector);
        tabbedPane.addTab("📈 Metrics", null, metricsDashboard, "Performance metrics and analytics");
        
        // History tab
        historyPanel = new QueryHistoryPanel(historyManager);
        tabbedPane.addTab("🕐 History", null, historyPanel, "Query history and comparison");
        
        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, tabbedPane);
        splitPane.setResizeWeight(0.25);
        splitPane.setDividerSize(8);
        splitPane.setDividerLocation(400);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // Create status bar
        statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        frame.add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        
        statusBar.setMessage("Ready. Graph loaded with " + maxNodeId + " nodes.", StatusBar.MessageType.INFO);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportItem = new JMenuItem("Export Results...");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> shutdown());
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // View Menu
        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem("Dark Mode");
        darkModeItem.addActionListener(e -> {
            themeManager.toggleTheme();
            // Apply theme would go here
        });
        viewMenu.add(darkModeItem);
        
        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Output text pane with styled document
        outputPane = new JTextPane();
        outputPane.setEditable(false);
        outputPane.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(outputPane);
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        
        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private void executeQuery(QueryInputPanel.QueryParameters params) {
        if (isQueryRunning) {
            statusBar.setMessage("A query is already running!", StatusBar.MessageType.WARNING);
            return;
        }
        
        isQueryRunning = true;
        inputPanel.setRunEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusBar.setMessage("Executing query...", StatusBar.MessageType.INFO);
        
        // Display input parameters
        displayInputParameters(params);
        
        SwingWorker<QueryResult, Void> worker = new SwingWorker<>() {
            @Override
            protected QueryResult doInBackground() {
                long startTime = System.currentTimeMillis();
                
                try {
                    BidirectionalAstar.setIntervalDuration(params.interval);
                    System.out.println("[GUI] Running query " + params.source + " -> " + params.destination);
                    
                    Result result = BidirectionalAstar.runSingleQuery(
                        params.source, params.destination, params.departure, params.interval, params.budget);
                    
                    long executionTime = System.currentTimeMillis() - startTime;
                    
                    if (result != null) {
                        return new QueryResult.Builder()
                            .setSourceNode(params.source)
                            .setDestinationNode(params.destination)
                            .setDepartureTime(params.departure)
                            .setIntervalDuration(params.interval)
                            .setBudget(params.budget)
                            .setActualDepartureTime(result.get_departureTime())
                            .setScore(result.get_score())
                            .setTravelTime(result.get_travel_time())
                            .setRightTurns(result.get_right_turns())
                            .setSharpTurns(result.get_sharp_turns())
                            .setPathNodes(result.get_pathNodes())
                            .setWideEdgeIndices(result.get_wideEdgeIndices())
                            .setExecutionTimeMs(executionTime)
                            .setSuccess(true)
                            .build();
                    } else {
                        return new QueryResult.Builder()
                            .setSourceNode(params.source)
                            .setDestinationNode(params.destination)
                            .setDepartureTime(params.departure)
                            .setIntervalDuration(params.interval)
                            .setBudget(params.budget)
                            .setExecutionTimeMs(executionTime)
                            .setSuccess(false)
                            .setErrorMessage("Query returned null result")
                            .build();
                    }
                } catch (Exception e) {
                    long executionTime = System.currentTimeMillis() - startTime;
                    return new QueryResult.Builder()
                        .setSourceNode(params.source)
                        .setDestinationNode(params.destination)
                        .setDepartureTime(params.departure)
                        .setIntervalDuration(params.interval)
                        .setBudget(params.budget)
                        .setExecutionTimeMs(executionTime)
                        .setSuccess(false)
                        .setErrorMessage(e.getMessage())
                        .build();
                }
            }

            @Override
            protected void done() {
                try {
                    QueryResult result = get();
                    
                    // Log the query to file
                    queryLogger.logQuery(result);
                    
                    // Update history and metrics
                    historyManager.addQuery(result);
                    metricsCollector.recordQuery(result.isSuccess(), result.getExecutionTimeMs(), 
                        result.getPathNodes() != null ? result.getPathNodes().size() : 0);
                    
                    // Display results
                    displayResults(result);
                    
                    // Update map
                    if (result.isSuccess() && result.getPathNodes() != null) {
                        currentPath = result.getPathNodes();
                        currentWideEdges = result.getWideEdgeIndices();
                        mapPanel.setPath(currentPath, currentWideEdges);
                    }
                    
                    // Update history panel
                    historyPanel.refreshTable();
                    
                    if (result.isSuccess()) {
                        statusBar.setMessage("Query completed successfully in " + result.getExecutionTimeMs() + " ms", 
                            StatusBar.MessageType.SUCCESS);
                    } else {
                        statusBar.setMessage("Query failed: " + result.getErrorMessage(), StatusBar.MessageType.ERROR);
                    }
                } catch (Exception e) {
                    statusBar.setMessage("Error processing results: " + e.getMessage(), StatusBar.MessageType.ERROR);
                } finally {
                    isQueryRunning = false;
                    inputPanel.setRunEnabled(true);
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                }
            }
        };
        
        worker.execute();
    }

    private void displayInputParameters(QueryInputPanel.QueryParameters params) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("        QUERY INPUT PARAMETERS\n");
        sb.append("═══════════════════════════════════════\n\n");
        sb.append(String.format("🎯 Source Node:        %d\n", params.source));
        sb.append(String.format("🏁 Destination Node:   %d\n", params.destination));
        sb.append(String.format("🕐 Departure Time:     %d min (%.2f hrs)\n", params.departure, params.departure/60.0));
        sb.append(String.format("⏱  Interval Duration:  %d min\n", params.interval));
        sb.append(String.format("💰 Budget:             %d min\n\n", params.budget));
        sb.append("Processing query, please wait...\n");
        sb.append("═══════════════════════════════════════\n\n");
        
        outputPane.setText(sb.toString());
    }

    private void displayResults(QueryResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(outputPane.getText());
        
        if (result.isSuccess()) {
            sb.append("\n═══════════════════════════════════════\n");
            sb.append("          QUERY RESULTS\n");
            sb.append("═══════════════════════════════════════\n\n");
            sb.append(String.format("✓ Route Found!\n\n"));
            sb.append(String.format("📍 Route:              %d → %d\n", result.getSourceNode(), result.getDestinationNode()));
            sb.append(String.format("⏰ Departure:          %.2f min\n", result.getActualDepartureTime()));
            sb.append(String.format("🎯 Score:              %.2f\n", result.getScore()));
            sb.append(String.format("⏱  Travel Time:        %.2f min (%.2f hrs)\n", 
                result.getTravelTime(), result.getTravelTime()/60.0));
            sb.append(String.format("↪  Right Turns:        %d\n", result.getRightTurns()));
            sb.append(String.format("⤴  Sharp Turns:        %d\n", result.getSharpTurns()));
            
            if (result.getPathNodes() != null && !result.getPathNodes().isEmpty()) {
                sb.append(String.format("\n📊 Path Statistics:\n"));
                sb.append(String.format("   Nodes in path:     %d\n", result.getPathNodes().size()));
                sb.append(String.format("   Wide edges:        %d\n", 
                    result.getWideEdgeIndices() != null ? result.getWideEdgeIndices().size() : 0));
            }
            
            sb.append(String.format("\n⚡ Execution Time:     %d ms\n", result.getExecutionTimeMs()));
            sb.append("\n═══════════════════════════════════════\n");
        } else {
            sb.append("\n═══════════════════════════════════════\n");
            sb.append("          QUERY FAILED\n");
            sb.append("═══════════════════════════════════════\n\n");
            sb.append(String.format("✗ Error: %s\n", result.getErrorMessage()));
            sb.append(String.format("⚡ Execution Time: %d ms\n", result.getExecutionTimeMs()));
            sb.append("\n═══════════════════════════════════════\n");
        }
        
        outputPane.setText(sb.toString());
        outputPane.setCaretPosition(outputPane.getDocument().getLength());
    }

    private void showAboutDialog() {
        String message = "Wide-Path Pro v2.0\n\n" +
                        "Advanced Pathfinding Analysis System\n\n" +
                        "Features:\n" +
                        "• Modern Material Design UI\n" +
                        "• Real-time performance metrics\n" +
                        "• Query history and analytics\n" +
                        "• Advanced path visualization\n" +
                        "• Multiple rendering modes\n\n" +
                        "© 2025 Wide-Path Team";
        
        JOptionPane.showMessageDialog(frame, message, "About Wide-Path Pro", JOptionPane.INFORMATION_MESSAGE);
    }
}
