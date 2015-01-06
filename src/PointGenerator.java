import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class PointGenerator {

    private Random r;
    private File output;
    private ArrayList<Point> p = new ArrayList<Point>(); 
    private double ratio;
    private double density;
    private String placementModel;
    private boolean noise = false;

    private int startOctave = 3;

    private int test = 10;

    public static void main(String[] args){
        new PointGenerator().handler();
    }

    public void handler(){
    	r = new Random(test);
		//for(int id = 100; id <= 10000; id += 100){
		//int id = 100000;
			output = new File("tests/gaatfout/10_4pos_notdense.txt");
			placementModel = "4pos";
			ratio = r.nextFloat() * 3;
			density = 0.001;
			int pointsAmount = 10;
			int width = 100;
			int height = 100;
			
			
			
			
			if(noise){
				System.out.println("Generating noise");
				float[][] baseNoise = GenerateWhiteNoise(width, height);
				//float[][] baseNoise = GetEmptyArray(width,height);
		        float[][] perlinNoise =  GeneratePerlinNoise(baseNoise, 5);
		        perlinNoise = AdjustLevels(perlinNoise, 0.0f, 1.0f);
		        
		        for(int i = 0; i < width; i++){
					for(int j = 0; j < height; j++){
						if(perlinNoise[i][j] > 0.5d){
							if(r.nextDouble() < density){
								p.add(new Point(i,j));
							}
							
						}
					}
				}
		        Collections.shuffle(p);
				
			}
			else {
				r.setSeed((long) test * 10000 * 4587);
				createPoints(pointsAmount);
				Collections.shuffle(p);
			}
			
			try {
				writeOutput();
			} catch (IOException e) {
				e.printStackTrace();
			}
		//}
		System.out.println("Done");

    }


    public void createPoints(int pointsAmount){
        for(int i = 0; i < 1000000000; i++){
            //if(r.nextDouble() < density){
            //	p.add(new Point(i,i));
            //}
            //if(p.size() >= pointsAmount) return;
            for(int j = 0; j < i; j++){
                if(r.nextDouble() < density){
                    Point point = new Point(i,j);
                    boolean in = false;
                    for(int k = 0; k < p.size(); k++){
                        if(p.get(k).getX() == i && p.get(k).getY() == j){ in = true; break; }
                    }
                    if(!in){ p.add(point); }
                }
                if(p.size() >= pointsAmount) return;
            }
            for(int j = 0; j < i; j++){
                if(r.nextDouble() < density){
                    Point point = new Point(j,i);
                    boolean in = false;
                    for(int k = 0; k < p.size(); k++){
                        if(p.get(k).getX() == j && p.get(k).getY() == i){ in = true; break; }
                    }
                    if(!in){ p.add(point); }
                }
                if(p.size() >= pointsAmount) return;
            }



        }

    }

    public void writeOutput() throws IOException{
        output.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(output));
        bw.write("placement model: " + placementModel);
        bw.newLine();
        bw.write("aspect ratio: " + ratio);
        bw.newLine();
        bw.write("number of points: " + p.size());
        bw.newLine();

        for(int i=0; i< p.size(); i++){
            bw.write("" + p.get(i).getX() + " " + p.get(i).getY());
            bw.newLine();
        }

        bw.close();

    }


    public void donoise(){
        int width = 500;
        int height = 500;

        System.out.println("Generating noise");
        float[][] baseNoise = GenerateWhiteNoise(width, height);
        //float[][] baseNoise = GetEmptyArray(width,height);
        float[][] perlinNoise =  GeneratePerlinNoise(baseNoise, 8);
        perlinNoise = AdjustLevels(perlinNoise, 0.0f, 1.0f);




    }

    public float[][] AdjustLevels(float[][] image, float low, float high)
    {
        int width = image.length;
        int height = image[0].length;

        float[][] newImage = GetEmptyArray(width, height);

        for (int i = 0; i < width; i++)
        {
            for(int j = 0; j < height; j++)
            {
                float col = image[i][j];

                if (col <= low)
                {
                    newImage[i][j] = 0;
                }
                else if (col >= high)
                {
                    newImage[i][j] = 1;
                }
                else
                {
                    newImage[i][j] = (col - low) / (high - low);
                }
            }
        }

        return newImage;
    }


    private float[][] GenerateWhiteNoise(int width, int height)
    {
        Random random = new Random(); //Seed to 0 for testing
        float[][] noise = GetEmptyArray(width,height);

        for (int i = 0; i < width; i++)
        {
            for (int j = 0; j < height; j++)
            {
                noise[i][j] = (float)random.nextDouble() % 1;
            }
        }

        return noise;
    }

    float[][] GenerateSmoothNoise(float[][] baseNoise, int octave)
    {
        int width = baseNoise.length;
        int height = baseNoise[0].length;

        float[][] smoothNoise = GetEmptyArray(width, height);

        int samplePeriod = 1 << octave; // calculates 2 ^ k
        float sampleFrequency = 1.0f / samplePeriod;

        for (int i = 0; i < width; i++)
        {
            //calculate the horizontal sampling indices
            int sample_i0 = (i / samplePeriod) * samplePeriod;
            int sample_i1 = (sample_i0 + samplePeriod) % width; //wrap around
            float horizontal_blend = (i - sample_i0) * sampleFrequency;

            for (int j = 0; j < height; j++)
            {
                //calculate the vertical sampling indices
                int sample_j0 = (j / samplePeriod) * samplePeriod;
                int sample_j1 = (sample_j0 + samplePeriod) % height; //wrap around
                float vertical_blend = (j - sample_j0) * sampleFrequency;

                //blend the top two corners
                float top = Interpolate(baseNoise[sample_i0][sample_j0],
                        baseNoise[sample_i1][sample_j0], horizontal_blend);

                //blend the bottom two corners
                float bottom = Interpolate(baseNoise[sample_i0][sample_j1],
                        baseNoise[sample_i1][sample_j1], horizontal_blend);

                //final blend
                smoothNoise[i][j] = Interpolate(top, bottom, vertical_blend);
            }
        }

        return smoothNoise;
    }

    private float[][] GetEmptyArray(int width, int height) {
        float[][] iets = new float[width][height];
        return iets;
    }

    private float Interpolate(float x0, float x1, float alpha){
        return x0 * (1 - alpha) + alpha * x1;
    }


    private float[][] GeneratePerlinNoise(float[][] baseNoise, int octaveCount)
    {
        int width = baseNoise.length;
        int height = baseNoise[0].length;

        float[][][] smoothNoise = new float[octaveCount][][]; //an array of 2D arrays containing

        float persistance = 0.5f;

        //generate smooth noise
        for (int i = startOctave; i < octaveCount; i++)
        {
            smoothNoise[i] = GenerateSmoothNoise(baseNoise, i);
        }

        float[][] perlinNoise = GetEmptyArray(width, height);
        float amplitude = 1.0f;
        float totalAmplitude = 0.0f;

        //blend noise together
        for (int octave = octaveCount - 1; octave >= startOctave; octave--)
        {
            amplitude *= persistance;
            totalAmplitude += amplitude;

            for (int i = 0; i < width; i++)
            {
                for (int j = 0; j < height; j++)
                {
                    perlinNoise[i][j] += smoothNoise[octave][i][j] * amplitude;
                }
            }
        }

        //normalisation
        for (int i = 0; i < width; i++)
        {
            for (int j = 0; j < height; j++)
            {
                perlinNoise[i][j] /= totalAmplitude;
            }
        }

        return perlinNoise;
    }


}
