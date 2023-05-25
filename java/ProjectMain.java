
import help.ECNRun;
import make_entity_data_file.*;
import org.jetbrains.annotations.NotNull;

/**
 * Main method to run code.
 * @author Shubham Chatterjee
 * @version 8/30/2021
 */

public class ProjectMain {

    private static void printTrainModeArguments(@NotNull String type) {
        switch (type) {
            case "SupportPsg":
                System.out.println("SupportPsg:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <entityPassageFile>: Path to the entity passage file.");
                System.out.println("  <entityRunFile>: Path to the entity run file.");
                System.out.println("  <posOrNegEntityFile>: Path to the positive or negative entity file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <queryIdToNameFile>: Path to the query ID to name mapping file.");
                System.out.println("  <entityIdToNameFile>: Path to the entity ID to name mapping file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "LeadText":
                System.out.println("LeadText:");
                System.out.println("  <entityIndex>: Path to the entity index file.");
                System.out.println("  <entityRun>: Path to the entity run file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "AspectCandidateSet":
                System.out.println("AspectCandidateSet:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <catalogIndex>: Path to the catalog index file.");
                System.out.println("  <psgRanking>: Path to the passage ranking file.");
                System.out.println("  <entityQrelFile>: Path to the entity QREL file.");
                System.out.println("  <queriesFile>: Path to the queries file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <posEntityToTextFile>: Path to the positive entity to text file.");
                System.out.println("  <negEntityToTextFile>: Path to the negative entity to text file.");
                System.out.println("  <takeKDocs>: Number of documents to consider.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "AspectSupportPsg":
                System.out.println("AspectSupportPsg:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <catalogIndex>: Path to the catalog index file.");
                System.out.println("  <entityPassageFile>: Path to the entity passage file.");
                System.out.println("  <entityRunFile>: Path to the entity run file.");
                System.out.println("  <entityFile>: Path to the entity file.");
                System.out.println("  <queryIdToNameFile>: Path to the query ID to name mapping file.");
                System.out.println("  <entityIdToNameFile>: Path to the entity ID to name mapping file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "BM25Psg":
                System.out.println("BM25Psg:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <entityParaFile>: Path to the entity paragraph file.");
                System.out.println("  <entityFile>: Path to the entity file.");
                System.out.println("  <queriesFile>: Path to the queries file.");
                System.out.println("  <entitiesFile>: Path to the entities file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "ECNRun":
                System.out.println("ECNRun:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <entityPassageFile>: Path to the entity passage file.");
                System.out.println("  <entityRunFile>: Path to the entity run file.");
                System.out.println("  <posOrNegEntityFile>: Path to the positive or negative entity file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <queryIdToNameFile>: Path to the query ID to name mapping file.");
                System.out.println("  <entityIdToNameFile>: Path to the entity ID to name mapping file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            default:
                System.out.println("Unknown type: " + type);
                break;
        }
    }

    private static void printDevTestModeArguments(@NotNull String type) {
        switch (type) {
            case "SupportPsg":
                System.out.println("SupportPsg:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <entityPassageFile>: Path to the entity passage file.");
                System.out.println("  <entityRunFile>: Path to the entity run file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <queryIdToNameFile>: Path to the query ID to name mapping file.");
                System.out.println("  <entityIdToNameFile>: Path to the entity ID to name mapping file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "LeadText":
                System.out.println("LeadText:");
                System.out.println("  <entityIndex>: Path to the entity index file.");
                System.out.println("  <entityRun>: Path to the entity run file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "AspectCandidateSet":
                System.out.println("AspectCandidateSet:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <catalogIndex>: Path to the catalog index file.");
                System.out.println("  <psgRanking>: Path to the passage ranking file.");
                System.out.println("  <entityQrelFile>: Path to the entity QREL file.");
                System.out.println("  <queriesFile>: Path to the queries file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <takeKDocs>: Number of documents to consider.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "AspectSupportPsg":
                System.out.println("AspectSupportPsg:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <catalogIndex>: Path to the catalog index file.");
                System.out.println("  <entityPassageFile>: Path to the entity passage file.");
                System.out.println("  <entityRunFile>: Path to the entity run file.");
                System.out.println("  <queryIdToNameFile>: Path to the query ID to name mapping file.");
                System.out.println("  <entityIdToNameFile>: Path to the entity ID to name mapping file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "BM25Psg":
                System.out.println("BM25Psg:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <entityParaFile>: Path to the entity paragraph file.");
                System.out.println("  <queriesFile>: Path to the queries file.");
                System.out.println("  <entitiesFile>: Path to the entities file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            case "ECNRun":
                System.out.println("ECNRun:");
                System.out.println("  <paraIndex>: Path to the paragraph index file.");
                System.out.println("  <entityPassageFile>: Path to the entity passage file.");
                System.out.println("  <entityRunFile>: Path to the entity run file.");
                System.out.println("  <outFile>: Path to the output file.");
                System.out.println("  <queryIdToNameFile>: Path to the query ID to name mapping file.");
                System.out.println("  <entityIdToNameFile>: Path to the entity ID to name mapping file.");
                System.out.println("  <stopWordsFile>: Path to the stop words file.");
                System.out.println("  <parallel>: Whether to run in parallel (true/false).");
                break;
            default:
                System.out.println("Unknown type: " + type);
                break;
        }
    }

    public static void main(@NotNull String[] args) {
        if (args.length == 0 || args.length == 1 && args[0].equals("--help")) {
            // Print help message
            // Print help message
            System.out.println("Help:");
            System.out.println("Usage: java <JarFile>.jar <mode> <type> <arguments>");
            System.out.println("Available modes: train, dev, test");
            System.out.println("Available types: SupportPsg, LeadText, AspectCandidateSet, AspectSupportPsg, BM25Psg, ECNRun");
            System.out.println("Run the JAR file with <mode> <type> to see the arguments for the mode and type.");
            return;


        } else if(args.length == 2) {
            String mode = args[0];
            String type = args[1];

            switch (mode) {
                case "train":
                    printTrainModeArguments(type);
                    break;
                case "dev":
                case "test":
                    printDevTestModeArguments(type);
                    break;
                default:
                    System.out.println("Unknown mode: " + mode);
                    break;
            }

            return;
        }

        String mode = args[0];
        String type = args[1];

        switch (type) {
            case "SupportPsg":
                if (mode.equals("train")) {
                    String paraIndex = args[2];
                    String entityPassageFile = args[3];
                    String entityRunFile = args[4];
                    String posOrNegEntityFile = args[5];
                    String outFile = args[6];
                    String queryIdToNameFile = args[7];
                    String entityIdToNameFile = args[8];
                    String stopWordsFile = args[9];
                    boolean parallel = args[10].equals("true");


                    SupportPsg ob = new SupportPsg(paraIndex, entityPassageFile, entityRunFile, posOrNegEntityFile,
                            queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
                    ob.doTask(ob.entities.keySet(), mode);
                    System.out.print("Writing to file....");
                    ob.writeToFile(outFile, ob.entityDataMap);
                    System.out.println("[Done].");


                } else if (mode.equals("dev") || mode.equals("test")) {
                    String paraIndex = args[2];
                    String entityPassageFile = args[3];
                    String entityRunFile = args[4];
                    String outFile = args[5];
                    String queryIdToNameFile = args[6];
                    String entityIdToNameFile = args[7];
                    String stopWordsFile = args[8];
                    boolean parallel = args[9].equals("true");


                    SupportPsg ob = new SupportPsg(paraIndex, entityPassageFile, entityRunFile,
                            queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
                    ob.doTask(ob.entityRunMap.keySet(), mode);
                    System.out.print("Writing to file....");
                    ob.writeToFile(outFile, ob.entityDataMap);
                    System.out.println("[Done].");

                }


                break;
            case "LeadText": {
                String entityIndex = args[2];
                String entityRun = args[3];
                String outFile = args[4];
                boolean parallel = args[5].equals("true");
                LeadText ob = new LeadText(entityIndex, entityRun, mode, parallel);

                if (mode.equals("train")) {
                    ob.doTask(ob.entities.keySet(), mode);
                } else {
                    ob.doTask(ob.entityRunMap.keySet(), mode);
                }
                System.out.print("Writing to file....");
                ob.writeToFile(outFile, ob.entityDataMap);
                System.out.println("[Done].");

                break;
            }

            case "AspectCandidateSet":
                if (mode.equals("train")) {
                    String paraIndex = args[2];
                    String catalogIndex = args[3];
                    String psgRanking = args[4];
                    String entityQrelFile = args[5];
                    String queriesFile = args[6];
                    String stopWordsFile = args[7];
                    String posEntityToTextFile = args[8];
                    String negEntityToTextFile = args[9];
                    int takeKDocs = Integer.parseInt(args[10]);
                    boolean parallel = args[11].equals("true");

                    new AspectsInCandidateSet(paraIndex, catalogIndex, psgRanking, entityQrelFile, queriesFile, stopWordsFile,
                            posEntityToTextFile, negEntityToTextFile, takeKDocs, mode, parallel);

                } else if (mode.equals("dev") || mode.equals("test")) {
                    String paraIndex = args[2];
                    String catalogIndex = args[3];
                    String psgRanking = args[4];
                    String queriesFile = args[5];
                    String stopWordsFile = args[6];
                    String outFile = args[7];
                    int takeKDocs = Integer.parseInt(args[8]);
                    boolean parallel = args[9].equals("true");

                    new AspectsInCandidateSet(paraIndex, catalogIndex, psgRanking, queriesFile, stopWordsFile,
                            outFile, takeKDocs, mode, parallel);

                } else {
                    System.err.println("ERROR! Mode can be either (train|dev|test)).");
                    System.exit(-1);
                }

                break;

            case "AspectSupportPsg":
                if (mode.equals("train")) {
                    String paraIndex = args[2];
                    String catalogIndex = args[3];
                    String entityPassageFile = args[4];
                    String entityRunFile = args[5];
                    String entityFile = args[6];
                    String queryIdToNameFile = args[7];
                    String entityIdToNameFile = args[8];
                    String stopWordsFile = args[9];
                    String outFile = args[10];
                    boolean parallel = args[11].equals("true");


                    AspectsInSupportPsg ob = new AspectsInSupportPsg(paraIndex, catalogIndex, entityPassageFile,
                            entityRunFile, entityFile, queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
                    ob.doTask(ob.entities.keySet(), mode);
                    System.out.print("Writing to file....");
                    ob.writeToFile(outFile, ob.entityDataMap);
                    System.out.println("[Done].");

                } else if (mode.equals("dev") || mode.equals("test")) {
                    String paraIndex = args[2];
                    String catalogIndex = args[3];
                    String entityPassageFile = args[4];
                    String entityRunFile = args[5];
                    String queryIdToNameFile = args[6];
                    String entityIdToNameFile = args[7];
                    String stopWordsFile = args[8];
                    String outFile = args[9];
                    boolean parallel = args[10].equals("true");


                    AspectsInSupportPsg ob = new AspectsInSupportPsg(paraIndex, catalogIndex, entityPassageFile,
                            entityRunFile, queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
                    ob.doTask(ob.entityRunMap.keySet(), mode);

                    System.out.print("Writing to file....");
                    ob.writeToFile(outFile, ob.entityDataMap);
                    System.out.println("[Done].");

                } else {
                    System.err.println("ERROR! Mode can be either (train|dev|test)).");
                    System.exit(-1);
                }

                break;

            case "BM25Psg": {
                String  paraIndex = args[2];
                String entityParaFile = args[3];
                String entityFile = args[4]; // If mode != train, then this file is entity run file, else pos/neg entity file.
                String queriesFile = args[5];
                String entitiesFile = args[6];
                String stopWordsFile = args[7];
                String outFile = args[8];
                boolean parallel = args[9].equals("true");

                BM25Psg ob = new BM25Psg(paraIndex, entityParaFile, entityFile, queriesFile, entitiesFile,
                        stopWordsFile, mode, parallel);

                if (mode.equals("train")) {
                    ob.doTask(ob.entities.keySet(), mode);
                } else {
                    ob.doTask(ob.entityRunMap.keySet(), mode);

                }

                System.out.print("Writing to file....");
                ob.writeToFile(outFile, ob.entityDataMap);
                System.out.println("[Done].");

                break;
            }

            case "WikiPage": {
                String index = args[2];
                String entityFile = args[3];
                String tfIdfFile = args[4];
                String stopWordsFile = args[5];
                String outFile = args[6];
                boolean parallel = args[7].equals("true");

                WikiPage ob = new WikiPage(index, entityFile, tfIdfFile, stopWordsFile, mode, parallel);

                if (mode.equals("train")) {
                    ob.doTask(ob.entities.keySet(), mode);
                } else {
                    ob.doTask(ob.entityRunMap.keySet(), mode);
                }
                System.out.print("Writing to file....");
                ob.writeToFile(outFile, ob.entityDataMap);
                System.out.println("[Done].");

                break;
            }

            case "QueryEntity": {
                String indexDir = args[2];
                String queryAnnotationsFile = args[3];
                String queryIdToNameFile = args[4];
                String entityNameToIdFile = args[5];
                String entityParaMapFile = args[6];
                String stopWordsFile = args[7];
                String dataType = args[8];
                String outFile = args[9];
                boolean parallel = args[10].equals("true");

                if (dataType.equals("bm25")) {
                    System.out.println("Getting BM25 passages for query entities.");
                } else if (dataType.equals("lead-text")) {
                    System.out.println("Getting LeadText of query entities.");
                } else {
                    System.err.println("Wrong data type specified. Data type can be either `bm25` or `lead-text`.");
                    System.exit(-1);
                }

                MakeQueryEntityData ob = new MakeQueryEntityData(indexDir, queryAnnotationsFile, queryIdToNameFile, entityNameToIdFile,
                        entityParaMapFile, stopWordsFile, dataType, parallel);

                ob.doTask(ob.queryIdToNameMap.keySet());

                System.out.print("Writing to file....");
                ob.writeToFile(outFile, ob.entityDataMap);
                System.out.println("[Done].");
                break;
            }

            case "ECNRun": {
                if (mode.equals("train")) {
                    String paraIndex = args[2];
                    String entityPassageFile = args[3];
                    String entityRunFile = args[4];
                    String posOrNegEntityFile = args[5];
                    String outFile = args[6];
                    String queryIdToNameFile = args[7];
                    String entityIdToNameFile = args[8];
                    String stopWordsFile = args[9];
                    boolean parallel = args[10].equals("true");


                    ECNRun ob = new ECNRun(paraIndex, entityPassageFile, entityRunFile, posOrNegEntityFile,
                            queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
                    ob.doTask(ob.entities.keySet(), mode);
                    System.out.print("Writing to run file.....");
                    ob.writeRunFile(ob.runStrings, outFile);
                    System.out.println("[Done].");
                    System.out.println("Run file written at: " + outFile);


                } else if (mode.equals("dev") || mode.equals("test")) {
                    String paraIndex = args[2];
                    String entityPassageFile = args[3];
                    String entityRunFile = args[4];
                    String outFile = args[5];
                    String queryIdToNameFile = args[6];
                    String entityIdToNameFile = args[7];
                    String stopWordsFile = args[8];
                    boolean parallel = args[9].equals("true");


                    ECNRun ob = new ECNRun(paraIndex, entityPassageFile, entityRunFile,
                            queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
                    ob.doTask(ob.entityRunMap.keySet(), mode);
                    System.out.print("Writing to run file.....");
                    ob.writeRunFile(ob.runStrings, outFile);
                    System.out.println("[Done].");
                    System.out.println("Run file written at: " + outFile);

                }

                break;
            }

            default:
                System.err.println("ERROR! Type can be either (SupportPsg|LeadText|AspectCandidateSet|AspectSupportPsg|BM25Psg|WikiPage).");
                System.exit(-1);
        }
    }
}

