import java.util.HashSet;
import java.util.Set;

public class Cluster {

    private int clusterId; // Unique identifier for the cluster
    private Set<Node> nodes; // Nodes belonging to this cluster

    // Constructor
    public Cluster(int clusterId) {
        this.clusterId = clusterId;
        this.nodes = new HashSet<>();
    }

    // Getter for cluster ID
    public int getClusterId() {
        return clusterId;
    }

    // Add a node to the cluster
    public void addNode(Node node) {
        nodes.add(node);
        node.setClusterId(clusterId); // Update the node's cluster ID
    }

    // Remove a node from the cluster
    public void removeNode(Node node) {
        nodes.remove(node);
        node.setClusterId(-1); // Reset the node's cluster ID
    }

    // Get all nodes in the cluster
    public Set<Node> getNodes() {
        return nodes;
    }

    // Check if the cluster contains a specific node
    public boolean containsNode(Node node) {
        return nodes.contains(node);
    }

    // Get the size of the cluster
    public int size() {
        return nodes.size();
    }
}