import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

/*
 * 2IO90 DBL Algorithms
 * 14-11-2014 Stephan Oostveen
 * This class represents the plane with all the points.
 * It will calculate the biggest height of the labels possible
 * such that no overlap between labels occurs. 
 */

public class Plane {
	private double aspectRatio;
	private int numberOfPoints;
	private double height;
	private PlacementModel pModel;

	int rangeX = 0;
	int rangeY = 0;

	private HashMap<ClauseValue, Integer> connectedComponents;// saves the connected component number the clauseValue is part of.
	private DirectedGraph minGraph;// saves the graph of the iteration with a solvable situation.
	private Map<ClauseValue, PosPoint> clauseToPoint;// easily find the point connected to a clauseValue.

	private SliderPoint[] sliderPoints;
	public int[] xPointArray; // slider pointers
	private PosPoint[] posPoints;


	private double delta = 0.001;// difference of border
	private boolean debug = true;

	public Plane(double aspectRatio, SliderPoint[] points, PlacementModel pModel) {
		this.aspectRatio = aspectRatio;
		this.numberOfPoints = points.length;
		this.sliderPoints = points;
		this.pModel = pModel;
		height = (aspectRatio < 1 ? aspectRatio : 1 / aspectRatio);
	}

	public Plane(double aspectRatio, PosPoint[] points, PlacementModel pModel) {
		this.aspectRatio = aspectRatio;
		this.numberOfPoints = points.length;
		this.posPoints = points;
		this.pModel = pModel;
	}

	public void debugPrint(String text) {
		if (MapLabeler.local && debug) {
			System.out.println(text);
		}
	}

	/**
	 * 
	 * @return The solution to the 2pos problem. It first calculates the maximum height and the orientation of every label. This method returns the orientation array. Where every element is one of PlacementModel.NE, PlacementModel.NW.
	 */
	public PosPoint[] find2PosSolution() {
		int[] xSortedOrder = MergeSort.sort(posPoints);// sorting the points onx-cor, referencing by index in this array.

		// int range = 10000;//the range of the coordinates from 0 to range

		double minHeight = (aspectRatio <= 1) ? 1d : (1d / (2d * aspectRatio));// minimal height
		// TODO is it really true that 2*aspectRatio for 2Pos solution? can't all labels be put in the same orientation,
		// TODO and still not cause overlap? all points have at least 1x1 free space, no matter the location, as long as all
		// TODO labels are orientated the same way, which is the simplest and minimal solution.
		double maxHeight = MaxSize.getMaxPossibleHeight(posPoints, xSortedOrder, aspectRatio, PlacementModel.TWOPOS);// find the max height

		MapLabeler.maxHeight = (float) maxHeight;

		height = maxHeight;// height to use initially is the max height.
		double lastHeight = 0;// a value to find out if you are checking for the same height twice in succession.

		clauseToPoint = new HashMap<ClauseValue, PosPoint>();// make a new mapping connecting clauseValues with points.

		rangeX = posPoints[xSortedOrder[xSortedOrder.length - 1]].getX();

		ArrayList<Label> allLabels = new ArrayList<Label>();

		for (int i = 0; i < xSortedOrder.length; i++) {// make the top allLabels for all points.
			int pointer = xSortedOrder[i];
			PosPoint p = posPoints[pointer];

			if (p.getY() > rangeY) {
				rangeY = p.getY();
			}

			if (i > 0) {
				if (i < posPoints.length - 1) {
					int indexToRight = i - 1;
					boolean clean = true;
					while (indexToRight <= posPoints.length - 1 && clean
							&& posPoints[xSortedOrder[indexToRight]].getX() - p.getX() < 2 * maxHeight * aspectRatio) {
						int difY = posPoints[xSortedOrder[indexToRight]].getY() - p.getY();
						if (difY < maxHeight && difY >= 0) {
							clean = false;
							break;
						}
						indexToRight++;
					}

					if (!clean) {
						Label NE = new Label(p, 1, true);
						allLabels.add(NE);// shift=1 and top=true gives us the NE label;
						clauseToPoint.put(NE.toClause(), p);

						Label NW = new Label(p, 0, true);
						allLabels.add(NW);// shift=1 and top=true gives us the NE label;
						clauseToPoint.put(NW.toClause(), p);

						clean = true;
					} else {
						p.setPosition(Orientation.NE);
					}

					if (!clean) {
						int indexToLeft = i - 1;
						clean = true;

						while (indexToLeft >= 0 && clean && p.getX() - posPoints[xSortedOrder[indexToLeft]].getX() < 2 * maxHeight * aspectRatio) {
							int difY = posPoints[xSortedOrder[indexToLeft]].getY() - p.getY();
							if (difY < maxHeight && difY >= 0) {
								clean = false;
								break;
							}
							indexToLeft--;
						}

						if (!clean) {
							Label NE = new Label(p, 1, true);
							allLabels.add(NE);// shift=1 and top=true gives us the NE label;
							clauseToPoint.put(NE.toClause(), p);

							Label NW = new Label(p, 0, true);
							allLabels.add(NW);// shift=1 and top=true gives us the NE label;
							clauseToPoint.put(NW.toClause(), p);
						} else {
							p.setPosition(Orientation.NW);
						}
					}
				} else {// i = posPoints.length-1
					Label NE = new Label(p, 1, true);
					allLabels.add(NE);// shift=1 and top=true gives us the
										// NE label;
					clauseToPoint.put(NE.toClause(), p);
		
					Label NW = new Label(p, 0, true);
					allLabels.add(NW);// shift=1 and top=true gives us the NE label;
					clauseToPoint.put(NW.toClause(), p);
					
					//p.setPosition(Orientation.NE);
				}
			} else {// i==0
				Label NE = new Label(p, 1, true);
				allLabels.add(NE);// shift=1 and top=true gives us the
									// NE label;
				clauseToPoint.put(NE.toClause(), p);
	
				Label NW = new Label(p, 0, true);
				allLabels.add(NW);// shift=1 and top=true gives us the NE label;
				clauseToPoint.put(NW.toClause(), p);
				
				//p.setPosition(Orientation.NW);
			}

		}
		
		
		QuadTree quad = new QuadTree(0, new Rectangle(0, 0, rangeX + 1, rangeY + 1),false);// new quadtree with (top) level 0 and dimensions (range+1)^2

		int loops = 0;

		MapLabeler.initTime += (System.nanoTime() - MapLabeler.startTime);

		while (lastHeight != height) {// as long as the height is not equal to
										// the last checked height
			debugPrint("Height: " + height + " lastHeight: " + lastHeight + " minHeight: " + minHeight + " maxHeight: " + maxHeight);
			HashMap<PosPoint, Orientation> validOrientation = new HashMap<PosPoint, Orientation>();
			ArrayList<Label> labels = new ArrayList<Label>(allLabels);// all labels will be stored in this arrayList.

			// the additional clauses are required to fix the value of a dead
			// label to the negation of the clauseValue of that label.
			long time = System.nanoTime();
			ArrayList<Clause> collisions = findCollisions2pos(labels, validOrientation, quad, height);

			long collisionTime = (System.nanoTime() - time);
			MapLabeler.avgColTimeLoop += collisionTime;
			if (MapLabeler.maxColTimeLoop < collisionTime) {
				MapLabeler.maxColTimeLoop = collisionTime;
			}
			// get the list of collisions

			time = System.nanoTime();
			if (checkTwoSatisfiability(collisions)) {// if a satisfiable
														// configuration exists
				if (minHeight <= height) {
					minHeight = height;// this height will be valid, so the
										// minimum height becomes this height.
					debugPrint("2-Sat satisfies.");
				}
			} else {// if no solution can be found with 2-sat
				if (maxHeight > height) {
					maxHeight = height;
					debugPrint("2-Sat is not satisfactory.");
				}
				// this height has no solution, so the maximum found height for
				// which this does not work is now height
			}

			long satTime = (System.nanoTime() - time);
			MapLabeler.avg2SatTimeLoop += satTime;
			if (MapLabeler.max2SatTimeLoop < satTime) {
				MapLabeler.max2SatTimeLoop = satTime;
			}

			debugPrint("new last height: " + height);
			lastHeight = height;// remember which height was used this
								// iteration.

			/*
			 * Calculate the next candidate value, considering that the distance between points is always an integer. This means that the height or the width (or both) MUST be divisible by 0.5.
			 */

			height = (maxHeight + minHeight) / 2;// calculate the average of the
													// maxHeight and minHeight
			double width = height * aspectRatio;// calculate the width.

			debugPrint(">" + height + "," + width + ":" + roundToHalf(height) + "," + roundToHalf(width) + ":" + Math.abs(height - roundToHalf(height)) + ","
					+ Math.abs(width - roundToHalf(width)));

			// height =
			// (Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width)))?
			// roundToHalf(height) : roundToHalf(width)/aspectRatio;

			if (Math.abs(height - roundToHalf(height)) < Math.abs(width - roundToHalf(width))) {
				if (roundToHalf(height) <= maxHeight && roundToHalf(height) >= minHeight) {
					height = roundToHalf(height);
				} else {
					height = roundToHalf(width) / aspectRatio;
				}
			} else {
				if (roundToHalf(width) / aspectRatio <= maxHeight && roundToHalf(width) / aspectRatio >= minHeight) {
					height = roundToHalf(width) / aspectRatio;
				} else {
					height = roundToHalf(height);
				}
			}
			// (Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width))):
			// Find if the distance to next height or the next width is smaller
			// than the other.
			debugPrint("new height: " + height);
			loops++;
		}

		MapLabeler.totalAvgColTime += MapLabeler.avgColTimeLoop;
		if (MapLabeler.totalMaxColTime < MapLabeler.avgColTimeLoop) {
			MapLabeler.totalMaxColTime = MapLabeler.avgColTimeLoop;
		}
		MapLabeler.avgColTimeLoop /= loops;

		MapLabeler.totalAvg2SatTime += MapLabeler.avg2SatTimeLoop;
		if (MapLabeler.totalMax2SatTime < MapLabeler.avg2SatTimeLoop) {
			MapLabeler.totalMax2SatTime = MapLabeler.avg2SatTimeLoop;
		}
		MapLabeler.avg2SatTimeLoop /= loops;

		MapLabeler.nrOfLoops += loops;

		height = minHeight;// To be sure that we have a valid height, take the
							// minHeight found.

		debugPrint("Resulting height: " + height + ", " + maxHeight + ", " + minHeight);

		long time = System.nanoTime();
		if (minGraph != null) {// border case: solution is minimal height
			Stack<ClauseValue> order = dfsOrder(reverseGraph(minGraph));// the
																		// depth
																		// first
																		// search
																		// order
																		// or
																		// the
																		// reverse
																		// of
																		// the
																		// graph.

			while (!order.isEmpty()) {// while elements in the order stack still
										// exist.
				ClauseValue next = order.pop();// pop the first element.
				getNext(next);// check which elements this one is connected to.
			}
		}

		for (PosPoint p : posPoints) {
			if (p.getPosition() == null) {
				p.setPosition(Orientation.NE);
			}
		}
		MapLabeler.placementTime += (System.nanoTime() - time);

		MapLabeler.realHeight = (float) height;

		return posPoints;// return the array of points, now with correct
							// positions.
	}

	public double roundToHalf(double d) {
		return Math.round(2 * d) / 2d;// returns the rounded value of 2*d, divided by double type value 2.
	}

	public void getNext(ClauseValue next) {
		if (clauseToPoint.get(next).getPosition() == null) {// if the connected point has no position set yet
			clauseToPoint.get(next).setPosition(next.isPositive() ? 1 : 0, true);// set the position to this clauseValues' position.
			if (!minGraph.edgesFrom(next).isEmpty()) {// if the edges from this clauseValue in the graph is not empty.
				for (ClauseValue value : minGraph.edgesFrom(next)) {// for all the edges:
					if (connectedComponents.get(value).equals(connectedComponents.get(next))) {// if they are in the same connected component:
						getNext(value);// check what they are connected to.
					}
				}
			}
		}
	}

	public Label[] labels;

	Random r = new Random();
	long timeColDect = 0;
	// long timeColDect = 0;
	private QuadTree quad;
	private Orientation from;
	private Orientation to;
	boolean print = false;
	boolean print2 = !true;
	boolean print3 = false;

	Orientation initial;

	public Label[] find4PosSolutionSA(){
		double coolingRate = 0.00003;
		double initialTemp = 10000;
		//double initialHeight = 250;
		
		
		//int range = 10000;
		
		
		
		LabelConfiguration best = null;
		int bestEnergy = Integer.MAX_VALUE;
		
		int[] xSortedOrder = MergeSort.sort(posPoints);//sorting the points on x-cor, referencing by index in this array.
		double minHeight = (aspectRatio < 1) ? 0.5 : (1/(2*aspectRatio));//minimal height	
		//TODO Melroy: minimale height is een half keer de aspectratio de 2* moet er bij
		//double maxHeight = MaxSize.getMaxPossibleHeight(posPoints, xSortedOrder, aspectRatio, PlacementModel.FOURPOS);//2x the maximal height, so that we start with the calculated max-height in the loop.
		double maxHeight = MaxSize.getMaxPossibleHeight(posPoints, xSortedOrder, aspectRatio, PlacementModel.FOURPOS);
		
		MapLabeler.maxHeight = (float)maxHeight;
		
		//height = maxHeight;//height to use is the average of max and min height
		height = (maxHeight+minHeight)/2;//calculate the average of the maxHeight and minHeight
		double width = height * aspectRatio;//calculate the width.

		//debugPrint(">" + height + "," + width + ":" + roundToHalf(height) + "," + roundToHalf(width) + ":" + Math.abs(height-roundToHalf(height)) + "," + Math.abs(width-roundToHalf(width)));
		
		//height = (Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width)))? roundToHalf(height) : roundToHalf(width)/aspectRatio;
		
		if(Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width))){
			if(roundToHalf(height)<=maxHeight && roundToHalf(height) >= minHeight){
				height = roundToHalf(height);
			}
			else{
				height = roundToHalf(width)/aspectRatio;
			}
		}
		else{
			if(roundToHalf(width)/aspectRatio<=maxHeight && roundToHalf(width)/aspectRatio >= minHeight){
				height = roundToHalf(width)/aspectRatio;
			}
			else{
				height = roundToHalf(height);
			}
		}
		
		
		
		double lastHeight = 0;
		
		LabelConfiguration finalBest = null;
		double finalHeight = 0;
		ArrayList<Integer> labelsFinalIndex = new ArrayList<Integer>();
		ArrayList<Label> labelsFinalDone = new ArrayList<Label>();
		ArrayList<Integer> labelsFinalDoneIndex = new ArrayList<Integer>();
		
		
		
		
		int loops = 0;
		while(lastHeight != height && height != finalHeight){
			
			rangeX = posPoints[xSortedOrder[xSortedOrder.length - 1]].getX();

			ArrayList<Label> labelsDone = new ArrayList<Label>();
			ArrayList<Integer> labelsDoneIndex = new ArrayList<Integer>();
			ArrayList<PosPoint> labelsToProcess = new ArrayList<PosPoint>();
			ArrayList<Integer> labelsToProcessIndex = new ArrayList<Integer>();

			for (int i = 0; i < xSortedOrder.length; i++) {// make the top allLabels for all points.
				int pointer = xSortedOrder[i];
				PosPoint p = posPoints[pointer];

				if (p.getY() > rangeY) {
					rangeY = p.getY();
				}
				
				boolean free = true;
				
				int indexRight = i + 1;
				while(indexRight < posPoints.length && free && posPoints[xSortedOrder[indexRight]].getX() - p.getX() < 2 * height * aspectRatio){
					int difY = Math.abs(posPoints[xSortedOrder[indexRight]].getY() - p.getY());
					if (difY < height * 2) {
						free = false;
						break;
					}
					indexRight++;
				}
				
				int indexLeft = i - 1;
				while(indexLeft >= 0 && free && p.getX() - posPoints[xSortedOrder[indexLeft]].getX() < 2 * height * aspectRatio){
					int difY = Math.abs(posPoints[xSortedOrder[indexLeft]].getY() - p.getY());
					if (difY < height * 2) {
						free = false;
						break;
					}
					indexLeft--;
				}
				if(print3) debugPrint("       " + p + ": " + free);
					
				if(free){
					labelsDone.add(new Label(p,1,true));
					labelsDoneIndex.add(pointer);
				}
				else {
					labelsToProcess.add(p);
					labelsToProcessIndex.add(pointer);
				}
				
				
			}
			
			if(print3) debugPrint("Done " + labelsDone.size());
			if(print3) debugPrint("To Process " + labelsToProcess.size());
			
			
			
			
			quad = new QuadTree(0, new Rectangle(0,0,rangeX,rangeY),true);
			
			loops++;
			LabelConfiguration current = new LabelConfiguration(labelsToProcess, height, aspectRatio);
			
			if(print) debugPrint("############################################################");
			debugPrint("------>  height: " + height + " min " +minHeight + " max " + maxHeight);
			if(print) debugPrint("############################################################");
			if(print) debugPrint("");
			
			best = new LabelConfiguration(current.getLabels());
			quad.init(current.getLabels(), height, aspectRatio, rangeX, rangeY);//initialize the quadtree
			labels = current.getLabels();
			
			int currentEnergy = calculateScore(quad, Integer.MAX_VALUE, null, null, null);
			
			bestEnergy = currentEnergy;
			int neighbourEnergy;
			
			simulatedAnnealing:
			for(int iteration = 0; iteration < 1; iteration++){
				double temp = initialTemp;
				debugPrint("Iteration: " + iteration);
			
			
			
			while(temp > 1 && bestEnergy > 0){
				LabelConfiguration newSolution = current;
				int position = (int) (newSolution.labelSize() * r.nextDouble());
				
				Label lChanged = newSolution.getLabels()[position];
				if(print) System.out.print ("Initial: " + lChanged);
				initial = lChanged.getOrientation();
				
				ArrayList<Label> before = getIntersections(quad,lChanged);
				
				//if(before.size() != 0){
					//skip already alive
				
				
					
					ArrayList<Orientation> options = new ArrayList<Orientation>();
					options.add(Orientation.NE);
					options.add(Orientation.NW);
					options.add(Orientation.SE);
					options.add(Orientation.SW);
					
					
					quad.remove(lChanged);
					newSolution.change(position, options);
					
					quad.insertNew(lChanged, height, aspectRatio);
					labels = current.getLabels();
					ArrayList<Label> after = getIntersections(quad,lChanged);
					//if(print) debugPrint(" change to " + lChanged.getOrientation());
					
					
					int counter = 0;
					from = initial;
					to = newSolution.getLastTo();
					boolean triedall = false;
					if(print) System.out.println (" from: " + newSolution.getLastFrom());
					if(print) debugPrint("To: " + newSolution.getLastTo() + " " + containsPoint2(after,lChanged, height));
					while(!containsPoint2(after,lChanged, height).isEmpty()){
						counter++;
						if(options.size() == 0){
							if(!triedall){
								options.add(initial);
								triedall = true;
							}
							else {
								debugPrint("AAAAAAAAAAAAAAAHHHH HEIGHT " + height + " NOT POSSIBLE");
								bestEnergy = Integer.MAX_VALUE;
								break simulatedAnnealing;
							}
						}
						quad.remove(lChanged);
						newSolution.change(position, options);
						to = newSolution.getLastTo();
						quad.insertNew(lChanged, height, aspectRatio);
						
						labels = current.getLabels();
						after = getIntersections(quad,lChanged);
						
						if(print) debugPrint("To 2: " + newSolution.getLastTo() + " " + containsPoint2(after,lChanged, height));
						
						
					}
					
					
					
					
					neighbourEnergy = calculateScore(quad, currentEnergy, lChanged, before, after);
					
					//int old = oldIntersect(false);
					//if(old != neighbourEnergy){
					//	if(print)debugPrint("fout: old " + old + " vs new " + neighbourEnergy);
					//	oldIntersect(true);
					//}
					
					
					
					if(neighbourEnergy < bestEnergy){
						labels = newSolution.getLabels();
						//if(oldIntersect(false) != neighbourEnergy){
							//System.out.println("Changed: " + lChanged);
						//	oldIntersect(true);
						//}
					}
					
					if (calculateAcceptance(currentEnergy, neighbourEnergy, temp) > r.nextDouble()) {
						current = newSolution;
						currentEnergy = neighbourEnergy;
						
						//labels = newSolution.getLabels();
						/*if(finalIntersectionTest(false) != currentEnergy){
							
							if(print)debugPrint("####################################################");
							if(print)debugPrint("############  EN WEL GODVERDOMME  ##################");
							if(print)debugPrint("####################################################");
							
							//labels = current
							labels = newSolution.getLabels();
							if(print2) debugPrint(finalIntersectionTest(print) + "");
							//ietsmeteentest(current.getLabels());
							
							if(print)debugPrint("");
							
						}*/
						
		            }
					else {
						if(print)debugPrint("    changeback");
						quad.remove(lChanged);
						newSolution.changeBack(initial);
						
						for(int i = 0; i < changed.size(); i++){
							Label l = changed.get(i);
							boolean had = l.isHasIntersect();
							l.setHasIntersect(!had);
						}
						quad.insertNew(lChanged, height, aspectRatio);
						current = newSolution;
					
					}
					
					if (neighbourEnergy < bestEnergy) {
						debugPrint("^new best: " + neighbourEnergy);
						best = new LabelConfiguration(newSolution.getLabels());
		                bestEnergy = neighbourEnergy;
		            }
					
					temp *= 1-coolingRate;
				//}
			}
			}
			
			if(bestEnergy == 0){
				labels = best.getLabels();
				//if(finalIntersectionTest(false) != 0){
				//	debugPrint("EN WEL GODVERDOMME");
					
					
				//	height = height-1;
					
					
					
				//}
				//else {
					
					if(finalHeight < height){
						finalBest = new LabelConfiguration(best.getLabels());
						finalHeight = height;
						
						labelsFinalIndex = labelsToProcessIndex;
						labelsFinalDone = labelsDone;
						labelsFinalDoneIndex = labelsDoneIndex;
						
						
						debugPrint("NEW FINAL HEIGHT");
						//break;
					}
					minHeight = height;
				//}
			}
			else {
				maxHeight = height;
			}
			

			
			
			
			lastHeight = height;
			//height = (maxHeight+minHeight)/2;//height to use is the average of max and min height
			//double width = height * aspectRatio;
			//height = (Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width)))? roundToHalf(height) : roundToHalf(width)/aspectRatio;
			
			height = (maxHeight+minHeight)/2;//calculate the average of the maxHeight and minHeight
			width = height * aspectRatio;//calculate the width.

			//debugPrint(">" + height + "," + width + ":" + roundToHalf(height) + "," + roundToHalf(width) + ":" + Math.abs(height-roundToHalf(height)) + "," + Math.abs(width-roundToHalf(width)));
			
			//height = (Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width)))? roundToHalf(height) : roundToHalf(width)/aspectRatio;
			
			if(Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width))){
				if(roundToHalf(height)<=maxHeight && roundToHalf(height) >= minHeight){
					height = roundToHalf(height);
				}
				else{
					height = roundToHalf(width)/aspectRatio;
				}
			}
			else{
				if(roundToHalf(width)/aspectRatio<=maxHeight && roundToHalf(width)/aspectRatio >= minHeight){
					height = roundToHalf(width)/aspectRatio;
				}
				else{
					height = roundToHalf(height);
				}
			}
		
		}
		height = minHeight;
		
		if(finalBest == null){
			finalBest = best;
			debugPrint("NOT FOUND");
		}
		
		debugPrint("FINAL HEIGHT: " + height);
		debugPrint("FINAL BEST: " + finalBest);
		
		
		
		Label[] processed = finalBest.getLabels();
		Label[] result = new Label[processed.length + labelsFinalDone.size()];
		
		for(int i = 0; i < processed.length; i++){
			int index = labelsFinalIndex.get(i);
			//System.out.println(index + " " + processed[i]);
			result[index] = processed[i];
		}
		for(int i = 0; i < labelsFinalDone.size(); i++){
			int index = labelsFinalDoneIndex.get(i);
			//System.out.println(index + " " + labelsFinalDone.get(i));
			result[index] = labelsFinalDone.get(i);
		}
		
		labels = result;
		debugPrint("Skipped by final optimalization: " + labelsFinalDone.size());
		
		
		debugPrint("Time for collision detection: " + timeColDect);
		
		MapLabeler.nrOfLoops = loops;
		MapLabeler.realHeight = (float)height;
		
		return result;
	}
	
	private double calculateAcceptance(int oldEnergy, int newEnergy, double temperature){
		if(newEnergy < oldEnergy){
			return 1.0;
		}
		return Math.exp((oldEnergy - newEnergy) / temperature);
	}
	
	public ArrayList<Label> containsPoint2(ArrayList<Label> labels, Label l, double height){
		 ArrayList<Label> contained = new ArrayList<Label>();//arraylist to be returned
		 for(Label l2: labels){//for all labels in the labels arraylist
			 PosPoint p = l.getBoundPoint();
			 PosPoint p2 = l2.getBoundPoint();
			 if(!p2.equals(p) && l.getRect().contains(l2.getX(),l2.getY())){//check if the point is in the label
				 if(
						 p.getX() != p2.getX() && 
						 p.getY() != p2.getY() && 
						 p.getX() != p2.getX() + height && 
						 p.getX() != p2.getX() - height && 
						 p.getY() != p2.getY() + height && 
						 p.getY() != p2.getY() - height){
				 contained.add(l2);//if so, add it to contained
				 }
			 }
		 }
		 //System.out.println(l + " Contains: " + contained);
			
		 return contained;//return the contained list
	 }
	
	
	ArrayList<Label> changed = new ArrayList<Label>();
	
	private int calculateScore(QuadTree quad, int oldScore, Label lChanged, ArrayList<Label> before, ArrayList<Label> after){
		long start = System.currentTimeMillis();
		int amount = 0;
		
		if(lChanged == null){
			
			for(int i = 0; i < labels.length; i++){
				labels[i].setHasIntersect(false);
			}
			for(int i = 0; i < labels.length; i++){
				Label l = labels[i];
				if(!l.isHasIntersect()){
					ArrayList<Label> returnObjects = new ArrayList<Label>();
					quad.retrieve(returnObjects, l);
					for(int j = 0; j < returnObjects.size(); j++){
						Label label2 = returnObjects.get(j);
						if(intersects(l, label2)){
							if(!l.isHasIntersect()){
								l.setHasIntersect(true);
								amount++;
							}
							if(!label2.isHasIntersect()){
								label2.setHasIntersect(true);
								amount++;
							}
						}	
					}	
				}
			}
		}
		else {
			
			changed = new ArrayList<Label>();
			
			ArrayList<Label> add = new ArrayList<Label>();
			
			//debugPrint("Old Score : " + oldScore);
			amount = oldScore;
			
			
			
			
			if(print) System.out.print("      Current: ");
			for(int i = 0; i < labels.length; i++){
				if(labels[i].isHasIntersect()) if(print) System.out.print(labels[i] + " ");
			}
			if(print) System.out.println();
			
			
			if(print) System.out.print("      Before: ");
			for(int i = 0; i < before.size(); i++){
				if(before.get(i).isHasIntersect()) if(print) System.out.print(before.get(i) + " ");
			}
			if(print) System.out.println();
			
			for (Label l : after){
				if (!before.contains(l)) before.add(l);
			}
			
			if (!before.contains(lChanged)) { before.add(lChanged); }
			
			
			
			
			if(print) System.out.print("      AFter : ");
			for(int i = 0; i < after.size(); i++){
				if(after.get(i).isHasIntersect()) if(print) System.out.print(after.get(i) + " ");
			}
			if(print) System.out.println();
			
			if(print) System.out.println("      Changed: " + lChanged + " comming from: " + initial + " to " + to);
			if(print) System.out.print("      Remove : ");
			
			int subtract = 0;
			for(int i = 0; i < before.size(); i++){
				Label l = before.get(i);
				if(l.isHasIntersect()) {
					changed.add(l);
					if(print) System.out.print(l + " ");
					l.setHasIntersect(false);
					subtract++;
				} else { if(print) System.out.print("(" + l + ") "); }
				
			}
			if(print) System.out.println();
			
			amount = amount - subtract;
			
			if(print) System.out.println("Size: " + before.size() + "   Add: ");
			ArrayList<Label> returnObjects = new ArrayList<Label>();
			
			
			
			for(int i = 0; i < before.size(); i++){
				Label l = before.get(i);
				//if(!l.isHasIntersect()){
					returnObjects = new ArrayList<Label>();
					quad.retrieve(returnObjects, l);
					if(print) System.out.println("                     Retrieved for: " + l + ": " + returnObjects);
					for(int j = 0; j < returnObjects.size(); j++){
						Label label2 = returnObjects.get(j);
						if(intersects(l, label2)){
							if(!l.isHasIntersect()){
								if(!add.contains(l)) add.add(l);
								if(print) System.out.println("                               ADD: " + l + " ");
								l.setHasIntersect(true);
								amount++;
							}
							if(!label2.isHasIntersect()){
								if(!add.contains(label2)) add.add(label2);
								if(print) System.out.println("                               ADD: " + l + " ");
								label2.setHasIntersect(true);
								amount++;
							}
						}	
					}	
				//}
			}
			
			if(print) System.out.println();
			if(print) System.out.print("   retrieved : ");
			if(print) for(Label l: returnObjects) System.out.print(l + " ");
			if(print) System.out.println();
			
			if(print) System.out.print("      Current: ");
			for(int i = 0; i < labels.length; i++){
				if(labels[i].isHasIntersect()) if(print) System.out.print(labels[i] + " ");
			}
			if(print) System.out.println();
			
			
			
			for(int i = 0; i < add.size(); i++){
				Label l = add.get(i);
				if(!changed.contains(l)) changed.add(l);
				else changed.remove(l);
			}
			
			if(print) System.out.print("      Changed: ");
			
			for(int i = 0; i < changed.size(); i++){
				if(print) System.out.print(changed.get(i) + " ");
			}
			
			
			if(print) System.out.println();
			if(print) System.out.println();
			
			
		}
		long stop = System.currentTimeMillis();
		timeColDect += (stop-start);
		return amount;
	}
	
	
	
	
	
	
	
	public int oldIntersect(boolean print){
		//boolean print = true;
		//print = false;
		
		int range = 10000;
		int amount = 0;
		Label[] otherLabels = new Label[labels.length];
		
		
		if(print) System.out.print("  Before: ");
		for(int i = 0; i < labels.length; i++){
			if(print) if(labels[i].isHasIntersect())  System.out.print(labels[i] + " ");
		}
		if(print) System.out.println();
		
		
		//otherLabels = labels.clone();
		
		for(int i = 0; i < labels.length; i++){
			otherLabels[i] = new Label(labels[i]);
			otherLabels[i].setHasIntersect(false);
		}
		
		QuadTree quad = new QuadTree(0, new Rectangle(0,0,range,range),false);
		quad.init(otherLabels, height, aspectRatio, range, range);//initialize the quadtree
		
		
		ArrayList<Label> intersecting = new ArrayList<Label>();
		
		
		for(int i = 0; i < otherLabels.length; i++){
			Label l = otherLabels[i];
			
			if(!intersecting.contains(l)){
				ArrayList<Label> returnObjects = new ArrayList<Label>();
				quad.retrieve(returnObjects, l);
				for(int j = 0; j < returnObjects.size(); j++){
					Label label2 = returnObjects.get(j);
					if(intersects(l, label2)){
						if(!intersecting.contains(l)){
							intersecting.add(l);
							amount++;
						}
						if(!intersecting.contains(label2)){
							intersecting.add(label2);
							amount++;
						}
					}	
				}	
			}
		}
		
		if(print) System.out.print("Currently intersected: " + amount + ": ");
		
		
		for(int j = 0; j < intersecting.size(); j++){
			if(print) System.out.print(intersecting.get(j) + " ");
		}
		if(print) System.out.println();
		
		//System.out.println();
		
		return amount;
	}
	
	public ArrayList<Label> getIntersections(QuadTree tree, Label l) {
		ArrayList<Label> possibleInters = new ArrayList<Label>();
		ArrayList<Label> intersections = new ArrayList<Label>();
		tree.retrieve(possibleInters, l);
		for (Label l2 : possibleInters) {
			// System.out.println("intersection between: " + l + " and " + l2 +
			// " is " + intersects(l, l2));
			if (intersects(l, l2)) {
				intersections.add(l2);
			}
		}
		return intersections;
	}
	

	/*
	 * public int oldIntersect(boolean print){ //boolean print = true; //print = false;
	 * 
	 * int range = 10000; int amount = 0; Label[] otherLabels = new Label[labels.length];
	 * 
	 * 
	 * if(print) System.out.print("  Before: "); for(int i = 0; i < labels.length; i++){ if(print) if(labels[i].isHasIntersect()) System.out.print(labels[i] + " "); } if(print) System.out.println();
	 * 
	 * 
	 * //otherLabels = labels.clone();
	 * 
	 * for(int i = 0; i < labels.length; i++){ otherLabels[i] = new Label(labels[i]); otherLabels[i].setHasIntersect(false); }
	 * 
	 * QuadTree quad = new QuadTree(0, new Rectangle(0,0,range,range)); quad.init(otherLabels, height, aspectRatio, 10000);//initialize the quadtree
	 * 
	 * 
	 * ArrayList<Label> intersecting = new ArrayList<Label>();
	 * 
	 * 
	 * for(int i = 0; i < otherLabels.length; i++){ Label l = otherLabels[i];
	 * 
	 * if(!intersecting.contains(l)){ ArrayList<Label> returnObjects = new ArrayList<Label>(); quad.retrieve(returnObjects, l); for(int j = 0; j < returnObjects.size(); j++){ Label label2 = returnObjects.get(j); if(intersects(l, label2)){ if(!intersecting.contains(l)){ intersecting.add(l); amount++; } if(!intersecting.contains(label2)){ intersecting.add(label2); amount++; } } } } }
	 * 
	 * if(print) System.out.print("Currently intersected: " + amount + ": ");
	 * 
	 * 
	 * for(int j = 0; j < intersecting.size(); j++){ if(print) System.out.print(intersecting.get(j) + " "); } if(print) System.out.println();
	 * 
	 * //System.out.println();
	 * 
	 * return amount; }
	 */

	public SliderPoint[] find1SliderSolution() {
		long time = System.nanoTime();
		long lastCheck = MapLabeler.totalAvgColTime;
		xPointArray = MergeSort.sort(sliderPoints);
		MapLabeler.initTime += (System.nanoTime() - time);

		CalcSlider(sliderPoints, xPointArray);

		MapLabeler.realHeight = (float) height;
		if (MapLabeler.totalMaxColTime < MapLabeler.totalAvgColTime - lastCheck) {
			MapLabeler.totalMaxColTime = MapLabeler.totalAvgColTime - lastCheck;
		}
		return sliderPoints;
	}

	void CalcSlider(SliderPoint[] sArray, int[] pointer) {
		long time = System.nanoTime();

		int i; // sliderPoints must be sorted on x-coordinates
		double minH = 0;
		double maxH = MaxSize.getMaxPossibleHeight(sArray, pointer, aspectRatio, PlacementModel.ONESLIDER);
		MapLabeler.maxHeight = (float) maxH;
		double T = 0.01;
		delta = 0.00000000000001; // maxH/(Math.pow(2,(1000/(sArray.length *
									// T))));
		// System.out.println("Precision: " + delta);
		// System.out.println("MaxSize gives: " + maxH);
		double currentH;
		double saveD = 0;
		int loops = 0;
		MapLabeler.initTime += (System.nanoTime() - time);
		while ((maxH - minH >= delta) && (System.currentTimeMillis() - MapLabeler.start <= 290000) && (saveD != maxH - minH)) {
			loops++;
			saveD = maxH - minH;
			// System.out.println(saveD);
			boolean mayContinue = true;
			currentH = (maxH + minH) / 2;
			debugPrint("Current: " + currentH);
			for (i = sliderPoints.length - 1; i >= 0; i--) {
				sliderPoints[pointer[i]].setNEWsize(currentH);
			}
			for (i = sliderPoints.length - 1; i >= 0; i--) { // for every point, from right to left
				if (sliderPoints[pointer[i]].getMayGrow() != true) { // if it doesn't have clearance to grow yet
					debugPrint("examine new situation for point (" + sliderPoints[pointer[i]].getX() + "," + sliderPoints[pointer[i]].getY() + ")");
					if (checkNewSituation(sliderPoints, xPointArray, i) == false) { // check if the new situation would work
						mayContinue = false;
						maxH = currentH; // current becomes up
						i = -1; // quits loop
					}
				}
			}
			if (mayContinue) {
				minH = currentH;
			}
			for (i = sliderPoints.length - 1; i >= 0; i--) {
				sliderPoints[pointer[i]].setMayGrow(false);
			}
			debugPrint("new max and min: " + maxH + ", " + minH);
		}
		MapLabeler.nrOfLoops = loops;

		// FINAL LOOP TO PLACE ALL LABELS FOR THE MAX HEIGHT
		// Calculate rounded height or width
		debugPrint("____________________LAST LOOP_________________________");
		// for (i = sliderPoints.length -1; i >= 0; i--)
		// {sliderPoints[pointer[i]].setNEWsize(minH);}
		// double MAXh = minH;
		// double MAXw = sliderPoints[0].getNEWRightX() -
		// sliderPoints[0].getNEWLeftX();
		// if (MAXh % 0.5 > MAXw % 0.5) { //height is closer to a half number
		// MAXh = ((double)Math.round(MAXh*2))/2;
		// }
		// else if (MAXh % 0.5 < MAXw % 0.5) { //width is closer to a half
		// number
		// MAXh = (((double)Math.round(MAXw*2))/2)/aspectRatio;
		// }
		// else { //both equally close to a half number
		// MAXh = ((double)Math.round(MAXh*2))/2;
		// MAXw = ((double)Math.round(MAXw*2))/2;
		// }
		time = System.nanoTime();
		for (i = sliderPoints.length - 1; i >= 0; i--) {
			sliderPoints[pointer[i]].setNEWsize(minH);
		}
		for (i = sliderPoints.length - 1; i >= 0; i--) { // for every point, from right to left
			if (sliderPoints[pointer[i]].getMayGrow() != true) { // if it doesn't have clearance to grow yet
				if (checkNewSituation(sliderPoints, xPointArray, i) == false) { // check if the new situation would work current becomes up
					i = -1;
					System.out.println("WOOPS1"); // NIET GOED HELEMAAL NIET
													// GOED
				}
			}
		}
		// Apply changes made in FINAL LOOP
		for (i = sliderPoints.length - 1; i >= 0; i--) {
			sliderPoints[pointer[i]].applyChanges();
		}
		// height = ((float)((long)(minH*1000000000)))/1000000000;
		// height = Math.floor(minH*1000000000)*1000000000;
		// height = minH;
		BigDecimal lel = new BigDecimal(minH);
		lel = lel.setScale(15, RoundingMode.FLOOR);
		height = lel.doubleValue();
		for (i = sliderPoints.length - 1; i >= 0; i--) {
			sliderPoints[pointer[i]].setNEWsize(height);
		}
		for (i = sliderPoints.length - 1; i >= 0; i--) { // for every point, from right to left
			if (sliderPoints[pointer[i]].getMayGrow() != true) { // if it doesn't have clearance to grow yet
				if (checkNewSituation(sliderPoints, xPointArray, i) == false) { // check if the new situation would work current becomes up
					i = -1;
					System.out.println("WOOPS2"); // NIET GOED HELEMAAL NIET
													// GOED
				}
			}
		}
		MapLabeler.placementTime = (System.nanoTime() - time);
	}

	boolean checkNewSituation(SliderPoint[] sArray, int[] pointer, int pointLoc) {
		long time = System.nanoTime();
		int i = pointLoc;
		int j = pointLoc - 1;
		if (i == 0) {
			debugPrint("clear, the last point");
			sliderPoints[pointer[i]].setMayGrow(true);
			MapLabeler.totalAvgColTime += System.nanoTime() - time;
			return true;
		} // the last label is always moveable
		while (j >= 0
				&& (sliderPoints[pointer[j]].getX() > sliderPoints[pointer[i]].getNEWLeftX()
						- (sliderPoints[pointer[i]].getNEWRightX() - sliderPoints[pointer[i]].getNEWLeftX()))) { // bound for possible collisions
			debugPrint(" point (" + sliderPoints[pointer[i]].getX() + "," + sliderPoints[pointer[i]].getY() + ") may collide with ("
					+ sliderPoints[pointer[j]].getX() + "," + sliderPoints[pointer[j]].getY() + ")");
			if ((sliderPoints[pointer[i]].getNEWLeftX() < sliderPoints[pointer[j]].getNEWRightX())
					&& ((sliderPoints[pointer[i]].getNEWTopY() >= sliderPoints[pointer[j]].getNEWTopY() && sliderPoints[pointer[i]].getY() <= sliderPoints[pointer[j]]
							.getNEWTopY()) || sliderPoints[pointer[i]].getNEWTopY() >= sliderPoints[pointer[j]].getY()
							&& sliderPoints[pointer[i]].getY() <= sliderPoints[pointer[j]].getY())) { // check collision

				debugPrint("  collision detected");
				double toShift = sliderPoints[pointer[j]].getNEWRightX() - sliderPoints[pointer[i]].getNEWLeftX();
				if (toShift <= (sliderPoints[pointer[j]].getNEWRightX() - sliderPoints[pointer[j]].getX())) { // check if current label can shift
					sliderPoints[pointer[j]].setNEWLeftX(sliderPoints[pointer[j]].getNEWLeftX() - toShift);
					sliderPoints[pointer[j]].setNEWRightX(sliderPoints[pointer[j]].getNEWRightX() - toShift);
					sliderPoints[pointer[j]].setNEWDirection("LEFT"); // if so, shift
					debugPrint("  shifting label with: " + toShift);
				}
				// else if (toShift == (sliderPoints[pointer[j]].getNEWRightX()
				// - sliderPoints[pointer[j]].getX())) { //if it fits exactly,
				// shift, but finish
				// sliderPoints[pointer[j]].setNEWLeftX(sliderPoints[pointer[j]].getNEWLeftX()
				// - toShift);
				// sliderPoints[pointer[j]].setNEWRightX(sliderPoints[pointer[j]].getNEWRightX()
				// - toShift);
				// sliderPoints[pointer[j]].setNEWDirection("LEFT");
				// System.out.println("MAXIMUM HEIGHT REACHED: " +
				// (sliderPoints[pointer[i]].getTopY() -
				// sliderPoints[pointer[i]].getY()) );
				// return false;
				// }
				else {
					sliderPoints[pointer[i]].setMayGrow(false);
					debugPrint("  not clear, shifting would sever the label from its point");
					// debugPrint("MAXIMUM HEIGHT REACHED: " +
					// (sliderPoints[pointer[i]].getTopY() -
					// sliderPoints[pointer[i]].getY()) );
					return false; // if not, we have reached our max size
				}
				if (checkNewSituation(sliderPoints, pointer, j) == true) { // check if colliding label can move
					sliderPoints[pointer[i]].setMayGrow(true);
					debugPrint("  clear, the others made room");
					MapLabeler.totalAvgColTime += System.nanoTime() - time;
					return true; // if so, than you can move too
				} else {
					sliderPoints[pointer[i]].setMayGrow(false);
					debugPrint("  not clear, the others couldnt made room");
					MapLabeler.totalAvgColTime += System.nanoTime() - time;
					return false;
				} // if not, you can't grow either
			}
			j--; // if there are no collisions, you can grow
			debugPrint("  height: " + (sliderPoints[pointer[i]].getNEWTopY() - sliderPoints[pointer[i]].getY()) + " new j: " + j);
		}
		sliderPoints[pointer[i]].setMayGrow(true);
		debugPrint("  clear, no collision left/found");
		MapLabeler.totalAvgColTime += System.nanoTime() - time;
		return true; // all collisions are solved
	}

	/**
	 * 
	 * @param labels
	 *            , list of all labels, including alive and dead labels.
	 * @param clauses
	 *            , empty arrayList of clauses, points with dead labels will get a special clause.
	 * @param tree
	 *            , the quadtree used to find collisions.
	 * @param height
	 *            , the height of the labels to check for.
	 * @return a hashmap of all labels to an arraylist of labels they collide with. null if a dead point exists.
	 */
	
	public ArrayList<Clause> findCollisions2pos(ArrayList<Label> labels, HashMap<PosPoint, Orientation> validOrientation, QuadTree tree, double height) {
		ArrayList<Clause> clauses = new ArrayList<Clause>();
		// long time = System.nanoTime();
		tree.init(labels, height, aspectRatio, rangeX, rangeY);// now only use the leftover labels
		// System.out.println("initialisation:" + (System.nanoTime()-time));
		// setIntersectionsQuad(tree, labels);//gives all labels the correct
		// boolean value for intersection.

		ArrayList<Label> overlap;// the list of labels the labels overlaps with
		// time = System.nanoTime();
		for (Label l : labels) {// for all retained labels
			// long time2 = System.nanoTime();
			overlap = findIntersectionQuad(tree, l);// find the labels this
													// label intersects with
			// System.out.println("findIntersectionQuad: " +
			// (System.nanoTime()-time2));
			for (Label l2 : overlap) {
				clauses.add(new Clause(l.toClause().negation(), l2.toClause().negation()));
			}
		}
		// System.out.println("fullDetection: " + (System.nanoTime()-time));
		return clauses;// return the list of collisions
	}

	/**
	 * 
	 * @param shift
	 *            : given shift of the label
	 * @param top
	 *            : if the label is at the top
	 * @return the connected Orientation to this configuration.
	 */
	public Orientation getPosition(double shift, boolean top) {
		if (shift == 0) {
			if (top) {
				return Orientation.NW;
			} else {
				return Orientation.SW;
			}
		} else {
			if (top) {
				return Orientation.NE;
			} else {
				return Orientation.SE;
			}
		}

	}

	/**
	 * 
	 * @param collisions
	 *            : list of all collisions found by the findCollisions method.
	 * @return returns the clauses generated by converting the collisions to clauses.
	 */

	public double getAspectRatio() {
		return aspectRatio;
	}

	public int getNumberOfPoints() {
		return numberOfPoints;
	}

	public double getHeight() {
		return height;
	}

	public SliderPoint[] getSliderPoints() {
		return sliderPoints;
	}

	public PosPoint[] getPosPoints() {
		return posPoints;
	}

	public boolean intersects(Label la, Label lb) {

		Rectangle2D a = la.getRect();
		Rectangle2D b = lb.getRect();

		double al = a.getX() + delta;
		double ar = a.getX() + a.getWidth() - delta;
		double ab = a.getY() + delta;
		double at = a.getY() + a.getHeight() - delta;

		double bl = b.getX() + delta;
		double br = b.getX() + b.getWidth() - delta;
		double bb = b.getY() + delta;
		double bt = b.getY() + b.getHeight() - delta;

		if (al == bl && ar == br && ab == bb && at == bt && la.getBoundPoint() != lb.getBoundPoint()) {
			return true;
		}

		if (la.getBoundPoint() == lb.getBoundPoint()) {
			// System.out.print("Same boundpoint");
			return false;
		}

		if (al == bl && ar == br && ab == bb && at == bt) {
			return false;
		}
		if (ar == br || al == bl) {
			if (at > bb && at < bt)
				return true;
			if (ab > bb && ab < bt)
				return true;
		} else if (at == bt || ab == bb) {
			if (al > bl && al < br)
				return true;
			if (ar > bl && ar < br)
				return true;
		} else if (bl < ar && bl > al) {
			if (bt < at && bt > ab)
				return true;
			else if (bb < at && bb > ab)
				return true;
		} else if (br > al && br < ar) {
			if (bt < at && bt > ab)
				return true;
			else if (bb < at && bb > ab)
				return true;
		} else if ((bl < al && bl > al) || (br < ar && br > ar)) {
			if (bt > ab && bt < at)
				return true;
			else if (bb < at && bb > ab)
				return true;
			else if (ab > bb && ab < bt)
				return true;
			else if (at > bb && at < bt)
				return true;
		} else if ((at < bt && at > bt) || (ab < bb && ab > bb)) {
			if (ar > bl && ar < br)
				return true;
			else if (al > bl && al < br)
				return true;
			else if (br > al && br < ar)
				return true;
			else if (bl > al && bl < ar)
				return true;
		}

		return false;

	}

	public void resetIntersections(ArrayList<Label> labels) {
		for (int i = 0; i < labels.size(); i++) {
			Label l = labels.get(i);
			l.setHasIntersect(false);
		}
	}

	public int countIntersections(QuadTree tree, Label[] labels) {
		int amount = 0;
		for (int i = 0; i < labels.length; i++) {
			Label l = labels[i];
			if (!l.isHasIntersect()) {
				ArrayList<Label> returnObjects = new ArrayList<Label>();
				tree.retrieve(returnObjects, l);
				for (Label label2 : returnObjects) {
					// label2.hasIntersect = false;

					if (intersects(l, label2)) {
						l.setHasIntersect(true);
						amount++;
						if (!label2.isHasIntersect()) {
							label2.setHasIntersect(true);
							amount++;
						}
					}
				}
			}
		}
		return amount;
	}

	/**
	 * 
	 * @param quadtree
	 *            generated with the quadtree constructor. sets the correct hasIntersect values for the labels.
	 */
	public void setIntersectionsQuad(QuadTree tree, ArrayList<Label> labels) {
		for (int i = 0; i < labels.size(); i++) {
			Label l = labels.get(i);

			if (!l.isHasIntersect()) {

				ArrayList<Label> returnObjects = new ArrayList<Label>();
				tree.retrieve(returnObjects, l);

				for (Label label2 : returnObjects) {
					if (Math.abs(l.getBoundPoint().getX() - label2.getBoundPoint().getX()) <= 2 * l.getRect().getWidth()) {
						if (Math.abs(l.getBoundPoint().getY() - label2.getBoundPoint().getY()) <= 2 * l.getRect().getHeight()) {
							if (intersects(l, label2)) {
								l.setHasIntersect(true);
								label2.setHasIntersect(true);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param quadtree
	 *            generated with the quadtree constructor.
	 * @param the
	 *            label you check for.
	 * @return returns if the label intersects with other labels.
	 */
	public boolean hasIntersectionQuad(QuadTree tree, Label l) {
		/*
		 * ArrayList<Label> returnObjects = new ArrayList<Label>(); tree.retrieve(returnObjects, l); for(Label l2: returnObjects){ if(Math .abs(l.getBoundPoint().getX()-l2.getBoundPoint().getX())<=2*l.getRect ().getWidth()){ if(Math.abs(l.getBoundPoint().getY()-l2.getBoundPoint( ).getY())<=2*l.getRect().getHeight()){ if(intersects(l, l2)){ return true; } } } } return false;
		 */
		return l.isHasIntersect();
	}

	/**
	 * 
	 * @param quadtree
	 *            generated with the quadtree constructor.
	 * @param the
	 *            label you check for.
	 * @return returns a list of labels that intersect with the given label.
	 **/
	public ArrayList<Label> findIntersectionQuad(QuadTree tree, Label l) {
		ArrayList<Label> possibleInters = new ArrayList<Label>();
		ArrayList<Label> intersections = new ArrayList<Label>();
		tree.retrieve(possibleInters, l);
		for (Label l2 : possibleInters) {
			if (Math.abs(l.getBoundPoint().getX() - l2.getBoundPoint().getX()) <= 2 * l.getRect().getWidth()) {
				if (Math.abs(l.getBoundPoint().getY() - l2.getBoundPoint().getY()) <= 2 * l.getRect().getHeight()) {
					if (intersects(l, l2)) {
						l.setHasIntersect(true);
						l2.setHasIntersect(true);
						intersections.add(l2);
					}
				}
			}
		}
		return intersections;
	}

	public boolean contains(Rectangle2D rec, double x, double y, boolean in) {
		double mx = rec.getX();// x coordinate of the label
		double my = rec.getY();// y coordinate of the label
		double w = rec.getWidth();// width of the label
		double h = rec.getHeight();// height of the label

		if (in) {// different comparisons for points IN (can be on the edge), or
					// point OUT (can't be on the edge).
			return w > 0 && h > 0 && x > mx && x < mx + w && y > my && y < my + h;
		} else {
			return w >= 0 && h >= 0 && x >= mx && x <= mx + w && y >= my && y <= my + h;
		}
	}

	public void findIntersections(ArrayList<Label> labels) {
		for (int i = 0; i < labels.size(); i++) {
			Label l1 = labels.get(i);

			double ayT = l1.getBoundPoint().getY() + (l1.isTop() ? height : 0);
			double ay = ayT - height;
			double axR = l1.getBoundPoint().getX() + height * aspectRatio * l1.getShift();
			double ax = axR - height * aspectRatio;

			for (int j = i + 1; j < labels.size(); j++) {
				Label l2 = labels.get(j);

				double byT = l2.getBoundPoint().getY() + (l2.isTop() ? height : 0);
				double by = byT - height;
				double bxR = l2.getBoundPoint().getX() + height * aspectRatio * l2.getShift();
				double bx = bxR - height * aspectRatio;

				if ((ax < bxR && axR > bx) && (ay < byT && ayT > by)) {
					l1.setHasIntersect(true);
					l2.setHasIntersect(true);
				}
			}
		}
	}

	/**
	 * 
	 * @param clauseList
	 *            : list of all clauses to consider
	 * @return returns if this array of clauses is satisfiable
	 */
	public boolean checkTwoSatisfiability(ArrayList<Clause> clauseList) {
		HashSet<String> variables = new HashSet<String>();// to be set of all string values of the labels.
		for (Clause clause : clauseList) {// for each clause in the clauselist
			variables.add(clause.getFirstStringValue());// add the first string value of the clause
			variables.add(clause.getSecondStringValue());// add the second string value of the clause
			// this will add all "variables" used in the 2-sat check.
		}

		DirectedGraph graph = new DirectedGraph();// make a new directed graph

		for (String variable : variables) {// for all variables in the variables set
			graph.addNode(new ClauseValue(variable, true));// make a node for the true variant of this variable
			graph.addNode(new ClauseValue(variable, false));// make a node for the false variant of this variable.
		}

		for (Clause clause : clauseList) {// for all clauses
			graph.addEdge(clause.getFirstValue().negation(), clause.getSecondValue());// add the implication (~a=>b)
			graph.addEdge(clause.getSecondValue().negation(), clause.getFirstValue());// add the implication (~b=>a)
		}

		HashMap<ClauseValue, Integer> stronglyConnected = stronglyConnectedComponents(graph);
		// save the strongly connected components as a mapping from label to the
		// number of the scc

		for (String variable : variables) {// for all variables used
			if (stronglyConnected.get(new ClauseValue(variable, true)).equals(stronglyConnected.get(new ClauseValue(variable, false)))) {
				// if the number of the scc is same for both the negation and the original, we have a logical contradiction, thus false
				return false;// so, return false, as the 2-sat is not satisfied.
			}
		}

		minGraph = graph;// make this graph the minGraph.
		connectedComponents = stronglyConnected;// save the strongly connected components.

		return true;// in any other case return true, as there exists a solution to this problem.
	}

	/**
	 * 
	 * @param graph
	 *            : the directed graph.
	 * @return a mapping from clauseValues to integers, with integer being the connected component the clauseValue is part of
	 */
	public HashMap<ClauseValue, Integer> stronglyConnectedComponents(DirectedGraph graph) {
		// debugPrint("graph:" + graph.getGraph().toString());//DEBUG
		Stack<ClauseValue> visitOrder = dfsOrder(reverseGraph(graph));
		// do a depth first search on the reversed iteration order of the graph,
		// which will be your visit order.

		HashMap<ClauseValue, Integer> result = new HashMap<ClauseValue, Integer>();// the result hashmap
		int scc = 0;// number for the strongly connected component
		while (!visitOrder.isEmpty()) {// if an element is still available on the stack
			ClauseValue startPoint = visitOrder.pop();// pop an element from the stack, make it the start point

			if (!result.containsKey(startPoint)) {// if the result does not contain this point yet:
				markReachableNodes(startPoint, graph, result, scc);// check which nodes are reachable from this node.
				++scc;// increase the scc number.
			} else {
				// do nothing, we already considered this point
			}
		}

		return result;// return the list of sccs.
	}

	/**
	 * 
	 * @param graph
	 *            : the directed graph
	 * @return the directed graph in reversed order
	 */
	private static DirectedGraph reverseGraph(DirectedGraph graph) {
		DirectedGraph result = new DirectedGraph();// generate a new directed graph

		for (ClauseValue node : graph) {// for all nodes in the graph
			result.addNode(node);// add the node to the new result graph
		}

		for (ClauseValue node : graph) {// for all nodes in the graph
			for (ClauseValue endpoint : graph.edgesFrom(node)) {// for all endpoints of this node
				result.addEdge(endpoint, node);// add the edge from the endpoint to that node, which is in the other direction.
			}
		}
		return result;// return the result
	}

	/**
	 * 
	 * @param graph
	 *            : the directed graph
	 * @return the depth first search order of the graph
	 */
	private Stack<ClauseValue> dfsOrder(DirectedGraph graph) {
		Stack<ClauseValue> result = new Stack<ClauseValue>();// the stack the result will be stored in

		HashSet<ClauseValue> visited = new HashSet<ClauseValue>();// the nodes that have been visited already

		for (ClauseValue node : graph) {// for all nodes in the graph
			explore(node, graph, result, visited);// recursively explore
		}
		return result;// return the result
	}

	/**
	 * 
	 * @param node
	 *            : the node that is used
	 * @param graph
	 *            : the directed graph
	 * @param result
	 *            : the stack of results
	 * @param visited
	 *            : the set of visited nodes
	 */
	private void explore(ClauseValue node, DirectedGraph graph, Stack<ClauseValue> result, Set<ClauseValue> visited) {

		if (!visited.contains(node)) {// if visited does not contain the node yet
			visited.add(node);// add the node to visited
			for (ClauseValue endpoint : graph.edgesFrom(node)) {// for all endpoints of the edges from this node
				explore(endpoint, graph, result, visited);// recursively explore the graph
			}
			result.push(node);// push the node onto the stack
		}
	}

	/**
	 * 
	 * @param node
	 *            : the node that is used
	 * @param graph
	 *            : the directed graph
	 * @param result
	 *            : the stack of results
	 * @param scc
	 *            : the number of this strongly connected component
	 */
	private void markReachableNodes(ClauseValue node, DirectedGraph graph, HashMap<ClauseValue, Integer> result, int scc) {

		if (!result.containsKey(node)) {// if the element was not present in the list yet:
			result.put(node, scc);// add this node to the result list
			for (ClauseValue endpoint : graph.edgesFrom(node)) {// for all endpoints for the edges of the node
				markReachableNodes(endpoint, graph, result, scc);// recursively mark the reachable nodes
			}
		}
	}

	public void checkFinalSolution() throws Exception {
		if(pModel == PlacementModel.FOURPOS){
			
		}
		
		
		else if (pModel == PlacementModel.ONESLIDER) {
			labels = new Label[numberOfPoints];
			for (int i = 0; i < labels.length; i++) {
				SliderPoint s = sliderPoints[i];
				PosPoint p = new PosPoint(s.getX(), s.getY());
				Label l = new Label(p, (float) s.getS(), true);
				labels[i] = l;
			}
		} else {
			labels = new Label[numberOfPoints];
			for (int i = 0; i < labels.length; i++) {
				PosPoint p = posPoints[i];
				Orientation o = p.getPosition();
				boolean top = true;
				float shift = 0.0f;

				if (o == Orientation.NE) {
					top = true;
					shift = 1.0f;
				} else if (o == Orientation.NW) {
					top = true;
					shift = 0.0f;
				} else if (o == Orientation.SE) {
					top = false;
					shift = 1.0f;
				} else if (o == Orientation.SW) {
					top = false;
					shift = 0.0f;
				}

				Label l = new Label(p, shift, top);
				labels[i] = l;
			}
		}
		if (finalIntersectionTest() > 0) {
			throw new Exception("Intersections detected in final output.");
		}
	}

	public int finalIntersectionTest() {
		// boolean print = true;
		// print = false;

		int range = 10000;
		int amount = 0;
		Label[] otherLabels = new Label[labels.length];

		for (int i = 0; i < labels.length; i++) {
			otherLabels[i] = new Label(labels[i]);
			otherLabels[i].setHasIntersect(false);
		}
		
		QuadTree quad;
		if(pModel.equals(PlacementModel.TWOPOS)){
			quad = new QuadTree(0, new Rectangle(0, 0, range, range),false);			
		}
		else{
			quad = new QuadTree(0, new Rectangle(0, 0, range, range),true);	
		}

		quad.init(otherLabels, height, aspectRatio, range, range);// initialize the quadtree

		ArrayList<Label> intersecting = new ArrayList<Label>();

		for (int i = 0; i < otherLabels.length; i++) {
			Label l = otherLabels[i];

			if (!intersecting.contains(l)) {
				ArrayList<Label> returnObjects = new ArrayList<Label>();
				quad.retrieve(returnObjects, l);
				for (int j = 0; j < returnObjects.size(); j++) {
					Label label2 = returnObjects.get(j);
					if (intersects(l, label2)) {
						if (!intersecting.contains(l)) {
							intersecting.add(l);
							amount++;
						}
						if (!intersecting.contains(label2)) {
							intersecting.add(label2);
							amount++;
						}
					}
				}
			}
		}
		/*
		 * if(amount > 0){ System.out.println(intersecting); }
		 */
		
		return amount;
	}
}