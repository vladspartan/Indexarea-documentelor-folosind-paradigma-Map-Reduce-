import java.util.ArrayList;
import java.util.HashMap;

public class PartialSolutionReduce extends PartialSolution {

	ArrayList<HashMap<String, Integer>> wordsList;
	HashMap<String, Float> freqWord;
	String fileName;
	
	PartialSolutionReduce(ArrayList<HashMap<String, Integer>> r, HashMap<String, Float> freq, String file) {
		wordsList = r;
		freqWord = freq;
		fileName = file;
	}
	
	public String toString() {
		return "Reduce " + fileName;
	}
}
