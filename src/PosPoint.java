
public class PosPoint extends Point{
	private Orientation position;
	
	public PosPoint(int x, int y){
		super(x, y);
	}
	
	public Orientation getPosition(){
		return position;
	}
	
	public void setPosition(float shift, boolean top){
		if(top){
			if(shift==0){
				position = Orientation.NW;
			}
			else{
				position = Orientation.NE;
			}
		}
		else{
			if(shift==0){
				position = Orientation.SW;
			}
			else{
				position = Orientation.SE;
			}
		}
	}	
	
	public void setPosition(Orientation position){
		this.position = position;
	}
	
	public void resetPosition(){
		position = null;
	}
}
