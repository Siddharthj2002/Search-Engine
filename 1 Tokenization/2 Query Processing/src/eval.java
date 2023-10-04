import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class eval {

    public static void evaluate(String trecrunFile, String qrelsFile, String outputFile) {
        try (BufferedReader trecrunFileReader = new BufferedReader(new FileReader(trecrunFile));
                BufferedReader qrelsFileReader = new BufferedReader(new FileReader(qrelsFile));
                BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter(outputFile))) {

            String line;

            HashMap<String, HashMap<String, Integer>> qrelsMap = new HashMap<>();

            int numQueries = 0;

            while ((line = qrelsFileReader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                String queryName = parts[0];
                String docId = parts[2];
                int relevance = Integer.parseInt(parts[3]);
                HashMap<String, Integer> docRelevanceMap = qrelsMap.getOrDefault(queryName, new HashMap<>());
                docRelevanceMap.put(docId, relevance);
                qrelsMap.put(queryName, docRelevanceMap);
            }

            HashMap<String, Double> dcgMap = new HashMap<>();
            HashMap<String, Integer> relFoundMap = new HashMap<>();
            HashMap<String, Double> rrMap = new HashMap<>();
            HashMap<String, Integer> relFoundCounterMap = new HashMap<>();
            HashMap<String, Integer> queryCounterMap = new HashMap<>();
            HashMap<String, Double> p10Map = new HashMap<>();
            HashMap<String, Double> r10Map = new HashMap<>();
            HashMap<String, Double> p20Map = new HashMap<>();
            HashMap<String, Double> apMap = new HashMap<>();

            ArrayList<String> docIds = new ArrayList<>();

            while ((line = trecrunFileReader.readLine()) != null) {
                numQueries++;
                String[] tokens = line.split("\\s+");
                String queryName = tokens[0];
                String docId = tokens[2];
                int rank = Integer.parseInt(tokens[3]);

                int queries = queryCounterMap.getOrDefault(queryName, 0) + 1;
                queryCounterMap.put(queryName, queries);

                HashMap<String, Integer> docRelevanceMap = qrelsMap.getOrDefault(queryName, new HashMap<>());
                int relevance = docRelevanceMap.getOrDefault(docId, 0);

                int relFound = relFoundMap.getOrDefault(queryName, 0);
                if (relevance > 0) {
                    relFound++;
                    relFoundMap.put(queryName, relFound);

                    double ap = relFound / (double) queryCounterMap.get(queryName);
                    double apVal = apMap.getOrDefault(queryName, 0.0);
                    apMap.put(queryName, apVal + ap);
                }

                double rr = rrMap.getOrDefault(queryName, 0.0);
                if (relevance > 0 && rr == 0) {
                    rrMap.put(queryName, 1.0 / rank);
                }

                int relFound10 = relFoundCounterMap.getOrDefault(queryName, 0);
                relFoundCounterMap.put(queryName, relFound10 + 1);

                if (relFoundCounterMap.getOrDefault(queryName, 0) == 10) {
                    p10Map.put(queryName, relFoundMap.getOrDefault(queryName, 0) / 10.0);
                    r10Map.put(queryName,
                            relFoundMap.getOrDefault(queryName, 0) / (double) numRelCalc(qrelsMap, queryName));
                }

                double precision = (double) relFound / (double) queries,
                        recall = (double) relFound / (double) numRelCalc(qrelsMap, queryName),
                        maxPrecision = p20Map.getOrDefault(queryName, 0.0);
                if (recall >= 0.20 && precision >= maxPrecision) {
                    p20Map.put(queryName, precision);
                }

                if (queries < 20) {
                    docIds.add(docId);
                }
                if (queries == 20) {
                    docIds.add(docId);
                    dcgMap.put(queryName, dcgFinder(queryName, qrelsMap, docIds));
                    docIds = new ArrayList<>();
                }
            }

            double totalNdcg = 0.0, mrr = 0.0, totalP10 = 0.0, totalR10 = 0.0, totalF10 = 0.0, totalP20 = 0.0,
                    map = 0.0;
            int totalNumRel = 0, totalRelFound = 0;

            List<String> queryNames = new ArrayList<>(queryCounterMap.keySet());

            Comparator<String> queryNameComparator = new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    int i1 = Integer.parseInt(s1);
                    int i2 = Integer.parseInt(s2);
                    int valueComparison = Integer.compare(i1, i2);
                    if (valueComparison != 0) {
                        return valueComparison;
                    }
                    return Integer.compare(s1.length(), s2.length());
                }
            };

            Collections.sort(queryNames, queryNameComparator);

            for (String queryName : queryNames) {
                int numRel = numRelCalc(qrelsMap, queryName);
                double ndcg = (numRel == 0) ? 0 : dcgMap.get(queryName) / idcgFinder(queryName, qrelsMap);
                int relFound = relFoundMap.getOrDefault(queryName, 0);
                double rr = (relFound == 0) ? 0 : rrMap.getOrDefault(queryName, 0.0);
                double p10 = p10Map.getOrDefault(queryName, 0.0);
                double r10 = (numRel == 0) ? 0 : r10Map.getOrDefault(queryName, 0.0);
                double f1 = (r10 == 0 || p10 == 0) ? 0 : (2 * r10 * p10) / (r10 + p10);
                double p20 = (numRel == 0) ? 0 : p20Map.getOrDefault(queryName, 0.0);
                double ap = (numRel == 0) ? 0 : apMap.getOrDefault(queryName, 0.0) / numRel;

                totalNdcg += ndcg;
                totalNumRel += numRel;
                totalRelFound += relFound;
                mrr += rr;
                totalP10 += p10;
                totalR10 += r10;
                totalF10 += f1;
                totalP20 += p20;
                map += ap;

                String output = String.format("NDCG@20  %-7s  %6.4f\n", queryName, ndcg);
                outputFileWriter.write(output);
                output = String.format("numRel   %-7s  %d\n", queryName, numRel);
                outputFileWriter.write(output);
                output = String.format("relFound %-7s  %d\n", queryName, relFound);
                outputFileWriter.write(output);
                output = String.format("RR       %-7s  %6.4f\n", queryName, rr);
                outputFileWriter.write(output);
                output = String.format("P@10     %-7s  %6.4f\n", queryName, p10);
                outputFileWriter.write(output);
                output = String.format("R@10     %-7s  %6.4f\n", queryName, r10);
                outputFileWriter.write(output);
                output = String.format("F1@10    %-7s  %6.4f\n", queryName, f1);
                outputFileWriter.write(output);
                output = String.format("P@20%%    %-7s  %6.4f\n", queryName, p20);
                outputFileWriter.write(output);
                output = String.format("AP       %-7s  %6.4f\n", queryName, ap);
                outputFileWriter.write(output);
            }

            double totalQueries = (double) numQueries / 1000.00;

            totalNdcg /= totalQueries;
            mrr /= totalQueries;
            totalP10 /= totalQueries;
            totalR10 /= totalQueries;
            totalF10 /= totalQueries;
            totalP20 /= totalQueries;
            map /= totalQueries;

            String output = String.format("NDCG@20  all      %6.4f\n", totalNdcg);
            outputFileWriter.write(output);
            output = String.format("numRel   all      %d\n", totalNumRel);
            outputFileWriter.write(output);
            output = String.format("relFound all      %d\n", totalRelFound);
            outputFileWriter.write(output);
            output = String.format("MRR      all      %6.4f\n", mrr);
            outputFileWriter.write(output);
            output = String.format("P@10     all      %6.4f\n", totalP10);
            outputFileWriter.write(output);
            output = String.format("R@10     all      %6.4f\n", totalR10);
            outputFileWriter.write(output);
            output = String.format("F1@10    all      %6.4f\n", totalF10);
            outputFileWriter.write(output);
            output = String.format("P@20%%    all      %6.4f\n", totalP20);
            outputFileWriter.write(output);
            output = String.format("MAP      all      %6.4f\n", map);
            outputFileWriter.write(output);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static int numRelCalc(HashMap<String, HashMap<String, Integer>> qrelsMap, String queryName) {
        int count = 0;
        HashMap<String, Integer> innerMap = qrelsMap.get(queryName);
        for (Map.Entry<String, Integer> entry : innerMap.entrySet()) {
            if (entry.getValue() > 0)
                count++;
        }
        return count;
    }

    public static double dcgFinder(String queryName, HashMap<String, HashMap<String, Integer>> qrelsMap,
            ArrayList<String> docIds) {
        HashMap<String, Integer> docRelevanceMap = qrelsMap.getOrDefault(queryName, new HashMap<>());
        int count = 1;
        double dcg = 0.0;
        for (String docId : docIds) {
            int relevance = docRelevanceMap.getOrDefault(docId, 0);
            if (count == 20) {
                dcg += ((double) relevance) / (Math.log(count) / Math.log(2));
                return dcg;
            }
            if (count == 1) {
                dcg = relevance;
            } else {
                dcg += ((double) relevance) / (Math.log(count) / Math.log(2));
            }
            count++;
        }
        return 0.0;
    }

    public static double idcgFinder(String queryName, HashMap<String, HashMap<String, Integer>> qrelsMap) {
        HashMap<String, Integer> docRelevanceMap = qrelsMap.getOrDefault(queryName, new HashMap<>());
        HashMap<String, Integer> docsMap = qrelsMap.get(queryName);
        List<Map.Entry<String, Integer>> docEntries = new ArrayList<>(docsMap.entrySet());
        Collections.sort(docEntries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        int count = 1;
        double idcg = 0.0;
        for (Map.Entry<String, Integer> entry : docEntries) {
            int relevance = docRelevanceMap.getOrDefault(entry.getKey(), 0);
            if (count == 20) {
                idcg += ((double) relevance) / (Math.log(count) / Math.log(2));
                return idcg;
            }
            if (count == 1) {
                idcg = relevance;
            } else {
                idcg += ((double) relevance) / (Math.log(count) / Math.log(2));
            }
            count++;
        }
        return 0.0;
    }

    public static void main(String[] args) {
        String runFile = args.length >= 1 ? args[0] : "P2eval/msmarcofull-bm25.trecrun";
        String qrelsFile = args.length >= 2 ? args[1] : "msmarco.qrels";
        String outputFile = args.length >= 3 ? args[2] : "bm25.eval";

        evaluate(runFile, qrelsFile, outputFile);
    }
}
