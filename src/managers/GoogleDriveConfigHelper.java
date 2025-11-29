package managers;

import java.util.Scanner;

/**
 * Helper utility to configure Google Drive dataset file IDs.
 * 
 * This interactive tool helps you:
 * 1. Extract file IDs from Google Drive links
 * 2. Generate the configuration code for GoogleDriveDatasetLoader
 * 
 * Usage:
 *   java -cp target/wide-path-1.0-SNAPSHOT.jar managers.GoogleDriveConfigHelper
 * 
 * @author Wide-Path Team
 */
public class GoogleDriveConfigHelper {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║   Google Drive Dataset Configuration Helper                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("This tool helps you configure file IDs for Google Drive datasets.");
        System.out.println();
        System.out.println("Instructions:");
        System.out.println("1. Open your Google Drive folder:");
        System.out.println("   https://drive.google.com/drive/folders/1l3NG641rHeshkYW7aDxpb7RhUy0kRuiP");
        System.out.println();
        System.out.println("2. For each file:");
        System.out.println("   - Right-click the file");
        System.out.println("   - Select 'Get link' or 'Share'");
        System.out.println("   - Make sure it's set to 'Anyone with the link'");
        System.out.println("   - Copy the sharing link");
        System.out.println();
        System.out.println("3. The link will look like:");
        System.out.println("   https://drive.google.com/file/d/FILE_ID_HERE/view?usp=sharing");
        System.out.println();
        System.out.println("4. Paste the full link when prompted below.");
        System.out.println();
        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.println();
        
        try (Scanner scanner = new Scanner(System.in)) {
            StringBuilder config = new StringBuilder();
            config.append("// Add this code to GoogleDriveDatasetLoader.initializeFileIdMap():\n\n");
            
            String[] fileTypes = {
                "nodes_264346.txt",
                "edges_264346.txt", 
                "node_264346.txt",
                "edge_264346.txt"
            };
            
            for (String filename : fileTypes) {
                System.out.println("Enter Google Drive link for: " + filename);
                System.out.print("> ");
                String link = scanner.nextLine().trim();
                
                if (link.isEmpty()) {
                    System.out.println("Skipped (no input provided)\n");
                    continue;
                }
                
                String fileId = extractFileId(link);
                if (fileId != null) {
                    System.out.println("✓ Extracted file ID: " + fileId);
                    config.append(String.format("fileIdMap.put(\"%s\", \"%s\");\n", filename, fileId));
                } else {
                    System.out.println("✗ Could not extract file ID from link. Please check the format.");
                    System.out.println("  Expected format: https://drive.google.com/file/d/FILE_ID/...");
                }
                System.out.println();
            }
            
            System.out.println("════════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("Configuration Code:");
            System.out.println("════════════════════════════════════════════════════════════════");
            System.out.println(config.toString());
            System.out.println("════════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("Copy the code above and replace the placeholder entries in:");
            System.out.println("src/managers/GoogleDriveDatasetLoader.java");
            System.out.println("(in the initializeFileIdMap() method)");
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Extract file ID from a Google Drive sharing link
     * 
     * @param link The Google Drive sharing link
     * @return The file ID, or null if not found
     */
    private static String extractFileId(String link) {
        // Pattern 1: https://drive.google.com/file/d/FILE_ID/view
        if (link.contains("/file/d/")) {
            int start = link.indexOf("/file/d/") + 8;
            int end = link.indexOf("/", start);
            if (end == -1) {
                end = link.indexOf("?", start);
            }
            if (end == -1) {
                end = link.length();
            }
            if (start < end) {
                return link.substring(start, end);
            }
        }
        
        // Pattern 2: https://drive.google.com/open?id=FILE_ID
        if (link.contains("id=")) {
            int start = link.indexOf("id=") + 3;
            int end = link.indexOf("&", start);
            if (end == -1) {
                end = link.length();
            }
            if (start < end) {
                return link.substring(start, end);
            }
        }
        
        // Pattern 3: Just the file ID itself
        if (!link.contains("/") && !link.contains("?")) {
            return link;
        }
        
        return null;
    }
    
    /**
     * Quick test method
     */
    public static void testExtraction() {
        String[] testLinks = {
            "https://drive.google.com/file/d/1ABC123xyz/view?usp=sharing",
            "https://drive.google.com/open?id=1ABC123xyz",
            "1ABC123xyz"
        };
        
        System.out.println("Testing file ID extraction:");
        for (String link : testLinks) {
            String id = extractFileId(link);
            System.out.println("Link: " + link);
            System.out.println("ID:   " + id);
            System.out.println();
        }
    }
}
