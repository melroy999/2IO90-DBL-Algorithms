public class MaxSize {

    // Finds the maximal possible height for a set of points such that there might be a solution.
    public static double getMaxPossibleHeight(Point[] points, int[] pointers, double ratio, PlacementModel pModel){
    	long time = System.nanoTime();
        double minimum = Double.MAX_VALUE;
        //Find the maximal height of a point and check if it is smaller than the current max.
        for(int i=0; i<pointers.length; i++){
            //Calculate maximal height of NE and NW orientation for point i
            double localMax = getPosMaxHeight(points, pointers, i, ratio, Orientation.NE, pModel);
            double localMax2 = getPosMaxHeight(points, pointers, i, ratio, Orientation.NW, pModel);
            
            /*
             * In case of the oneslider model the "best" height would be if the label goes from i-1 to i+1
             * So add the width of NE and NW and calculate a height.
             */
            if(pModel == PlacementModel.ONESLIDER){
                localMax = (localMax + localMax2);
            }
            /*
             * In the case of the others just find the biggest position
             */
            else{
                localMax = (localMax < localMax2) ? localMax2 : localMax;
                if(pModel == PlacementModel.FOURPOS){
                    localMax2 = getPosMaxHeight(points, pointers, i, ratio, Orientation.SE, pModel);
                    localMax = (localMax < localMax2) ? localMax2 : localMax;
                    localMax2 = getPosMaxHeight(points, pointers, i, ratio, Orientation.SW, pModel);
                    localMax = (localMax < localMax2) ? localMax2 : localMax;
                }
            }
            minimum = (localMax < minimum) ? localMax : minimum;
        }
        MapLabeler.maxHeightTime += (System.nanoTime()-time);
        return minimum;
    }
    
    private static boolean isInLabel(Point p, Point p2, double maxLabelHeight, double ratio, Orientation orientation){
        if(orientation == Orientation.NE){
            return !(p2.getY() < p.getY()) && (p2.getY() <= p.getY() + maxLabelHeight) && p2.getX() <= p.getX() + maxLabelHeight*ratio;
        }
        else if(orientation == Orientation.NW){
            return !(p2.getY() < p.getY()) && (p2.getY() <= p.getY() + maxLabelHeight) && p.getX() - maxLabelHeight*ratio <= p2.getX();
        }
        else if(orientation == Orientation.SE){
            return p2.getY() <= p.getY() && (p.getY() - maxLabelHeight <= p2.getY()) && p2.getX() <= p.getX() + maxLabelHeight*ratio;
        }
        else{
            return p2.getY() <= p.getY() && (p.getY() - maxLabelHeight <= p2.getY()) && p.getX() - maxLabelHeight*ratio <= p2.getX();
        }
    }

    private static double getPosMaxHeight(Point[] points, int[] pointers, int j, double ratio, Orientation orientation, PlacementModel model){
        double maximum = (ratio < 1) ? 10000/ratio : 10000;
        Point p = points[pointers[j]];

        if(orientation == Orientation.NE){
            int nrOfVerticalPoints = 0;
            int nrOfHorizontalPoints = 0;
            j++;
            while(j<pointers.length){
                Point p2 = points[pointers[j]];
                //Check if point lies in the potential label, if so find the new maximum height
                if(isInLabel(p, p2, maximum, ratio, orientation)){
                    double height = p2.getY()-p.getY(); 
                    //Determine max height if p2 lies on the left side of the new potential label
                    if(p2.getX() == p.getX()){
                        //In 4pos only the 3th point is the limit
                        if(nrOfVerticalPoints == 3 && model == PlacementModel.FOURPOS){
                            maximum = height;
                        }
                        else if(nrOfVerticalPoints == 2 && model == PlacementModel.TWOPOS){
                            maximum = height;
                        }
                        nrOfVerticalPoints++;
                    }
                    //Determine max height if p2 is on bottom border of the new label and model == 4pos
                    // If model == 2pos then go to the else (p2 is on the right)
                    else if(p.getY() == p2.getY() && model == PlacementModel.FOURPOS){
                        if(nrOfHorizontalPoints == 3){
                            maximum = (p2.getX() - p.getX())/ratio;
                            break;
                        }
                        nrOfHorizontalPoints++;
                    }
                    //Determine max height if p2 lies on the top border of the new potential label
                    else if(p2.getX() < (p.getX() + height*ratio)){
                        maximum = height;
                    }
                    else{
                        //points hits on the right
                        maximum = (p2.getX() - p.getX())/ratio;
                        break;
                    }
                }
                // next point lies to the right of the max label, so no other point will be inside the max label
                else if(p2.getX() > (p.getX()+ maximum*ratio)){
                    break;
                }
                j++;
            }
        }
        else if(orientation == Orientation.NW){
            int nrOfVerticalPoints = 0;
            int nrOfHorizontalPoints = 0;
            int k = j+1;
            while(k<points.length && p.getX() == points[pointers[k]].getX()){
                if(nrOfVerticalPoints == 3 && model == PlacementModel.FOURPOS){
                    maximum = points[pointers[k]].getY() - p.getY();
                    break;
                }
                else if( nrOfVerticalPoints == 2 && model == PlacementModel.TWOPOS){
                    maximum = points[pointers[k]].getY() - p.getY();
                    break;
                }
                nrOfVerticalPoints++;
                k++;
            }
            j--;
            while(0 <= j){
                Point p2 = points[pointers[j]];
                //Check if point lies in the potential label, if so find the new maximum height
                if(isInLabel(p, p2, maximum, ratio, orientation)){
                    double height = p2.getY()-p.getY(); 
                    if(p2.getX() == p.getX() && model == PlacementModel.FOURPOS){
                        if(nrOfHorizontalPoints == 3){
                            maximum = (p.getX() - p2.getX())/ratio;
                            break;
                        }
                        nrOfHorizontalPoints++;
                    }
                    else if((p.getX() - height*ratio) < p2.getX()){
                        //point hits on top
                        maximum = height;
                    }
                    else{
                        //points hits on the right
                        maximum = (p.getX() - p2.getX())/ratio;
                        break;
                    }
                }
                else if(p2.getX() < (p.getX() - maximum*ratio)){
                    break;
                }
                j--;
            }
        }
        else  if(orientation == Orientation.SE){
            int nrOfVerticalPoints = 0;
            int nrOfHorizontalPoints = 0;
            int k = j-1;
            while(0<=k && p.getX() == points[pointers[k]].getX()){
                if(nrOfVerticalPoints == 3 && model == PlacementModel.FOURPOS){
                    maximum = p.getY()-points[pointers[k]].getY();
                    break;
                }
                else if(nrOfVerticalPoints == 2 && model == PlacementModel.TWOPOS){
                    maximum = p.getY()-points[pointers[k]].getY();
                    break;
                }
                nrOfVerticalPoints++;
                k--;
            }
            j++;
            while(j<pointers.length){
                Point p2 = points[pointers[j]];
                //Check if point lies in the potential label, if so find the new maximum height
                if(isInLabel(p, p2, maximum, ratio, orientation)){
                    double height = p.getY()-p2.getY(); 
                    if(p2.getY() == p.getY() && model == PlacementModel.FOURPOS){
                        if(nrOfHorizontalPoints == 3){
                            maximum = (p2.getX()-p.getX())/ratio;
                            break;
                        }
                        nrOfHorizontalPoints++;
                    }
                    else if(p2.getX() < (p.getX() + height*ratio)){
                        //point hits on top
                        maximum = height;
                    }
                    else{
                        //points hits on the right
                        maximum = (p2.getX() - p.getX())/ratio;
                        break;
                    }
                }
                else if((p.getX() + maximum*ratio) < p2.getX()){
                    break;
                }
                j++;
            }
        }
        else{
            int nrOfVerticalPoints = 0;
            int nrOfHorizontalPoints = 0;
            j--;
            while(0 <= j){
                Point p2 = points[pointers[j]];
                //Check if point lies in the potential label, if so find the new maximum height
                if(isInLabel(p, p2, maximum, ratio, orientation)){
                    double height = p.getY()-p2.getY(); 
                    if(p2.getX() == p.getX()){
                        // Point is right above so ignore the first one above it and find next collision
                        if(nrOfVerticalPoints == 3 && model == PlacementModel.FOURPOS){
                            maximum = height;
                        }
                        else if(nrOfVerticalPoints == 2 && model == PlacementModel.TWOPOS){
                            maximum = height;
                        }
                        nrOfVerticalPoints++;
                    }
                    else if(p.getY() == p2.getY() && model == PlacementModel.FOURPOS){
                        if(nrOfHorizontalPoints == 3){
                            maximum = (p.getX()-p2.getX())/ratio;
                            break;
                        }
                        nrOfHorizontalPoints++;
                    }
                    else if((p.getX() - height*ratio) < p2.getX()){
                        //point hits on top
                        maximum = height;
                    }
                    else{
                        //points hits on the right
                        maximum = (p.getX() - p2.getX())/ratio;
                        break;
                    }
                }
                else if(p2.getX() < (p.getX() - maximum*ratio)){
                    break;
                }
                j--;
            }
        }
        return maximum;
    }
}
