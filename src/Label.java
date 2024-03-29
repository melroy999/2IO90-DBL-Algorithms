
import java.awt.geom.Rectangle2D;

public class Label {
	private boolean top;
	private float shift; // 0 means left
	private boolean hasIntersect = false;
	// public Vector2i boundPoint;
	private PosPoint boundPoint;
	private Rectangle2D.Double rect;
	private boolean viable;
	private int index;

	private int ltDeg = 0;
	private int rtDeg = 0;
	private int lbDeg = 0;
	private int rbDeg = 0;

	private boolean ltShow = true;
	private boolean rtShow = true;
	private boolean lbShow = true;
	private boolean rbShow = true;

	public Label(PosPoint point, float labelShift, boolean labelTop) {
		this.boundPoint = point;
		this.shift = labelShift;
		this.top = labelTop;
		this.viable = true;
	}
	
	public Label(Label l){
		this.boundPoint = l.getBoundPoint();
		this.shift = l.getShift();
		this.top = l.isTop();
		this.viable = l.viable;
	}
	
	public int getIndex(){
		return index;
	}
	
	public void setViability(boolean viability){
		this.viable = viability;
	}
	
	public boolean getViability(){
		return this.viable;
	}
	
	public void deleteGreatestDegree() {
		int max = ltDeg;
		if (rtDeg > max)
			max = rtDeg;
		if (lbDeg > max)
			max = lbDeg;
		if (rbDeg > max)
			max = rbDeg;

		int del = -1;
		if (max == ltDeg && del == -1) {
				del = 1;
		}
		if (max == rtDeg && del == -1) {
				del = 2;
		}
		if (max == lbDeg && del == -1) {
				del = 3;
		}
		if (max == rbDeg && del == -1) {
				del = 4;
		}
		
		if (del == 1) {
			ltShow = false;
		} 
		else if (del == 2) {
			rtShow = false;
		} 
		else if (del == 3) {
			lbShow = false;
		} 
		else if (del == 4) {
			rbShow = false;
		}
	}

	public void setOrientation(Orientation o){
		if(o == Orientation.NE){
			this.top = true;
			this.shift = 1.0f;
		}
		else if(o == Orientation.NW){
			this.top = true;
			this.shift = 0.0f;
		}
		else if(o == Orientation.SE){
			this.top = false;
			this.shift = 1.0f;
		}
		else if(o == Orientation.SW) {
			this.top = false;
			this.shift = 0.0f;
		}
		else {
			System.out.println("WTF?");
		}
		
	}
	
	public Orientation getOrientation(){
		Orientation o = Orientation.NW;
		if(this.shift < 0.5){
			if(this.top) o = Orientation.NW;
			if(!this.top) o = Orientation.SW;
		}
		if(this.shift > 0.5){
			if(this.top) o = Orientation.NE;
			if(!this.top) o = Orientation.SE;
		}
		return o;
	}
	
	public int getX() {
		return boundPoint.getX();
	}

	public int getY() {
		return boundPoint.getY();
	}

	public boolean isTop() {
		return top;
	}

	public void setTop(boolean top) {
		this.top = top;
	}

	public float getShift() {
		return shift;
	}

	public void setShift(float shift) {
		this.shift = shift;
	}

	public boolean isHasIntersect() {
		return hasIntersect;
	}

	public void setHasIntersect(boolean hasIntersect) {
		this.hasIntersect = hasIntersect;
	}

	public PosPoint getBoundPoint() {
		return boundPoint;
	}

	public void setBoundPoint(PosPoint boundPoint) {
		this.boundPoint = boundPoint;
	}

	public Rectangle2D getRect() {
		return rect;
	}

	public void setRect(Rectangle2D.Double rect) {
		this.rect = rect;
	}

	public int getLtDeg() {
		return ltDeg;
	}

	public void setLtDeg(int ltDeg) {
		this.ltDeg = ltDeg;
	}

	public int getRtDeg() {
		return rtDeg;
	}

	public void setRtDeg(int rtDeg) {
		this.rtDeg = rtDeg;
	}

	public int getLbDeg() {
		return lbDeg;
	}

	public void setLbDeg(int lbDeg) {
		this.lbDeg = lbDeg;
	}

	public int getRbDeg() {
		return rbDeg;
	}

	public void setRbDeg(int rbDeg) {
		this.rbDeg = rbDeg;
	}

	public boolean isLtShow() {
		return ltShow;
	}

	public void setLtShow(boolean ltShow) {
		this.ltShow = ltShow;
	}

	public boolean isRtShow() {
		return rtShow;
	}

	public void setRtShow(boolean rtShow) {
		this.rtShow = rtShow;
	}

	public boolean isLbShow() {
		return lbShow;
	}

	public void setLbShow(boolean lbShow) {
		this.lbShow = lbShow;
	}

	public boolean isRbShow() {
		return rbShow;
	}

	public void setRbShow(boolean rbShow) {
		this.rbShow = rbShow;
	}
	
	/**
	 * @return string in the form x~y|direction|top
	 * Do not use this function to get the value of the label in string form to be used in clauses!	 
	 */
	public String toString(){
		String ret = "";
		//ret += Integer.toHexString(System.identityHashCode(this)) + ": ";
		ret += boundPoint.toString() + "|" + (shift<0.5 ? "L" : "R") + (top ? ("|T") : ("|B"));
		return (ret);
		//returns a string of the form x~y|direction|to
	}
	
	/**
	 * 
	 * @return returns the label as a ClauseValue
	 */
	public ClauseValue toClause(){
		return new ClauseValue(this.boundPoint.toString(),(this.shift==0 ? false : true));
		//returns a clausevalue with the correct boolean value and string value. the boundpoint string value has an additional value for the southern points
	}
}
