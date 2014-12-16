public class MaxSize {

	// Finds the maximal possible height for a set of points such that there might be a solution.
	public static float getMaxPossibleHeight(Point[] points, int[] pointers, float ratio, PlacementModel pModel){
	    float minimum = 10000;
	    
	    for(int i=0; i<pointers.length; i++){
	        float localMax = getPosMaxHeight(points, pointers, i, ratio, Orientation.NE);
	        float localMax2 = getPosMaxHeight(points, pointers, i, ratio, Orientation.NW);
	        
	        if(pModel == PlacementModel.ONESLIDER){
	            float width = (localMax + localMax2)*ratio;
	            localMax = width/ratio;
	        }
	        else{
	            localMax = (localMax < localMax2) ? localMax2 : localMax;
	            if(pModel == PlacementModel.FOURPOS){
	                localMax2 = getPosMaxHeight(points, pointers, i, ratio, Orientation.SE);
	                localMax = (localMax < localMax2) ? localMax2 : localMax;
	                localMax2 = getPosMaxHeight(points, pointers, i, ratio, Orientation.SW);
	                localMax = (localMax < localMax2) ? localMax2 : localMax;
	            }
	        }
	        minimum = (localMax < minimum) ? localMax : minimum;
	    }
	    
	    return minimum;
	}
	
	private static float getPosMaxHeight(Point[] points, int[] pointers, int j, float ratio, Orientation orientation){
	    float maximum = 0;
	    Point p = points[pointers[j]];
	    
	    if(orientation == Orientation.NE){
	        boolean rightAbove = false;
	        float orientationMaxHeight = 10000;
	        j++;
	        while(j<pointers.length){
	            Point p2 = points[pointers[j]];
	            //Check if point lies in the potential label, if so find the new maximum height
	            if(!(p2.getY() < p.getY()) && (p2.getY() <= p.getY() + orientationMaxHeight) && p2.getX() <= p.getX() + orientationMaxHeight*ratio ){
	                float height = p2.getY()-p.getY(); 
	                if(p2.getX() == p.getX()){
	                 // Point is right above so ignore the first one above it and find next collision
	                    if(rightAbove){
	                        orientationMaxHeight = height;
	                    }
	                    rightAbove = true;
	                }
	                else if(p2.getX() < (p.getX() + height*ratio)){
	                    //point hits on top
	                    orientationMaxHeight = height;
	                }
	                else{
	                    //points hits on the right
	                    orientationMaxHeight = (p2.getX() - p.getX())/ratio;
	                    break;
	                }
	            }
	            j++;
	        }
	        maximum = (maximum < orientationMaxHeight) ? orientationMaxHeight : maximum;
	    }
	    else  if(orientation == Orientation.NW){
	        boolean rightAbove = false;
            float orientationMaxHeight = 10000;
            j--;
            while(0 <= j){
                Point p2 = points[pointers[j]];
                //Check if point lies in the potential label, if so find the new maximum height
                if(!(p2.getY() < p.getY()) && (p2.getY() <= p.getY() + orientationMaxHeight) && p.getX() - orientationMaxHeight*ratio >= p2.getX() ){
                    float height = p2.getY()-p.getY(); 
                    if(p2.getX() == p.getX()){
                     // Point is right above so ignore the first one above it and find next collision
                        if(rightAbove){
                            orientationMaxHeight = height;
                        }
                        rightAbove = true;
                    }
                    else if((p.getX() - height*ratio) < p2.getX()){
                        //point hits on top
                        orientationMaxHeight = height;
                    }
                    else{
                        //points hits on the right
                        orientationMaxHeight = (p.getX() - p2.getX())/ratio;
                        break;
                    }
                }
                j--;
            }
            maximum = (maximum < orientationMaxHeight) ? orientationMaxHeight : maximum;
	    }
	    else  if(orientation == Orientation.SE){
	        boolean rightAbove = false;
            float orientationMaxHeight = 10000;
            j++;
            while(j<pointers.length){
                Point p2 = points[pointers[j]];
                //Check if point lies in the potential label, if so find the new maximum height
                if(p2.getY() <= p.getY() && (p.getY() - orientationMaxHeight <= p2.getY()) && p2.getX() <= p.getX() + orientationMaxHeight*ratio){
                    float height = p.getY()-p2.getY(); 
                    if(p2.getX() == p.getX()){
                     // Point is right above so ignore the first one above it and find next collision
                        if(rightAbove){
                            orientationMaxHeight = height;
                        }
                        rightAbove = true;
                    }
                    else if(p2.getX() < (p.getX() + height*ratio)){
                        //point hits on top
                        orientationMaxHeight = height;
                    }
                    else{
                        //points hits on the right
                        orientationMaxHeight = (p2.getX() - p.getX())/ratio;
                        break;
                    }
                }
                j++;
            }
            maximum = (maximum < orientationMaxHeight) ? orientationMaxHeight : maximum;
	    }
	    else  {
	        boolean rightAbove = false;
            float orientationMaxHeight = 10000;
            j--;
            while(0 <= j){
                Point p2 = points[pointers[j]];
                //Check if point lies in the potential label, if so find the new maximum height
                if(p2.getY() <= p.getY() && (p.getY() - orientationMaxHeight <= p2.getY()) && p.getX() - orientationMaxHeight*ratio >= p2.getX() ){
                    float height = p.getY()-p2.getY(); 
                    if(p2.getX() == p.getX()){
                     // Point is right above so ignore the first one above it and find next collision
                        if(rightAbove){
                            orientationMaxHeight = height;
                        }
                        rightAbove = true;
                    }
                    else if((p.getX() - height*ratio) < p2.getX()){
                        //point hits on top
                        orientationMaxHeight = height;
                    }
                    else{
                        //points hits on the right
                        orientationMaxHeight = (p.getX() - p2.getX())/ratio;
                        break;
                    }
                }
                j--;
            }
            maximum = (maximum < orientationMaxHeight) ? orientationMaxHeight : maximum;
	    }
	    
	    return maximum;
	}
}