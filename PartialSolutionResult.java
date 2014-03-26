import java.util.ArrayList;
import java.util.HashMap;

public class PartialSolutionResult extends PartialSolution{

	String docToCompare, otherDoc;
	HashMap<String, Float> freqWordsDocCompare;
	HashMap<String, Float> freqWordsOtherDoc;
	ArrayList<Float> sim;
	
	PartialSolutionResult(String docName, HashMap<String, Float> f1, String other, HashMap<String, Float> f2, ArrayList<Float> s) {
		docToCompare = docName;
		otherDoc = other;
		freqWordsDocCompare = f1;
		freqWordsOtherDoc = f2;
		sim = s;
	}
	
	public String toString() {
		return "Compare <" + docToCompare + ", " + otherDoc + ">";
	}
}
