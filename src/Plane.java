import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
	
	private DirectedGraph minGraph;
	private Map<ClauseValue, PosPoint> clausePoint;
	
	private SliderPoint[] sliderPoints;
	public int[] xPointArray;  //slider pointers
	private PosPoint[] posPoints;
	
	private Map<String,String> pointINV;

	private double delta = 0.001;//difference of border
	
	private boolean debug = true;
	
	private HashMap<Integer, ArrayList<ClauseValue>> outResult;

	//public ArrayList<Vector2i> points;
	//public ArrayList<Label> labels;

	public Plane(double aspectRatio, SliderPoint[] points, PlacementModel pModel){
		this.aspectRatio = aspectRatio;
		this.numberOfPoints = points.length;
		this.sliderPoints = points;
		height = (aspectRatio < 1 ? aspectRatio : 1/aspectRatio);
		this.pModel = pModel;
	}

	public Plane(double aspectRatio, PosPoint[] points, PlacementModel pModel){
		this.aspectRatio = aspectRatio;
		this.numberOfPoints = points.length;
		this.posPoints = points;
		this.pModel = pModel;
	}
	
	public void debugPrint(String text){
		if(MapLabeler.local && debug){
			System.out.println(text);
		}
	}
	
	public void removeLabel(Label label, ArrayList<Label> labels){
		label.getBoundPoint().removeLabel(label);
		labels.remove(label);
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
		//TODO: we might have to use doubles, as the values aren't that accurate if a not fitting start value is used.
	    int[] xSortedOrder = MergeSort.sort(posPoints);//sorting the points on x-cor, referencing by index in this array.
	    
	    /*
	     * TODO: max size algorithm for 2pos
	     */

	    int range = 10000;//TEMP for best performance find x-min and x-max, y-min and y-max and change the range to this.
		QuadTree quad = new QuadTree(0, new Rectangle(0,0,range+1,range+1));//new quadtree with (top) level 0 and dimensions (range+1)^2
		
		height = 100;//TODO initial height of label, to be calculated by jorrick's max-height algorithm.		
		double minHeight = (aspectRatio < 1) ? 1 : (1/aspectRatio);//minimal height
		
		double maxHeight = height*2;//2x the maximal height, so that we start with the calculated max-height in the loop.
		int rounds = 0;//amount of completed loops
		while(maxHeight-minHeight>delta){//while the difference isn't too small
			height = (maxHeight+minHeight)/2;//height to use is the average of max and min height
			
			clearLabels(posPoints);//clear all labels from the arraylists in the points
			ArrayList<Label> labels = new ArrayList<Label>();//all labels will be stored in this arraylist. 
			debugPrint("currentHeight: " + height + ", minHeight: " + minHeight + ", maxHeight: " + maxHeight + ", difference: " + (maxHeight-minHeight));//DEBUG
			
			for(PosPoint p:posPoints){//make the top labels for all points.
				addLabel(new Label(p, 1, true), labels);//shift=1 and top=true gives us the NE label;
				addLabel(new Label(p, 0, true), labels);//shift=0 and top=true gives us the NW label;
			}
			
			if(clausePoint==null){
				clausePoint = new HashMap<ClauseValue, PosPoint>();
				for(PosPoint p: posPoints){
					clausePoint.put((new Label(p, 1, true)).toClause(), p);
					clausePoint.put((new Label(p, 0, true)).toClause(), p);
				}
			}
			
			ArrayList<Clause> clauses = new ArrayList<Clause>();//a list which will initially contain additional clauses.
			//the additional clauses are required to fix the value of a dead label to the negation of the clauseValue of that label.
			HashMap<Label, ArrayList<Label>> collisions = findCollisions2pos(labels, clauses, quad, height);
			//get the list of collisions	
			if (collisions!=null){//collisions will return null if a point with only dead labels exists
				clauses.addAll(getClauses(collisions));//add the clauses generated with the collisions to the clauses list.
				if(checkTwoSatisfiability(clauses)){//if a satisfiable configuration exists
					minHeight = height;//this height will be valid, so the minimum height becomes this height.
					debugPrint("Solution is possible");//DEBUG
				}
				else{//if no solution can be found with 2-sat
					maxHeight = height;
					//this height has no solution, so the maximum found height for which this does not work is now height
					debugPrint("2-Sat has no solution.");//DEBUG
				}
			}
			else {//if a point has only dead labels
				maxHeight = height;
				//this height has no solution, so the maximum found height for which this does not work is now height
				debugPrint("A point is completely dead.");//DEBUG
			}
			rounds++;//DEBUG
			debugPrint("--------------------------------------------------------------------------");//DEBUG
		}
		
		debugPrint("Given minHeight: " + height);
		height = Math.floor(height*(1/(delta*10)))*delta*10;
		debugPrint("The height solution is: " + height + ", this took " + rounds + " loops.");//DEBUG
		
		if(minGraph!=null){
			Stack<ClauseValue> order = dfsOrder(reverseGraph(minGraph));
			debugPrint("dfs order:" + order.toString());	
			while(!order.isEmpty()){
				ClauseValue next = order.pop();
				if(clausePoint.get(next).getPosition()==null){
					getNext(next);
				}
				else{
					//skip this point
				}
			}
		}
		else {//if somehow, the input has duplicate points which is not allowed according to the assignment:
			//give all points the same value, with height = 0.
			for(PosPoint p : posPoints){
				p.setPosition(0, true);
			}
		}
		return posPoints;//return the array of points, now with correct positions.
	}
	
	/*public void getNext(ClauseValue next){
		clausePoint.get(next).setPosition(next.isPositive() ? 1 : 0,true);
		if(!minGraph.edgesFrom(next).isEmpty()){			
			for(ClauseValue value : minGraph.edgesFrom(next)){
				getNext(value);
			}	
		}
	}*/
	
	public void getNext(ClauseValue next){
		if(clausePoint.get(next).getPosition()==null){
			clausePoint.get(next).setPosition(next.isPositive() ? 1 : 0,true);
			if(!minGraph.edgesFrom(next).isEmpty()){			
				for(ClauseValue value : minGraph.edgesFrom(next)){
					getNext(value);
				}	
			}
		}
	}
	
	public void getNext(ClauseValue next, HashMap<ClauseValue, ClauseValue> inverse, Set<ClauseValue> dead){
		if(inverse.containsKey(next) && clausePoint.get(inverse.get(next)).getPosition()==null){
			System.out.println((inverse.get(next).getValue()).substring((inverse.get(next).getValue()).length() - 1));
			clausePoint.get(inverse.get(next)).setPosition((inverse.get(next)).isPositive() ? 1 : 0, (inverse.get(next).getValue()).substring((inverse.get(next).getValue()).length() - 1).equals("1") ? true : false);
			if(!minGraph.edgesFrom(next).isEmpty()){			
				for(ClauseValue value : minGraph.edgesFrom(next)){
					getNext(value, inverse, dead);
				}	
			}
		}
	}

	/**
	 * 
	 * @return The solution to the 4pos problem. It first calculates the maximum height
	 * and the orientation of every label. This method returns the orientation array.
	 */
	public PosPoint[] find4PosSolution(){
		delta = 0.001;
		
		int range = 10000;//TEMP for best performance find x-min and x-max, y-min and y-max and change the range to this.
		QuadTree quad = new QuadTree(0, new Rectangle(0,0,range+1,range+1));//new quadtree with (top) level 0 and dimensions (range+1)^2
		
		int[] xSortedOrder = MergeSort.sort(posPoints);//sorting the points on x-cor, referencing by index in this array.
	    		
		height = 40;//TODO initial height of label, to be calculated by jorrick's max-height algorithm.		
		double minHeight = (aspectRatio < 1) ? 1 : (1/aspectRatio);//TODO the minimal possible height of a label, could be replaced by the actual minimal.
		minHeight = 0;
		double maxHeight = height*2;//2x the maximal height, so that we start with the calculated max-height in the loop.
	    
		ArrayList<Label> labels = new ArrayList<Label>();

		Set<Label> deadLabels; 
		Set<Label> aliveLabels;
		
		HashMap<ClauseValue, ClauseValue> clauseToDummySolved = new HashMap<ClauseValue, ClauseValue>();
		HashMap<ClauseValue, ClauseValue> clauseToDummyInvSolved = new HashMap<ClauseValue, ClauseValue>();
		Set<ClauseValue> deadLabelsSolved = new HashSet<ClauseValue>(); 
		
		while(maxHeight-minHeight>delta){
			clearLabels(posPoints);//clear all labels from the arraylists in the points
			labels = new ArrayList<Label>();//all labels will be stored in this arraylist. 
			
			height = (minHeight + maxHeight)/2; 
			
			debugPrint("currentHeight: " + height + ", minHeight: " + minHeight + ", maxHeight: " + maxHeight + ", difference: " + (maxHeight-minHeight));//DEBUG
			
			for(PosPoint p:posPoints){//make the top labels for all points.
				addLabel(new Label(p, 1, true), labels);//shift=1 and top=true gives us the NE label;
				addLabel(new Label(p, 0, true), labels);//shift=0 and top=true gives us the NW label;
				addLabel(new Label(p, 1, false), labels);//shift=1 and top=true gives us the SE label;
				addLabel(new Label(p, 0, false), labels);//shift=0 and top=true gives us the SW label;
			}
			
			HashMap<Label, ArrayList<Label>> collisions = new HashMap<Label, ArrayList<Label>>();
			quad.init(labels, height, aspectRatio, 10000);//initialize the quadtree
			setIntersectionsQuad(quad, labels);//gives all labels the correct boolean value for intersection.
			
			if(clausePoint==null){
				clausePoint = new HashMap<ClauseValue, PosPoint>();
				for(PosPoint p: posPoints){
					clausePoint.put((new Label(p, 1, true)).toClause(), p);
					clausePoint.put((new Label(p, 0, true)).toClause(), p);
					clausePoint.put((new Label(p, 1, false)).toClause(), p);
					clausePoint.put((new Label(p, 0, false)).toClause(), p);
				}
			}
			
			deadLabels = removeAllDead(collisions);
			
			String lastAlive = deadLabels.toString();
			aliveLabels = removeAllAlive(collisions);
			
			while(!lastAlive.equals(aliveLabels.toString())){//this also finds additional alive labels.
				lastAlive = aliveLabels.toString();
				aliveLabels = removeAllAlive(collisions);
			}	
		
			/**
			 * the collisions should not have any dead labels or alive labels (including their other options) in it now. 
			 * The only thing left is to check if the collisions have a solution.
			 */
		
			ArrayList<Clause> clauses = new ArrayList<Clause>();		
			collisions = findCollisions4pos(labels, clauses, quad, height);

			System.out.println("collisions: " + collisions);
			if(collisions!=null){
				boolean satisfied = true;
				for(Label l : collisions.keySet()){
					if(collisions.get(l).size()>2){
						satisfied=false;
						break;
					}
				}
				if(satisfied){
					HashMap<ClauseValue, ClauseValue> clauseToDummy = new HashMap<ClauseValue, ClauseValue>();
					HashMap<ClauseValue, ClauseValue> clauseToDummyInv = new HashMap<ClauseValue, ClauseValue>();
					int i = 0;
					for(PosPoint p : posPoints){	
						boolean negation = false;
						for(Label l : p.getLabels()){
							if(!collisions.get(l).isEmpty()){
								if(!negation){
									clauseToDummy.put(l.toClause(), new ClauseValue("" + i,true));
									clauseToDummyInv.put(new ClauseValue("" + i,true), l.toClause());
									negation = true;
								}
								else{
									clauseToDummy.put(l.toClause(), new ClauseValue("" + i,false));
									clauseToDummyInv.put(new ClauseValue("" + i,false), l.toClause());
								}
							}						
						}
						i++;//number of the point
					}
					clauses.addAll(getClauses(collisions, clauseToDummy));
					
					System.out.println("clauses: " + clauses);
					
					boolean satisfiability = checkTwoSatisfiability(clauses);
					if(satisfiability){
						minHeight = height;
						clauseToDummySolved = new HashMap<ClauseValue,ClauseValue>(clauseToDummy);
						clauseToDummyInvSolved = new HashMap<ClauseValue,ClauseValue>(clauseToDummyInv);
						deadLabelsSolved = new HashSet<ClauseValue>();
						for(Label l : deadLabels){
							deadLabelsSolved.add(l.toClause());
						}
						
						System.out.println("Solution possible");
					}
					else {
						maxHeight = height;
						System.out.println("No solution");
					}
				}
				else {
					maxHeight = height;
				}
			}
			else {
				maxHeight = height;
			}
		}	
		
		height = minHeight;
		
		debugPrint("Given minHeight: " + height);
		height = Math.floor(height*(1/(delta*10)))*delta*10;
		debugPrint("The height solution is: " + height);//DEBUG
		
		for(PosPoint p : posPoints){
			if(p.getPosition()!=null){
				System.out.println(p + ":"+ p.getPosition());
			}
		}
		
		System.out.println(clauseToDummyInvSolved.toString());
		
		if(minGraph!=null){
			Stack<ClauseValue> order = dfsOrder(reverseGraph(minGraph));
			debugPrint("dfs order:" + order.toString());	
			while(!order.isEmpty()){
				ClauseValue next = order.pop();
				//System.out.println(clausePoint.get(next));
				//System.out.println(clausePoint.get(next).getPosition());
				//System.out.println(next);
				//System.out.println(clauseToDummySolved);
				if(clauseToDummyInvSolved.containsKey(next) && !deadLabelsSolved.contains(clauseToDummyInvSolved.get(next)) && clausePoint.get(clauseToDummyInvSolved.get(next)).getPosition()==null){
					getNext(next, clauseToDummyInvSolved, deadLabelsSolved);
				}
				else{
					//skip this point
				}
			}
		}
		else {//if somehow, the input has duplicate points which is not allowed according to the assignment:
			//give all points the same value, with height = 0.
			for(PosPoint p : posPoints){
				p.setPosition(0, true);
			}
		}
		
		return posPoints;
	}

	public void resetViability(){
		for(PosPoint p : posPoints){
			for(Label l : p.getLabels()){
				l.setViability(true);
			}
		}
	}
	
	public Set<Label> removeAllDead(HashMap<Label, ArrayList<Label>> collisions){
		ArrayList<Label> contained;
		Set<Label> deadLabels = new HashSet<Label>();
		for(Label l : collisions.keySet()){
			contained = containsPoint(collisions.get(l),l);
			if(contained.size()>0){
				for(Label l2 : contained){
					collisions.get(l2).remove(l);
					collisions.get(l).remove(l2);
				}
				deadLabels.add(l);
				l.setViability(false);//extra information to know if the label could be chosen.
			}		
		}
		return deadLabels;
	}
	
	public Set<Label> removeAllAlive(HashMap<Label, ArrayList<Label>> collisions){
		Set<Label> aliveLabels = new HashSet<Label>();
		for(Label l : collisions.keySet()){
			if(collisions.get(l).isEmpty()){
				//set the position, so a wrong one can never be chosen later?
				for(Label l2 : l.getBoundPoint().getLabels()){
					if(!collisions.get(l2).isEmpty()){
						for(Label l3 : collisions.get(l2)){
							collisions.get(l3).remove(l2);//remove label l2 from the childs.
						}
						l2.setViability(false);//don't choose this one, you have other great options!
						collisions.put(l2,new ArrayList<Label>());
					}
					else{
						aliveLabels.add(l2);
					}					
				}
			}
		}
		return aliveLabels;
	}	
	
	public SliderPoint[] find1SliderSolution(){
		delta= 0.1;
		xPointArray = MergeSort.sort(sliderPoints);
		CalcSlider(sliderPoints,xPointArray);
		return sliderPoints;
	}
	
	void CalcSlider(SliderPoint[] sArray, int[] pointer) {  
		int i;																									//sliderPoints must be sorted on x-coordinates
		double minH = 0;
		double maxH = 20;
		double currentH;
		while (maxH-minH > delta) {
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
				for (i = sliderPoints.length -1; i >= 0; i--) {
					sliderPoints[pointer[i]].setMayGrow(false);
				}
				minH = currentH;				
			}
			debugPrint("new max and min: " + maxH + ", " +  minH);
		}
		
		//FINAL LOOP TO PLACE ALL LABELS FOR THE MAX HEIGHT
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
		height = minH;
	}
	
	boolean checkNewSituation(SliderPoint[] sArray, int[] pointer, int pointLoc) {
		int i = pointLoc;
		int j = pointLoc - 1;
		if (i==0) {
			debugPrint("clear, the last point");return true;}																//the last label is always moveable			
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
	
	/*public HashMap<Label, ArrayList<Label>> findCollisions(ArrayList<Label> labels, QuadTree tree){
		setIntersectionsQuad(tree);
		HashMap<Label, ArrayList<Label>> collisions = new HashMap<Label, ArrayList<Label>>();
		ArrayList<Label> temp;
		ArrayList<Label> contained = new ArrayList<Label>();
		for(Label l: labels){
			if(l.isHasIntersect()){
				temp = findIntersectionQuad(tree, l);
				contained.addAll(containsPoint(temp, l));
				temp.removeAll(contained);
				collisions.put(l, temp);
			}
	/**
	 * 
	 * @param points: the points given in the input of the program
	 */
	public void clearLabels(Point[] points){
		for(Point p : points){//for all points
			p.getLabels().clear();//clears the list of labels in the point p
		}
	}
	
	/**
	 * 
	 * @param labels, list of all labels, including alive and dead labels.
	 * @param clauses, empty arrayList of clauses, points with dead labels will get a special clause.
	 * @param tree, the quadtree used to find collisions.
	 * @param height, the height of the labels to check for.
	 * @return a hashmap of all labels to an arraylist of labels they collide with. null if a dead point exists.
	 */
	public HashMap<Label, ArrayList<Label>> findCollisions2pos(ArrayList<Label> labels, ArrayList<Clause> clauses, QuadTree tree, double height){			
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
				for(Label l: toRemove){//for all labels that have to be removed
					clauses.add(new Clause(l.toClause().negation(),
							l.toClause().negation()));//add a clause to the clauses list that will force the negation of this table to be true;
					p.setPosition((l.getShift()-1)*-1, l.isTop());//set the inverse dead label as the orientation.
					//TODO how is this done in 4 position?
					//This can be done in the form (x+x), with x being the negation of the cluaseValue.
					//for (x + x),   x has to be true to satisfy!
					removeLabel(l, labels);//remove the label from the labels list.
				}
				if(p.getLabels().isEmpty()){//if somehow all labels are dead for a point
					return null;//return null as collision value, associated with this error
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
					p.setPosition(aliveLabel.getShift(), aliveLabel.isTop());//set the alive label as the orientation.
					ArrayList<Label> pointLabels = new ArrayList<Label>(p.getLabels());//new arraylist of labels containing the labels associated with that point
					//if a point has an alive label, we do not have to consider other collisions for this point.
					for(Label l: pointLabels){//for all labels associated with this point
						labels.remove(l);//TODO remove the label from the list, not remove it in the point itself, for later use.
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
	
	
	public HashMap<Label, ArrayList<Label>> findCollisions4pos(ArrayList<Label> labels, ArrayList<Clause> clauses, QuadTree tree, double height){			
		HashMap<Label, ArrayList<Label>> collisions = new HashMap<Label, ArrayList<Label>>();//make a new hashmap of labels to arraylist of labels

		tree.init(labels, height, aspectRatio, 10000);//initialize the quadtree
		setIntersectionsQuad(tree, labels);//gives all labels the correct boolean value for intersection.
		
		ArrayList<Label> deadLabels = new ArrayList<Label>();
		ArrayList<Label> toRemove;//to avoid the editing iteration items while iterating, add the dead labels to an arraylist
		for(PosPoint p: posPoints){//for all points
			toRemove = new ArrayList<Label>();//make a new arrayList for toRemove arraylist.
			for(Label l: p.getLabels()){//for all labels in the point
				if(l.isHasIntersect()){//if the label collides with other labels
					if(!containsPoint4pos(findIntersectionQuad(tree,l),l).isEmpty()){
						//if the list of contained points is not empty for the specified label
						toRemove.add(l);//add this label to the arraylist of dead labels
						deadLabels.add(l);
					}
				}
				else{
					//the label is safe, most likely alive.
					//alive check is done later, to assure that labels that became alive by deleting a dead label are also present. 
				}
			}
			for(Label l: toRemove){//for all labels that have to be removed
				removeLabel(l, labels);//remove the label from the labels list.
			}
			if(p.getLabels().isEmpty()){//if somehow all labels are dead for a point
				System.out.println("deaaaad!");
				return null;//return null as collision value, associated with this error
			}
		}
		
		tree.init(labels, height, aspectRatio, 10000);//initialize the tree again, now without the dead labels.
		setIntersectionsQuad(tree, labels);//gives all labels the correct boolean value for intersection.
		
		/*ArrayList<Label> aliveLabels = new ArrayList<Label>();
		String lastAlive = deadLabels.toString();
		aliveLabels = getAlive(labels);
		int i = 0;
		while(!lastAlive.equals(aliveLabels.toString()) && i!=3){
			lastAlive = aliveLabels.toString();
			aliveLabels.addAll(getAlive(labels));
			//System.out.println(aliveLabels);
			i++;
		}
		System.out.println(i);*/
		
		ArrayList<Label> aliveLabels = new ArrayList<Label>();
		ArrayList<Label> lastAlive = getAlive(labels);
		aliveLabels = new ArrayList<Label>(lastAlive);
		while(!lastAlive.isEmpty()){
			tree.init(labels, height, aspectRatio, 10000);//initialize the tree again, now without the dead labels.
			setIntersectionsQuad(tree, labels);//gives all labels the correct boolean value for intersection.
			lastAlive = getAlive(labels);
			aliveLabels.addAll(lastAlive);
		}

		System.out.println("labels: " + labels);
		System.out.println("alive: " + aliveLabels.toString());
		System.out.println("dead: " + deadLabels.toString());
		
		tree.init(labels, height, aspectRatio, 10000);//initialize the tree again, now without the dead labels.
		setIntersectionsQuad(tree, labels);//gives all labels the correct boolean value for intersection.
		
		ArrayList<Label> overlap;//the list of labels the labels overlaps with
		for(Label l: labels){//for all retained labels
			if(l.isHasIntersect()){//if it has an intersection
				overlap = findIntersectionQuad(tree, l);//find the labels this label intersects with
				collisions.put(l, overlap);//put them in the mapping
			}
		}
		
		for(PosPoint p : posPoints){
			int k = 0;
			for(Label l : p.getLabels()){
				if(collisions.get(l)!=null && !deadLabels.contains(l) && !aliveLabels.contains(l)){
					System.out.println(l + ":" + collisions.get(l));
					if(!collisions.get(l).isEmpty()){
						k++;
					}
					for(Label l2: collisions.get(l)){
						if(deadLabels.contains(l2) || aliveLabels.contains(l2)){
							System.out.println("->" + l2);
						}
					}
				}
			}
			System.out.println(k);
		}
		
		System.out.println("done");
		
		/*while(true){
		}*/
		/*String lastAlive = deadLabels.toString();
		aliveLabels = removeAllAlive(collisions);
		
		while(!lastAlive.equals(aliveLabels.toString())){//this also finds additional alive labels.
			lastAlive = aliveLabels.toString();
			aliveLabels = removeAllAlive(collisions);
		}*/
		//return collisions;//return the list of collisions
		
		return collisions;//return the list of collisions
	}
	
	public ArrayList<Label> getAlive(ArrayList<Label> labels){
		ArrayList<Label> alive;//arrayList of all alive labels, to be removed after the loop is done.
		ArrayList<Label> aliveLabels = new ArrayList<Label>();
		for(PosPoint p: posPoints){//for all points
			alive = new ArrayList<Label>();//make a new alive arraylist
			for(Label l: p.getLabels()){//for all labels in the point
				if(!l.isHasIntersect()){//if the label has no intersections
					alive.add(l);//it is alive, so add to the alive list
					aliveLabels.add(l);
				}
				else {
					//label is not alive, we will consider pending labels later.
					//aliveLabels.add(l);
				}
			}	
			if(!alive.isEmpty()){//if the alive array is not empty
				Label aliveLabel = alive.get(0);//get the first alive label in the list
				p.setPosition(aliveLabel.getShift(), aliveLabel.isTop());//set the alive label as the orientation.
				ArrayList<Label> pointLabels = new ArrayList<Label>(p.getLabels());//new arraylist of labels containing the labels associated with that point
				//if a point has an alive label, we do not have to consider other collisions for this point.
				for(Label l: pointLabels){//for all labels associated with this point
					//labels.remove(l);//TODO remove the label from the list, not remove it in the point itself, for later use.
					removeLabel(l, labels);
					/*if(!aliveLabels.contains(l)){
						aliveLabels.add(l);
					}*/
					//TODO possibly remove the not alive labels, as they should never be chosen.
				}
				//TODO choose the alive label, one of the labels in alive
			}
			else{
				//if no alive labels, we should continue to the pending labels
			}
		}
		return aliveLabels;
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
	
	public ArrayList<Clause> getClauses(HashMap<Label, ArrayList<Label>> collisions, HashMap<ClauseValue, ClauseValue> correction){
		ArrayList<Clause> newClauses = new ArrayList<Clause>();//the arraylist that will be returned
		for(Label l:collisions.keySet()){//for all labels (as keys) in the mapping
			for(Label lb:collisions.get(l)){//for the labels the above label intersects with
				newClauses.add(
						new Clause(
								correction.get(l.toClause()).negation(), 
								correction.get(lb.toClause()).negation())
						);
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


	/**
	 * 
	 * @param quadtree generated with the quadtree constructor.
	 * sets the correct hasIntersect values for the labels.
	 */
	 public void setIntersectionsQuad(QuadTree tree, ArrayList<Label> labels){
		 for(int i = 0; i < labels.size(); i++){
			 Label l = labels.get(i);

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
			 if(!l2.getBoundPoint().equals(l.getBoundPoint()) && l.getRect().contains(l2.getX(),l2.getY())){//check if the point is in the label
				 contained.add(l2);//if so, add it to contained
			 }
		 }
		 return contained;//return the contained list
	 }
	 
	 public ArrayList<Label> containsPoint4pos(ArrayList<Label> labels, Label l){
		 ArrayList<Label> contained = new ArrayList<Label>();//arraylist to be returned
		 for(Label l2: labels){//for all labels in the labels arraylist
			 /*if(!l2.getBoundPoint().equals(l.getBoundPoint()) && contains(l.getRect(),l2.getX(),l2.getY(), l.getShift(), l.isTop())){//check if the point is in the label
				 contained.add(l2);//if so, add it to contained
			 }*/
			 int i = 0;
			 if(contains(l.getRect(),l2.getX(),l2.getY(), false)){
				 if(!contains(l.getRect(),l2.getX(),l2.getY(), true)){
					 for(Label l3 : l.getBoundPoint().getLabels()){
						 if(l3.getRect().contains(l2.getX(),l2.getY())){
							 i++;
						 }			 
					 }
					 if(i>1){
						 contained.add(l2);
					 }
				 }
				 else{
					 contained.add(l2); 
				 }
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
	    	return w >= 0 && h > 0 && x > mx && x < mx + w && y > my && y < my + h;
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
