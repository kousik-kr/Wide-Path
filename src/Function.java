/**
 * 
 */

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class Function {
	private List<BreakPoint> break_points;
	private Function next_function = null;
	private double max_value;
	
	public Function(List<BreakPoint> breakpoints, double score) {
		break_points = new ArrayList<BreakPoint>();
		break_points.addAll(breakpoints);
		this.max_value = score;
			
	}
	
	public void updateValue(double val) {
		if(val>this.max_value)
			this.max_value = val;
	}

	public List<BreakPoint> getBreakpoints(){
		return this.break_points;
	}
	
	public Function getNextFunction() {
		return this.next_function;
	}
	
	public void setNextFunction(Function function) {
		this.next_function = function;
	}

	public boolean inInterval(double departure_time) {
		if(departure_time>=this.break_points.get(0).getX() && departure_time<=this.break_points.get(this.break_points.size()-1).getX())
			return true;
		return false;
	}

	public double getMaxValue() {
		return this.max_value;
	}
}
