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
	private File input = new File("2pos100.txt");
	private File output = new File("2pos100_solved.txt");

	private Plane plane;
	private PlacementModel pModel;

	public static final boolean local = true;

	public static final long start = System.currentTimeMillis();
	
	public static float maxHeight = 0;//
	public static float realHeight = 0;//
	public static long maxHeightTime = 0;//
	public static long initTime = 0;//
	public static long avgColTime = 0;//
	public static long maxColTime = 0;//
	public static long avg2SatTime = 0;//
	public static long max2SatTime = 0;//
	public static long totalAvgColTime = 0;//
	public static long totalMaxColTime = 0;//
	public static long totalAvg2SatTime = 0;//
	public static long totalMax2SatTime = 0;//
	public static int nrOfLoops = 0;//
	public static long placementTime = 0;//
	public static long finalCheckTime = 0;
	public static long averageRunningTime = 0;//
	
	public static long avgColTimeLoop = 0;//
	public static long avg2SatTimeLoop = 0;//
	public static long maxColTimeLoop = 0;//
	public static long max2SatTimeLoop = 0;//
	
	public static long startTime = 0;
	
	int repeat = 10;

	public MapLabeler() throws IOException{
		pModel = PlacementModel.TWOPOS;
		String testing = "2pos";
		File outputFile = new File("tests/"+testing+"/testResult_maxHeight_v"+System.currentTimeMillis()+".csv");
		outputFile.createNewFile();
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		String s = "";
		if(pModel == PlacementModel.TWOPOS){
			s = "test_nr; max_height; real_height; max_height_time; init_time; average_collision_time; max_collision_time; total_average_collision_time; total_max_collision_time; "
					+ "average_2sat_time; max_2sat_time; total_average_2sat_time; total_max_2sat_time; nr_of_loops; placement_time; final_check_time; average_running_time";
		}
		else if(pModel == PlacementModel.FOURPOS){
			s = "";
		}
		else{
			s = "test_nr; maxHeight; realHeight; maxHeightTime; initTime; avgColTime; maxColTime; totalAvgColTime; totalMaxColTime"
					+"; nrOfLoops; finalCheckTime; averageRunningTime";
		}
		writer.write(s);
		writer.newLine();
		for(int points = 10000; points <= 10000; points += 100){
			System.out.println(points);
			for(int test = 1; test <= 10; test++){
				maxHeight = 0;
				realHeight = 0;
				maxHeightTime = 0;
				initTime = 0;
				avgColTime = 0;
				maxColTime = 0;
				avg2SatTime = 0;
				max2SatTime = 0;
				nrOfLoops = 0;
				placementTime = 0;
				finalCheckTime = 0;
				averageRunningTime = 0;
				totalAvgColTime = 0;//
				totalAvg2SatTime = 0;//
				totalMaxColTime = 0;//
				totalMax2SatTime = 0;//
				input = new File("tests/"+testing+"/test" + test + "/pointsamm_" + points + ".txt");
				output = new File("tests/"+testing+"/test" + test + "/pointsamm_" + points + "_solved.txt");
				for(int iteration = 0; iteration < repeat; iteration++){
					avgColTimeLoop = 0;
					maxColTimeLoop = 0;
					avg2SatTimeLoop = 0;
					max2SatTimeLoop = 0;
					startTime = System.nanoTime();
					try{
						if(local){
							sc = new Scanner(input);
						}
						else{
							System.out.println("Gib input pl0x");
							sc = new Scanner(System.in);
						}
						readInput();
					} catch (FileNotFoundException e){
						System.out.println("Input file not found: " + input.getName());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					solvePlacementProblem();
					writeOutput();	
					if(!pModel.equals(PlacementModel.ONESLIDER)){
						try{
							long time = System.nanoTime();
							plane.checkFinalSolution();
							finalCheckTime += (System.nanoTime() - time);
						}
						catch(Exception e){
							System.out.println(test + ";" + iteration);
							e.printStackTrace();
						}
					}
					averageRunningTime += (System.nanoTime()-startTime);
					
					avgColTime += avgColTimeLoop;
					avg2SatTime += avg2SatTimeLoop;
					if(max2SatTimeLoop > max2SatTime){
						max2SatTime = max2SatTimeLoop;
					}
					if(maxColTimeLoop > maxColTime){
						maxColTime = maxColTimeLoop;
					}
					if(max2SatTimeLoop > max2SatTime){
						max2SatTime = max2SatTimeLoop;
					}
					if(maxColTimeLoop > maxColTime){
						maxColTime = maxColTimeLoop;
					}
				}
				maxHeightTime /= repeat;
				initTime /= repeat;
				avgColTime /= repeat;
				avg2SatTime /= repeat;
				nrOfLoops /= repeat;
				placementTime /= repeat;
				finalCheckTime /= repeat;
				averageRunningTime /= repeat;
				totalAvgColTime /= repeat;//
				totalAvg2SatTime /= repeat;//
								
				if(pModel == PlacementModel.TWOPOS){
					s = ""+points+"_"+test+";"+maxHeight+";"+realHeight+";"+round(maxHeightTime)+";"+round(initTime)+";"+round(avgColTime)+";"+round(maxColTime)+";"+round(totalAvgColTime)+";"+round(totalMaxColTime)
							+";"+round(totalAvg2SatTime)+";"+round(totalMax2SatTime)+";"+round(avg2SatTime)+";"+round(max2SatTime)+";"+nrOfLoops+";"+round(placementTime)+";"+round(finalCheckTime)+";"+round(averageRunningTime);

				}
				else if(pModel == PlacementModel.ONESLIDER){
					s = ""+points+"_"+test+";"+maxHeight+";"+realHeight+";"+round(maxHeightTime)+";"+round(initTime)+";"+round(avgColTime)+";"+round(maxColTime)+";"+round(totalAvgColTime)+";"+round(totalMaxColTime)
							+";"+nrOfLoops+";"+round(finalCheckTime)+";"+round(averageRunningTime);
				}
				else{
					s = "";
				}
				s = s.replaceAll("\\.", ",");
				writer.write(s);
				writer.newLine();
			}
		}
		writer.close();
	}
	
	private double round(double d){
		d /= 100000d;
		d = Math.round(d);
		d /= 10d;
		return d;
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

			plane = new Plane(ratio, points, pModel);
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

			plane = new Plane(ratio, points, pModel);
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
			bw.write("height: " + plane.getHeight());
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
			System.out.println("height: " + plane.getHeight());
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
