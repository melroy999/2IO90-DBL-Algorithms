import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

	private HashMap<ClauseValue, Integer> connectedComponents;
	private DirectedGraph minGraph;
	private HashMap<PosPoint, Orientation> validConfiguration;
	
	private Map<ClauseValue, PosPoint> clauseToPoint;
	
	private SliderPoint[] sliderPoints;
	public int[] xPointArray;  //slider pointers
	private PosPoint[] posPoints;

	private double delta = 0.001;//difference of border
	
	private boolean debug = !true;

	public Plane(double aspectRatio, SliderPoint[] points){
		this.aspectRatio = aspectRatio;
		this.numberOfPoints = points.length;
		this.sliderPoints = points;
		height = (aspectRatio < 1 ? aspectRatio : 1/aspectRatio);
	}

	public Plane(double aspectRatio, PosPoint[] points){
		this.aspectRatio = aspectRatio;
		this.numberOfPoints = points.length;
		this.posPoints = points;
	}
	
	public void debugPrint(String text){
		if(MapLabeler.local && debug){
			System.out.println(text);
		}
	}
	
	public void addLabel(Label label, ArrayList<Label> labels){
		label.getBoundPoint().addLabel(label);
		labels.add(label);
	}
	
	/**
	 * 
	 * @return The solution to the 2pos problem. It first calculates the maximum height
	 * and the orientation of every label. This method returns the orientation array.
	 * Where every element is one of PlacementModel.NE, PlacementModel.NW.
	 */
	public PosPoint[] find2PosSolution(){
	    int[] xSortedOrder = MergeSort.sort(posPoints);//sorting the points on x-cor, referencing by index in this array.

	    int range = 10000;//the range of the coordinates from 0 to range
		QuadTree quad = new QuadTree(0, new Rectangle(0,0,range+1,range+1));//new quadtree with (top) level 0 and dimensions (range+1)^2
		
		double minHeight = (aspectRatio < 1) ? 1 : (1/aspectRatio);//minimal height		
		double maxHeight = MaxSize.getMaxPossibleHeight(posPoints, xSortedOrder, aspectRatio, PlacementModel.TWOPOS);//find the max height
		height = maxHeight;//height to use is the average of max and min height
		double lastHeight = 0;//a value to find out if you are checking for the same height twice in succession.
		
		ArrayList<Label> allLabels = new ArrayList<Label>();//all labels will be stored in this arrayList, will be copied later after each iteration.
		
		clauseToPoint = new HashMap<ClauseValue, PosPoint>();
		
		for(PosPoint p :posPoints){//make the top labels for all points.
			Label NE = new Label(p, 1, true);
			addLabel(NE, allLabels);//shift=1 and top=true gives us the NE label;
			clauseToPoint.put((NE).toClause(), p);
			
			Label NW = new Label(p, 0, true);
			addLabel(NW, allLabels);//shift=0 and top=true gives us the NW label;
			clauseToPoint.put((NW).toClause(), p);
		}
		
		while(lastHeight != height){//as long as the height is not equal to the last checked height
			HashMap<PosPoint, Orientation> validOrientation = new HashMap<PosPoint, Orientation>();
			ArrayList<Label> labels = new ArrayList<Label>(allLabels);//all labels will be stored in this arrayList. 	
			ArrayList<Clause> clauses = new ArrayList<Clause>();//a list which will initially contain additional clauses.
			
			//the additional clauses are required to fix the value of a dead label to the negation of the clauseValue of that label.
			HashMap<Label, ArrayList<Label>> collisions = findCollisions2pos(labels, clauses, validOrientation, quad, height);
			//get the list of collisions	
			
			if (collisions!=null){//collisions will return null if a point with only dead labels exists
				clauses.addAll(getClauses(collisions));//add the clauses generated with the collisions to the clauses list.
				if(checkTwoSatisfiability(clauses)){//if a satisfiable configuration exists
					minHeight = height;//this height will be valid, so the minimum height becomes this height.
					validConfiguration = new HashMap<PosPoint, Orientation>(validOrientation);
				}
				else{//if no solution can be found with 2-sat
					maxHeight = height;
					//this height has no solution, so the maximum found height for which this does not work is now height
				}
			}
			else {//if a point has only dead labels
				maxHeight = height;
				//this height has no solution, so the maximum found height for which this does not work is now height
			}
			
			lastHeight = height;//remember which height was used this iteration.
			
			
			/*
			 * Calculate the next candidate value, considering that the distance between points is always an integer.
			 * This means that the height or the width (or both) MUST be divisible by 0.5.
			 */
			
			height = (maxHeight+minHeight)/2;//calculate the average of the maxHeight and minHeight
			double width = height * aspectRatio;//calculate the width.
			
			height = (Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width)))? roundToHalf(height) : roundToHalf(width)/aspectRatio;
			//(Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width))): 
			//	Find if the distance to next height or the next width is smaller than the other.
		}
		
		height = minHeight;
		
		for(PosPoint p : posPoints){
			if(validConfiguration.containsKey(p)){
				p.setPosition(validConfiguration.get(p));
			}
		}
		
		//validConfiguration;
		
		Stack<ClauseValue> order = dfsOrder(reverseGraph(minGraph));
		debugPrint("dfs order:" + order.toString());
		
		while(!order.isEmpty()){
			ClauseValue next = order.pop();
			getNext(next);
		}
		
		return posPoints;//return the array of points, now with correct positions.
	}
	
	public double roundToHalf(double d){
		return Math.round(2*d)/2d;//returns the rounded value of 2*d, divided by double type value 2.
	}
	
	public void getNext(ClauseValue next){
		if(clauseToPoint.get(next).getPosition()==null){
			clauseToPoint.get(next).setPosition(next.isPositive() ? 1 : 0,true);
			if(!minGraph.edgesFrom(next).isEmpty()){
				for(ClauseValue value : minGraph.edgesFrom(next)){
					if(connectedComponents.get(value).equals(connectedComponents.get(next))){
						getNext(value);
					}	
				}	
			}
		}
	}

	public Label[] labels;
	
	Random r = new Random();
	public Label[] find4PosSolutionSA(){
		
		
		int range = 10000;
		
		
		double coolingRate = 0.003;
		
		LabelConfiguration best = null;
		int bestEnergy;
		
		
		
		//double maxHeight = 10000;
		
		int[] xSortedOrder = MergeSort.sort(posPoints);//sorting the points on x-cor, referencing by index in this array.
		double minHeight = (aspectRatio < 1) ? 1 : (1/aspectRatio);//minimal height		
		double maxHeight = MaxSize.getMaxPossibleHeight(posPoints, xSortedOrder, aspectRatio, PlacementModel.FOURPOS);//2x the maximal height, so that we start with the calculated max-height in the loop.
		maxHeight = 100;
		
		height = maxHeight;//height to use is the average of max and min height
		double lastHeight = 0;
		
		LabelConfiguration finalBest = null;
		int finalBestEnergy = Integer.MAX_VALUE;
		double finalHeight = 0;
		
		while(lastHeight != height){
			System.out.println(height);
			//height = 10;
			double temp = 10000;
			
			LabelConfiguration current = new LabelConfiguration(posPoints);
			
			best = new LabelConfiguration(current.getLabels());
			bestEnergy = calculateScore(best);
			
			//System.out.println("BEst: " + bestEnergy);
			labels = current.getLabels();
			
			while(temp > 1 && bestEnergy > 0){
				//System.out.println(temp);
				
				int currentEnergy = calculateScore(current);
				//System.out.println("old: " + current);
				
				
				LabelConfiguration newSolution = new LabelConfiguration(current.getLabels());
				int position = (int) (newSolution.labelSize() * r.nextDouble());
				newSolution.change(position);
				
				int neighbourEnergy = calculateScore(newSolution);
				
				//System.out.println("new: " + newSolution);
				
				
				if (calculateAcceptance(currentEnergy, neighbourEnergy, temp) > r.nextDouble()) {
					//System.out.println("new for " + neighbourEnergy);
	                current = new LabelConfiguration(newSolution.getLabels());
	            }
				else {
					//System.out.println("NOT NEW FOR " + neighbourEnergy);
				}
				if (neighbourEnergy < bestEnergy) {
					//System.out.println("#################### New best" + neighbourEnergy);
					//System.out.println("     " + newSolution);
	                best = new LabelConfiguration(newSolution.getLabels());
	                bestEnergy = neighbourEnergy;
	            }
				
				
				
				temp *= 1-coolingRate;
	            
				
			}
			
			if(bestEnergy == 0){
				
				if(finalHeight < height){
					//System.out.println("CHANGE THE FINAL THING ######################################3");
					finalBestEnergy = bestEnergy;
					finalBest = new LabelConfiguration(best.getLabels());
					finalHeight = height;
				}
				
				minHeight = height;
			}
			else {
				maxHeight = height;
			}
			
			lastHeight = height;
			
			height = (maxHeight+minHeight)/2;//height to use is the average of max and min height
			double width = height * aspectRatio;
			
			height = (Math.abs(height-roundToHalf(height))<Math.abs(width-roundToHalf(width)))? roundToHalf(height) : roundToHalf(width)/aspectRatio;
			
			//System.out.println("Best " + best);
			//System.out.println("Best: " + bestEnergy);
			
			
		}
		height = minHeight;
		System.out.println("FINAL HEIGHT: " + height);
		System.out.println("FINAL BEST: " + finalBest);
		return finalBest.getLabels();
	}
	
	private double calculateAcceptance(int oldEnergy, int newEnergy, double temperature){
		//System.out.println("old: " + oldEnergy);
		//System.out.println("new: " + newEnergy);
		
		if(newEnergy < oldEnergy){
			//System.out.println("better");
			return 1.0;
		}
		return Math.exp((oldEnergy - newEnergy) / temperature);
	}
	
	
	private int calculateScore(LabelConfiguration config){
		int range = 10000;
		QuadTree quad = new QuadTree(0, new Rectangle(0,0,range,range));
		quad.init(config.getLabels(), height, aspectRatio, 10000);//initialize the quadtree
		int amount = countIntersections(quad, config.getLabels());//gives all labels the correct boolean value for intersection.
		//System.out.println(amount);
		return amount;
	}
	
	
	
	
	/**
	 * 
	 * @return The solution to the 4pos problem. It first calculates the maximum height
	 * and the orientation of every label. This method returns the orientation array.
	 */
	public PosPoint[] find4PosSolution(){
		return posPoints;
	}
	
	public SliderPoint[] find1SliderSolution(){
		xPointArray = MergeSort.sort(sliderPoints);
		CalcSlider(sliderPoints,xPointArray);
		return sliderPoints;
	}
	
	void CalcSlider(SliderPoint[] sArray, int[] pointer) {  
		int i;																									//sliderPoints must be sorted on x-coordinates
		double minH = 0;
		double maxH = MaxSize.getMaxPossibleHeight(sArray, pointer, aspectRatio, PlacementModel.ONESLIDER);
		double T = 0.008;
		delta= maxH/(Math.pow(2,(1000/(sArray.length * T))));
		System.out.println("Precision: " + delta);
		System.out.println("MaxSize gives: " + maxH);
		double currentH;
		while (maxH-minH >= delta) {
			boolean mayContinue = true;
			currentH = (maxH + minH)/2;
			debugPrint("Current: " + currentH);
			for (i = sliderPoints.length -1; i >= 0; i--) {sliderPoints[pointer[i]].setNEWsize(currentH);}
			for (i = sliderPoints.length -1; i >= 0; i--) {											//for every point, from right to left
				if ( sliderPoints[pointer[i]].getMayGrow() != true ) {								//if it doesn't have clearance to grow yet
					debugPrint("examine new situation for point (" + sliderPoints[pointer[i]].getX() + "," + sliderPoints[pointer[i]].getY() + ")" );
					if ( checkNewSituation(sliderPoints, xPointArray, i) == false ) {					//check if the new situation would work	
						mayContinue = false;
						maxH = currentH;															//current becomes up
						i = -1;																		//quits loop
					}
				}
			}
			if (mayContinue) {
				minH = currentH;				
			}
			for (i = sliderPoints.length -1; i >= 0; i--) {
				sliderPoints[pointer[i]].setMayGrow(false);
			}
			debugPrint("new max and min: " + maxH + ", " +  minH);
		}
		
		//FINAL LOOP TO PLACE ALL LABELS FOR THE MAX HEIGHT
		//Calculate rounded height or width
		debugPrint("____________________LAST LOOP_________________________");
//		for (i = sliderPoints.length -1; i >= 0; i--) {sliderPoints[pointer[i]].setNEWsize(minH);}
//		double MAXh = minH;
//		double MAXw = sliderPoints[0].getNEWRightX() - sliderPoints[0].getNEWLeftX();
//		if 		(MAXh % 0.5 > MAXw % 0.5) {				//height is closer to a half number
//			MAXh = ((double)Math.round(MAXh*2))/2;
//		}
//		else if (MAXh % 0.5 < MAXw % 0.5) {				//width is closer to a half number
//			MAXh = (((double)Math.round(MAXw*2))/2)/aspectRatio;
//		}
//		else {											//both equally close to a half number
//			MAXh = ((double)Math.round(MAXh*2))/2;
//			MAXw = ((double)Math.round(MAXw*2))/2;
//		}
		for (i = sliderPoints.length -1; i >= 0; i--) {sliderPoints[pointer[i]].setNEWsize(minH);}
		for (i = sliderPoints.length -1; i >= 0; i--) {											//for every point, from right to left
			if ( sliderPoints[pointer[i]].getMayGrow() != true ) {								//if it doesn't have clearance to grow yet
				if ( checkNewSituation(sliderPoints, xPointArray, i) == false ) {				//check if the new situation would work																//current becomes up
					i = -1;																		
					System.out.println("WOOPS DIT ZAG IK NIET AANKOMEN");						//NIET GOED HELEMAAL NIET GOED
				}
			}
		}
		//Apply changes made in FINAL LOOP
		for (i = sliderPoints.length -1; i >= 0; i--) {
			sliderPoints[pointer[i]].applyChanges();
		}
		//height = ((float)((long)(minH*1000000000)))/1000000000;
		//height = Math.floor(minH*1000000000)*1000000000;
		//height = minH;
		BigDecimal lel = new BigDecimal(minH);
		lel = lel.setScale(10, RoundingMode.FLOOR);
		height = lel.doubleValue();
	}
	
	boolean checkNewSituation(SliderPoint[] sArray, int[] pointer, int pointLoc) {
		int i = pointLoc;
		int j = pointLoc - 1;
		if (i==0) {
			debugPrint("clear, the last point");
			sliderPoints[pointer[i]].setMayGrow(true);
			return true;
			}																//the last label is always moveable			
		while (j >= 0  && (sliderPoints[pointer[j]].getX() > sliderPoints[pointer[i]].getNEWLeftX()- (sliderPoints[pointer[i]].getNEWRightX() - sliderPoints[pointer[i]].getNEWLeftX()))) {		//bound for possible collisions
			debugPrint(" point (" + sliderPoints[pointer[i]].getX() + "," + sliderPoints[pointer[i]].getY() + ") may collide with (" + sliderPoints[pointer[j]].getX() + "," + sliderPoints[pointer[j]].getY() + ")" );
			if ( (sliderPoints[pointer[i]].getNEWLeftX() <  sliderPoints[pointer[j]].getNEWRightX()) &&
				((sliderPoints[pointer[i]].getNEWTopY()  >= sliderPoints[pointer[j]].getNEWTopY()    &&
				  sliderPoints[pointer[i]].getY() 	   <= sliderPoints[pointer[j]].getNEWTopY())   ||
				  sliderPoints[pointer[i]].getNEWTopY()  >= sliderPoints[pointer[j]].getY() 		 &&
				  sliderPoints[pointer[i]].getY() 	   <= sliderPoints[pointer[j]].getY()) ) {			//check collision
				
				debugPrint("  collision detected");
				double toShift = sliderPoints[pointer[j]].getNEWRightX() - sliderPoints[pointer[i]].getNEWLeftX();
				if (toShift <= (sliderPoints[pointer[j]].getNEWRightX() - sliderPoints[pointer[j]].getX())) {	//check if current label can shift
					sliderPoints[pointer[j]].setNEWLeftX(sliderPoints[pointer[j]].getNEWLeftX() - toShift);
					sliderPoints[pointer[j]].setNEWRightX(sliderPoints[pointer[j]].getNEWRightX() - toShift);
					sliderPoints[pointer[j]].setNEWDirection("LEFT");													//if so, shift
					debugPrint("  shifting label with: " + toShift );
				}
//				else if (toShift == (sliderPoints[pointer[j]].getNEWRightX() - sliderPoints[pointer[j]].getX())) {		//if it fits exactly, shift, but finish
//					sliderPoints[pointer[j]].setNEWLeftX(sliderPoints[pointer[j]].getNEWLeftX() - toShift);
//					sliderPoints[pointer[j]].setNEWRightX(sliderPoints[pointer[j]].getNEWRightX() - toShift);
//					sliderPoints[pointer[j]].setNEWDirection("LEFT");
//					System.out.println("MAXIMUM HEIGHT REACHED: " + (sliderPoints[pointer[i]].getTopY() - sliderPoints[pointer[i]].getY()) );
//					return false;
//				}
				else {
					sliderPoints[pointer[i]].setMayGrow(false);
					debugPrint("  not clear, shifting would sever the label from its point");
					//debugPrint("MAXIMUM HEIGHT REACHED: " + (sliderPoints[pointer[i]].getTopY() - sliderPoints[pointer[i]].getY()) );
					return false;																					//if not, we have reached our max size 
					}
				if ( checkNewSituation(sliderPoints, pointer, j) == true ) {		//check if colliding label can move
					sliderPoints[pointer[i]].setMayGrow(true);
					debugPrint("  clear, the others made room");
					return true;																//if so, than you can move too
				}
				else{ 
					sliderPoints[pointer[i]].setMayGrow(false);
					debugPrint("  not clear, the others couldnt made room");
					return false;
					}																			//if not, you can't grow either
			}
			j--;												//if there are no collisions, you can grow
			debugPrint("  height: " + (sliderPoints[pointer[i]].getNEWTopY()-sliderPoints[pointer[i]].getY()) + " new j: " + j);
		}
		sliderPoints[pointer[i]].setMayGrow(true);
		debugPrint("  clear, no collision left/found");
		return true;  											//all collisions are solved
	}
	
	/**
	 * 
	 * @param labels, list of all labels, including alive and dead labels.
	 * @param clauses, empty arrayList of clauses, points with dead labels will get a special clause.
	 * @param tree, the quadtree used to find collisions.
	 * @param height, the height of the labels to check for.
	 * @return a hashmap of all labels to an arraylist of labels they collide with. null if a dead point exists.
	 */
	public HashMap<Label, ArrayList<Label>> findCollisions2pos(ArrayList<Label> labels, ArrayList<Clause> clauses, HashMap<PosPoint, Orientation> validOrientation, QuadTree tree, double height){			
		HashMap<Label, ArrayList<Label>> collisions = new HashMap<Label, ArrayList<Label>>();//make a new hashmap of labels to arraylist of labels
		if(posPoints!=null){//if we are not doing 1slider
			tree.init(labels, height, aspectRatio, 10000);//initialize the quadtree
			setIntersectionsQuad(tree, labels);//gives all labels the correct boolean value for intersection.
			
			ArrayList<Label> toRemove;//to avoid the editing iteration items while iterating, add the dead labels to an arraylist
			for(PosPoint p: posPoints){//for all points
				toRemove = new ArrayList<Label>();//make a new arrayList for toRemove arraylist.
				for(Label l: p.getLabels()){//for all labels in the point
					if(l.isHasIntersect()){//if the label collides with other labels
						if(!containsPoint(findIntersectionQuad(tree,l),l).isEmpty()){
							//if the list of contained points is not empty for the specified label
							toRemove.add(l);//add this label to the arraylist of dead labels
						}
					}
					else{
						//the label is safe, most likely alive.
						//alive check is done later, to assure that labels that became alive by deleting a dead label are also present. 
					}
				}
				if(toRemove.size()==2){//if somehow all labels are dead for a point
					return null;//return null as collision value, associated with this error
				}
				else{
					for(Label l: toRemove){//for all labels that have to be removed
						clauses.add(new Clause(l.toClause().negation(),l.toClause().negation()));
						//add a clause to the clauses list that will force the negation of this table to be true;
						validOrientation.put(l.getBoundPoint(),getPosition((l.getShift()-1)*-1,l.isTop()));
						//note that this point has a fixed position
						labels.remove(l);//remove the label from the labels list.
					}
				}
			}
			
			tree.init(labels, height, aspectRatio, 10000);//initialize the tree again, now without the dead labels.
			setIntersectionsQuad(tree, labels);//gives all labels the correct boolean value for intersection.
			
			ArrayList<Label> alive;//arrayList of all alive labels, to be removed after the loop is done.
			for(PosPoint p: posPoints){//for all points
				alive = new ArrayList<Label>();//make a new alive arraylist
				for(Label l: p.getLabels()){//for all labels in the point
					if(!l.isHasIntersect()){//if the label has no intersections
						alive.add(l);//it is alive, so add to the alive list
					}
					else {
						//label is not alive, we will consider pending labels later.
					}
				}	
				if(!alive.isEmpty()){//if the alive array is not empty
					Label aliveLabel = alive.get(0);//get the first alive label in the list
					validOrientation.put(aliveLabel.getBoundPoint(), getPosition(aliveLabel.getShift(), aliveLabel.isTop()));
					ArrayList<Label> pointLabels = new ArrayList<Label>(p.getLabels());//new arraylist of labels containing the labels associated with that point
					//if a point has an alive label, we do not have to consider other collisions for this point.
					for(Label l: pointLabels){//for all labels associated with this point
						labels.remove(l);//TODO remove the label from the list.
						//TODO possibly remove the not alive labels, as they should never be chosen.
					}
					//TODO choose the alive label, one of the labels in alive
				}
				else{
					//if no alive labels, we should continue to the pending labels
				}
			}	
		}
		
		tree.init(labels, height, aspectRatio, 10000);//now only use the leftover labels
		setIntersectionsQuad(tree, labels);//gives all labels the correct boolean value for intersection.
		
		ArrayList<Label> overlap;//the list of labels the labels overlaps with
		for(Label l: labels){//for all retained labels
			if(l.isHasIntersect()){//if it has an intersection
				overlap = findIntersectionQuad(tree, l);//find the labels this label intersects with
				collisions.put(l, overlap);//put them in the mapping
			}
		}
		return collisions;//return the list of collisions
	}	
	
	public Orientation getPosition(double shift,boolean top){
		if(shift==0){
			if(top){
				return Orientation.NW;
			}
			else{
				return Orientation.SW;
			}
		}
		else{
			if(top){
				return Orientation.NE;
			}
			else{
				return Orientation.SE;
			}
		}
		
	}
	
	/**
	 * 
	 * @param collisions: list of all collisions found by the findCollisions method.
	 * @return returns the clauses generated by converting the collisions to clauses.
	 */
	public ArrayList<Clause> getClauses(HashMap<Label, ArrayList<Label>> collisions){
		ArrayList<Clause> newClauses = new ArrayList<Clause>();//the arraylist that will be returned
		for(Label l:collisions.keySet()){//for all labels (as keys) in the mapping
			for(Label lb:collisions.get(l)){//for the labels the above label intersects with
				newClauses.add(new Clause(l.toClause().negation(), lb.toClause().negation()));
				//add the negation of the associated clausevalues to the arraylist.
				//see literature for explanation.
			}
		}
		return newClauses;//return the clauses
	}

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

	public boolean intersects(Label la, Label lb){

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

		
		if(al == bl && ar == br && ab == bb && at == bt && la.getBoundPoint() != lb.getBoundPoint()){
    		return true;
    	}
		
		if(al == bl && ar == br && ab == bb && at == bt){
			return false;
		}
		if(ar == br || al == bl){
			if(at > bb && at < bt) return true;
			if(ab > bb && ab < bt) return true;
		}
		else if(at == bt || ab == bb){
			if(al > bl && al < br) return true;
			if(ar > bl && ar < br) return true;
		}
		else if(bl < ar && bl > al){
			if(bt < at && bt > ab) return true;
			else if(bb < at && bb > ab) return true;
		}
		else if(br > al && br < ar){
			if(bt < at && bt > ab) return true;
			else if(bb < at && bb > ab) return true;
		}
		else if((bl < al && bl > al) || (br < ar && br > ar)){
			if(bt > ab && bt < at) return true;
			else if(bb < at && bb > ab) return true;
			else if(ab > bb && ab < bt) return true;
			else if(at > bb && at < bt) return true;
		}
		else if((at < bt && at > bt) || (ab < bb && ab > bb)){
			if(ar > bl && ar < br) return true;
			else if(al > bl && al < br) return true;
			else if(br > al && br < ar) return true;
			else if(bl > al && bl < ar) return true;
		}

		return false;

	}

	public void resetIntersections(ArrayList<Label> labels){
		for(int i = 0; i < labels.size(); i++){
			Label l = labels.get(i);
			l.setHasIntersect(false);
		}
	}

	
	
	
	public int countIntersections(QuadTree tree, Label[] labels){
		int amount = 0;
		for(int i = 0; i < labels.length; i++){
			Label l = labels[i];
			if(!l.isHasIntersect()){
				ArrayList<Label> returnObjects = new ArrayList<Label>();
				tree.retrieve(returnObjects, l);
				for(Label label2: returnObjects){
					//label2.hasIntersect = false;
					
					if(intersects(l, label2)){
						l.setHasIntersect(true);
						amount++;
						if(!label2.isHasIntersect()){
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
	 * @param quadtree generated with the quadtree constructor.
	 * sets the correct hasIntersect values for the labels.
	 */
	 public void setIntersectionsQuad(QuadTree tree, ArrayList<Label> labels){
		 for(int i = 0; i < labels.size(); i++){
			 Label l = labels.get(i);

			 if(!l.isHasIntersect()){
				 double l1 = l.getRect().getX();
				 double r1 = l.getRect().getX() + l.getRect().getWidth();
				 double b1 = l.getRect().getY();
				 double t1 = l.getRect().getY() + l.getRect().getHeight();
	
				 ArrayList<Label> returnObjects = new ArrayList<Label>();
				 tree.retrieve(returnObjects, l);
				 for(Label label2: returnObjects){
					 //label2.hasIntersect = false;

					 if(intersects(l, label2)){
						 l.setHasIntersect(true);
						 label2.setHasIntersect(true);
					 }
				 }
			 }
		 }
	 }
	 /**
	  * 
	  * @param quadtree generated with the quadtree constructor.
	  * @param the label you check for.
	  * @return returns if the label intersects with other labels.
	  */
	 public boolean hasIntersectionQuad(QuadTree tree, Label l){
		 ArrayList<Label> returnObjects = new ArrayList<Label>();
		 tree.retrieve(returnObjects, l);
		 for(Label l2: returnObjects){
			 if(intersects(l, l2)){
				 return true;
			 }
		 }
		 return false;
	 }

	 /**
	  * 
	  * @param quadtree generated with the quadtree constructor.
	  * @param the label you check for.
	  * @return returns a list of labels that intersect with the given label. 
	  **/
	 public ArrayList<Label> findIntersectionQuad(QuadTree tree, Label l){
		 ArrayList<Label> possibleInters = new ArrayList<Label>();
		 ArrayList<Label> intersections = new ArrayList<Label>();
		 tree.retrieve(possibleInters, l);
		 for(Label l2: possibleInters){
			 if(intersects(l, l2) && !l.getBoundPoint().equals(l2.getBoundPoint())){
				 l.setHasIntersect(true);
				 l2.setHasIntersect(true);
				 intersections.add(l2);
			 }
		 }
		 return intersections;
	 }


	 /**
	  * 
	  * @param labels are the labels that overlap with the given label
	  * @param the label that could contain these points
	  * @return returns the labels that have their boundpoint contained in this label
	  */
	 public ArrayList<Label> containsPoint(ArrayList<Label> labels, Label l){
		 ArrayList<Label> contained = new ArrayList<Label>();//arraylist to be returned
		 for(Label l2: labels){//for all labels in the labels arraylist
			 if(!l2.getBoundPoint().equals(l.getBoundPoint()) && contains(l.getRect(),l2.getX(),l2.getY(),true)){//check if the point is IN the label
				 contained.add(l2);//if so, add it to contained
			 }
		 }
		 return contained;//return the contained list
	 }
	
	 public boolean contains(Rectangle2D rec, double x, double y, boolean in){
	     double mx = rec.getX();//TODO find out why i cant add delta here, it causes stack overflows.
	     double my = rec.getY();
	     double w = rec.getWidth();
	     double h = rec.getHeight();
	     
	    if(in){
	    	return w > 0 && h > 0 && x > mx && x < mx + w && y > my && y < my + h;
	    }
	    else{
	    	return w >= 0 && h >= 0 && x >= mx && x <= mx + w && y >= my && y <= my + h; 
	    }	       
	 }
	 
	 public void findIntersections(ArrayList<Label> labels){
		 for(int i = 0; i < labels.size(); i++){
			 Label l1 = labels.get(i);

			 double ayT = l1.getBoundPoint().getY() + (l1.isTop() ? height : 0);
			 double ay = ayT - height;
			 double axR = l1.getBoundPoint().getX() + height * aspectRatio * l1.getShift();
			 double ax = axR - height * aspectRatio;

			 for(int j = i + 1; j < labels.size(); j++){
				 Label l2 = labels.get(j);

				 double byT = l2.getBoundPoint().getY() + (l2.isTop() ? height : 0);
				 double by = byT - height;
				 double bxR = l2.getBoundPoint().getX() + height * aspectRatio * l2.getShift();
				 double bx = bxR - height * aspectRatio;

				 if ((ax < bxR && axR > bx) && (ay < byT && ayT > by)){                
					 l1.setHasIntersect(true);
					 l2.setHasIntersect(true);
				 } 
			 }
		 }
	 }
	 
	 /**
	  * 
	  * @param clauseList: list of all clauses to consider
	  * @return returns if this array of clauses is satisfiable
	  */
	 public boolean checkTwoSatisfiability(ArrayList<Clause> clauseList){
		 HashSet<String> variables = new HashSet<String>();//to be set of all string values of the labels. 
		 for(Clause clause: clauseList){//for each clause in the clauselist
			 variables.add(clause.getFirstStringValue());//add the first string value of the clause
			 variables.add(clause.getSecondStringValue());//add the second string value of the clause
			 //this will add all "variables" used in the 2-sat check.
		 }
		 
		 DirectedGraph graph = new DirectedGraph();//make a new directed graph

		 for (String variable: variables) {//for all variables in the variables set
	         graph.addNode(new ClauseValue(variable, true));//make a node for the true variant of this variable
	         graph.addNode(new ClauseValue(variable, false));//make a node for the false variant of this variable.
		 }
		 
		 for (Clause clause: clauseList) {//for all clauses
	         graph.addEdge(clause.getFirstValue().negation(), clause.getSecondValue());//add the implication (~a=>b)
	         graph.addEdge(clause.getSecondValue().negation(), clause.getFirstValue());//add the implication (~b=>a)
	     }

		 HashMap<ClauseValue, Integer> stronglyConnected = stronglyConnectedComponents(graph);
		 //save the strongly connected components as a mapping from label to the number of the scc
		 
		 for(String variable: variables){//for all variables used
			 if(stronglyConnected.get(new ClauseValue(variable, true)).equals(
					 stronglyConnected.get(new ClauseValue(variable, false)))){
				 //if the number of the scc is same for both the negation and the original, we have a logical contradiction, thus false 
				 return false;//so, return false, as the 2-sat is not satisfied.
			 }
		 }
		 
		 minGraph = graph;
		 connectedComponents = stronglyConnected;
		 
		 return true;//in any other case return true, as there exists a solution to this problem.
	 }
 
	 
	 /**
	  * 
	  * @param graph: the directed graph.
	  * @return a mapping from clauseValues to integers, with integer being the connected component the clauseValue is part of
	  */
	 public HashMap<ClauseValue, Integer> stronglyConnectedComponents(DirectedGraph graph) {
	 	//debugPrint("graph:" + graph.getGraph().toString());//DEBUG
        Stack<ClauseValue> visitOrder = dfsOrder(reverseGraph(graph));
        //do a depth first search on the reversed iteration order of the graph, which will be your visit order.

        HashMap<ClauseValue, Integer> result = new HashMap<ClauseValue, Integer>();//the result hashmap
        int scc = 0;//number for the strongly connected component   
        while (!visitOrder.isEmpty()) {//if an element is still available on the stack
            ClauseValue startPoint = visitOrder.pop();//pop an element from the stack, make it the start point
            
            if (!result.containsKey(startPoint)){//if the result does not contain this point yet:
            	markReachableNodes(startPoint, graph, result, scc);//check which nodes are reachable from this node.
            	++scc;//increase the scc number.
        	}
            else{
            	//do nothing, we already considered this point
            }
        }

        return result;//return the list of sccs.
    }
	 
	 /**
	  * 
	  * @param graph: the directed graph
	  * @return the directed graph in reversed order
	  */
	 private static DirectedGraph reverseGraph(DirectedGraph graph) {
        DirectedGraph result = new DirectedGraph();//generate a new directed graph

        for (ClauseValue node: graph) {//for all nodes in the graph
            result.addNode(node);//add the node to the new result graph
        }

        for (ClauseValue node: graph){//for all nodes in the graph
            for (ClauseValue endpoint: graph.edgesFrom(node)){//for all endpoints of this node
                result.addEdge(endpoint, node);//add the edge from the endpoint to that node, which is in the other direction.
            }
        }
        return result;//return the result
 	}
	 	
	/**
	 *  	
	 * @param graph: the directed graph
	 * @return the depth first search order of the graph
	 */
    private Stack<ClauseValue> dfsOrder(DirectedGraph graph) {
        Stack<ClauseValue> result = new Stack<ClauseValue>();//the stack the result will be stored in

        HashSet<ClauseValue> visited = new HashSet<ClauseValue>();//the nodes that have been visited already

        for (ClauseValue node: graph){//for all nodes in the graph
            explore(node, graph, result, visited);//recursively explore
        }
        return result;//return the result
	}

    /**
     * 
     * @param node: the node that is used
     * @param graph: the directed graph
     * @param result: the stack of results
     * @param visited: the set of visited nodes
     */
    private void explore(ClauseValue node, DirectedGraph graph,
                                       Stack<ClauseValue> result, Set<ClauseValue> visited) {
    	
    	if (!visited.contains(node)){//if visited does not contain the node yet 
    		visited.add(node);//add the node to visited
        	for (ClauseValue endpoint: graph.edgesFrom(node)){//for all endpoints of the edges from this node
        		explore(endpoint, graph, result, visited);//recursively explore the graph
        	}
        	result.push(node);//push the node onto the stack
    	}
    }
    
    /**
     * 
     * @param node: the node that is used
     * @param graph: the directed graph
     * @param result: the stack of results
     * @param scc: the number of this strongly connected component
     */
    private void markReachableNodes(ClauseValue node, DirectedGraph graph,
                                               HashMap<ClauseValue, Integer> result,
                                               int scc) {
        
    	if (!result.containsKey(node)){//if the element was not present in the list yet:
        result.put(node, scc);//add this node to the result list
        for (ClauseValue endpoint: graph.edgesFrom(node)){//for all endpoints for the edges of the node
        		markReachableNodes(endpoint, graph, result, scc);//recursively mark the reachable nodes
        	}
    	}
	}
}
