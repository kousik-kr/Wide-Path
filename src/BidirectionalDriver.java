//import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.PriorityBlockingQueue;

public class BidirectionalDriver {
	private int source;
	private	int destination;
	private	double start_departure_time;
	private	double end_departure_time;
	private	double budget;
	
	public BidirectionalDriver(Query query, double budget) {
		this.source = query.get_source();
		this.destination = query.get_destination();
		this.start_departure_time = query.get_start_departure_time();
		this.end_departure_time = query.get_end_departure_time();
		this.budget = budget;
	}

	static class SharedState {
	    private static final int MAX_LABELS_PER_NODE = 1; // keep top 10 labels per node

	    ConcurrentHashMap<Integer, PriorityBlockingQueue<Label>> forwardVisited = new ConcurrentHashMap<>();
	    ConcurrentHashMap<Integer, PriorityBlockingQueue<Label>> backwardVisited = new ConcurrentHashMap<>();
	    Set<Integer> intersectionNodes = ConcurrentHashMap.newKeySet();

	    // Min-heap comparator based on right turn
	    Comparator<Label> worstFirstComparator = (a, b) -> {
	        // compare by RightTurns (more turns is worse)
	        int cmp = Integer.compare(b.getRightTurns(), a.getRightTurns());
	        if (cmp != 0) return cmp;
	        // if same turns, compare by MaxPercentageWidth (lower is worse)
	        return Double.compare(a.getMaxPercentageWideRoad(), b.getMaxPercentageWideRoad());
	    };


	    public void addForwardLabel(int nodeId, Label label) {
	        forwardVisited.computeIfAbsent(
	            nodeId,
	            k -> new PriorityBlockingQueue<>(MAX_LABELS_PER_NODE, worstFirstComparator)
	        );
	        boundedAdd(forwardVisited.get(nodeId), label);
	    }

	    public void addBackwardLabel(int nodeId, Label label) {
	        backwardVisited.computeIfAbsent(
	            nodeId,
	            k -> new PriorityBlockingQueue<>(MAX_LABELS_PER_NODE, worstFirstComparator)
	        );
	        boundedAdd(backwardVisited.get(nodeId), label);
	    }

	    public void addIntersectionNode(int nodeId) {
	        intersectionNodes.add(nodeId);
	    }

	    public boolean isIntersection(int nodeId) {
	        return forwardVisited.containsKey(nodeId) && backwardVisited.containsKey(nodeId);
	    }

	    /**
	     * Efficient bounded insert:
	     * - If heap not full → add directly
	     * - If full → only replace if new label has higher score than min in heap
	     */
	    private void boundedAdd(PriorityBlockingQueue<Label> heap, Label label) {
	        if (heap.size() < MAX_LABELS_PER_NODE) {
	            heap.offer(label);
	        } else {
	            Label worst = heap.peek(); // worst element
	            if (worst != null && betterThan(label, worst)) {
	                heap.poll();
	                heap.offer(label);
	            }
	        }
	    }

	    private boolean betterThan(Label newLabel, Label oldLabel) {
	        // better = fewer turns, or equal turns but higher width %
	        if (newLabel.getRightTurns() != oldLabel.getRightTurns()) {
	            return newLabel.getRightTurns() < oldLabel.getRightTurns();
	        }
	        return newLabel.getMaxPercentageWideRoad() > oldLabel.getMaxPercentageWideRoad();
	    }

	}


	public Result driver() throws InterruptedException, ExecutionException {
		Graph.forwardAstar(source, destination, budget);
		
		Graph.backwardAstar(source, destination, budget);

		if(Graph.get_node(source).isFeasible()) {
			SharedState shared = new SharedState();

			shared.backwardVisited.clear();
			shared.forwardVisited.clear();
			shared.intersectionNodes.clear();
			
			//creating forward task
			List<Double> forward_arrival_time_series = new ArrayList<Double>();
			forward_arrival_time_series.add(start_departure_time);
			
			List<Double> forward_tmp_time_series = Graph.getArrivalTimeSeries(start_departure_time, end_departure_time);
			
			forward_arrival_time_series.addAll(forward_tmp_time_series);
			forward_arrival_time_series.add(end_departure_time);
			
			List<BreakPoint> forward_arrival_break_points = createArrivalBreakpoints(forward_arrival_time_series);
			
			List<Double> forward_wide_distance_time_series = new ArrayList<Double>();
			forward_wide_distance_time_series.add(start_departure_time);
			
			List<Double> forward_tmp_wide_distance_time_series = Graph.getWidthTimeSeries(start_departure_time, end_departure_time);
			
			forward_wide_distance_time_series.addAll(forward_tmp_wide_distance_time_series);
			forward_wide_distance_time_series.add(end_departure_time);
			List<BreakPoint> forward_wide_distance_break_points = createScoreBreakpoints(forward_wide_distance_time_series);
			
			Function forward_arrival_time = new Function(forward_arrival_break_points, -1);
			Function forward_wide_distance = new Function(forward_wide_distance_break_points, 0);
			
			Label sourceLabel = new Label(source, forward_arrival_time, forward_wide_distance, 0, 0.0);
			//sourceLabel.initializeLists();
			sourceLabel.setVisited(source, -1);
			
			BidirectionalLabeling forward_task = new BidirectionalLabeling(destination, budget/2, sourceLabel, shared, true);
			//forward_task.run();
			
			
			//creating backward task
			List<Double> backward_arrival_time_series = new ArrayList<Double>();
			double fastest_path_cost = Graph.get_node(destination).get_forward_hTime();
			backward_arrival_time_series.add(start_departure_time+fastest_path_cost);
			
			List<Double> backward_tmp_time_series = Graph.getArrivalTimeSeries(start_departure_time+fastest_path_cost, end_departure_time+budget);
			
			backward_arrival_time_series.addAll(backward_tmp_time_series);
			backward_arrival_time_series.add(end_departure_time);
			
			List<BreakPoint> backward_arrival_break_points = createArrivalBreakpoints(backward_arrival_time_series);
			
			List<Double> backward_wide_distance_time_series = new ArrayList<Double>();
			backward_wide_distance_time_series.add(start_departure_time+fastest_path_cost);
			
			List<Double> backward_tmp_wide_distance_time_series = Graph.getWidthTimeSeries(start_departure_time+fastest_path_cost, end_departure_time+budget);
			
			backward_wide_distance_time_series.addAll(backward_tmp_wide_distance_time_series);
			backward_wide_distance_time_series.add(end_departure_time);
			List<BreakPoint> backward_wide_distance_break_points = createScoreBreakpoints(backward_wide_distance_time_series);
			
			Function backward_arrival_time = new Function(backward_arrival_break_points, -1);
			Function backward_wide_distance = new Function(backward_wide_distance_break_points, 0);
			
			Label destinationLabel = new Label(destination, backward_arrival_time, backward_wide_distance, 0, 0.0);
			//sourceLabel.initializeLists();
			destinationLabel.setVisited(destination, -1);
			BidirectionalLabeling backward_task = new BidirectionalLabeling(source, budget/2, destinationLabel, shared, false);
			//backward_task.run();
			ForkJoinTask<?> forwardFuture = BidirectionalAstar.pool.submit(forward_task);
			ForkJoinTask<?> backwardFuture = BidirectionalAstar.pool.submit(backward_task);
			forwardFuture.join();
			backwardFuture.join();
//			String analysis_file = "Analysis"+index+"_" + Graph.get_vertex_count() +".txt";
//			FileWriter fanalysis = new FileWriter(analysis_file);
//			BufferedWriter writer2 = new BufferedWriter(fanalysis);
//			
//			String path_file = "Path"+index+"_" + Graph.get_vertex_count() +".txt";
//			FileWriter fpath = new FileWriter(path_file);
//			BufferedWriter writer3 = new BufferedWriter(fpath);
//			
//			index++;
			
			//BidirectionalDriver driver = new BidirectionalDriver(queries.peek().get_destination(), budget);
//			ForwardLabeling forwardSolver = new ForwardLabeling(destination, budget, sourceLabel);
//			Map<Integer,List<Label>> forward_labels = forwardSolver.call();
//			forwardSolver.setMaster(); 
			//Map<Integer,Result> pruned_forward_labels = pruneDomination(forward_labels);
			
			
//			BackwardLabeling backwardSolver = new BackwardLabeling(source, budget, destinationLabel);
//			Map<Integer,List<Label>> backward_labels = backwardSolver.call();
			//Map<Integer,Result> pruned_backward_labels = pruneDomination(backward_labels);
			Result result = formOutputLabels(shared.intersectionNodes, shared.forwardVisited, shared.backwardVisited);
			
			return result;
		}
		return null;
	}

	private Result formOutputLabels1(Set<Integer> intersectionNodes, ConcurrentHashMap<Integer, PriorityBlockingQueue<Label>> forwardVisited, ConcurrentHashMap<Integer, PriorityBlockingQueue<Label>> backwardVisited) {
		Result finalResult = null;
		
		for(int current_join_node:intersectionNodes) {
			PriorityBlockingQueue<Label> current_backward_labels = backwardVisited.get(current_join_node);
			PriorityBlockingQueue<Label> current_forward_labels = forwardVisited.get(current_join_node);
			printLabel(current_join_node, current_forward_labels, current_backward_labels);
			System.out.println("Node: " + current_join_node + ", Forward: " + current_forward_labels.size() + ", Backward: " + current_backward_labels.size() + ", Total: " + (long)current_forward_labels.size()*(long)current_backward_labels.size());
			long i=0;
			for(Label current_backward_label:current_backward_labels) {
				for(Label current_forward_label:current_forward_labels) {
					if (finalResult == null) {
						Result currentResult = getResult(current_forward_label, current_backward_label);
						finalResult = currentResult;
					}
					else if(finalResult != null && current_forward_label.getMaxPercentageWideRoad() + current_backward_label.getMaxPercentageWideRoad() <= finalResult.get_score()) {
						i++;
					}
					
					else if(finalResult != null && current_forward_label.getMaxPercentageWideRoad() + current_backward_label.getMaxPercentageWideRoad() > finalResult.get_score()) {
						Result currentResult = getResult(current_forward_label, current_backward_label);
					    if(currentResult.get_score()>finalResult.get_score())
					    	finalResult = currentResult;
					}
					
				}
			}
			System.out.println(i);
			
		}
		return finalResult;
	}
	
	private void printLabel(int node, PriorityBlockingQueue<Label> current_forward_labels, PriorityBlockingQueue<Label> current_backward_labels) {
		String output_file = "Analysis_" + node + ".txt";
		FileWriter fout = null;
		try {
			fout = new FileWriter(output_file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedWriter writer = new BufferedWriter(fout);
		try {
			writer.write("Forward Labels:\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(Label label:current_forward_labels) {
			write(writer, label, source, node, true);
		}
		
		try {
			writer.write("\nBackward Labels:\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(Label label:current_backward_labels) {
			write(writer, label, node, destination, false);
		}
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write(BufferedWriter writer, Label label, int src, int dest, boolean isForward) {

		List<Integer> path = new ArrayList<Integer>();
		int current;
		if (isForward)
			current = dest;
		else
			current = src;
		
		while(!label.getVisitedList().get(current).equals(-1)) {
			path.add(current);
		   	current = label.getVisitedList().get(current);
		}

		path.add(current);
		
		if(isForward)
			Collections.reverse(path);
		
		for(int i:path)
			try {
				writer.write(i+",");
			} catch (IOException e) {
				e.printStackTrace();
			}
		try {
			writer.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			writer.write("[");
		} catch (IOException e) {
			e.printStackTrace();
		}
		Function forward_score_function = label.get_wide_distance();
		Function current_arrival_function = label.get_arrivalTime();
		
		while(forward_score_function != null) {
			List<BreakPoint> score_breakpoints = forward_score_function.getBreakpoints();
			List<BreakPoint> arrival_time_breakpoints = current_arrival_function.getBreakpoints();
			for(int i =0;i<score_breakpoints.size();i++) {
				
				try {
					writer.write("("+ score_breakpoints.get(i).getX()+": " + arrival_time_breakpoints.get(i).getY()+", " + score_breakpoints.get(i).getY()+"), ");
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			current_arrival_function = current_arrival_function.getNextFunction();
			
			forward_score_function = forward_score_function.getNextFunction();
		}
		try {
			writer.write("],\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

//	private Result formOutputLabels(Set<Integer> intersectionNodes, ConcurrentHashMap<Integer, PriorityBlockingQueue<Label>> forwardVisited, ConcurrentHashMap<Integer, PriorityBlockingQueue<Label>> backwardVisited) {
//
//		return intersectionNodes.parallelStream().map(current_join_node -> {
//			PriorityBlockingQueue<Label> current_backward_labels = backwardVisited.get(current_join_node);
//			PriorityBlockingQueue<Label> current_forward_labels = forwardVisited.get(current_join_node);
			
//			if (current_backward_labels == null || current_forward_labels == null) 
//				return null;
//			
//			Result bestLocalResult = null;
//			
//			for (Label backwardLabel : current_backward_labels) {
//				for (Label forwardLabel : current_forward_labels) {
					
//					bestLocalResult = getResult(forwardLabel, backwardLabel);
//					if (bestLocalResult == null) {
//						Result currentResult = getResult(forwardLabel, backwardLabel);
//					    bestLocalResult = currentResult;
//					}
//					else if(!BidirectionalAstar.Optimization || forwardLabel.getMaxPercentageWideRoad() + backwardLabel.getMaxPercentageWideRoad() > bestLocalResult.get_score()) {
//						Result currentResult = getResult(forwardLabel, backwardLabel);
//					    if(currentResult.get_score()>bestLocalResult.get_score())
//							bestLocalResult = currentResult;
//					}
//				}
//			}
//			return bestLocalResult;
//		}).filter(Objects::nonNull).max(Comparator.comparingDouble(Result::get_score)).orElse(null);//TODO
//	}
	
	private Result formOutputLabels(
	        Set<Integer> intersectionNodes,
	        ConcurrentHashMap<Integer, PriorityBlockingQueue<Label>> forwardVisited,
	        ConcurrentHashMap<Integer, PriorityBlockingQueue<Label>> backwardVisited) {

	    return intersectionNodes.parallelStream()
	        .map(node -> {
	            Label forward = forwardVisited.get(node).peek();
	            Label backward = backwardVisited.get(node).peek();

	            if (forward == null || backward == null) return null;

	            return getResult(forward, backward);
	        })
	        .filter(Objects::nonNull)
	        .min(Comparator
	                .comparingInt(Result::get_right_turns)        // fewer right turns first
	                .thenComparing(Comparator.comparingDouble(Result::get_score).reversed()) // higher score wins if tie
	        ).orElse(null);
	}




	private Result getResult(Label current_forward_label, Label current_backward_label) {
		
		double dep_time = -1;
		double scr = -1;

		
					
//		int current = destination;
//		List<Integer> path = new ArrayList<Integer>();
//		while(!destination_label.getVisitedList().get(current).equals(-1)) {
//			path.add(current);
//		   	current = destination_label.getVisitedList().get(current);
//		}
//
//		path.add(current);
//		Collections.reverse(path);
//		
//		for(int i:path)
//			writer3.write(i+",");
//		writer3.write("\n");
		//writer2.write("[");
		Function forward_score_function = current_forward_label.get_wide_distance();
		Function current_arrival_function = current_forward_label.get_arrivalTime();
		double forward_distance = current_forward_label.getDistance();
		double backward_distance = current_backward_label.getDistance();
		
		while(forward_score_function != null) {
			List<BreakPoint> score_breakpoints = forward_score_function.getBreakpoints();
			List<BreakPoint> arrival_time_breakpoints = current_arrival_function.getBreakpoints();
			for(int i =0;i<score_breakpoints.size();i++) {
				double forward_score = score_breakpoints.get(i).getY();
				double tmp_dep_time = arrival_time_breakpoints.get(i).getY();
				double backward_score = current_backward_label.get_wide_distance(tmp_dep_time);
				
				//writer2.write("("+ score_breakpoints.get(i).getX()+","+score_breakpoints.get(i).getY()+"), ");
				if((forward_score+backward_score)*100/(forward_distance+backward_distance)>scr) {
					
					scr = (forward_score+backward_score)*100/(forward_distance+backward_distance);
					dep_time = tmp_dep_time;
				}
			}
			
			forward_score_function = forward_score_function.getNextFunction();
			current_arrival_function = current_arrival_function.getNextFunction();
		}
		//writer2.write("],\n");
//			/int i= (int) start_departure_time;
//			if(destination_label.get_arrivalTime().getBreakpoints().get(0).getX() >= i)
//				i= (int) Math.ceil(destination_label.get_arrivalTime().getBreakpoints().get(0).getX());
//			
//			int j = (int) end_departure_time;
//			if(destination_label.get_arrivalTime().getBreakpoints().get(destination_label.get_arrivalTime().getBreakpoints().size()-1).getX() <= j) 
//				j= (int) Math.floor(destination_label.get_arrivalTime().getBreakpoints().get(destination_label.get_arrivalTime().getBreakpoints().size()-1).getX());
//			
//			for(; i<=j;i++) {
//				double tmp_arr_time = destination_label.get_arrivalTime(i);//TODO
//				if(tmp_arr_time-i<=budget) {
//					int tmp_score = destination_label.get_score(i);
//					
//					if(tmp_score>scr) {
//						dep_time = i;
//						arr_time = tmp_arr_time;
//						scr = tmp_score;
//					}
//				}
//			}
		//writer2.flush();
		//writer3.flush();
		int total_right_turns = current_forward_label.getRightTurns()+current_backward_label.getRightTurns();
		Result result = new Result(dep_time, scr, total_right_turns);
		return result;
	}
	
	private static List<BreakPoint> createScoreBreakpoints(List<Double> time_series) {
		List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();
		
		for(double time_point: time_series) {
			BreakPoint break_point = new BreakPoint(time_point, 0);
			breakpoints.add(break_point);
		}
		return breakpoints; 
	}

	private static List<BreakPoint> createArrivalBreakpoints(List<Double> time_series) {
		List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();
		
		for(double time_point: time_series) {
			BreakPoint break_point = new BreakPoint(time_point, time_point);
			breakpoints.add(break_point);
		}
		return breakpoints; 
	}

}
