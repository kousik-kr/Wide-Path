# Google Drive Integration - Quick Start Guide

## ⚡ 5-Minute Setup

Get your Wide-Path application downloading datasets from Google Drive in 5 minutes!

---

## Step 1: Get Your Google Drive File IDs (2 minutes)

### Method A: Using the Helper Tool (Recommended)

```powershell
# Build the project
mvn clean package -DskipTests

# Run the configuration helper
java -cp target/wide-path-1.0-SNAPSHOT.jar managers.GoogleDriveConfigHelper
```

The helper will:
1. Ask you to paste each Google Drive link
2. Extract the file IDs automatically
3. Generate the configuration code for you

**Just copy the generated code!**

### Method B: Manual Extraction

For each file in your Google Drive folder:

1. Visit: https://drive.google.com/drive/folders/1l3NG641rHeshkYW7aDxpb7RhUy0kRuiP
2. Right-click on a file (e.g., `nodes_264346.txt`)
3. Select **"Get link"** or **"Share"**
4. Copy the link - it looks like:
   ```
   https://drive.google.com/file/d/1ABCxyz123EXAMPLE_FILE_ID/view?usp=sharing
   ```
5. Extract the FILE_ID (the part between `/file/d/` and `/view`):
   ```
   1ABCxyz123EXAMPLE_FILE_ID
   ```

**Repeat for all 4 files:**
- `nodes_264346.txt`
- `edges_264346.txt`
- `node_264346.txt`
- `edge_264346.txt`

---

## Step 2: Update the Code (1 minute)

1. Open `src/managers/GoogleDriveDatasetLoader.java`

2. Find the `initializeFileIdMap()` method (around line 65)

3. **Replace the PLACEHOLDER values** with your actual file IDs:

```java
private void initializeFileIdMap() {
    // Replace these PLACEHOLDER values with your actual Google Drive file IDs
    fileIdMap.put("nodes_264346.txt", "YOUR_ACTUAL_FILE_ID_1");
    fileIdMap.put("edges_264346.txt", "YOUR_ACTUAL_FILE_ID_2");
    fileIdMap.put("node_264346.txt", "YOUR_ACTUAL_FILE_ID_3");
    fileIdMap.put("edge_264346.txt", "YOUR_ACTUAL_FILE_ID_4");
}
```

4. **Save the file**

---

## Step 3: Rebuild (1 minute)

```powershell
mvn clean package -DskipTests
```

Wait for the build to complete. You should see:
```
BUILD SUCCESS
```

---

## Step 4: Test It! (1 minute)

### Test GUI:

```powershell
java -cp target/wide-path-1.0-SNAPSHOT.jar GuiLauncher
```

**What happens:**
1. A dialog appears: "Dataset not found locally. Would you like to download it from Google Drive?"
2. Click **"Yes"**
3. Progress dialog shows: "Downloading dataset... Please wait."
4. Files download to `datasets/cache/`
5. Application loads automatically

### Test API:

```powershell
java -cp target/wide-path-1.0-SNAPSHOT.jar ApiServer
```

**What happens:**
1. Console shows: "[GoogleDriveLoader] Checking dataset availability..."
2. Console shows: "[GoogleDriveLoader] Downloading 4 missing files..."
3. Progress messages for each file
4. Server starts: "API server started on port 8080"

---

## ✅ Verification

Check that files were downloaded:

```powershell
# List downloaded files
Get-ChildItem datasets/cache

# Check total size
Get-ChildItem datasets/cache -Recurse | 
  Measure-Object -Property Length -Sum | 
  Select-Object @{Name="Size(MB)";Expression={$_.Sum / 1MB}}
```

Expected output:
```
Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
-a---          11/29/2024  1:23 PM       12345678 nodes_264346.txt
-a---          11/29/2024  1:24 PM       98765432 edges_264346.txt
-a---          11/29/2024  1:25 PM       11223344 node_264346.txt
-a---          11/29/2024  1:26 PM       44332211 edge_264346.txt
```

---

## 🎉 You're Done!

**Next runs will be instant** - the application uses cached files!

---

## 🆘 Troubleshooting

### Problem: "File ID not configured for: nodes_264346.txt"

**Cause**: You didn't replace the PLACEHOLDER values

**Solution**: 
1. Go back to Step 2
2. Make sure you replaced ALL four PLACEHOLDER entries
3. Rebuild with `mvn clean package`

---

### Problem: "HTTP error code: 403" or "404"

**Possible causes:**
- File is not shared publicly
- Wrong file ID
- File was deleted

**Solution**:
1. Open the Google Drive folder in your browser
2. For each file, right-click → "Share"
3. Set to **"Anyone with the link can view"**
4. Re-copy the file IDs
5. Update the code again

---

### Problem: Download is stuck or very slow

**Tips:**
- Large datasets can take 5-15 minutes on slow connections
- Check your internet speed
- Try downloading one file manually to test connection
- If stuck, press Ctrl+C and try again (retry logic will help)

**Manual fallback:**
1. Download files manually from Google Drive
2. Place them in `datasets/cache/` directory
3. Run the application - it will detect and use them

---

## 📋 Quick Commands Cheat Sheet

```powershell
# Build project
mvn clean package -DskipTests

# Run config helper
java -cp target/wide-path-1.0-SNAPSHOT.jar managers.GoogleDriveConfigHelper

# Run GUI
java -cp target/wide-path-1.0-SNAPSHOT.jar GuiLauncher

# Run API
java -cp target/wide-path-1.0-SNAPSHOT.jar ApiServer

# Check cache
Get-ChildItem datasets/cache

# Clear cache (force re-download)
Remove-Item datasets/cache/* -Recurse -Force
```

---

## 🔗 Need More Help?

- **Full setup guide**: See `GOOGLE_DRIVE_SETUP.md`
- **Implementation details**: See `GOOGLE_DRIVE_IMPLEMENTATION.md`
- **General help**: See `README.md`

---

## 📊 What's Happening Behind the Scenes?

```
Application Start
    ↓
Check if dataset cached in datasets/cache/
    ↓
    ├─→ [Found] → Load immediately
    │
    └─→ [Not Found]
         ↓
         Show dialog (GUI) or log (API)
         ↓
         Download from Google Drive
         ↓
         Cache files in datasets/cache/
         ↓
         Load application
```

---

**Time to complete**: 5 minutes  
**Network required**: Only for first download  
**Subsequent runs**: Instant (uses cache)  

**Happy pathfinding! 🎯**
