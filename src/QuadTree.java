
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;

public class QuadTree {

	private int MAX_OBJECTS = 20;
	private int MAX_LEVELS = 20;
	private int level;
	private ArrayList<Label> elements;
	public Rectangle2D bounds;
	private QuadTree[] nodes;
	//private Random r = new Random();
	
	public QuadTree(int l, Rectangle2D b){
		
		level = l;
		bounds = b;
		
		
		
		//System.out.println(bounds);
		
		elements = new ArrayList<Label>();
		nodes = new QuadTree[4];
		
		//drawPoint(new Vector2f(5,5), 3.1f, Color.GREEN);
		
	}
	
	
	public void printTree(){
		double subWidth = (double)(bounds.getWidth() / 2.0f);
		double subHeight = (double)(bounds.getHeight() / 2.0f);
		 
		double x = (double)bounds.getX();
		double y = (double)bounds.getY();
		
		System.out.println("" + x + ", " + y + " until " + (x + 2 * subWidth) + " , " + (y + 2 * subHeight));
		System.out.println("has " + elements.size() + " elements");
		
		for(int i = 0; i < 4; i++){
			if(nodes[i] != null){
				nodes[i].printTree();
			}
		}
	}
	
	public ArrayList<Label> retrieve(ArrayList<Label> returnObjects, Label l) {
		int index = getPos(l);
		if (index != -1 && nodes[0] != null) {
			nodes[index].retrieve(returnObjects, l);
		}
		
		returnObjects.addAll(elements);
		
		return returnObjects;
	}
	
	public void remove(Label l){
		int pos = getPos(l);
		if(pos == -1 || nodes[0] == null){
			Point a = l.getBoundPoint();
			for(int i = 0; i < elements.size(); i++){
				Point b = elements.get(i).getBoundPoint();
				if(a == b) elements.remove(i); 
			}
			//l.setHasIntersect(false);
			elements.remove(l);
		}
		else {
			nodes[pos].remove(l);
		}
	}
	
	public void updateLabel(Label l, double height, double ratio){
		//System.out.println("lChanged1: " + l.isHasIntersect());
		remove(l);
		//System.out.println("lChanged2: " + l.isHasIntersect());
		insertNew(l,height,ratio);
		//System.out.println("lChanged3: " + l.isHasIntersect());
		//System.out.println("height will instert: " + height);
	}
	
	/**
	 * 
	 * @param l: the label to be inserted into the tree;
	 * @param height: height of the label;
	 * @param ratio; aspect ratio of the label;
	 */
	public void insertNew(Label l, double height, double ratio){
		//l.setHasIntersect(false);
    	double top = l.getBoundPoint().getY() + (l.isTop() ? height : 0);
        double bottom = top - height;
        double right = l.getBoundPoint().getX() + (height * ratio * l.getShift());
        double left = right - (height * ratio);
            
        Rectangle2D.Double rect = new Rectangle2D.Double(left, bottom, right-left, top-bottom);
        l.setRect(rect);
        this.insert(l);
	}
	
	private void insert(Label l){
		//Rectangle2D r;
		
		
		
		
		if(nodes[0] != null){
			int pos = getPos(l);
			if(pos != -1) {
				nodes[pos].insert(l);
				//System.out.println("pos" + pos);
				return;
			}
			
		}
		//System.out.println("insert: " + r.getX() + " " + r.getY() + " " + r.getWidth() + " " + r.getHeight() );
		this.elements.add(l);
		
		if(this.elements.size() > MAX_OBJECTS && level < MAX_LEVELS){
			if (nodes[0] == null) { 
				split(); 
			}
			
			int i = 0;
			while (i < elements.size()) {
				int index = getPos(elements.get(i));
				if (index != -1) {
					nodes[index].insert(elements.remove(i));
				}
				else {
					i++;
				}
			}
		}
		
	}
	
	private void split() {
		//System.out.println("split");
		double subWidth = (double)(bounds.getWidth() / 2.0f);
		double subHeight = (double)(bounds.getHeight() / 2.0f);
		
		double x = (double)bounds.getX();
		double y = (double)bounds.getY();
		
		//System.out.println("SPLIT ON  " + x + ", " + y + " until " + (x + 2 * subWidth) + " , " + (y + 2 * subHeight));
		
		nodes[0] = new QuadTree(level+1, new Rectangle2D.Double(x + subWidth, y, subWidth, subHeight));
		nodes[1] = new QuadTree(level+1, new Rectangle2D.Double(x, y, subWidth, subHeight));
		nodes[2] = new QuadTree(level+1, new Rectangle2D.Double(x, y + subHeight, subWidth, subHeight));
		nodes[3] = new QuadTree(level+1, new Rectangle2D.Double(x + subWidth, y + subHeight, subWidth, subHeight));
	}
	
	
	public void empty(){
		elements.clear();
		for(int i = 0; i < 4; i++){
			if(nodes[i] != null){
				nodes[i].empty();
				nodes[i] = null;
			}
		}
	}
	
	public void init(ArrayList<Label> labels, double vSize, double ratio, int range){
		this.empty();
		
		
		this.bounds = new Rectangle2D.Double(0 - vSize * ratio,0 - vSize,range + (2 * vSize * ratio),range + (2 * vSize));
    	
		for (Label l: labels) l.setHasIntersect(false);
    	for (int i = 0; i < labels.size(); i++) {
    		Label l = labels.get(i);
    		l.setHasIntersect(false);
    		double top = l.getBoundPoint().getY() + (l.isTop() ? vSize : 0);
            double bottom = top - vSize;
            double right = l.getBoundPoint().getX() + (vSize * ratio * l.getShift());
            double left = right - (vSize * ratio);
            
            Rectangle2D.Double rect = new Rectangle2D.Double(left, bottom, right-left, top-bottom);
            l.setRect(rect);
            this.insert(l);
            
    	}
    	//.toString();
    	//
    	
	}
	
	public void init(ArrayList<Label> labels, double vSize, double ratio, int rangeX, int rangeY){
		this.empty();
		
		
		this.bounds = new Rectangle2D.Double(0 - vSize * ratio,0 - vSize,rangeX + (2 * vSize * ratio),rangeY + (2 * vSize));
    	
		for (Label l: labels) l.setHasIntersect(false);
    	for (int i = 0; i < labels.size(); i++) {
    		Label l = labels.get(i);
    		l.setHasIntersect(false);
    		double top = l.getBoundPoint().getY() + (l.isTop() ? vSize : 0);
            double bottom = top - vSize;
            double right = l.getBoundPoint().getX() + (vSize * ratio * l.getShift());
            double left = right - (vSize * ratio);
            
            Rectangle2D.Double rect = new Rectangle2D.Double(left, bottom, right-left, top-bottom);
            l.setRect(rect);
            this.insert(l);
            
    	}
    	//.toString();
    	//
    	
	}
	
	public void init(Label[] labels, double vSize, double ratio, int range){
		this.empty();
		this.bounds = new Rectangle2D.Double(0 - vSize * ratio,0 - vSize,range + (2 * vSize * ratio),range + (2 * vSize));
    	
		for (int i = 0; i < labels.length; i++) {
    		Label l = labels[i];
    		l.setHasIntersect(false);
    		double top = l.getBoundPoint().getY() + (l.isTop() ? vSize : 0);
            double bottom = top - vSize;
            double right = l.getBoundPoint().getX() + (vSize * ratio * l.getShift());
            double left = right - (vSize * ratio);
            
            Rectangle2D.Double rect = new Rectangle2D.Double(left, bottom, right-left, top-bottom);
            l.setRect(rect);
            this.insert(l);
            
    	}
    	//.toString();
    	//
    	
	}
	
	
	/*
	 * Determine which node the object belongs to. -1 means
	 * object cannot completely fit within a child node and is part
	 * of the parent node
	 */
	private int getPos(Label l) {
		Rectangle2D pRect = l.getRect();
		//System.out.println(pRect);
		int index = -1;
		double verticalMidpoint = bounds.getX() + (bounds.getWidth() / 2.0);
		double horizontalMidpoint = bounds.getY() + (bounds.getHeight() / 2.0d);
	   
		//drawPoint(new Vector2f((double)verticalMidpoint,(double)horizontalMidpoint), 0.05f, Color.WHITE);
		
		// Object can completely fit within the top quadrants
		boolean topQuadrant = (
				pRect.getY() < 
				horizontalMidpoint && 
				pRect.getY() + 
				pRect.getHeight() < 
				horizontalMidpoint);
		// Object can completely fit within the bottom quadrants
		boolean bottomQuadrant = (pRect.getY() > horizontalMidpoint);
		
		//System.out.println(topQuadrant);
		
		// Object can completely fit within the left quadrants
		if (pRect.getX() < verticalMidpoint && pRect.getX() + pRect.getWidth() < verticalMidpoint) {
			if (topQuadrant) {
				index = 1;
			}
			else if (bottomQuadrant) {
				index = 2;
			}
		}
		// Object can completely fit within the right quadrants
		else if (pRect.getX() > verticalMidpoint) {
			if (topQuadrant) {
				index = 0;
			}
			else if (bottomQuadrant) {
				index = 3;
			}
		}
	 
		return index;
	}
}
