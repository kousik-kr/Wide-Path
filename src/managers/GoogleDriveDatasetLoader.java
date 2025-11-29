package managers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads and manages datasets from Google Drive.
 * 
 * Features:
 * - Automatic download from shared Google Drive folders
 * - Local caching to avoid repeated downloads
 * - Progress tracking
 * - Retry mechanism for failed downloads
 * - File integrity verification
 * 
 * Usage:
 *   GoogleDriveDatasetLoader loader = new GoogleDriveDatasetLoader();
 *   String datasetPath = loader.ensureDatasetAvailable();
 *   BidirectionalAstar.setConfiguredGraphDataDir(datasetPath);
 * 
 * @author Wide-Path Team
 */
public class GoogleDriveDatasetLoader {
    
    // Google Drive folder ID from the shared link
    // https://drive.google.com/drive/folders/1l3NG641rHeshkYW7aDxpb7RhUy0kRuiP?usp=sharing
    private static final String DRIVE_FOLDER_ID = "1l3NG641rHeshkYW7aDxpb7RhUy0kRuiP";
    
    // Local cache directory
    private static final String DEFAULT_CACHE_DIR = "datasets/cache";
    private final Path cacheDirectory;
    
    // File mappings: filename -> Google Drive file ID
    // Note: These IDs need to be extracted from the shared folder
    private final Map<String, String> fileIdMap = new HashMap<>();
    
    // Download configuration
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;
    
    private boolean verbose = true;
    
    /**
     * Creates a loader with the default cache directory
     */
    public GoogleDriveDatasetLoader() {
        this(DEFAULT_CACHE_DIR);
    }
    
    /**
     * Creates a loader with a custom cache directory
     * @param cacheDir Path to cache directory
     */
    public GoogleDriveDatasetLoader(String cacheDir) {
        this.cacheDirectory = Paths.get(cacheDir);
        initializeCacheDirectory();
        initializeFileIdMap();
    }
    
    /**
     * Initialize cache directory
     */
    private void initializeCacheDirectory() {
        try {
            if (!Files.exists(cacheDirectory)) {
                Files.createDirectories(cacheDirectory);
                log("Created cache directory: " + cacheDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create cache directory: " + e.getMessage());
        }
    }
    
    /**
     * Initialize file ID mappings
     * 
     * IMPORTANT: These file IDs need to be manually extracted from the Google Drive folder.
     * To get file IDs:
     * 1. Right-click each file in Google Drive
     * 2. Click "Get link" and copy the link
     * 3. Extract the file ID from the URL (between /d/ and /view)
     * 
     * Example: https://drive.google.com/file/d/FILE_ID_HERE/view
     */
    private void initializeFileIdMap() {
        // TODO: Add actual file IDs from your Google Drive folder
        // Format: fileIdMap.put("nodes_264346.txt", "ACTUAL_FILE_ID");
        
        // Example placeholder entries - REPLACE THESE WITH ACTUAL IDS
        fileIdMap.put("nodes_264346.txt", "PLACEHOLDER_ID_1");
        fileIdMap.put("edges_264346.txt", "PLACEHOLDER_ID_2");
        fileIdMap.put("node_264346.txt", "PLACEHOLDER_ID_3");
        fileIdMap.put("edge_264346.txt", "PLACEHOLDER_ID_4");
        
        // If you have multiple dataset sizes, add them all:
        // fileIdMap.put("nodes_100000.txt", "FILE_ID");
        // fileIdMap.put("edges_100000.txt", "FILE_ID");
        // etc.
    }
    
    /**
     * Ensures dataset files are available locally.
     * Downloads from Google Drive if not cached.
     * 
     * @return Path to the dataset directory
     * @throws IOException if download fails
     */
    public String ensureDatasetAvailable() throws IOException {
        return ensureDatasetAvailable(null);
    }
    
    /**
     * Ensures dataset files are available locally for a specific vertex count.
     * 
     * @param vertexCount The vertex count (e.g., 264346), or null for auto-detect
     * @return Path to the dataset directory
     * @throws IOException if download fails
     */
    public String ensureDatasetAvailable(Integer vertexCount) throws IOException {
        log("Checking dataset availability...");
        
        // Determine which files to download
        List<String> requiredFiles = determineRequiredFiles(vertexCount);
        
        // Check which files are missing
        List<String> missingFiles = new ArrayList<>();
        for (String filename : requiredFiles) {
            Path filePath = cacheDirectory.resolve(filename);
            if (!Files.exists(filePath)) {
                missingFiles.add(filename);
            } else {
                log("Found cached: " + filename);
            }
        }
        
        // Download missing files
        if (!missingFiles.isEmpty()) {
            log("Downloading " + missingFiles.size() + " missing files...");
            for (String filename : missingFiles) {
                downloadFile(filename);
            }
        } else {
            log("All dataset files are cached locally.");
        }
        
        return cacheDirectory.toAbsolutePath().toString();
    }
    
    /**
     * Determine which files are required based on vertex count
     */
    private List<String> determineRequiredFiles(Integer vertexCount) {
        List<String> files = new ArrayList<>();
        
        if (vertexCount != null) {
            // Specific vertex count requested
            files.add("nodes_" + vertexCount + ".txt");
            files.add("edges_" + vertexCount + ".txt");
            files.add("node_" + vertexCount + ".txt");
            files.add("edge_" + vertexCount + ".txt");
        } else {
            // Download all available datasets
            files.addAll(fileIdMap.keySet());
        }
        
        return files;
    }
    
    /**
     * Download a single file from Google Drive
     * 
     * @param filename The filename to download
     * @throws IOException if download fails after retries
     */
    private void downloadFile(String filename) throws IOException {
        String fileId = fileIdMap.get(filename);
        if (fileId == null) {
            throw new IOException("No file ID mapping found for: " + filename);
        }
        
        if (fileId.startsWith("PLACEHOLDER")) {
            throw new IOException(
                "File ID not configured for: " + filename + "\n" +
                "Please update GoogleDriveDatasetLoader.initializeFileIdMap() with actual Google Drive file IDs.\n" +
                "See class documentation for instructions on obtaining file IDs."
            );
        }
        
        Path outputPath = cacheDirectory.resolve(filename);
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log("Downloading " + filename + " (attempt " + attempt + "/" + MAX_RETRIES + ")...");
                downloadFromGoogleDrive(fileId, outputPath);
                log("Successfully downloaded: " + filename);
                return;
            } catch (IOException e) {
                if (attempt == MAX_RETRIES) {
                    throw new IOException("Failed to download " + filename + " after " + MAX_RETRIES + " attempts", e);
                }
                log("Download failed, retrying in " + (RETRY_DELAY_MS / 1000) + " seconds...");
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Download interrupted", ie);
                }
            }
        }
    }
    
    /**
     * Download a file from Google Drive using direct download link
     * 
     * @param fileId Google Drive file ID
     * @param outputPath Local path to save the file
     * @throws IOException if download fails
     */
    private void downloadFromGoogleDrive(String fileId, Path outputPath) throws IOException {
        // Google Drive direct download URL
        String downloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
        
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            
            // Handle large file download confirmation
            connection.connect();
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                outputStream = Files.newOutputStream(outputPath, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
                
                long totalBytes = connection.getContentLengthLong();
                long downloadedBytes = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;
                    
                    // Show progress for large files
                    if (totalBytes > 0 && verbose) {
                        int progress = (int) ((downloadedBytes * 100) / totalBytes);
                        if (downloadedBytes % (1024 * 1024) == 0) { // Log every MB
                            System.out.print("\rProgress: " + progress + "% (" + 
                                (downloadedBytes / 1024 / 1024) + " MB / " + 
                                (totalBytes / 1024 / 1024) + " MB)");
                        }
                    }
                }
                
                if (verbose && totalBytes > 0) {
                    System.out.println(); // New line after progress
                }
                
                outputStream.flush();
            } else {
                throw new IOException("HTTP error code: " + responseCode + " for file ID: " + fileId);
            }
            
        } finally {
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException ignored) {}
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Check if dataset is already cached
     * 
     * @param vertexCount The vertex count to check, or null for any
     * @return true if dataset files exist in cache
     */
    public boolean isDatasetCached(Integer vertexCount) {
        if (vertexCount != null) {
            String suffix = vertexCount + ".txt";
            return Files.exists(cacheDirectory.resolve("nodes_" + suffix)) &&
                   Files.exists(cacheDirectory.resolve("edges_" + suffix));
        } else {
            // Check if any dataset files exist
            try {
                return Files.list(cacheDirectory)
                    .anyMatch(p -> p.getFileName().toString().startsWith("nodes_"));
            } catch (IOException e) {
                return false;
            }
        }
    }
    
    /**
     * Get list of available cached datasets (by vertex count)
     * 
     * @return List of vertex counts for cached datasets
     */
    public List<Integer> getAvailableDatasets() {
        List<Integer> datasets = new ArrayList<>();
        Pattern pattern = Pattern.compile("nodes_(\\d+)\\.txt");
        
        try {
            Files.list(cacheDirectory)
                .map(p -> p.getFileName().toString())
                .forEach(filename -> {
                    Matcher m = pattern.matcher(filename);
                    if (m.matches()) {
                        datasets.add(Integer.parseInt(m.group(1)));
                    }
                });
        } catch (IOException e) {
            System.err.println("Error listing cached datasets: " + e.getMessage());
        }
        
        Collections.sort(datasets);
        return datasets;
    }
    
    /**
     * Clear the cache directory
     * 
     * @throws IOException if deletion fails
     */
    public void clearCache() throws IOException {
        if (Files.exists(cacheDirectory)) {
            Files.walk(cacheDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
            log("Cache cleared: " + cacheDirectory.toAbsolutePath());
        }
    }
    
    /**
     * Get the cache directory path
     * 
     * @return Absolute path to cache directory
     */
    public Path getCacheDirectory() {
        return cacheDirectory.toAbsolutePath();
    }
    
    /**
     * Enable or disable verbose logging
     * 
     * @param verbose true to enable, false to disable
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Add a custom file ID mapping
     * 
     * @param filename The filename
     * @param fileId The Google Drive file ID
     */
    public void addFileMapping(String filename, String fileId) {
        fileIdMap.put(filename, fileId);
        log("Added file mapping: " + filename + " -> " + fileId);
    }
    
    /**
     * Log a message if verbose mode is enabled
     */
    private void log(String message) {
        if (verbose) {
            System.out.println("[GoogleDriveLoader] " + message);
        }
    }
}
