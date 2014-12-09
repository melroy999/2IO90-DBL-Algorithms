import java.util.ArrayList;

public class Point {
	private int x;
	private int y;
	private ArrayList<Label> labels;

	public Point(int x, int y){
		this.x = x;
		this.y = y;
		labels = new ArrayList<Label>();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
	
	public ArrayList<Label> getLabels(){
		return labels;
	}
	
	public boolean isNotDead(){
		return !labels.isEmpty();
	}
	
	public void removeLabel(Label l){
		labels.remove(l);
	}
	
	public void addLabel(Label l){
		labels.add(l);
	}
	
	public void setLabels(ArrayList<Label> labelList){
		this.labels = labelList;
	}
	
	/**
	 * returns a string representation of this point
	 */
	public String toString(){
		return (x+"~"+y);
		//string of the form x~y
	}
	
	/**
	 * 
	 * @param int i, different values for 4pos, see it as top boolean
	 * @return a string representation of this point considering 4 positions
	 */
	public String toString(int i){
		return (x+"~"+y+"~"+i);
		//string of the form x~y~i
	}
	
	@Override
    public boolean equals(Object obj) {
        Point point = (Point) obj;
        return (point.getX() == this.getX()) && (point.getY() == this.getY());
    }
}
