
public class SliderPoint extends Point {
	public enum slideDirection{LEFT,RIGHT}
	private slideDirection direction;
	private double ratio;
	private double leftX;
	private double rightX;
	private double topY;
	private boolean mayGrow;
	private double NEWleftX;
	private double NEWrightX;
	private double NEWtopY;
	
	public SliderPoint(int x, int y, double ratio){
		super(x, y);
		direction = slideDirection.RIGHT;
		this.ratio = ratio;
		leftX = x;
		rightX = x+0;
		topY = y+(0/ratio);
		mayGrow = false;
	}
	
	public void setLeftX(double x)	{ leftX = x;}
	public double getLeftX()		{return leftX;}
	
	public void setRightX(double x)	{rightX = x;}
	public double getRightX()		{return rightX;}
	
	public double getTopY()			{return topY;}
	
	public void setMayGrow(boolean a) {mayGrow = a;}
	public boolean getMayGrow() 	  {return mayGrow;}
	
	public void setNEWLeftX(double newx)	{ NEWleftX = newx;}
	public double getNEWLeftX()				{return NEWleftX;}
	
	public void setNEWRightX(double newx)	{NEWrightX = newx;}
	public double getNEWRightX()			{return NEWrightX;}
	
	public double getNEWTopY()				{return NEWtopY;}
	
	public void setDirection(String direction){
		this.direction = slideDirection.valueOf(direction);
	}
	public String getDirection(){
		return direction.toString();
	}
	
	public double getS(){
		if (rightX-leftX == 0) { System.out.println("WTF M8");}
		double s = (rightX-this.getX())/(rightX-leftX);
		return s;
	}
	
	public void calcGrowth(double newGrow) {
		if (direction == direction.RIGHT) {
			NEWleftX  = leftX;
			NEWrightX = rightX + newGrow;
			NEWtopY   = topY + (newGrow/ratio);
		}
		if (direction == direction.LEFT) {
			NEWleftX  = leftX - newGrow;
			NEWrightX = rightX;
			NEWtopY   = topY + (newGrow/ratio);
		}
	}
	
	public void applyChanges(){
		leftX 	= NEWleftX;
		rightX 	= NEWrightX;
		topY 	= NEWtopY;
	}

		
}
