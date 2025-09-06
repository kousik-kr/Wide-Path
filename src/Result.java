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
	
	public Result(double dep_time, double scr, int turns) {
		this.departure_time = dep_time;
		this.score = scr;
		this.right_turns = turns;
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

	public void updateScore(double final_score) {
		this.score = final_score;
		
	}
	
}
