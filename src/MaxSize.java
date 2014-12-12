import java.util.Arrays;


public class MaxSize {
	private static int smallestMax;
	private static int pointMax;
	
	
	
	// So we want to receive the point and the pointer values.
	
	// Start with i=0, look for the left and right part
	// Supply all the points, the sortingpointer, the ratio and whether we are using it for 2-pos(true=2-pos, false=4-pos)
	private static void maxsize(Point[] points, int[] sortedPoints, float ratio, boolean pos2){
		int j; // Which point we are comparing it with right now
		for (int i=0; i<points.length; i++){
			
			double fixedMaxN = 0; // The amount of points above our current point(same x), 0 if there are not 2 points above it.
			double fixedMaxS = 0; // The amount of points below our current point(same x), 0 if there are not 2 points below it, 
			
			// Amount of points it already noted that has the same X(if it has more then 2 points above it, fixedMax = the value with this in mind)
			int sameXN = 0; 
			int sameXS = 0; 
			
			// Amount of points that have the same Y value, only needed for 4-pos and it is reset to 0 when switch from left to right side
			int sameY = 0;
			
			// as we need i all the time we make a simple variable of it
			int pointX = points[sortedPoints[i]].getX();
			int pointY = points[sortedPoints[i]].getY();
			
			// Current known max X(so current known smallest square), this x is absolute point so not relative(size) but real x position
			double maxXNW = 0; 
			double maxXSW = 0; 
			double maxXNE = 0; 
			double maxXSE = 0; 
			
			
			j = i-1;
			// Calculate the maxX for the left upper side
			// We continue the search of a smaller square until we are at a point that is more to the left then the smallest square
			while (j>=0 && (points[sortedPoints[j]].getX()>maxXNW || (points[sortedPoints[j]].getX()>maxXSW && !pos2)) ){
				
				
				// Start with what happens if the next point has the same X coordinate
				if(points[sortedPoints[j]].getX()==pointX){
					
					//If the point is above our point
					// North Part
					if(points[sortedPoints[j]].getY() > pointY){
						sameXN++;
						if(sameXN==2){
							fixedMaxN = (double)(points[sortedPoints[j]].getY()-pointY)*(double)ratio;
						} else if(sameXN>2){
							double k = (double)(points[sortedPoints[j]].getY()-pointY)*(double)ratio;
							if(k<fixedMaxN){
								fixedMaxN = k;
							}
						}
						if(sameXN>1){
							maxXNW = pointX - fixedMaxN; // Note, now these are the first points we look at.. 
							// In the east part of this algorithm we therefore look out that we do check if maxXNW is bigger then the current.
							maxXNE = pointX + fixedMaxN;
						}
					}
					
					// If the point is below our point
					// South Part
					else if(!pos2 &&points[sortedPoints[j]].getY() < pointY){
						sameXS++;
						if(sameXS==2){
							fixedMaxS = (double)(points[sortedPoints[j]].getY()-pointY)*(double)ratio;
						} else if(sameXS>2){
							double k = (double)(points[sortedPoints[j]].getY()-pointY)*(double)ratio;
							if(k<fixedMaxS){
								fixedMaxS = k;
							}
						}
						if(sameXS>1){
							maxXSW = pointX - fixedMaxS; // Note, now these are the first points we look at.. 
							// In the east part of this algorithm we therefore look out that we do check if maxXNW is bigger then the current.
							maxXSE = pointX + fixedMaxS;
						}						
					}
				}
				
				// Continue with what happens with all the points to the left that are still relevant
				else if(points[sortedPoints[j]].getX()<pointX){
					//pointY+((pointX-maxXSW)/ratio) represents the Y value of the current smallest square..
					// We use this to see if the upcoming point is even relevant related to the y-as
					// North-west part
					if(points[sortedPoints[j]].getY() > pointY && pointY+((pointX-maxXSW)/ratio) > points[sortedPoints[j]].getY() ){
						// The point J hits the square(from I) on the north side
						// Difference y*ratio> difference x
						if((points[sortedPoints[j]].getY()-pointY)*ratio > (pointX-points[sortedPoints[j]].getX())){
							maxXNW = pointX - (points[sortedPoints[j]].getY()-pointY)*ratio;
							// The new max is the point - the max square width(which is equal to the vertical difference*ratio)
						}
						// The point I hits the square(from I) on the west side
						// Difference y*ratio<= difference x
						// North West corner is included here as that would also define the max square
						else if((points[sortedPoints[j]].getY()-pointY)*ratio <= (pointX-points[sortedPoints[j]].getX())){
							maxXNW = points[sortedPoints[j]].getX();
							// Now the maxXNW is set in such a way that the next points that are on the left of this point are outside
							// the square which means that the 'while' condition is not true anymore
						}
					}
					//pointY-((pointX-maxXSW)/ratio) represents the Y value of the current smallest square..
					// We use this to see if the upcoming point is even relevant related to the y-as
					// South-West part
					else if(!pos2 && points[sortedPoints[j]].getY() < pointY  && pointY-((pointX-maxXSW)/ratio) < points[sortedPoints[j]].getY()){
						if((pointY-points[sortedPoints[j]].getY())*ratio > (pointX-points[sortedPoints[j]].getX())){
							maxXSW = pointX - (pointY-points[sortedPoints[j]].getY())*ratio;
							// The new max is the point - the max square width(which is equal to the vertical difference*ratio)
						}
						// South West corner is included here as that would also define the max square
						else if((pointY-points[sortedPoints[j]].getY())*ratio <= (pointX-points[sortedPoints[j]].getX())){
							maxXSW = points[sortedPoints[j]].getX();
						}
					}
					else if(points[sortedPoints[j]].getY() == pointY){ //Could be replaced by 'else'! This is different for formality
						if(pos2){
							maxXNW = points[sortedPoints[j]].getX();
						}
						else if(!pos2){
							// We can continue but we found our smallest size at the 3th point we encounter here!!!!
							sameY ++;
							if(sameY>3){
								if(points[sortedPoints[j]].getX() > maxXNW){
									maxXNW = points[sortedPoints[j]].getX();
								}
								if(points[sortedPoints[j]].getX() > maxXSW){
									maxXSW = points[sortedPoints[j]].getX();
								}
							}
						}
					}
				}
				j--;
			}
			
			
			
			
			
			
			
			
			sameY = 0;
			j = i+1;
			//Calculate the maxY for the right side
			/////// NEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEDS TOOOOO BEEE EDITEEDDDDD
			while (j<points.length & j!=0){
			/////// NEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEDS TOOOOO BEEE EDITEEDDDDD
				
				// Start with what happens if the next point has the same X coordinate
				if(points[sortedPoints[j]].getX()==pointX){
					//If the point is above our point
					// North Part
					if(points[sortedPoints[j]].getY() > pointY){
						sameXN++;
						if(sameXN==2){
							fixedMaxN = (double)(points[sortedPoints[j]].getY()-pointY)*(double)ratio;
						} else if(sameXN>2){
							double k = (double)(points[sortedPoints[j]].getY()-pointY)*(double)ratio;
							if(k<fixedMaxN){
								fixedMaxN = k;
							}
						}
						if(sameXN>1){
							if(maxXNW < pointX - fixedMaxN){
								maxXNW = pointX - fixedMaxN;
							}
							// Note, now these are not the first points we look at for the east side
							// In the east part of this algorithm we therefore look out that we do check if maxXNW is bigger then the current.
							maxXNE = pointX + fixedMaxN;
						}
					}
					
					// If the point is below our point
					// South Part
					if(!pos2 &&points[sortedPoints[j]].getY() < pointY){ 
						sameXS++;
						if(sameXS==2){
							fixedMaxS = (double)(points[sortedPoints[j]].getY()-pointY)*(double)ratio;
						} else if(sameXS>2){
							double k = (double)(points[sortedPoints[j]].getY()-pointY)*(double)ratio;
							if(k<fixedMaxS){
								fixedMaxS = k;
							}
						}
						if(sameXS>1){
							if(maxXSW < pointX - fixedMaxN){
								maxXSW = pointX - fixedMaxS; // Note, now these are the first points we look at.. 
							}
							// In the east part of this algorithm we therefore look out that we do check if maxXNW is bigger then the current.
							maxXSE = pointX + fixedMaxS;
						}						
					}
				}
				
				// Continue with what happens with all the points to the left that are still relevant
				else if(points[sortedPoints[j]].getX()>pointX){
					//pointY+((pointX-maxXSW)/ratio) represents the Y value of the current smallest square..
					// We use this to see if the upcoming point is even relevant related to the y-as
					// North-east part
					if(points[sortedPoints[j]].getY() > pointY && pointY+((maxXSW- pointX)/ratio) > points[sortedPoints[j]].getY() ){
						// The point J hits the square(from I) on the north side
						// Difference y*ratio> difference x
						if((points[sortedPoints[j]].getY()-pointY)*ratio > (points[sortedPoints[j]].getX()-pointX)){
							maxXNE = pointX - (points[sortedPoints[j]].getY()-pointY)*ratio;
							// The new max is the point - the max square width(which is equal to the vertical difference*ratio)
						}
						// The point I hits the square(from I) on the west side
						// Difference y*ratio<= difference x
						// North West corner is included here as that would also define the max square
						else if((points[sortedPoints[j]].getY()-pointY)*ratio <= (points[sortedPoints[j]].getX()-pointX)){
							maxXNE = points[sortedPoints[j]].getX();
							// Now the maxXNW is set in such a way that the next points that are on the left of this point are outside
							// the square which means that the 'while' condition is not true anymore
						}
					}
					//pointY-((pointX-maxXSW)/ratio) represents the Y value of the current smallest square..
					// We use this to see if the upcoming point is even relevant related to the y-as
					// South-East part
					else if(!pos2 && points[sortedPoints[j]].getY() < pointY  && pointY-((pointX-maxXSE)/ratio) < points[sortedPoints[j]].getY()){
						if((pointY-points[sortedPoints[j]].getY())*ratio > (points[sortedPoints[j]].getX()-pointX)){
							maxXSE = pointX + (pointY-points[sortedPoints[j]].getY())*ratio;
							// The new max is the point - the max square width(which is equal to the vertical difference*ratio)
						}
						// South West corner is included here as that would also define the max square
						else if((pointY-points[sortedPoints[j]].getY())*ratio <= (points[sortedPoints[j]].getX()-pointX)){
							maxXSE = points[sortedPoints[j]].getX();
						}
					}

					else if(points[sortedPoints[j]].getY() == pointY){ //Could be replaced by 'else'! This is different for formality
						if(pos2){
							maxXNE = points[sortedPoints[j]].getX();
						}

						else if(!pos2){
							// We can continue but we found our smallest size at the 3th point we encounter here!!!!
							sameY ++;
							if(sameY>3){
								if(points[sortedPoints[j]].getX() < maxXNE){
									maxXNE = points[sortedPoints[j]].getX();
								}
								if(points[sortedPoints[j]].getX() < maxXSE ){
									maxXSE = points[sortedPoints[j]].getX();
								}
							}
							
						}
					}
				}
				
				j++;
			}
			
			

			/////// NEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEDS TOOOOO BEEE EDITEEDDDDD
			// Determine the max for pointX

			/////// NEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEDS TOOOOO BEEE EDITEEDDDDD
		}
		
	}

}
