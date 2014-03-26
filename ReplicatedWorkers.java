import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Clasa ce reprezinta un thread worker.
 */
class Worker extends Thread {
	WorkPool wp;

	public Worker(WorkPool workpool) {
		this.wp = workpool;
	}


	void processPartialSolution(PartialSolution ps) {
		if(ps instanceof PartialSolutionMap) {
			processPartialSolutionMap((PartialSolutionMap)ps);
		} else if(ps instanceof PartialSolutionReduce){
			processPartialSolutionReduce((PartialSolutionReduce)ps);
		} else {
			processPartialSolutionResult((PartialSolutionResult)ps);
		}
	}

	// executare task de tip map
	private void processPartialSolutionMap(PartialSolutionMap ps) {
		String fileName = ps.fileName;
		String word = "";
		int n=0;
		char ch;
		HashMap<String, Integer> res = ps.res;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			
			// se verifica daca fragmentul incepe in mijlocul unui cuvant
			if(ps.posStart > 0) {
				reader.skip(ps.posStart-1);		
				ch = (char) reader.read();
				while(ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= 0 && ch <=9) {
					ch = (char) reader.read();
					ps.posStart++;
				}
			}
			
			// pentru fiecare caracter din fragment
			for(int i=ps.posStart; ; i++) {
				ch = (char) reader.read();
				// daca este litera sau cifra se adauga la sfarsitul cuvantului curent
				if(ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= 0 && ch <=9) {
					word += ch;
				} else { // daca s-a ajuns la un separator se adauga cuvantul format in res si se creste numarul de aparitii
					if(word != "") {
						word = word.toLowerCase();
						if(!res.containsKey(word)) {
							res.put(word, 1);
						} else {
							n = res.get(word);
							n++;
							res.put(word, n);
						}
					}
					word = "";
					if(i >= ps.posEnd) {
						break;
					}
				}
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	// executare task de tip reduce
	private void processPartialSolutionReduce(PartialSolutionReduce ps) {

		ArrayList<HashMap<String, Integer>> wordsList = ps.wordsList;
		HashMap<String, Float> freqWords = ps.freqWord;
		HashMap<String, Integer> totalWords = new HashMap<String, Integer>();
		int wordsNo = 0;
		
		// pentru fiecare cuvant din fiecare fragment din document
		for(int i=0; i<wordsList.size(); i++) {
			HashMap<String, Integer> words = wordsList.get(i);
			// se calculeaza numarul total de aparitii din document
			for(Map.Entry<String, Integer> entry : words.entrySet()) {
				if(!totalWords.containsKey(entry.getKey())) {
					totalWords.put(entry.getKey(), entry.getValue());
				} else {
					int val = totalWords.get(entry.getKey());
					val += entry.getValue();
					totalWords.put(entry.getKey(), val);
				}
				wordsNo += entry.getValue();
			}
		}
		
		float f;
		// pentru fiecare cuvant din fisier se calculeaza frecventa de aparitie
		for(Map.Entry<String, Integer> entry : totalWords.entrySet()) {
			f = (float) entry.getValue() / wordsNo * 100;
			freqWords.put(entry.getKey(), f);
		}
		
	}
	
	// executare task ce compara 2 fisiere plecand de la hashmap-urile (Cuvinte, Frecvente) asociate lor
	private void processPartialSolutionResult(PartialSolutionResult ps) {
		HashMap<String, Float> freqWordsDocCompare = ps.freqWordsDocCompare;
		HashMap<String, Float> freqWordsOtherDoc = ps.freqWordsOtherDoc;
		ArrayList<Float> sim = ps.sim;
		float s = 0;
		
		// se calculeaza gradul de similaritate intre cele 2 documente
		//pentru fiecare cuvant din primul document
		for(Map.Entry<String, Float> entry : freqWordsDocCompare.entrySet()) {
			// daca apare si in cel de-al doilea document
			if(freqWordsOtherDoc.containsKey(entry.getKey())) {
				s += entry.getValue() * freqWordsOtherDoc.get(entry.getKey());
			}
		}
		sim.add(0,s);
		
	}

	public void run() {
		System.out.println("Thread-ul worker " + this.getName() + " a pornit...");
		while (true) {
			PartialSolution ps = wp.getWork();
			if (ps == null)
				break;
			
			processPartialSolution(ps);
		}
		System.out.println("Thread-ul worker " + this.getName() + " s-a terminat...");
	}
}


public class ReplicatedWorkers {

	public static void main(String args[]) {
		
		if(args.length < 3) {
			System.out.println("Numar insuficient de argumente!");
			System.exit(-1);
		}
		
		// se preiau numarul de thread-uri, numele fisierului de intrare si numele fisierului de iesire
		int NT = Integer.parseInt(args[0]);
		String fileIn = args[1], fileOut = args[2];
		
		HashMap<String, ArrayList<HashMap<String, Integer>>> resultMap = new HashMap<String, ArrayList<HashMap<String, Integer>>>();
		HashMap<String, HashMap<String, Float>> resultReduce = new HashMap<String, HashMap<String, Float>>();
		HashMap<String, ArrayList<Float>> resultResult = new HashMap<String, ArrayList<Float>>();
		ArrayList<Worker> workers = new ArrayList<Worker>();
		BufferedReader reader = null;
		String docName = null;
		int D = 0, ND = 0;
		float X = 0;
		
		boolean docContained = true;
		
		// lista cu numele fisierelor ce trebuie indexate
		ArrayList<String> NamesOfDocs = new ArrayList<String>();
		
		try {
			reader = new BufferedReader(new FileReader(fileIn));

			// se preia din fisier numele documentului caruia i se doreste aflarea gradului de plagiere
			docName = reader.readLine();

			D = Integer.parseInt(reader.readLine());
			X = Float.parseFloat(reader.readLine());
			ND = Integer.parseInt(reader.readLine());
			
			for(int i=0; i<ND; i++) {
				NamesOfDocs.add(i, reader.readLine());
			}
			
			// daca fisierul ce trebuie verificat nu se afla in lista fisierelor ce trebuie indexate, atunci se adauga
			if(!NamesOfDocs.contains(docName)) {
				NamesOfDocs.add(ND, docName);
				docContained = false;
				ND++;
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		float dimFile=0;
		int posStart=0, posEnd = 0;
		
		// se creaza un WorkPool in care vor fi adaugate task-uri pentru operatia de map
		WorkPool wp = new WorkPool(NT);
		for(int i=0; i<ND; i++) {
			
			// lista cu hashmap-uri de forma (cuvant, nr aparitii) - cate un hashmap pentru fiecare fragment din fisier
			ArrayList<HashMap<String, Integer>> resMap = new ArrayList<HashMap<String, Integer>>();
			
			File file = new File(NamesOfDocs.get(i));
			int sizeFile = (int) file.length();
			
			if(sizeFile % D == 0) {
				dimFile = sizeFile / D;
			} else {
				dimFile = sizeFile / D + 1;
			}
			
			// pentru fiecare fragment din fisier
			for(int j=0; j<dimFile; j++) {
				// in res se salveaza perechi de forma (cuvant, numar de aparitii in fragment)
				HashMap<String, Integer> res = new HashMap<String, Integer>();
				
				// se determina pozitia de start si de final a fragmentului
				posStart = j * D;
				posEnd = (j + 1) * D - 1;
				if(sizeFile % D != 0 && j == dimFile - 1) {
					posEnd = sizeFile - 1;
				}
				// se trimite spre procesare fragmentul 
				PartialSolutionMap psM = new PartialSolutionMap(NamesOfDocs.get(i), posStart, posEnd, res);
				wp.putWork(psM);
				
				// in resMap se adauga rezultatul obtinut pentru fragmentul j
				resMap.add(j,res);
			}
			
			// se salveaza toate solutiile partiale obtinute pentru fiecare document
			resultMap.put(NamesOfDocs.get(i), resMap);
		}
		
		// fiecare thread va procesa un fragment dintr-un fisier
		for(int i=0; i<NT; i++) {
			Worker worker = new Worker(wp);
			worker.start();
			workers.add(i, worker);
		}
		
		// se asteapta ca toate task-urile sa isi incheie executia inainte de a se trece la pasul urmator
		for(int i=0; i<NT; i++) {
			try {
				workers.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// se creaza un WorkPool in care vor fi adaugate task-uri 
		// pentru obtinerea unei liste cu frecventa pentru fiecare cuvant din fiecare fisier 
		WorkPool wpR = new WorkPool(NT);
		// pentru fiecare document
		for(int i=0; i<ND; i++) {
			// se preia rezultatul obtinut in pasul de mapare
			ArrayList<HashMap<String, Integer>> r = resultMap.get(NamesOfDocs.get(i));
			// in freqWords se retin perechi de forma (cuvant, frecventa)
			HashMap<String, Float> freqWords = new HashMap<String, Float>();
			// se trimite spre procesare
			PartialSolutionReduce psR = new PartialSolutionReduce(r, freqWords, NamesOfDocs.get(i));
			wpR.putWork(psR);
			// in resultReduce se salveaza toate solutiile obtinute pentru fiecare document,
			// sub forma (numeFisier, HashMap cu frecvente pentru fiecare cuvant)
			resultReduce.put(NamesOfDocs.get(i), freqWords);
		}
		
		// se pornesc task-urile de tip reduce
		for(int i=0; i<NT; i++) {
			Worker worker = new Worker(wpR);
			worker.start();
			workers.add(i, worker);
		}
		
		// se asteapta inchierea tututor thread-urilor
		for(int i=0; i<NT; i++) {
			try {
				workers.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// se creaza un WorkPool in care vor fi adaugate task-uri pentru operatia de result
		WorkPool wpRes = new WorkPool(NT);
		for(int i=0; i<ND; i++) {
			// in sim, pe pozitia 0, se salveaza gradul de similaritate pentru fiecare document indexat
			ArrayList<Float> sim = new ArrayList<Float>();
			
			// se preiau rezultatele obtinute in pasul anterior
			
			// frecventele cuvintelor din fisierul principal
			HashMap<String, Float> freqDoc = resultReduce.get(docName);
			// frecventele cuvintelor din fisierul cu care se compara
			HashMap<String, Float> freqOtherDoc = resultReduce.get(NamesOfDocs.get(i));
			
			// se trimite spre aflarea gradului de similaritate cele 2 documente
			PartialSolutionResult psRes = new PartialSolutionResult(docName, freqDoc, NamesOfDocs.get(i), freqOtherDoc, sim);
			wpRes.putWork(psRes);
			
			// in resultResult se salveaza pentru fiecare document gradul se similaritate
			resultResult.put(NamesOfDocs.get(i), sim);
		}
		
		// se pornesc thread-urile
		for(int i=0; i<NT; i++) {
			Worker worker = new Worker(wpRes);
			worker.start();
			workers.add(i, worker);
		}
		
		// se asteapta incheierea tututor thread-urilor
		for(int i=0; i<NT; i++) {
			try {
				workers.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		String resultsSorted[] = new String[ND];
		
		if(!docContained) {
			ND--;
		}
		
		// se sorteaza rezultatul final descrescator dupa gradul de similaritate
		for(int i=0; i<ND; i++) {
			float s1  = resultResult.get( NamesOfDocs.get(i) ).get(0);
			int n = 0;
			for(int j=0; j<ND; j++) {
				float s2  = resultResult.get( NamesOfDocs.get(j) ).get(0);
				if(s2 > s1) n++;
				else if(s1 == s2 && NamesOfDocs.get(i).compareTo(NamesOfDocs.get(j)) < 0) {
					n++;
				}
			}
			
			resultsSorted[n] = NamesOfDocs.get(i);
		}
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileOut));
			String firstLine = "Rezultate pentru: (" + docName + ")";
			writer.write (firstLine);
			writer.write("\n");
			for(int i=0; i<ND; i++) {
				String name = resultsSorted[i];
				
				float s = resultResult.get(name).get(0) / 100;

				String dm = new DecimalFormat("##.###").format(s);
				
				if(dm.contains(",")) {
					dm = dm.replace(",", ".");
				}

				s = Float.parseFloat(dm);

				// sunt scrise in fisier documentele care au gradul de similaritate mai mare ca X
				if(s > X) {
					writer.write("\n");
					
					String text = name + " (" + s + "%)";
					writer.write(text);
				}
			}
			
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
}