# Query Logging - Quick Start Guide

## 🚀 Quick Start

### For GUI Users
1. Launch the GUI application
2. Run any query
3. Check `logs/queries/query_log_YYYY-MM-DD.log`

### For API Users
1. Start the API server
2. Send a POST request to `/api/queries/run`
3. Check `logs/queries/query_log_YYYY-MM-DD.log`

## 📂 Log Location

```
Wide-Path/
  └── logs/
      └── queries/
          ├── query_log_2025-11-29.log
          ├── query_log_2025-11-30.log
          └── ...
```

## 📝 Example Commands

### View Today's Logs
```powershell
Get-Content logs/queries/query_log_$(Get-Date -Format yyyy-MM-dd).log
```

### Search Logs
```powershell
# Find queries from node 100
Select-String -Path "logs/queries/*.log" -Pattern "Source Node:           100"

# Find failed queries
Select-String -Path "logs/queries/*.log" -Pattern "Success:               NO"
```

### Clean Old Logs
```powershell
# Delete logs older than 30 days
Get-ChildItem logs/queries -Filter *.log | 
  Where-Object {$_.LastWriteTime -lt (Get-Date).AddDays(-30)} | 
  Remove-Item
```

## 🔧 Configuration

### Change Log Directory

**GuiLauncher.java:**
```java
this.queryLogger = new QueryLogger("path/to/logs");
```

**ApiServer.java:**
```java
private static final QueryLogger queryLogger = new QueryLogger("path/to/logs");
```

### Disable Logging
```java
queryLogger.setEnabled(false);
```

## 📊 Log Format

```
================================================================================
QUERY LOG ENTRY - 2025-11-29 11:52:35.123
================================================================================

[INPUT PARAMETERS]
  Source Node:           100
  Destination Node:      500
  Departure Time:        480.00 min (8.00 hrs)
  ...

[QUERY STATUS]
  Success:               YES
  Execution Time:        1234 ms

[RESULTS]
  Score:                 0.8765
  Travel Time:           45.50 min
  ...

[PATH DETAILS]
  Path Length:           25 nodes
  Path Nodes:            [100, 105, ..., 500]
  ...
```

## 🛠️ Build & Run

### Compile
```powershell
mvn clean compile
```

### Package
```powershell
mvn package -DskipTests
```

### Run GUI
```powershell
java -cp target/wide-path-1.0-SNAPSHOT.jar GuiLauncher
```

### Run API
```powershell
java -cp target/wide-path-1.0-SNAPSHOT.jar ApiServer
```

## 📖 More Information

- **Full Documentation:** See `QUERY_LOGGING.md`
- **Implementation Details:** See `IMPLEMENTATION_SUMMARY.md`
- **Source Code:** `src/managers/QueryLogger.java`

## 💡 Tips

1. **Daily Rotation:** New log file created automatically each day
2. **Thread-Safe:** Safe for concurrent queries
3. **Auto-Create:** Log directory created automatically
4. **No Config:** Works out of the box
5. **Graceful:** Errors don't crash the app

## 🐛 Troubleshooting

### No Logs Created?
- Check console for error messages
- Verify write permissions
- Ensure queries are actually running

### Logs are Empty?
- Check if logging is enabled: `queryLogger.isEnabled()`
- Verify queries complete successfully

### Can't Find Logs?
```powershell
# Get absolute path
java -cp target/wide-path-1.0-SNAPSHOT.jar -version
# Check working directory
pwd
# Full path to logs
ls logs/queries
```

## ✅ Verification

After running a query, verify logging:

```powershell
# Check if log file exists
Test-Path "logs/queries/query_log_$(Get-Date -Format yyyy-MM-dd).log"

# Count log entries (each query creates one entry with separator lines)
(Get-Content "logs/queries/query_log_$(Get-Date -Format yyyy-MM-dd).log" | 
  Select-String "QUERY LOG ENTRY").Count

# View last query
Get-Content "logs/queries/query_log_$(Get-Date -Format yyyy-MM-dd).log" -Tail 30
```

---

**Need more help?** Check the full documentation in `QUERY_LOGGING.md`
