
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;


public class LabelConfiguration {
	private Label[] labels;
	private Random r = new Random();
	
	private int lastChangePos;
	private Orientation lastChangeFrom;
	private Orientation lastChangeTo;
	
	public LabelConfiguration(PosPoint[] p, double height, double ratio){
		
		this.labels = new Label[p.length];
		for (int i = 0; i < p.length; i++) {
            this.labels[i] = new Label(p[i],r.nextBoolean() ? 1 : 0,r.nextBoolean());
            
            Label l = labels[i];
    		double top = l.getBoundPoint().getY() + (l.isTop() ? height : 0);
            double bottom = top - height;
            double right = l.getBoundPoint().getX() + (height * ratio * l.getShift());
            double left = right - (height * ratio);
            
            Rectangle2D rect = new Rectangle2D.Double(left, bottom, right-left, top-bottom);
            l.setRect(rect);
            
            
        }
	}
	
	public LabelConfiguration(Label[] config){
		this.labels = new Label[config.length];
		for(int i = 0; i < config.length; i++){
			this.labels[i] = new Label(config[i]);
		}
		//this.labels = config.clone();
	}
	
	public Label[] getLabels(){
		return labels;
	}
	
	public int labelSize(){
		return labels.length;
	}
	
	public void changeBack(Orientation o){
		//TODO iets met de verandering hier gaat fout
		Label l = labels[lastChangePos];
		//System.out.print("     Changed back: " + l + " TO " + o + " (becomes ");
		
		l.setOrientation(o);
		//System.out.print(l + ")");
		//System.out.println();
	}
	
	public int getLastPos(){
		return lastChangePos;
	}
	
	public Orientation getLastFrom(){
		return lastChangeFrom;
	}
	
	public Orientation getLastTo(){
		return lastChangeTo;
	}
	
	
	public void change(int position, ArrayList<Orientation> options){
		Label l = labels[position];
		
		
		Orientation o = l.getOrientation();
		options.remove(o);
		
		
		//System.out.print("set " + l.getX() + " " + l.getY() + " from " + o);
		/*if(o == Orientation.NE){
			options[0] = Orientation.NW;
			options[1] = Orientation.SE;
			options[2] = Orientation.SW;
		}
		else if(o == Orientation.NW){
			options[0] = Orientation.SE;
			options[1] = Orientation.SW;
			options[2] = Orientation.NE;
		}
		else if(o == Orientation.SE){
			options[0] = Orientation.SW;
			options[1] = Orientation.NE;
			options[2] = Orientation.NW;
		}
		else {
			options[0] = Orientation.NE;
			options[1] = Orientation.NW;
			options[2] = Orientation.SE;
		}*/
		
		
		
		Orientation to = options.get((int)(r.nextInt(options.size())));
		options.remove(to);
		//System.out.print(" to " + to);
		//System.out.println();
		
		//System.out.print("     Changed TO: " + l + " from " + o + " to " + to + " becomes ");
		
		
		l.setOrientation(to);
		
		//System.out.print(l);
		//System.out.println();
		
		lastChangePos = position;
		lastChangeFrom = o;
		lastChangeTo = to;
		
		//r.nextDouble() * 
	}
	
	@Override
    public String toString() {
        String geneString = "";
        for (int i = 0; i < labelSize(); i++) {
            geneString += labels[i].getX() + ", " + labels[i].getY() + "; " + labels[i].getOrientation().toString() + " | ";
        }
        return geneString;
    }
	
	
}
