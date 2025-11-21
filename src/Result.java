/**
 * 
 */

/**
 * 
 */
public class Result {
	private double departure_time;
	private double score;
	private int right_turns;
	private int sharp_turns;
	private double travel_time;
	private java.util.List<Integer> pathNodes;
	private java.util.List<Integer> wideEdgeIndices;
	
	public Result(double dep_time, double scr, int turns, int sharpTurns, double travelTime,
			java.util.List<Integer> pathNodes, java.util.List<Integer> wideEdgeIndices) {
		this.departure_time = dep_time;
		this.score = scr;
		this.right_turns = turns;
		this.sharp_turns = sharpTurns;
		this.travel_time = travelTime;
		this.pathNodes = pathNodes;
		this.wideEdgeIndices = wideEdgeIndices;
	}

	public double get_departureTime() {
		return this.departure_time;
	}

	public double get_score() {
		return this.score;
	}
	
	public int get_right_turns() {
		return this.right_turns;
	}

	public int get_sharp_turns() {
		return this.sharp_turns;
	}

	public double get_travel_time() {
		return this.travel_time;
	}

	public java.util.List<Integer> get_pathNodes() {
		return pathNodes;
	}

	public java.util.List<Integer> get_wideEdgeIndices() {
		return wideEdgeIndices;
	}

	public void updateScore(double final_score) {
		this.score = final_score;
		
	}
	
}
