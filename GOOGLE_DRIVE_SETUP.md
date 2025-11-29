# Google Drive Dataset Integration Guide

## Overview

The Wide-Path application now supports automatic dataset downloading from Google Drive. This feature eliminates the need to manually download and place dataset files in the correct directories.

## 🎯 Features

- ✅ **Automatic Download**: Downloads datasets from Google Drive on first run
- ✅ **Local Caching**: Caches downloaded files to avoid repeated downloads
- ✅ **Progress Tracking**: Shows download progress for large files
- ✅ **GUI Integration**: Prompts user before downloading in GUI mode
- ✅ **API Integration**: Auto-downloads in API mode
- ✅ **Retry Mechanism**: Automatically retries failed downloads
- ✅ **Multiple Datasets**: Supports different dataset sizes

## 📋 Prerequisites

### Step 1: Get Google Drive File IDs

Your Google Drive folder is shared at:
```
https://drive.google.com/drive/folders/1l3NG641rHeshkYW7aDxpb7RhUy0kRuiP
```

For **each file** in this folder, you need to extract the file ID:

1. **Open the Google Drive folder** in your browser
2. **Right-click on a file** (e.g., `nodes_264346.txt`)
3. **Select "Get link"** or "Share"
4. **Ensure sharing is set to**: "Anyone with the link can view"
5. **Copy the link** - it will look like:
   ```
   https://drive.google.com/file/d/FILE_ID_HERE/view?usp=sharing
   ```
6. **Extract the FILE_ID** - it's the part between `/file/d/` and `/view`

### Step 2: Configure File IDs

Run the interactive configuration helper:

```powershell
# Build the project first
mvn clean package -DskipTests

# Run the configuration helper
java -cp target/wide-path-1.0-SNAPSHOT.jar managers.GoogleDriveConfigHelper
```

The helper will:
- Prompt you to paste each Google Drive link
- Extract the file IDs automatically
- Generate the configuration code for you

### Step 3: Update the Code

1. Open `src/managers/GoogleDriveDatasetLoader.java`
2. Find the `initializeFileIdMap()` method (around line 65)
3. **Replace the PLACEHOLDER entries** with the actual file IDs from Step 2

**Example:**
```java
private void initializeFileIdMap() {
    // Replace these with your actual Google Drive file IDs
    fileIdMap.put("nodes_264346.txt", "1ABCxyz123_ACTUAL_FILE_ID");
    fileIdMap.put("edges_264346.txt", "1DEFabc456_ACTUAL_FILE_ID");
    fileIdMap.put("node_264346.txt", "1GHIdef789_ACTUAL_FILE_ID");
    fileIdMap.put("edge_264346.txt", "1JKLghi012_ACTUAL_FILE_ID");
    
    // Add more datasets if you have them:
    // fileIdMap.put("nodes_100000.txt", "ANOTHER_FILE_ID");
    // fileIdMap.put("edges_100000.txt", "ANOTHER_FILE_ID");
}
```

4. **Rebuild the project**:
   ```powershell
   mvn clean package -DskipTests
   ```

## 🚀 Usage

### GUI Mode

1. **Launch the GUI**:
   ```powershell
   java -cp target/wide-path-1.0-SNAPSHOT.jar GuiLauncher
   ```

2. **First-time experience**:
   - If dataset files are not found locally, you'll see a dialog:
     ```
     Dataset not found locally.
     Would you like to download it from Google Drive?
     
     Note: This may take several minutes depending on dataset size.
     ```
   - Click **"Yes"** to download
   - A progress dialog will appear showing download status
   - Once complete, the application will load normally

3. **Subsequent runs**:
   - Datasets are cached in `datasets/cache/`
   - No download needed - loads instantly

### API Mode

1. **Launch the API server**:
   ```powershell
   java -cp target/wide-path-1.0-SNAPSHOT.jar ApiServer
   ```

2. **Auto-download**:
   - Server checks for cached datasets
   - If not found, automatically downloads from Google Drive
   - Shows progress in console output
   - Server starts once download completes

3. **Console output**:
   ```
   [GoogleDriveLoader] Checking dataset availability...
   [GoogleDriveLoader] Downloading 4 missing files...
   [GoogleDriveLoader] Downloading nodes_264346.txt (attempt 1/3)...
   Progress: 25% (5 MB / 20 MB)
   [GoogleDriveLoader] Successfully downloaded: nodes_264346.txt
   ...
   [ApiServer] Dataset downloaded to: datasets/cache
   API server started on port 8080
   ```

## 📁 File Locations

### Cache Directory
```
Wide-Path/
  └── datasets/
      └── cache/
          ├── nodes_264346.txt
          ├── edges_264346.txt
          ├── node_264346.txt
          └── edge_264346.txt
```

### Logs
Query logs remain in:
```
Wide-Path/
  └── logs/
      └── queries/
          └── query_log_YYYY-MM-DD.log
```

## 🔧 Advanced Configuration

### Custom Cache Directory

**GUI (GuiLauncher.java)**:
```java
GoogleDriveDatasetLoader driveLoader = new GoogleDriveDatasetLoader("custom/path");
```

**API (ApiServer.java)**:
```java
GoogleDriveDatasetLoader driveLoader = new GoogleDriveDatasetLoader("custom/path");
```

### Disable Verbose Logging

```java
driveLoader.setVerbose(false);
```

### Add More Datasets

If you have multiple dataset sizes:

```java
// In GoogleDriveDatasetLoader.initializeFileIdMap()
fileIdMap.put("nodes_100000.txt", "FILE_ID");
fileIdMap.put("edges_100000.txt", "FILE_ID");
fileIdMap.put("node_100000.txt", "FILE_ID");
fileIdMap.put("edge_100000.txt", "FILE_ID");

fileIdMap.put("nodes_500000.txt", "FILE_ID");
fileIdMap.put("edges_500000.txt", "FILE_ID");
// etc.
```

### Manual Download (Fallback)

If automatic download fails, you can manually download:

1. Visit: `https://drive.google.com/drive/folders/1l3NG641rHeshkYW7aDxpb7RhUy0kRuiP`
2. Download all files
3. Place them in `datasets/cache/` directory
4. The application will detect and use them

## 🛠️ Troubleshooting

### Problem: "File ID not configured"

**Error message:**
```
Failed to download dataset: File ID not configured for: nodes_264346.txt
Please update GoogleDriveDatasetLoader.initializeFileIdMap() with actual Google Drive file IDs.
```

**Solution:**
- You haven't replaced the PLACEHOLDER file IDs
- Follow Step 2 and Step 3 in Prerequisites section above

### Problem: "HTTP error code: 403" or "404"

**Possible causes:**
1. File is not shared publicly
2. Wrong file ID
3. File was deleted from Google Drive

**Solution:**
- Verify sharing settings: "Anyone with the link can view"
- Double-check file IDs
- Ensure files exist in the Google Drive folder

### Problem: Download is very slow

**Tips:**
- Large datasets may take 5-15 minutes depending on connection
- Check your internet speed
- Consider downloading manually and placing in cache directory

### Problem: "Connection timeout"

**Solution:**
- Check internet connection
- Try again - the retry mechanism will kick in
- If persistent, download manually

### Problem: Downloaded files are corrupt

**Solution:**
- Clear the cache: Delete `datasets/cache/` directory
- Run the application again to re-download

## 📊 Dataset Information

### Standard Dataset (264,346 nodes)

Expected files:
- `nodes_264346.txt` - Node coordinates (~5-10 MB)
- `edges_264346.txt` - Edge information (~50-100 MB)
- `node_264346.txt` - Cluster information (~5-10 MB)
- `edge_264346.txt` - Edge width information (~20-40 MB)

### Estimated Download Time

| Connection Speed | Approx. Time |
|-----------------|--------------|
| 1 Mbps          | 20-30 min    |
| 5 Mbps          | 5-10 min     |
| 10 Mbps         | 2-5 min      |
| 50 Mbps         | 30-60 sec    |
| 100 Mbps        | 15-30 sec    |

## 🔍 Verification

### Check if dataset is cached:

```java
GoogleDriveDatasetLoader loader = new GoogleDriveDatasetLoader();
boolean cached = loader.isDatasetCached(264346);
System.out.println("Dataset cached: " + cached);
```

### List available datasets:

```java
List<Integer> datasets = loader.getAvailableDatasets();
System.out.println("Available datasets: " + datasets);
// Output: [264346] (or more if you have multiple)
```

### Clear cache programmatically:

```java
loader.clearCache();
```

## 💡 Best Practices

1. **Pre-download datasets** before demos/presentations
2. **Backup cached datasets** to avoid re-downloading
3. **Use manual download** for slow connections
4. **Configure all file IDs** upfront to avoid runtime errors
5. **Test download** once after configuration

## 🆘 Support

### Quick Checks

```powershell
# Verify cache directory exists
Test-Path datasets/cache

# List cached files
Get-ChildItem datasets/cache

# Check cache size
Get-ChildItem datasets/cache -Recurse | 
  Measure-Object -Property Length -Sum | 
  Select-Object @{Name="Size(MB)";Expression={$_.Sum / 1MB}}

# View last 20 lines of console output
# (if you redirected output to a file)
Get-Content output.log -Tail 20
```

### Still Having Issues?

1. Check the console output for error messages
2. Verify file IDs are correct (no PLACEHOLDER text)
3. Test a single file download manually from Google Drive
4. Ensure Google Drive folder is publicly accessible
5. Try the manual download fallback method

## 📝 Configuration Checklist

- [ ] Obtained all Google Drive file IDs
- [ ] Ran `GoogleDriveConfigHelper` to generate configuration
- [ ] Updated `initializeFileIdMap()` in `GoogleDriveDatasetLoader.java`
- [ ] Removed all PLACEHOLDER entries
- [ ] Rebuilt project with `mvn clean package`
- [ ] Tested GUI launch
- [ ] Tested API launch
- [ ] Verified downloaded files in `datasets/cache/`
- [ ] Tested query execution with downloaded dataset

---

**Congratulations!** Your Wide-Path application is now configured to automatically download datasets from Google Drive! 🎉
