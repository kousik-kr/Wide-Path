package managers;

import models.QueryResult;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe query logger that persists query inputs and outputs to log files.
 * Logs are organized by date and include comprehensive query details.
 * 
 * Features:
 * - Separate log files for each day
 * - Thread-safe write operations
 * - Structured output format (human-readable and parseable)
 * - Configurable log directory
 * - Automatic directory creation
 * - Both GUI and API compatible
 * 
 * @author Wide-Path Team
 */
public class QueryLogger {
    private static final String DEFAULT_LOG_DIR = "logs/queries";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_SEPARATOR = "=".repeat(80);
    
    private final Path logDirectory;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean enabled = true;

    /**
     * Creates a QueryLogger with the default log directory
     */
    public QueryLogger() {
        this(DEFAULT_LOG_DIR);
    }

    /**
     * Creates a QueryLogger with a custom log directory
     * @param logDirectoryPath Path to the log directory
     */
    public QueryLogger(String logDirectoryPath) {
        this.logDirectory = Paths.get(logDirectoryPath);
        initializeLogDirectory();
    }

    /**
     * Initializes the log directory, creating it if it doesn't exist
     */
    private void initializeLogDirectory() {
        try {
            if (!Files.exists(logDirectory)) {
                Files.createDirectories(logDirectory);
                System.out.println("[QueryLogger] Created log directory: " + logDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[QueryLogger] Failed to create log directory: " + e.getMessage());
            enabled = false;
        }
    }

    /**
     * Logs a query result to the daily log file
     * @param result The QueryResult to log
     */
    public void logQuery(QueryResult result) {
        if (!enabled) {
            return;
        }

        lock.lock();
        try {
            Path logFile = getLogFileForToday();
            String logEntry = formatQueryResult(result);
            
            // Append to file
            Files.writeString(logFile, logEntry, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            System.err.println("[QueryLogger] Failed to write log entry: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Logs a query with raw parameters (for API server usage)
     * @param source Source node ID
     * @param destination Destination node ID
     * @param departure Departure time
     * @param interval Interval duration
     * @param budget Time budget
     * @param result The result object (can be null if query failed) - uses default package Result class
     * @param executionTimeMs Execution time in milliseconds
     * @param errorMessage Error message if query failed (null if successful)
     */
    public void logQueryRaw(int source, int destination, double departure, 
                            double interval, double budget, Object resultObj, 
                            long executionTimeMs, String errorMessage) {
        if (!enabled) {
            return;
        }

        lock.lock();
        try {
            Path logFile = getLogFileForToday();
            String logEntry = formatRawQuery(source, destination, departure, interval, 
                                            budget, resultObj, executionTimeMs, errorMessage);
            
            Files.writeString(logFile, logEntry,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            System.err.println("[QueryLogger] Failed to write log entry: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the log file path for today
     * @return Path to today's log file
     */
    private Path getLogFileForToday() {
        String dateStr = LocalDateTime.now().format(FILE_DATE_FORMAT);
        return logDirectory.resolve("query_log_" + dateStr + ".log");
    }

    /**
     * Formats a QueryResult into a structured log entry
     * @param result The QueryResult to format
     * @return Formatted log entry string
     */
    private String formatQueryResult(QueryResult result) {
        StringBuilder sb = new StringBuilder();
        String timestamp = result.getTimestamp().format(TIMESTAMP_FORMAT);
        
        sb.append("\n").append(LOG_SEPARATOR).append("\n");
        sb.append("QUERY LOG ENTRY - ").append(timestamp).append("\n");
        sb.append(LOG_SEPARATOR).append("\n");
        
        // Input Parameters
        sb.append("\n[INPUT PARAMETERS]\n");
        sb.append(String.format("  Source Node:           %d\n", result.getSourceNode()));
        sb.append(String.format("  Destination Node:      %d\n", result.getDestinationNode()));
        sb.append(String.format("  Departure Time:        %.2f min (%.2f hrs)\n", 
            result.getDepartureTime(), result.getDepartureTime() / 60.0));
        sb.append(String.format("  Interval Duration:     %.2f min\n", result.getIntervalDuration()));
        sb.append(String.format("  Budget:                %.2f min\n", result.getBudget()));
        
        // Query Status
        sb.append("\n[QUERY STATUS]\n");
        sb.append(String.format("  Success:               %s\n", result.isSuccess() ? "YES" : "NO"));
        sb.append(String.format("  Execution Time:        %d ms\n", result.getExecutionTimeMs()));
        
        if (result.isSuccess()) {
            // Results
            sb.append("\n[RESULTS]\n");
            sb.append(String.format("  Actual Departure:      %.2f min\n", result.getActualDepartureTime()));
            sb.append(String.format("  Score:                 %.4f\n", result.getScore()));
            sb.append(String.format("  Travel Time:           %.2f min (%.2f hrs)\n", 
                result.getTravelTime(), result.getTravelTime() / 60.0));
            sb.append(String.format("  Right Turns:           %d\n", result.getRightTurns()));
            sb.append(String.format("  Sharp Turns:           %d\n", result.getSharpTurns()));
            
            // Path Information
            if (result.getPathNodes() != null && !result.getPathNodes().isEmpty()) {
                sb.append("\n[PATH DETAILS]\n");
                sb.append(String.format("  Path Length:           %d nodes\n", result.getPathNodes().size()));
                sb.append(String.format("  Wide Edges:            %d\n", 
                    result.getWideEdgeIndices() != null ? result.getWideEdgeIndices().size() : 0));
                
                // Path nodes (limited to first/last few if too long)
                List<Integer> pathNodes = result.getPathNodes();
                sb.append("  Path Nodes:            ");
                if (pathNodes.size() <= 20) {
                    sb.append(pathNodes.toString());
                } else {
                    sb.append("[");
                    for (int i = 0; i < 10; i++) {
                        sb.append(pathNodes.get(i)).append(", ");
                    }
                    sb.append("... (").append(pathNodes.size() - 20).append(" more) ..., ");
                    for (int i = pathNodes.size() - 10; i < pathNodes.size(); i++) {
                        sb.append(pathNodes.get(i));
                        if (i < pathNodes.size() - 1) sb.append(", ");
                    }
                    sb.append("]");
                }
                sb.append("\n");
                
                // Wide edge indices
                if (result.getWideEdgeIndices() != null && !result.getWideEdgeIndices().isEmpty()) {
                    sb.append("  Wide Edge Indices:     ").append(result.getWideEdgeIndices().toString()).append("\n");
                }
            }
        } else {
            // Error Information
            sb.append("\n[ERROR DETAILS]\n");
            sb.append(String.format("  Error Message:         %s\n", result.getErrorMessage()));
        }
        
        sb.append("\n").append(LOG_SEPARATOR).append("\n\n");
        
        return sb.toString();
    }

    /**
     * Formats a raw query result into a structured log entry (for API usage)
     */
    private String formatRawQuery(int source, int destination, double departure,
                                  double interval, double budget, Object resultObj,
                                  long executionTimeMs, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        sb.append("\n").append(LOG_SEPARATOR).append("\n");
        sb.append("QUERY LOG ENTRY - ").append(timestamp).append("\n");
        sb.append(LOG_SEPARATOR).append("\n");
        
        // Input Parameters
        sb.append("\n[INPUT PARAMETERS]\n");
        sb.append(String.format("  Source Node:           %d\n", source));
        sb.append(String.format("  Destination Node:      %d\n", destination));
        sb.append(String.format("  Departure Time:        %.2f min (%.2f hrs)\n", 
            departure, departure / 60.0));
        sb.append(String.format("  Interval Duration:     %.2f min\n", interval));
        sb.append(String.format("  Budget:                %.2f min\n", budget));
        
        // Query Status
        sb.append("\n[QUERY STATUS]\n");
        boolean success = (resultObj != null && errorMessage == null);
        sb.append(String.format("  Success:               %s\n", success ? "YES" : "NO"));
        sb.append(String.format("  Execution Time:        %d ms\n", executionTimeMs));
        
        if (success && resultObj != null) {
            // Use reflection to access Result fields since it's in default package
            try {
                Class<?> resultClass = resultObj.getClass();
                java.lang.reflect.Method getDeparture = resultClass.getMethod("get_departureTime");
                java.lang.reflect.Method getScore = resultClass.getMethod("get_score");
                java.lang.reflect.Method getTravelTime = resultClass.getMethod("get_travel_time");
                java.lang.reflect.Method getRightTurns = resultClass.getMethod("get_right_turns");
                java.lang.reflect.Method getSharpTurns = resultClass.getMethod("get_sharp_turns");
                java.lang.reflect.Method getPathNodes = resultClass.getMethod("get_pathNodes");
                java.lang.reflect.Method getWideEdges = resultClass.getMethod("get_wideEdgeIndices");
                
                double actualDeparture = (Double) getDeparture.invoke(resultObj);
                double score = (Double) getScore.invoke(resultObj);
                double travelTime = (Double) getTravelTime.invoke(resultObj);
                int rightTurns = (Integer) getRightTurns.invoke(resultObj);
                int sharpTurns = (Integer) getSharpTurns.invoke(resultObj);
                @SuppressWarnings("unchecked")
                List<Integer> pathNodes = (List<Integer>) getPathNodes.invoke(resultObj);
                @SuppressWarnings("unchecked")
                List<Integer> wideEdgeIndices = (List<Integer>) getWideEdges.invoke(resultObj);
                
                // Results
                sb.append("\n[RESULTS]\n");
                sb.append(String.format("  Actual Departure:      %.2f min\n", actualDeparture));
                sb.append(String.format("  Score:                 %.4f\n", score));
                sb.append(String.format("  Travel Time:           %.2f min (%.2f hrs)\n", 
                    travelTime, travelTime / 60.0));
                sb.append(String.format("  Right Turns:           %d\n", rightTurns));
                sb.append(String.format("  Sharp Turns:           %d\n", sharpTurns));
                
                // Path Information
                if (pathNodes != null && !pathNodes.isEmpty()) {
                    sb.append("\n[PATH DETAILS]\n");
                    sb.append(String.format("  Path Length:           %d nodes\n", pathNodes.size()));
                    sb.append(String.format("  Wide Edges:            %d\n", 
                        wideEdgeIndices != null ? wideEdgeIndices.size() : 0));
                    
                    // Path nodes (limited to first/last few if too long)
                    sb.append("  Path Nodes:            ");
                    if (pathNodes.size() <= 20) {
                        sb.append(pathNodes.toString());
                    } else {
                        sb.append("[");
                        for (int i = 0; i < 10; i++) {
                            sb.append(pathNodes.get(i)).append(", ");
                        }
                        sb.append("... (").append(pathNodes.size() - 20).append(" more) ..., ");
                        for (int i = pathNodes.size() - 10; i < pathNodes.size(); i++) {
                            sb.append(pathNodes.get(i));
                            if (i < pathNodes.size() - 1) sb.append(", ");
                        }
                        sb.append("]");
                    }
                    sb.append("\n");
                    
                    // Wide edge indices
                    if (wideEdgeIndices != null && !wideEdgeIndices.isEmpty()) {
                        sb.append("  Wide Edge Indices:     ").append(wideEdgeIndices.toString()).append("\n");
                    }
                }
            } catch (Exception e) {
                sb.append("\n[ERROR ACCESSING RESULT DATA]\n");
                sb.append("  Exception:             ").append(e.getMessage()).append("\n");
            }
        } else {
            // Error Information
            sb.append("\n[ERROR DETAILS]\n");
            sb.append(String.format("  Error Message:         %s\n", 
                errorMessage != null ? errorMessage : "Unknown error"));
        }
        
        sb.append("\n").append(LOG_SEPARATOR).append("\n\n");
        
        return sb.toString();
    }

    /**
     * Enables or disables logging
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks if logging is enabled
     * @return true if logging is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the absolute path to the log directory
     * @return Log directory path
     */
    public Path getLogDirectory() {
        return logDirectory.toAbsolutePath();
    }

    /**
     * Writes a custom message to the log (for debugging/info purposes)
     * @param message The message to log
     */
    public void logMessage(String message) {
        if (!enabled) {
            return;
        }

        lock.lock();
        try {
            Path logFile = getLogFileForToday();
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String logEntry = String.format("\n[%s] %s\n", timestamp, message);
            
            Files.writeString(logFile, logEntry,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            System.err.println("[QueryLogger] Failed to write message: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
