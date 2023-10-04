import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class tokens {

	// The hardcoded stopword list
	static String allStopWords[] = { "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
			"has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to",
			"was", "were", "with" };

	private static final Pattern URL_PATTERN = Pattern.compile("https?://.*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern NUMBER_PATTERN = Pattern.compile("^[\\d\\-+,.-]*\\d+[\\d\\-+,.-]*$");
	private static final Pattern ABBREVIATION_PATTERN = Pattern.compile("^[a-zA-Z0-9.]*$");

	private static ArrayList<String> tokenList = new ArrayList<String>();

	public static void main(String[] args) throws IOException {
		// Read arguments from command line or use sane defaults for IDE
		String inputZipFile = args.length >= 1 ? args[0] : "sense-and-sensibility.gz";
		String outPrefix = args.length >= 2 ? args[1] : "SAS";
		String tokenize_type = args.length >= 3 ? args[2] : "fancy";
		String stoplist_type = args.length >= 4 ? args[3] : "yesStop";
		String stemming_type = args.length >= 5 ? args[4] : "porterStem";
		tokenizeFile(inputZipFile, outPrefix, tokenize_type, stoplist_type, stemming_type);
		dataCalculator(outPrefix);
	}

	/**
	 * Tokenizes the input file based on the specified tokenization method, stopword
	 * list, and stemming algorithm.
	 *
	 * @param inputZipFile  the input file to be tokenized
	 * @param outPrefix     the output file prefix
	 * @param tokenize_type the tokenization method to be used ("spaces" or "fancy")
	 * @param stopList_type whether to remove stop words ("yesStop" or "noStop")
	 * @param stemming_type the stemming algorithm to be used ("porterStem" or
	 *                      "noStem")
	 * @throws IOException if an I/O error occurs while reading or writing the files
	 */
	public static void tokenizeFile(String inputZipFile, String outPrefix, String tokenize_type, String stopList_type,
			String stemming_type) throws IOException {
		try (BufferedReader tokenizeReader = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new FileInputStream(inputZipFile))))) {
			try (BufferedWriter tokenizeWriter = new BufferedWriter(new FileWriter(outPrefix + "-tokens.txt"))) {
				String strLine;
				while ((strLine = tokenizeReader.readLine()) != null) {
					strLine = strLine.trim();
					if (!strLine.isEmpty()) {
						// Split the line into tokens using space as a delimiter
						ArrayList<String> tokens = new ArrayList<>(Arrays.asList(strLine.split("\\s+")));
						switch (tokenize_type) {
							case "spaces": // Tokenize based on spaces
								for (String token : tokens) {
									String newToken = token;
									if (stopList_type.equals("yesStop")) { // Handling stopword removal
										for (String stopWord : allStopWords) {
											if (newToken.equals(stopWord)) {
												newToken = "";
												break;
											}
										}
									}
									if (stemming_type.equals("porterStem")) // Handling stemming
										newToken = porterStemmer(newToken);
									if (newToken.equals(""))
										tokenizeWriter.write(token + "\n");
									else {
										tokenizeWriter.write(token + " " + newToken + "\n");
										tokenList.add(newToken);
									}
								}
								break;
							case "fancy": // Tokenize using the fancy tokenizer
								for (String token : tokens) {
									ArrayList<String> finalTokens = fancyTokenizer(token);
									if (stopList_type.equals("yesStop")) { // Handling stopword removal
										for (String stopWord : allStopWords) {
											while (finalTokens.contains(stopWord))
												finalTokens.remove(stopWord);
										}
									}
									StringBuilder finalToken = new StringBuilder(token);
									for (String ft : finalTokens) {
										if (stemming_type.equals("porterStem")) {
											ft = porterStemmer(ft); // Handling stemming
										}
										finalToken.append(" ");
										finalToken.append(ft);
										tokenList.add(ft);
									}
									tokenizeWriter.write(finalToken.toString() + "\n");
								}
								break;
							default:
								throw new IllegalArgumentException("Invalid tokenize_type: " + tokenize_type);
						}
					}
				}
				tokenizeWriter.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * This method calculates the word frequencies for the tokens in the input file
	 * and writes the results to output files with the specified prefix.
	 * 
	 * @param outPrefix The prefix for the output file names.
	 * @throws IOException If there is an error reading or writing to a file.
	 */
	public static void dataCalculator(String outPrefix) throws IOException {
		// Create a hashmap to store the word frequencies
		Map<String, Integer> wordFrequencies = new HashMap<>();
		try (BufferedWriter heapsWriter = new BufferedWriter(new FileWriter(outPrefix + "-heaps.txt"))) {
			int counter = 1;
			int numUniqueTokens = 0;
			// Iterate through each token in the list and add it to the hashmap
			for (String token : tokenList) {
				if (wordFrequencies.get(token) == null) {
					numUniqueTokens++;
					wordFrequencies.put(token, 1);
				} else {
					wordFrequencies.put(token, wordFrequencies.get(token) + 1);
				}
				// Write the number of tokens and number of unique tokens every 10 tokens
				if (counter % 10 == 0) {
					heapsWriter.write(counter + " " + numUniqueTokens + "\n");
				}
				counter++;
			}
			counter--;
			// Write the final number of tokens and number of unique tokens
			heapsWriter.write(counter + " " + numUniqueTokens + "\n");

			// Create a writer for the statistics output file
			BufferedWriter statsWriter = new BufferedWriter(new FileWriter(outPrefix + "-stats.txt"));

			statsWriter.write(counter + "\n" + numUniqueTokens + "\n");

			// Create a comparator to sort the hashmap key-value pairs in descending order
			// by value, then by key alphabetically for keys with same value
			Comparator<Map.Entry<String, Integer>> valueComparator = (e1, e2) -> {
				int valueCompare = e2.getValue().compareTo(e1.getValue());
				if (valueCompare == 0) {
					return e1.getKey().compareTo(e2.getKey());
				}
				return valueCompare;
			};

			// Create a new hashmap with the top 100 most frequent words, sorted by
			// frequency and then alphabetically for words with the same frequency
			LinkedHashMap<String, Integer> sortedMap = wordFrequencies.entrySet()
					.stream()
					.sorted(valueComparator)
					.limit(100)
					.collect(Collectors.toMap(Map.Entry::getKey,
							Map.Entry::getValue,
							(oldValue, newValue) -> oldValue,
							LinkedHashMap::new));

			// Write the 100 most frequent words and their frequencies to the statistics
			// output file
			for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
				statsWriter.write(entry.getKey() + " " + entry.getValue().toString() + "\n");
			}
			statsWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Tokenizes a given string into a list of words using fancy rules to handle
	 * URLs, numbers, abbreviations, hyphenated words and other punctuation.
	 * 
	 * @param token The string to tokenize.
	 * @return An ArrayList containing the individual words in the input string.
	 */
	public static ArrayList<String> fancyTokenizer(String token) {
		ArrayList<String> finalList = new ArrayList<String>();

		// Handling URLs
		Matcher matcher = URL_PATTERN.matcher(token);
		if (matcher.matches()) {
			token = matcher.group();
			token = token.replaceAll("\\p{P}+$", ""); // Removing trailing punctuation
			finalList.add(token);
			return finalList;
		}

		token = token.toLowerCase(); // Converting to lower case

		// Handling numbers
		matcher = NUMBER_PATTERN.matcher(token);
		if (matcher.matches()) {
			finalList.add(token);
			return finalList;
		}

		// Handling apostrophes
		token = token.replaceAll("'", "");

		if (token.contains(".")) { // Handling abbreviations
			StringBuffer sb = new StringBuffer();
			matcher = ABBREVIATION_PATTERN.matcher(token);
			while (matcher.find()) {
				matcher.appendReplacement(sb, matcher.group().replace(".", ""));
			}
			matcher.appendTail(sb);
			token = sb.toString();
		}

		if (token.contains("-")) { // Handling hyphenated words
			ArrayList<String> hyphenList = new ArrayList<>(Arrays.asList(token.split("-")));
			hyphenList.add((String.join("", hyphenList)));
			for (String word : hyphenList) {
				finalList.addAll(fancyTokenizer(word));
			}
		}

		// Handling remaining words
		if (finalList.size() == 0) {
			finalList.add(token);
		}
		ArrayList<String> finalTokens = new ArrayList<String>();
		ArrayList<String> new_list = new ArrayList<String>();

		for (String word : finalList) {
			new_list = new ArrayList<>(Arrays.asList(word.split("[^a-zA-Z0-9.-]+")));
			while (new_list.contains("")) // Removing empty strings from list
				new_list.remove("");
			if (new_list.size() == 1 && new_list.get(0) == token) { // Checking if token can further be tokenized or not
				finalTokens.add(token.trim());
			} else {
				for (String item : new_list) {
					finalTokens.addAll(fancyTokenizer(item)); // Tokenizing all the newly generated tokens
				}
			}
		}
		return finalTokens;
	}

	/**
	 * Implementing the steps A, B, and C of the Porter stemming algorithm
	 * 
	 * @param word the words to apply the rule to
	 * @return the modified word after applying the rules in order : A > B > C
	 */
	private static String porterStemmer(String word) throws IOException {
		return step1C(step1B(step1A(word)));
	}

	/**
	 * Implements step1B of the Porter Stemming algorithm
	 * 
	 * @param word the word to apply the rule to
	 * @return the modified word after applying the rule
	 */
	private static String step1A(String word) {

		// Replace sses by ss (e.g., stresses → stress)
		if (word.endsWith("sses")) {
			return word.substring(0, word.length() - 2);
		}

		// Replace ied or ies by i if preceded by more than one letter, otherwise by ie
		// (e.g., ties → tie, cries → cri)
		if (word.endsWith("ies") || word.endsWith("ied")) {
			return (word.length() >= 5) ? word.substring(0, word.length() - 2)
					: word.substring(0, word.length() - 1);
		}

		// If suffix is us or ss do nothing (e.g., stress → stress)
		if (word.endsWith("us") || word.endsWith("ss")) {
			return word;
		}

		// Delete s if the preceding word part contains a vowel not immediately before
		// the s (e.g., gaps → gap but gas → gas)
		if (word.endsWith("s") && word.length() > 2) {
			Pattern pattern = Pattern.compile(".*[aeiouy].*");
			Matcher matcher = pattern.matcher(word.substring(0, word.length() - 2));
			if (matcher.find()) {
				return word.substring(0, word.length() - 1);
			}
		}
		return word;
	}

	/**
	 * Implements step1B of the Porter Stemming algorithm
	 * 
	 * @param word the word to apply the rule to
	 * @return the modified word after applying the rule
	 */
	public static String step1B(String word) {
		String tempWord = word;
		// Replace eed, eedly by ee if it is in the part of the word after the first
		// non-vowel following a vowel (e.g., agreed → agree, feed → feed)
		if (word.endsWith("eedly") || word.endsWith("eed")) {
			tempWord = (word.endsWith("eedly")) ? word.substring(0, word.length() - 5)
					: word.substring(0, word.length() - 3);

			// Regex for making sure there is at least one vowel before a consonant
			Pattern pattern = Pattern.compile("[aeiouy].*?[^aeiouy]");
			Matcher matcher = pattern.matcher(tempWord);
			if (matcher.find()) {
				return tempWord = tempWord.concat("ee");
			}
			return word;
		}
		// Delete ed, edly, ing, ingly if the preceding word part contains a vowel
		if (word.endsWith("ed") || word.endsWith("edly") || word.endsWith("ing") || word.endsWith("ingly")) {
			
			// Removing the suffix
			if (word.endsWith("ingly")) {
				tempWord = word.substring(0, word.length() - 5);
			} else if (word.endsWith("edly")) {
				tempWord = word.substring(0, word.length() - 4);
			} else if (word.endsWith("ing")) {
				tempWord = word.substring(0, word.length() - 3);
			} else {
				tempWord = word.substring(0, word.length() - 2);
			}
			
			// Regex for finding vowels
			Pattern pattern = Pattern.compile("(.*)[aeiouy](.*)");
			Matcher matcher = pattern.matcher(tempWord);

			// If there are no vowels in the word, then we do nothing
			if (!matcher.find()) {
				return word;
			}

			word = tempWord;

			// If the word ends in at, bl, or iz add e
			// (e.g., fished → fish, pirating → pirate)
			if (tempWord.endsWith("at") || tempWord.endsWith("bl") || tempWord.endsWith("iz")) {
				word = tempWord.concat("e");
			}

			// If the word ends in bb, dd, ff, gg, mm, nn, pp, rr, or tt remove the last
			// letter
			// (e.g., falling→fall, dripping→drip)
			else if (tempWord.endsWith("bb") || tempWord.endsWith("dd") || tempWord.endsWith("ff")
					|| tempWord.endsWith("gg") || tempWord.endsWith("mm") || tempWord.endsWith("nn")
					|| tempWord.endsWith("pp") || tempWord.endsWith("rr") || tempWord.endsWith("tt")) {
				word = tempWord.substring(0, tempWord.length() - 1);
			}

			// If the word is short, add e
			// (e.g., us → use, shed → shede)
			else if (isShortWord(word)) {
				word = word.concat("e");
			}
		}
		return word;
	}

	/**
	 * Applies the first rule of the Porter stemming algorithm's step 1C to the
	 * given word.
	 *
	 * @param word the word to apply the rule to
	 * @return the modified word after applying the rule
	 */
	private static String step1C(String word) {
		
		// If the word ends in 'y' and the second-to-last letter is a consonant, replace
		// 'y' with 'i'
		if (word.endsWith("y") && word.length() > 2 && getLetterType(word.charAt(word.length() - 2)) != 'V') {
			word = word.substring(0, word.length() - 1) + 'i';
		}

		// Return the modified word
		return word;
	}

	/**
	 * Determines if the given word is a short word according to the Porter stemming
	 * algorithm.
	 *
	 * @param word the word to check
	 * @return true if the word is a short word, false otherwise
	 */
	private static boolean isShortWord(String word) {
		int len = word.length();

		// If word length is less than 2, it is not a short word
		if (len < 2) {
			return false;
		}

		// If word length is 2, it is a short word only if the first letter is a vowel
		// and the second is a consonant
		else if (len == 2) {
			return getLetterType(word.charAt(0)) == 'V' && getLetterType(word.charAt(1)) == 'C';
		}

		// for words longer than 2 letters
		else {

			// If the word ends with certain letters or its second-to-last letter is a
			// consonant, it is not a short word
			if (word.endsWith("x") || word.endsWith("w") || getLetterType(word.charAt(len - 1)) == 'V'
					|| getLetterType(word.charAt(len - 2)) == 'C') {
				return false;
			}

			// Otherwise, check if the word without its last two letters has a vowel in it
			String tempWord = word.substring(0, len - 2);
			Pattern pattern = Pattern.compile("(.*)[aeiouy](.*)");
			Matcher matcher = pattern.matcher(tempWord);

			if (matcher.find()) {
				return false;
			}
		}

		// if none of the above conditions are met, the word is a short word
		return true;
	}

	/**
	 * Determines if the given character is a vowel or consonant.
	 *
	 * @param c the character to check
	 * @return 'V' if the character is a vowel, 'C' if it is a consonant
	 */
	private static char getLetterType(char c) {
		return (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y') ? 'V' : 'C';
	}

}