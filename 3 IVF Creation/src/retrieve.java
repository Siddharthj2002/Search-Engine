import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class retrieve {

    private Map<Integer, String> storyIdMap;
    private Map<String, PostingList> invertedLists;
    private List<Integer> docLengths;
    private long collectionSize;
    private int numDoc;

    public retrieve() {
        storyIdMap = new HashMap<Integer, String>();
        invertedLists = new HashMap<String, PostingList>();
        docLengths = new ArrayList<Integer>();
        collectionSize = 0;
        numDoc = 0;
    }

    public static void main(String[] args) {
        // Read arguments from command line; or use sane defaults for IDE.
        String inputFile = args.length >= 1 ? args[0] : "sciam.json.gz";
        String queriesFile = args.length >= 2 ? args[1] : "P3train.tsv";
        String outputFile = args.length >= 3 ? args[2] : "P3train.trecrun";

        retrieve obj = new retrieve();
        obj.buildIndex(inputFile);

        try (BufferedReader br = new BufferedReader(new FileReader(queriesFile))) {
            try (PrintWriter writer = new PrintWriter(outputFile)) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split("\t");
                    if (values[0].equals("or")) {
                        Set<Integer> uniqueValues = new HashSet<>();
                        for (int i = 2; i < values.length; i++) {
                            Map<Integer, Double> result = obj.retrievePhrase(values[i]);
                            uniqueValues.addAll(result.keySet());
                        }
                        int rank = 1;
                        for (Integer doc : uniqueValues) {
                            String output = String.format("%-11s %-4s %-18s %5d %s", values[1], "skip",
                                    obj.getStoryId(doc), rank++, "1.0000 siddharthjai");
                            writer.write(output + "\n");
                        }
                    } else if (values[0].equals("and")) {
                        Map<Integer, Double> result_0 = obj.retrievePhrase(values[2]);
                        Set<Integer> commonIntValues = new HashSet<>(result_0.keySet());
                        for (int i = 3; i < values.length; i++) {
                            Map<Integer, Double> resultMap = obj.retrievePhrase(values[i]);
                            Set<Integer> tempIntValues = new HashSet<>(resultMap.keySet());
                            commonIntValues.retainAll(tempIntValues);
                        }
                        int rank = 1;
                        for (Integer doc : commonIntValues) {
                            String output = String.format("%-11s %-4s %-18s %5d %s", values[1], "skip",
                                    obj.getStoryId(doc), rank++, "1.0000 siddharthjai");
                            writer.write(output + "\n");
                        }
                    } else if (values[0].equals("ql")) {

                        HashMap<String, Double> finalResults = new HashMap<String, Double>();
                        HashMap<Integer, Double> DocumentsNeeded = new HashMap<Integer, Double>();

                        double C = obj.getCollectionSize();
                        double mu = 300;

                        for (int i = 2; i < values.length; i++) {
                            Map<Integer, Double> results = obj.retrievePhrase(values[i]);
                            double cq = (double) obj.getTermFreq(values[i]);

                            results.forEach((key, value) -> {
                                DocumentsNeeded.putIfAbsent(key, 0.0);
                            });

                            DocumentsNeeded.forEach((key, value) -> {
                                double totalVal = DocumentsNeeded.get(key);
                                double fq = (results.containsKey(key)) ? results.get(key) : 0.0;
                                totalVal += Math.log((fq + (mu * (cq / C))) / ((double) obj.getDocLength(key) + mu));
                                DocumentsNeeded.put(key, totalVal);
                            });
                        }

                        DocumentsNeeded.forEach((key, value) -> {
                            String docName = obj.getStoryId(key);
                            finalResults.putIfAbsent(docName, value);
                        });

                        List<Map.Entry<String, Double>> entryList = new ArrayList<>(finalResults.entrySet());

                        Collections.sort(entryList, new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> entry1, Map.Entry<String, Double> entry2) {
                                int valueComparison = entry2.getValue().compareTo(entry1.getValue());
                                if (valueComparison == 0) {
                                    return entry2.getKey().compareTo(entry1.getKey());
                                }
                                return valueComparison;
                            }
                        });

                        int rank = 1;
                        for (Map.Entry<String, Double> entry : entryList) {
                            double score = entry.getValue();
                            String output = String.format("%-11s %-4s %-18s %5d %.4f%s", values[1],
                                    "skip",
                                    entry.getKey(), rank++, score, " siddharthjai");
                            writer.write(output + "\n");
                        }
                    } else {
                        HashMap<String, Double> finalResults = new HashMap<String, Double>();
                        HashMap<Integer, Double> DocumentsNeeded = new HashMap<Integer, Double>();

                        int N = obj.getDocCount();
                        double k1 = 1.8;
                        double b = 0.75;
                        double k2 = 5;
                        double avgdl = obj.getAverageDocLength();

                        HashMap<String, Double> qf_map = new HashMap<>();
                        for (int i = 2; i < values.length; i++) {
                            double qf_i = qf_map.getOrDefault(values[i], 0.0);
                            qf_map.put(values[i], qf_i + 1.0);
                        }

                        for (int i = 2; i < values.length; i++) {
                            Map<Integer, Double> results = obj.retrievePhrase(values[i]);
                            int n = results.size();

                            results.forEach((key, value) -> {
                                DocumentsNeeded.putIfAbsent(key, 0.0);
                            });

                            DocumentsNeeded.forEach((key, value) -> {
                                if (results.containsKey(key)) {
                                    int dl = obj.getDocLength(key);
                                    double K = k1 * ((1 - b) + b * (dl / avgdl));
                                    double f = results.get(key);
                                    double qf = 1;
                                    double val = Math.log((N - n + 0.5) / (n + 0.5)) * (((k1 + 1) * f) / (K + f))
                                            * (((k2 + 1) * qf) / (k2 + qf));
                                    double currVal = DocumentsNeeded.get(key) + val;
                                    DocumentsNeeded.put(key, currVal);
                                }
                            });
                        }

                        DocumentsNeeded.forEach((key, value) -> {
                            String docName = obj.getStoryId(key);
                            finalResults.putIfAbsent(docName, value);
                        });

                        List<Map.Entry<String, Double>> entryList = new ArrayList<>(finalResults.entrySet());

                        Collections.sort(entryList, new Comparator<Map.Entry<String, Double>>() {
                            @Override
                            public int compare(Map.Entry<String, Double> entry1, Map.Entry<String, Double> entry2) {
                                int valueComparison = entry2.getValue().compareTo(entry1.getValue());
                                if (valueComparison == 0) {
                                    return entry2.getKey().compareTo(entry1.getKey());
                                }
                                return valueComparison;
                            }
                        });

                        int rank = 1;
                        for (Map.Entry<String, Double> entry : entryList) {
                            double score = entry.getValue();
                            String output = String.format("%-11s %-4s %-18s %5d %.4f%s", values[1],
                                    "skip",
                                    entry.getKey(), rank++, score, " siddharthjai");
                            writer.write(output + "\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseFile(String filename) {
        JSONParser parser = new JSONParser();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(filename)), "UTF-8"));
            JSONObject jsonObject = (JSONObject) parser.parse(br);
            JSONArray stories = (JSONArray) jsonObject.get("corpus");

            for (int idx = 0; idx < stories.size(); idx++) {
                JSONObject story = (JSONObject) stories.get(idx);
                int docId = idx + 1;
                String storyId = (String) story.get("storyID");
                storyIdMap.put(docId, storyId);
                String text = (String) story.get("text");
                text = text.trim();
                String[] words = text.split("\\s+");
                docLengths.add(words.length);
                for (int pos = 0; pos < words.length; pos++) {
                    String word = words[pos];
                    PostingList postingList = invertedLists.computeIfAbsent(word, k -> new PostingList());
                    postingList.add(docId, pos + 1);
                }
                numDoc = docId;
                collectionSize += words.length;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildIndex(String sourcefile) {
        parseFile(sourcefile);
    }

    public PostingList getPostings(String term) {
        PostingList list = invertedLists.get(term);
        list.startIteration();
        return list;
    }

    public int getTermFreq(String term) {
        return invertedLists.get(term).termFrequency();
    }

    public int getDocFreq(String term) {
        return invertedLists.get(term).documentCount();
    }

    public int getDocCount() {
        return numDoc;
    }

    public int getDocLength(int docId) {
        return docLengths.get(docId - 1);
    }

    public double getAverageDocLength() {
        return (double) collectionSize / (double) numDoc;
    }

    public long getCollectionSize() {
        return collectionSize;
    }

    public String getStoryId(int docId) {
        return storyIdMap.get(docId);
    }

    private Posting calculateWindows(ArrayList<Posting> postings) {
        int prev;
        int distance = 1;
        boolean found = false;
        Posting p_result = null;
        if (postings.size() == 1) {
            return postings.get(0);
        }
        Integer[] p0 = postings.get(0).getPositionsArray();
        for (int i = 0; i < p0.length; i++) {
            prev = p0[i];
            for (int j = 1; j < postings.size(); j++) {
                Integer[] p = postings.get(j).getPositionsArray();
                found = false;
                for (int k = 0; k < p.length; k++) {
                    int cur = p[k];
                    if (prev < cur && cur <= prev + distance) {
                        found = true;
                        prev = cur;
                        break;
                    }
                }
                if (!found) {
                    break;
                }
            }
            if (found) {
                if (p_result == null) {
                    p_result = new Posting(postings.get(0).getDocId(), p0[i]);
                } else {
                    p_result.add(p0[i]);
                }
            }
        }
        return p_result;
    }

    private boolean allHaveMore(PostingList[] lists) {
        boolean hasMore = true;
        for (PostingList l : lists) {
            hasMore = hasMore && l.hasMore();
        }
        return hasMore;
    }

    private int candidate(PostingList[] lists) {
        int max = -1;
        for (PostingList l : lists) {
            if (l.hasMore() && l.getCurrentPosting().getDocId() > max) {
                max = l.getCurrentPosting().getDocId();
            }
        }
        return max;
    }

    private boolean allMatch(PostingList[] lists, Integer candidate) {
        for (PostingList l : lists) {
            if (!(l.hasMore() && l.getCurrentPosting().getDocId().equals(candidate))) {
                return false;
            }
        }
        return true;

    }

    protected PostingList intersectPostings(PostingList[] lists) {
        PostingList postingList = new PostingList();

        ArrayList<Posting> matchingPostings = new ArrayList<Posting>();

        while (allHaveMore(lists)) {
            Integer next = candidate(lists);
            for (PostingList l : lists) {
                l.skipTo(next);
            }

            if (allMatch(lists, next)) {
                for (PostingList child : lists)
                    matchingPostings.add(child.getCurrentPosting());
                Posting p = calculateWindows(matchingPostings);
                if (p != null) {
                    postingList.add(p);
                }
            }
            matchingPostings.clear();
            for (PostingList l : lists) {
                l.skipTo(next + 1);
            }
        }
        postingList.startIteration();
        return postingList;
    }

    public Map<Integer, Double> retrievePhrase(String query) {
        Map<Integer, Double> result = new HashMap<Integer, Double>();
        String[] queryTerms = query.split("\\s+");
        PostingList[] lists = new PostingList[queryTerms.length];
        for (int i = 0; i < queryTerms.length; i++) {
            lists[i] = getPostings(queryTerms[i]);
        }
        PostingList finalList = lists[0];
        if (lists.length > 1) {
            finalList = intersectPostings(lists);
        }
        for (Integer doc = 1; doc <= getDocCount(); doc++) {
            finalList.skipTo(doc);
            Posting p = finalList.getCurrentPosting();
            if (p != null && p.getDocId().equals(doc)) {
                result.put(p.getDocId(), p.getTermFreq().doubleValue());
            }
        }
        return result;
    }
}
