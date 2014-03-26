import java.util.HashMap;

public class PartialSolutionMap extends PartialSolution {

	public String fileName;
	public int posStart, posEnd;
	HashMap<String, Integer> res;
	
	public PartialSolutionMap(String name, int s, int e, HashMap<String, Integer> r) {
		fileName = name;
		posStart = s;
		posEnd = e;
		res = r;
	}
	
	public String toString() {
		return "Map: " + fileName + " (" + posStart + ", " + posEnd + ")";
	}
}
