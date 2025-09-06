import java.util.HashMap;

public class Label{
	private int node;
	private Function arrival_time;
	private Function wide_distance;
	private HashMap<Integer, Integer> visitedList;
	private double max_distance_wide_road;
	private int right_turn;
	private double total_distance;
	//private List<Integer> trace;
	//private int [] predecessorList;
	
	public Label(int n, Function arrival, Function w_dist, int r_turn, double dist){
		this.node = n;
		this.arrival_time = arrival;
		this.wide_distance = w_dist;
		this.visitedList = new HashMap<Integer, Integer>();
		this.max_distance_wide_road = w_dist.getMaxValue();
		this.right_turn=r_turn;
		this.total_distance = dist;
		//this.trace = new ArrayList<Integer>();
		//this.predecessorList = new int[Main.subgraphSize];
	}
	
	public double getMaxPercentageWideRoad() {
		return this.max_distance_wide_road*100/this.total_distance;
	}
	
	public double getDistance() {
		return this.total_distance;
	}
	
	public int getRightTurns() {
		return this.right_turn;
	}
	
	public double get_arrivalTime(int departure_time) {
		Function required_function = null;
		Function current_function = this.arrival_time;
		while(current_function!=null) {
			if(current_function.inInterval(departure_time)) {
				required_function = current_function;
				break;
			}
			else
				current_function = current_function.getNextFunction();
		}

		if(required_function==null)
			return -1;
		
		double x1, x2, y1, y2;
		BreakPoint element = null, next_element = null;
		int i=0;
		for(; i<required_function.getBreakpoints().size();i++) {
			BreakPoint breakpoint = required_function.getBreakpoints().get(i);
			
			if(departure_time==breakpoint.getX())
				return breakpoint.getY();
			if(departure_time>breakpoint.getX()) {
				element = breakpoint;
			}
			else
				break;
		}
		next_element = required_function.getBreakpoints().get(i);
		
		x1 = element.getX();
		y1 = element.getY();
		x2 = next_element.getX();
		y2 = next_element.getY();
		
		return linear_function(x1, x2, y1, y2, departure_time);
	}

	public int get_wide_distance(double departure_time) {
		Function required_function = null;
		Function current_function = this.wide_distance;
		while(current_function!=null) {
			if(current_function.inInterval(departure_time)) {
				required_function = current_function;
				break;
			}
			else
				current_function = current_function.getNextFunction();
		}
		if(required_function==null)
			return -1;
		
		
		BreakPoint element = null;

		for(BreakPoint breakpoint : required_function.getBreakpoints()) {
			if(departure_time>=breakpoint.getX())
				element = breakpoint;
			else
				break;
		}
		
		return (int) element.getY();
	}

	private	double linear_function(double x1, double x2, double y1, double y2, double x){
		return (y2-y1)*(x-x1)/(x2-x1) + y1;
	}

	public int get_nodeID() {
		return this.node;
	}
	
	public Function get_arrivalTime() {
		return this.arrival_time;
	}
	
	public Function get_wide_distance() {
		return this.wide_distance;
	}
	
//	public void initializeLists() {
//		for(int i=0;i<Main.subgraphSize;i++) {
//			visitedList[i] = false;
//			predecessorList[i] = -1;
//		}
//	}
	
	public void copyLists(HashMap<Integer, Integer> visit) {
//		for(int i=0;i<Main.subgraphSize;i++) {
//			visitedList[i] = visit[i];
//			//predecessorList[i] = predecessor[i];
//		}
		visitedList.putAll(visit);
		//trace.addAll(trc);
	}
	
	public void setVisited(int n, int pred) {
		//int index = Main.getIndex(n);
		visitedList.put(n, pred);
		//trace.add(n);
	}
	
//	public void setPredecessor(int current_node, int predecessor_node) {
//		int index = Main.getIndex(current_node);
//		predecessorList[index] = predecessor_node;
//	}
	
	public boolean getVisited(int n) {
		//int index = Main.getIndex(n);
		if(visitedList.containsKey(n))
			return true;
		return false;
	}
	
	public HashMap<Integer, Integer> getVisitedList() {
		return visitedList;
	}

//	public List<Integer> getTrace() {
//		return trace;
//	}
//	public int[] getPredecessorList() {
//		return predecessorList;
//	}
}