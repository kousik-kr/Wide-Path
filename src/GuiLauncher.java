import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;

public class GuiLauncher {
    private JFrame frame;
    private JTextField sourceField;
    private JTextField destinationField;
    private JTextField departureField;
    private JTextField intervalField;
    private JTextField budgetField;
    private JTextArea outputArea;
    private JLabel statusLabel;

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
                    "Failed to load graph files. Ensure GRAPH_DATA_DIR points to your dataset.",
                    "Graph load error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        initUi();
    }

    private void initUi() {
        frame = new JFrame("Wide-Path Demo");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

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

        outputArea = new JTextArea(10, 40);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        statusLabel = new JLabel("Graph loaded. Ready.");

        frame.add(form, BorderLayout.NORTH);
        frame.add(new JScrollPane(outputArea), BorderLayout.CENTER);
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
                    result = BidirectionalAstar.runSingleQuery(source, dest, departure, interval, budget);
                } catch (InterruptedException | ExecutionException e) {
                    result = null;
                    outputArea.setText("Error: " + e.getMessage());
                } finally {
                    elapsedMs = System.currentTimeMillis() - start;
                }
                return null;
            }

            @Override
            protected void done() {
                if (result == null) {
                    statusLabel.setText("Query failed.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Source -> Destination: ").append(source).append(" -> ").append(dest).append("\n");
                sb.append("Departure (input / result): ").append(departure).append(" / ").append(result.get_departureTime()).append("\n");
                sb.append("Score: ").append(result.get_score()).append("\n");
                sb.append("Right turns: ").append(result.get_right_turns()).append("\n");
                sb.append("Elapsed: ").append(elapsedMs / 1000.0).append(" seconds\n");
                outputArea.setText(sb.toString());
                statusLabel.setText("Done.");
            }
        };
        worker.execute();
    }
}
