import java.util.Arrays;

/*
 * Mergesort by Lars Vogel www.vogella.com
 * http://www.vogella.com/tutorials/JavaAlgorithmsMergesort/article.html
 * Site accessed on 21-11-2014
 * 
 * Class edited by Stephan Oostveen such that it returns an array of pointers
 */

public class MergeSort {
	private static Point[] numbers;
	private static Point[] helper;

	private static int n;
	private static int[] pointers;
	private static int[] pointersHelper;

	public static int[] sort(Point[] values) {
		n = values.length;
		numbers = Arrays.copyOf(values, n);
		helper = new Point[n];
		pointers = new int[n];
		pointersHelper = new int[n];

		for(int i=0; i<n; i++){
			pointers[i]=i;
		}

		mergesort(0, n - 1);
		return pointers;
	}

	private static void mergesort(int low, int high) {
		// check if low is smaller then high, if not then the array is sorted
		if (low < high) {
			// Get the index of the element which is in the middle
			int middle = low + (high - low) / 2;
			// Sort the left side of the array
			mergesort(low, middle);
			// Sort the right side of the array
			mergesort(middle + 1, high);
			// Combine them both
			merge(low, middle, high);
		}
	}

	private static void merge(int low, int middle, int high) {

		// Copy both parts into the helper array
		for (int i = low; i <= high; i++) {
			helper[i] = numbers[i];
			pointersHelper[i] = pointers[i];
		}

		int i = low;
		int j = middle + 1;
		int k = low;
		// Copy the smallest values from either the left or the right side back
		// to the original array
		while (i <= middle && j <= high) {
			if (helper[i].getX() < helper[j].getX()) {
				numbers[k] = helper[i];
				pointers[k] = pointersHelper[i];
				i++;
			} 
			else if(helper[i].getX() == helper[j].getX()){
			    if(helper[i].getY() <= helper[j].getY()){
			        numbers[k] = helper[i];
			        pointers[k] = pointersHelper[i];
			        i++;
			    }
			    else{
			        numbers[k] = helper[j];
			        pointers[k] = pointersHelper[j];
			        j++;
			    }
			}
			else {
				numbers[k] = helper[j];
				pointers[k] = pointersHelper[j];
				j++;
			}
			k++;
		}
		// Copy the rest of the left side of the array into the target array
		while (i <= middle) {
			numbers[k] = helper[i];
			pointers[k] = pointersHelper[i];
			k++;
			i++;
		}
	}
} 