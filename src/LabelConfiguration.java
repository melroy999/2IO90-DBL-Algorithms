import java.util.Random;


public class LabelConfiguration {
	private Label[] labels;
	private Random r = new Random();
	
	public LabelConfiguration(PosPoint[] p){
		
		labels = new Label[p.length];
		for (int i = 0; i < p.length; i++) {
            labels[i] = new Label(p[i],r.nextBoolean() ? 1 : 0,r.nextBoolean());
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
	
	public void change(int position){
		Label l = labels[position];
		Orientation[] options = new Orientation[3];
		Orientation o = l.getOrientation();
		//System.out.print("set " + l.getX() + " " + l.getY() + " from " + o);
		if(o == Orientation.NE){
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
		}
		Orientation to = options[(int)(r.nextInt(3))];
		//System.out.print(" to " + to);
		//System.out.println();
		l.setOrientation(to);
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
