import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/*
 * 2IO90 DBL Algorithms
 * This is the main class of the labeler software.
 * It reads input, let it process and outputs the solution
 * as specified in the problem description.
 */
public class MapLabeler {
	private Scanner sc;


	//private File input = new File("tests/pointsamm_100_4pos_notdense2.txt");
	//private File output = new File("tests/pointsamm_100_4pos_notdense2_solved.txt");
	
	

	private File input;
	private File output;

	//private File input = new File("2pos100.txt");
	//private File output = new File("2pos100_solved.txt");

	
	//private File input = new File("tests/gaatfout/testert3.txt");
	//private File output = new File("tests/gaatfout/testert3_solved.txt");
	
	

	private Plane plane;
	private PlacementModel pModel;

	public static final boolean local = true;

	private ArrayList<Long> times = new ArrayList<Long>();
	
	
	public MapLabeler() throws IOException{
		for(int points = 9500; points <= 10000; points += 100){
			System.out.print(points + " ");
			for(int test = 1; test <= 10; test++){
				input = new File("tests/2pos/test" + test + "/pointsamm_" + points + ".txt");
				output = new File("tests/2pos/test" + test + "/pointsamm_" + points + "_solved.txt");
				
				try{
					if(local){
						sc = new Scanner(input);
					}
					else{
					    System.out.println("Gib input pl0x");
						sc = new Scanner(System.in);
					}
				} catch (FileNotFoundException e){
					System.out.println("Input file not found: " + input.getName());
				}
			    try {
					readInput();
					times.clear();
					for(int iteration = 0; iteration < 1; iteration++){
						long start = System.currentTimeMillis();
						solvePlacementProblem();
						writeOutput();
						long stop = System.currentTimeMillis();
						long time = (stop - start);
						times.add(time);
					}
					System.out.print(calculateAverage(times) + " ");
					//times.clear();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			System.out.println();
		}
	    
	}
	
	private double calculateAverage(List <Long> marks) {
		  long sum = 0;
		  if(!marks.isEmpty()) {
		    for (long mark : marks) {
		        sum += mark;
		    }
		    return (double)sum / marks.size();
		  }
		  return sum;
		}
	

	public void readInput() throws Exception{
		sc.useLocale(Locale.US);
		sc.next();
		sc.next();
		pModel = PlacementModel.fromString(sc.next());
		sc.next();
		sc.next();
		double ratio = sc.nextDouble();
		sc.next();
		sc.next();
		sc.next();
		int numberOfPoints = sc.nextInt();
		if(pModel == PlacementModel.ONESLIDER){
			HashSet<String> pointsSet = new HashSet<String>();
			SliderPoint[] points = new SliderPoint[numberOfPoints];

			for(int i=0; i<numberOfPoints; i++){
				int x = sc.nextInt();
				int y = sc.nextInt();
				points[i] = new SliderPoint(x, y, ratio);
				pointsSet.add(points[i].toString());
			}

			if(points.length != pointsSet.size()){
				throw new Exception("Duplicate points detected");
			}

			plane = new Plane(ratio, points);
		}
		else{
			HashSet<String> pointsSet = new HashSet<String>();
			PosPoint[] points = new PosPoint[numberOfPoints];

			for(int i=0; i<numberOfPoints; i++){
				int x = sc.nextInt();
				int y = sc.nextInt();
				points[i] = new PosPoint(x, y);
				pointsSet.add(points[i].toString());
			}
			
			if(points.length != pointsSet.size()){
				throw new Exception("Duplicate points detected");
			}

			plane = new Plane(ratio, points);
		}	

	}

	Label[] iets = new Label[1];
	
	public void solvePlacementProblem(){
		if(pModel == PlacementModel.TWOPOS){
			plane.find2PosSolution();
		}
		else if(pModel == PlacementModel.FOURPOS){
			iets = plane.find4PosSolutionSA();
		}
		else{
			plane.find1SliderSolution();
		}
	}

	public void writeOutput() throws IOException{
		if(local){
			output.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			bw.write("placement model: " + pModel.toString());
			bw.newLine();
			bw.write("aspect ratio: " + plane.getAspectRatio());
			bw.newLine();
			bw.write("number of points: " + plane.getNumberOfPoints());
			bw.newLine();
			bw.write("label height: " + plane.getHeight());
			bw.newLine();
			if(pModel == PlacementModel.ONESLIDER){
				SliderPoint[] s = plane.getSliderPoints();
				for(int i=0; i<s.length; i++){
					bw.write("" + s[i].getX() + " " + s[i].getY() + " " + s[i].getS());
					bw.newLine();
				}
			}
			else if(pModel == PlacementModel.FOURPOS){
				Label[] l = iets;
				for(int i = 0; i < l.length; i++){
					bw.write("" + l[i].getX() + " " + l[i].getY() + " " + l[i].getOrientation().toString());//TODO redo orientation text
					bw.newLine();
				}
			}
			else{
				PosPoint[] s = plane.getPosPoints();
				for(int i=0; i<s.length; i++){
					bw.write("" + s[i].getX() + " " + s[i].getY() + " " + s[i].getPosition().toString());//TODO redo orientation text
					bw.newLine();
				}
			}
			bw.close();
		}
		else{
			System.out.println("placement model: " + pModel.toString());
			System.out.println("aspect ratio: " + plane.getAspectRatio());
			System.out.println("number of points: " + plane.getNumberOfPoints());
			System.out.println("label height: " + plane.getHeight());
			if(pModel == PlacementModel.ONESLIDER){
				SliderPoint[] s = plane.getSliderPoints();
				for(int i=0; i<s.length; i++){
					System.out.println("" + s[i].getX() + " " + s[i].getY() + " " + s[i].getS());
				}
			}
			else{
				PosPoint[] s = plane.getPosPoints();
				for(int i=0; i<s.length; i++){
					System.out.println("" + s[i].getX() + " " + s[i].getY() + " " + s[i].getPosition().toString());//TODO redo orientation text
				}
			}
		}
	}

	public static void main(String[] args){
		try {
			new MapLabeler();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
