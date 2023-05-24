package make_entity_data_file;

import help.LuceneHelper;
import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Class to make a file containing entityId and text representing the entity.
 * @author Shubham Chatterjee
 * @version 8/30/2021
 */
abstract public class MakeEntityData {
    protected final IndexSearcher indexSearcher;
    protected int total = 0;
    public Map<String, Set<String>> entities = new HashMap<>();
    protected AtomicInteger count = new AtomicInteger(0);
    protected boolean parallel;

    public  final Map<String, Map<String, String>> entityDataMap = new HashMap<>();

    public MakeEntityData(String paraIndex, String entityFile, boolean parallel) {

        this.parallel = parallel;

        System.out.print("Setting up paragraph index...");
        this.indexSearcher = LuceneHelper.createSearcher(paraIndex, "bm25");
        System.out.println("[Done].");

        System.out.print("Loading entity file...");
        entities = readEntityFile(entityFile);
        System.out.println("[Done].");


    }

    public MakeEntityData(String paraIndex, boolean parallel) {

        this.parallel = parallel;

        System.out.print("Setting up paragraph index...");
        this.indexSearcher = LuceneHelper.createSearcher(paraIndex, "bm25");
        System.out.println("[Done].");
    }

    public void doTask(@NotNull Set<String> querySet, String mode) {
        total = querySet.size();

        if (parallel) {
            System.out.println("Using Parallel Streams.");
            int parallelism = ForkJoinPool.commonPool().getParallelism();
            int numOfCores = Runtime.getRuntime().availableProcessors();
            System.out.println("Number of available processors = " + numOfCores);
            System.out.println("Number of threads generated = " + parallelism);

            if (parallelism == numOfCores - 1) {
                System.err.println("WARNING: USING ALL AVAILABLE PROCESSORS");
                System.err.println("USE: \"-Djava.util.concurrent.ForkJoinPool.common.parallelism=N\" " +
                        "to set the number of threads used");
            }
            // Do in parallel
            querySet.parallelStream().forEach(queryId -> getEntityData(queryId, mode));
        } else {
            System.out.println("Using Sequential Streams.");

            // Do in serial
            ProgressBar pb = new ProgressBar("Progress", querySet.size());
            for (String q : querySet) {
                getEntityData(q, mode);
                pb.step();
            }
            pb.close();
        }
    }

    @NotNull
    public Map<String, Set<String>> readEntityFile(String entityFile) {
        Map<String, Set<String>> entityFileMap = new HashMap<>();

        BufferedReader br = null;
        String line , queryID ,entityID;

        try {
            br = new BufferedReader(new FileReader(entityFile));
            while((line = br.readLine()) != null) {
                String[] fields = line.split(" ");
                queryID = fields[0];
                entityID = fields[2];
                Set<String> entitySet = new HashSet<>();
                if(entityFileMap.containsKey(queryID)) {
                    entitySet = entityFileMap.get(queryID);
                }
                entitySet.add(entityID);
                entityFileMap.put(queryID, entitySet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    br.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return entityFileMap;
    }

    @NotNull
    public Map<String, LinkedHashMap<String, Double>> readRunFile(String inFilePath) {
        BufferedReader br = null;
        String line , queryID ,field2;
        double score;
        Map<String, LinkedHashMap<String, Double>> rankings = new HashMap<>();

        try {
            br = new BufferedReader(new FileReader(inFilePath));
            while((line = br.readLine()) != null) {
                String[] fields = line.split(" ");
                queryID = fields[0];
                field2 = fields[2];
                score = Double.parseDouble(fields[4]);
                LinkedHashMap<String, Double> map = new LinkedHashMap<>();
                if(rankings.containsKey(queryID)) {
                    map = rankings.get(queryID);
                }
                map.put(field2, score);
                rankings.put(queryID, map);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    br.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rankings;
    }

    /**
     * Write a list of strings to a file in TSV format.
     * @param outFile Path to the output file.
     * @param entityToTextMap Map of data to write.
     */

    public void writeToFile(String outFile, @NotNull Map<String, Map<String, String>> entityToTextMap) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(outFile,true));

            for(String queryId : entityToTextMap.keySet() ) {
                Map<String, String> entityMap = entityToTextMap.get(queryId);
                for (String entityId : entityMap.keySet()) {
                    String entityText = entityMap.get(entityId);
                    out.write(queryId + "\t" + entityId + "\t" + entityText);
                    out.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null) {
                    out.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Return a JSON encoded string representation of the passed parameters.
     * @param paraId Paragraph Id
     * @param aspectId Aspect Id
     * @param entityText Text representing the entity
     * @param entityScore Score of the entity
     * @return JSON encoded string
     */

    protected String toJSONString(String paraId, String aspectId, String entityText, double entityScore) {
        JSONObject example = new JSONObject();
        try {
            example.put("para_id", paraId);
            example.put("aspect_id", aspectId);
            example.put("text", entityText);
            example.put("score", entityScore);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return example.toString();
    }

    protected String idToText(String id, String field, IndexSearcher searcher) {
        try {
            Document doc = LuceneHelper.searchIndex("Id", id, searcher);
            if (doc != null) {
                return doc
                        .get(field)
                        .replaceAll("\n", " ")
                        .replaceAll("\r", " ");

            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return "";

    }

    @NotNull
    protected <K, V>LinkedHashMap<K, V> sortByValueDescending(@NotNull Map<K, V> map) {
        LinkedHashMap<K, V> reverseSortedMap = new LinkedHashMap<>();
        map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue((Comparator<? super V>) Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
        return reverseSortedMap;
    }

    /**
     * Read a TSV file.
     * @param file Path to file.
     * @return Map representation of the file.
     */

    @NotNull
    protected  Map<String, String> readTsvFile(String file) {
        BufferedReader br = null;
        Map<String, String> fileMap = new HashMap<>();
        String line;

        try {
            br = new BufferedReader(new FileReader(file));
            while((line = br.readLine()) != null) {
                String[] fields = line.split("\t");
                if (fields.length == 2) {
                    String key = fields[0];
                    String value = fields[1];
                    fileMap.put(key, value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    br.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fileMap;
    }


    /**
     * Read file containing  a list of stop words.
     * @param stopWordsFilePath Path to stop words file.
     * @return List of words.
     */

    @NotNull
    protected List<String> getStopWords(String stopWordsFilePath) {
        List<String> stopWords = new ArrayList<>();
        BufferedReader br = null;
        String line;

        try {
            br = new BufferedReader(new FileReader(stopWordsFilePath));
            while((line = br.readLine()) != null) {
                stopWords.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    br.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return stopWords;
    }


    protected abstract void getEntityData(String queryId, String mode);


}



