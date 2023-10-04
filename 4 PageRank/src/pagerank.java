package src;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class pagerank {

	/**
 	* @param args <inputFile> <lambda> <tau> <inlinkFileName> <pageRankFilename> <k>
 	*/

	private static Map<String, Integer> pageInlinks = new HashMap<String, Integer>(); 
	private static List<String> urls = new ArrayList<String>(); 
	private static List<List<Integer>> links = new ArrayList<List<Integer>>(); 

	public static void main(String[] args) throws IOException {

		// Read arguments from command line; or use sane defaults for IDE.
		String inputFile = args.length >= 1 ? args[0] : "links.srt.gz"; 

		double lambda = args.length >= 2 ? Double.parseDouble(args[1]) : 0.2;
		double tau = args.length >= 3 ? Double.parseDouble(args[2]) : 0.005;
		String inlinksFile = args.length >= 4 ? args[3] : "inlinks.txt";
		String pagerankFile = args.length >= 5 ? args[4] : "pagerank.txt";
		int k = args.length >= 6 ? Integer.parseInt(args[5]) : 100;

		webGraphGenerator(inputFile); // Generating the web graph. Basically making the source, destination page for all the webpages/ urls

		double[] pageRanks = calculatePageRank(lambda, tau); // Calculating the pageRanks for each webpage/url

		int rank = 1;
		int minK = Math.min(urls.size(), k);
		PriorityQueue<Map.Entry<String, Double>> prPairs = new PriorityQueue<>(Map.Entry.<String, Double>comparingByValue());
		
		// Finding webpages with top k pageRanks
		for (int i = 0; i < urls.size(); i++) {
			Map.Entry<String, Double> doc = new AbstractMap.SimpleEntry<String, Double>(urls.get(i), pageRanks[i]);
			prPairs.add(doc);
			if (prPairs.size() > minK) {
				prPairs.poll();
			}
		}
		ArrayList<Map.Entry<String, Double>> topPRPairs = new ArrayList<Map.Entry<String, Double>>();
		ArrayList<String> topPR = new ArrayList<>();
		topPRPairs.addAll(prPairs);
		topPRPairs.sort(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())); // Sorting the pagerank values in descending order
		
		// Converting the key, value pairs into strings seperates by tabs
		for (Map.Entry<String, Double> t : topPRPairs) {
			topPR.add(t.getKey() + "\t" + rank++ + "\t" + t.getValue());
		}

		// Printing the sorted arraylist in the output file
		Path output = Paths.get(pagerankFile);
		Files.write(output, topPR);


		rank = 1;
		minK = Math.min(pageInlinks.size(), k);
		PriorityQueue<Map.Entry<String, Integer>> inlinkPairs = new PriorityQueue<>(Map.Entry.<String, Integer>comparingByValue());
		for (Map.Entry<String, Integer> doc : pageInlinks.entrySet()) {
			inlinkPairs.add(doc);
			if (inlinkPairs.size() > minK) {
				inlinkPairs.poll();
			}
		}
		ArrayList<Map.Entry<String, Integer>> topInlinkPairs = new ArrayList<Map.Entry<String, Integer>>();
		ArrayList<String> topInlinks = new ArrayList<>();
		topInlinkPairs.addAll(inlinkPairs);
		topInlinkPairs.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())); // Sorting the inlink values in descending order

		// Converting the key, value pairs into strings seperates by tabs
		for (Map.Entry<String, Integer> t : topInlinkPairs) {
			topInlinks.add(t.getKey() + "\t" + rank++ + "\t" + t.getValue());
		}

		// Printing the sorted arraylist in the output file
		output = Paths.get(inlinksFile);
		Files.write(output, topInlinks);
	}
	
	/*
	 * Calculates the pageRank 
	 * It first assigns a page rank to all the pages: 1 / N
	 * In each iteration, it creates a result vector R which stores lambda/the urls size in each entry
	 * Then we calculate the probability of a random jump and add it to each quantity in R
	 * We keep doing this till the page rank values converge
	 */
	public static double[] calculatePageRank(double lambda, double tau) {
		double[] I = new double[urls.size()]; // Input vector
		double[] R = new double[urls.size()]; // Page rank vector with better estimate

		double initialPR = 1 / (double)urls.size(); // Intial pagerank = 1/N so that the sum = 1
		double randomJumpWeight = 0.0;
		double l2_norm = 0.0;

		boolean hasNotConverged = true;
	
		Arrays.fill(I, initialPR); // Assigning inital values to the input vector

		while(hasNotConverged) {
			Arrays.fill(R, lambda / urls.size()); // Updating the vector with better estimates of the pageranks
			randomJumpWeight = 0.0;

			for (int p = 0; p < urls.size(); p++) { // computing the probability of landing on a page because of a clicked link
				List<Integer> outlinks = links.get(p); // The set of pages such that (p,q) ∈ L and q ∈ P
				if (outlinks.size() > 0) {
					for (Integer q : outlinks) {
						R[q] += (1 - lambda) * I[p] / ((double) outlinks.size()); // Probability Ip of being at page p
					}
				} else {
					randomJumpWeight += (1 - lambda) * I[p] / ((double) urls.size());
				}
			}

			for (int q = 0; q < R.length; q++) {
				R[q] += randomJumpWeight;
			}
		
			l2_norm = l2NormCalculator(I, R); // Calculating the new L2 norm
			if(l2_norm < tau){ // Checking for convergence
				hasNotConverged = false;
				break; // Exiting the loop if values have converged 
			}

			for (int i = 0; i < R.length; i++) {
				I[i] = R[i]; // Updating the current PageRank estimate
			}
		}
		return R; // Returning the final estimate for the pageranks
	}

	/*
	 * Basically a helper function for the calculatePageRank() function
	 * It helps calculate the l2 norm for a certain p, q pair so that it can be compared with tau to check convergence of the page rank values
	 */
	private static double l2NormCalculator(double[] p, double[] q) {
		double norm = 0;
		for (int i = 0; i < p.length; i++)
			norm += Math.pow(q[i] - p[i], 2); //Finding the Euclidean distance
		return Math.sqrt(norm);
	}

	private static void webGraphGenerator(String inputFile) throws IOException {
		// Reading the input file
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new FileInputStream(inputFile)), "UTF-8"));
		Map<String, Integer> urlMap = new HashMap<String, Integer>();
		String str;
		while ((str = br.readLine()) != null) { // Iterating over each line of the file
			String[] tokens = str.split("\t"); // Tokenizing using "\t" as delimitter
			if (tokens.length == 2) {
				String sourcePage = tokens[0];
				String destinationPage = tokens[1];
				int sourceId, destId;
				if (urlMap.containsKey(sourcePage)) {
					sourceId = urlMap.get(sourcePage);
				} else {
					sourceId = urls.size();
					urlMap.put(sourcePage, sourceId);
					urls.add(sourcePage);
					links.add(new ArrayList<Integer>());
				}
				if (urlMap.containsKey(destinationPage)) {
					destId = urlMap.get(destinationPage);
				} else {
					destId = urls.size();
					urlMap.put(destinationPage, destId);
					urls.add(destinationPage);
					links.add(new ArrayList<Integer>());
				}
				links.get(sourceId).add(destId);
				pageInlinks.put(sourcePage, pageInlinks.getOrDefault(sourcePage, 0)); // Finding inlinks of the source pages
				pageInlinks.put(destinationPage, pageInlinks.getOrDefault(destinationPage, 0) + 1); // Finding inlinks of the destination pages
			}
		}
		br.close();
	}
}