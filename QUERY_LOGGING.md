# Query Logging Feature

## Overview

The Wide-Path application now includes comprehensive query logging functionality that captures all query inputs and outputs to structured log files. This feature is available in both the **GUI** and **API** interfaces.

## Features

✅ **Thread-safe logging** - Safe for concurrent query execution  
✅ **Automatic daily log files** - Logs organized by date  
✅ **Structured output format** - Human-readable and parseable  
✅ **Complete query details** - Input parameters, results, path info, and execution metrics  
✅ **Error logging** - Captures failed queries with error messages  
✅ **Zero configuration** - Works out of the box with sensible defaults  

## Log File Location

By default, query logs are stored in:
```
logs/queries/
```

Log files are named by date:
```
query_log_2025-11-29.log
query_log_2025-11-30.log
...
```

## Log Entry Format

Each query creates a structured log entry with the following sections:

### 1. Input Parameters
- Source Node ID
- Destination Node ID
- Departure Time (in minutes and hours)
- Interval Duration
- Time Budget

### 2. Query Status
- Success/Failure status
- Execution time in milliseconds

### 3. Results (for successful queries)
- Actual Departure Time
- Score
- Travel Time
- Right Turns count
- Sharp Turns count

### 4. Path Details (when available)
- Path length (number of nodes)
- Wide edges count
- Complete path node sequence
- Wide edge indices

### 5. Error Details (for failed queries)
- Error message

## Example Log Entry

```
================================================================================
QUERY LOG ENTRY - 2025-11-29 11:52:35.123
================================================================================

[INPUT PARAMETERS]
  Source Node:           100
  Destination Node:      500
  Departure Time:        480.00 min (8.00 hrs)
  Interval Duration:     360.00 min
  Budget:                60.00 min

[QUERY STATUS]
  Success:               YES
  Execution Time:        1234 ms

[RESULTS]
  Actual Departure:      480.00 min
  Score:                 0.8765
  Travel Time:           45.50 min (0.76 hrs)
  Right Turns:           3
  Sharp Turns:           1

[PATH DETAILS]
  Path Length:           25 nodes
  Wide Edges:            5
  Path Nodes:            [100, 105, 112, 115, ..., 495, 500]
  Wide Edge Indices:     [2, 7, 11, 18, 22]

================================================================================
```

## GUI Integration

The query logger is automatically integrated into the GUI application (`GuiLauncher`). Every query executed through the GUI is logged:

```java
// Logging happens automatically after query execution
queryLogger.logQuery(result);
```

## API Integration

The query logger is integrated into the API server (`ApiServer`). All queries submitted via the `/api/queries/run` endpoint are logged:

```java
// API automatically logs with raw parameters
queryLogger.logQueryRaw(source, destination, departure, interval, budget, 
                        result, elapsed, errorMessage);
```

## Configuration

### Change Log Directory

You can customize the log directory by modifying the `QueryLogger` initialization:

**In GuiLauncher.java:**
```java
this.queryLogger = new QueryLogger("custom/path/to/logs");
```

**In ApiServer.java:**
```java
private static final QueryLogger queryLogger = new QueryLogger("custom/path/to/logs");
```

### Disable Logging

To temporarily disable logging without removing the code:

```java
queryLogger.setEnabled(false);
```

To re-enable:
```java
queryLogger.setEnabled(true);
```

## Log Management

### File Rotation
Logs are automatically rotated daily. Each day gets its own log file.

### Log Size
Log files grow with each query. Consider implementing a cleanup strategy for production:

- Archive old logs periodically
- Delete logs older than N days
- Compress archived logs

Example cleanup script (PowerShell):
```powershell
# Delete logs older than 30 days
Get-ChildItem "logs/queries" -Filter "*.log" | 
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-30) } | 
    Remove-Item
```

### Viewing Logs

Use any text editor to view logs:
```powershell
# View latest log
Get-Content logs/queries/query_log_$(Get-Date -Format yyyy-MM-dd).log -Tail 50

# Search for specific query
Select-String -Path "logs/queries/*.log" -Pattern "Source Node:           100"
```

## Thread Safety

The `QueryLogger` uses a `ReentrantLock` to ensure thread-safe writes. Multiple queries can execute concurrently without corrupting the log file.

## Performance

Query logging has minimal performance impact:
- Asynchronous file writes
- Efficient string formatting
- No blocking on successful writes
- Graceful degradation on errors

## Troubleshooting

### Logs not being created

**Problem:** Log directory doesn't exist or no write permissions

**Solution:** Check console output for error messages:
```
[QueryLogger] Failed to create log directory: ...
```

Ensure the application has write permissions to the log directory.

### Logs are empty

**Problem:** Logging might be disabled

**Solution:** Check if logging is enabled:
```java
if (queryLogger.isEnabled()) {
    System.out.println("Logging is enabled");
}
```

## API Reference

### QueryLogger Class

Located in: `src/managers/QueryLogger.java`

#### Constructors
- `QueryLogger()` - Creates logger with default directory
- `QueryLogger(String path)` - Creates logger with custom directory

#### Methods
- `logQuery(QueryResult result)` - Log a GUI query result
- `logQueryRaw(...)` - Log an API query with raw parameters
- `setEnabled(boolean enabled)` - Enable/disable logging
- `isEnabled()` - Check if logging is enabled
- `getLogDirectory()` - Get absolute path to log directory
- `logMessage(String message)` - Log a custom message

## Future Enhancements

Potential improvements for the query logging system:

- [ ] JSON format option for easier parsing
- [ ] Configurable log level (ERROR, WARN, INFO, DEBUG)
- [ ] Built-in log rotation and archival
- [ ] Export to CSV for analysis
- [ ] Real-time log viewer in GUI
- [ ] Query statistics dashboard from logs
- [ ] Integration with external logging frameworks (Log4j, SLF4J)

## Related Files

- `src/managers/QueryLogger.java` - Core logging implementation
- `src/GuiLauncher.java` - GUI integration
- `src/ApiServer.java` - API integration
- `src/models/QueryResult.java` - Result model for GUI queries
- `src/Result.java` - Result class for API queries

## License

This feature is part of the Wide-Path project and follows the same license.
