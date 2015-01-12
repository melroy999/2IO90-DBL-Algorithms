import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderPoint extends Point {
	public enum slideDirection{LEFT,RIGHT}
	private slideDirection direction;
	private slideDirection NEWdirection;
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
		rightX = x;
		topY = y;
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
	
	public void setNEWDirection(String direction){
		this.NEWdirection = slideDirection.valueOf(direction);
	}
	public String getNEWDirection(){
		return NEWdirection.toString();
	}
	
	public double getS(){
		if (rightX-leftX == 0) { System.out.println(rightX + " " + leftX + " " + this.getX());}
		double s = (rightX-this.getX())/(rightX-leftX);
		BigDecimal lel = new BigDecimal(String.valueOf(s));
		lel = lel.setScale(7, RoundingMode.FLOOR);
		s = lel.doubleValue();		
		return s;
	}
	
	public void calcGrowth(double newGrow) {
		if (direction == direction.RIGHT) {
			NEWleftX  = leftX;
			NEWrightX = rightX + (newGrow*ratio);
			NEWtopY   = topY + newGrow;
		}
		if (direction == direction.LEFT) {
			NEWleftX  = leftX - (newGrow*ratio);
			NEWrightX = rightX;
			NEWtopY   = topY + newGrow;
		}
	}
	public void setNEWsize(double NEWheight) {
		if (direction == direction.RIGHT) {
			NEWleftX  = leftX;
			NEWrightX = rightX + (NEWheight*ratio);
		}
		if (direction == direction.LEFT) {
			NEWleftX  = leftX - (NEWheight*ratio);
			NEWrightX = rightX;
		}
		NEWtopY   = NEWheight + this.getY();
		
	}
	
	public void applyChanges(){
		leftX 	  = NEWleftX;
		rightX 	  = NEWrightX;
		topY 	  = NEWtopY;
		direction = NEWdirection;
	}	
	public void revertChanges(){
		NEWleftX	= leftX 	   ;
		NEWrightX	= rightX 	  ;
		NEWtopY		= topY 	   ;
		NEWdirection= direction  ;
	}
}
