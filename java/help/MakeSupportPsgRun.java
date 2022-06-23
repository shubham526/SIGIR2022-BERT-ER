package help;


import make_entity_data_file.SupportPsg;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;


/**
 * Makes a support passage run using the method Entity Context Neighbor (ECN).
 * @author Shubham Chatterjee
 * @version 9/4/21
 */

public class MakeSupportPsgRun extends SupportPsg {

    private Map<String, Set<String>> entities = new HashMap<>();
    private final List<String> runStrings;

    /**
     * Constructor for train data.
     * When making the train data, we specify an entity file.
     * Entity file --> Positive entities (a.k.a qrel file) OR Negative entities.
     * The entity run file is used to filter the co-occurring entities of the target entity. Only co-occurring entities
     * that have been retrieved for the query are kept and the others are discarded.
     * @param indexDir Paragraph index.
     * @param entityPassageFile File containing mapping from entity_id --> List[passage_id]
     * @param entityRunFile Entity fun file.
     * @param entityFile Positive/Negative entity file.
     * @param parallel Whether to do in parallel.
     */



    public MakeSupportPsgRun(String indexDir,
                             String entityPassageFile,
                             String entityRunFile,
                             String entityFile,
                             String queryIdToNameFile,
                             String entityIdToNameFile,
                             String stopWordsFile,
                             boolean parallel) {

        super(indexDir, entityPassageFile, entityRunFile, queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);

        this.runStrings = new ArrayList<>();

        System.out.print("Loading entity file...");
        entities = readEntityFile(entityFile);
        System.out.println("[Done].");


    }

    /**
     * Constructor for test data.
     * @param indexDir Paragraph index.
     * @param entityPassageFile File containing mapping from entity_id --> List[passage_id]
     * @param entityRunFile Entity fun file.
     * @param parallel Whether to do in parallel.
     */

    public MakeSupportPsgRun(String indexDir,
                             String entityPassageFile,
                             String entityRunFile,
                             String queryIdToNameFile,
                             String entityIdToNameFile,
                             String stopWordsFile,
                             boolean parallel) {

        super(indexDir, entityPassageFile, entityRunFile,queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
        this.runStrings = new ArrayList<>();

    }

    private void writeRunFile(@NotNull List<String> runStrings, String filePath) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filePath,true));

            for(String s : runStrings) {
                if (s != null) {
                    out.write(s);
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
     * Method to calculate the feature.
     * Works in parallel using Java 8 parallelStreams.
     * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
     * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
     * @param outputFilePath String Path to the output file.
     */

    private  void doTask(String outputFilePath, boolean trainMode) {
        Set<String> querySet = entityRunMap.keySet();
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
            querySet.parallelStream().forEach(queryId -> findSupportPsg(queryId, trainMode));
        } else {
            System.out.println("Using Sequential Streams.");

            // Do in serial
            ProgressBar pb = new ProgressBar("Progress", querySet.size());
            for (String q : querySet) {
                findSupportPsg(q, trainMode);
                pb.step();
            }
            pb.close();
        }


        // Create the run file
        System.out.print("Writing to run file.....");
        writeRunFile(runStrings, outputFilePath);
        System.out.println("[Done].");
        System.out.println("Run file written at: " + outputFilePath);
    }

    /**
     * Helper method.
     * For every query, look at all the entities relevant for the query.
     * For every such entity, create a pseudo-document consisting of passages which contain this entity.
     * For every co-occurring entity in the pseudo-document, if the entity is also relevant for the query,
     * then find the frequency of this entity in the pseudo-document and score the passages using this frequency information.
     *
     * @param queryId String
     */

    private void findSupportPsg(String queryId, boolean trainMode) {

        Set<String> retEntitySet = entityRunMap.get(queryId).keySet();

        // Depending on the train or test mode, the candidate entity set changes.
        // Train --> Candidate entities = Positive/Negative entities for query.
        // Test --> Candidate entities = Entities from a run file (retrieved entities).

        if (trainMode) {
            Set<String> entitySet = entities.get(queryId);
            findSupportPsg(queryId, entitySet, retEntitySet);
        } else {
            findSupportPsg(queryId, retEntitySet, retEntitySet);
        }


        if (parallel) {
            System.out.println("Done query: " + queryId);
        }

    }

    private void findSupportPsg(String queryId,
                                @NotNull Set<String> candidateEntitySet,
                                Set<String> retEntitySet) {

        for (String entityId : candidateEntitySet) {
            try {
                // Get the paragraphs which mention the entity
                List<String> paraList = JSONArrayToList(new JSONObject(entityParaMap.get(entityId)).getJSONArray("paragraphs"));

                // Rank these paragraphs for the query
                List<RankingHelper.ScoredDocument> rankedParaList = rankParasForQuery(queryId, entityId, paraList);

                // Create the ECD using the ranked paragraphs
                EntityContextDocument d = createECD(entityId, rankedParaList);
                if (d != null) {
                    List<String> contextEntityList = d.getEntityList();
                    Map<String, Double> freqDist = getDistribution(contextEntityList, retEntitySet);
                    Map<String, Double> scoreMap = scoreDoc(d, freqDist);
                    makeRunStrings(queryId, entityId, scoreMap);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, Double> scoreDoc(EntityContextDocument d, Map<String, Double> freqDist) {
        return null;
    }

    /**
     * Method to make the run file strings.
     *
     * @param queryId  Query ID
     * @param scoreMap HashMap of the scores for each paragraph
     */

    private void makeRunStrings(String queryId, String entityId, Map<String, Double> scoreMap) {
        LinkedHashMap<String, Double> paraScore = sortByValueDescending(scoreMap);
        String runFileString;
        int rank = 1;

        for (String paraId : paraScore.keySet()) {
            double score = paraScore.get(paraId);
            if (score > 0) {
                runFileString = queryId + " " + entityId + "/" + paraId + " Q0 " + rank + " " + score + " " + "ECN";
                runStrings.add(runFileString);
                rank++;
            }

        }
    }

    /**
     * Main method.
     * @param args Command Line arguments
     */

    public static void main(@NotNull String[] args) {

        boolean trainMode = args[0].equals("true");

        if (trainMode) {
            String indexDir = args[1];
            String entityPassageFile = args[2];
            String entityRunFile = args[3];
            String entityFile = args[4];
            String queryIdToNameFile = args[5];
            String entityIdToNameFile = args[6];
            String stopWordsFile = args[7];
            String outDir = args[8];
            boolean parallel = args[9].equals("true");

            String outFile = outDir + "/ECN-Train.run";
            MakeSupportPsgRun ob = new MakeSupportPsgRun(indexDir, entityPassageFile, entityRunFile, entityFile,
                    queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
            ob.doTask(outFile, trainMode);


        } else {
            String indexDir = args[1];
            String entityPassageFile = args[2];
            String entityRunFile = args[3];
            String queryIdToNameFile = args[4];
            String entityIdToNameFile = args[5];
            String stopWordsFile = args[6];
            String outDir = args[7];
            boolean parallel = args[8].equals("true");

            String outFile = outDir + "/ECN-Test.run";
            MakeSupportPsgRun ob =  new MakeSupportPsgRun(indexDir, entityPassageFile, entityRunFile,
                    queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
            ob.doTask(outFile, trainMode);

        }
    }
}
