
import help.ECNRun;
import make_entity_data_file.*;
import org.jetbrains.annotations.NotNull;

/**
 * Main method to run code.
 * @author Shubham Chatterjee
 * @version 8/30/2021
 */

public class ProjectMain {

    public static void main(@NotNull String[] args) {

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
                System.err.println("ERROR! Type can be either (SupportPsg|LeadText|AspectCandidateSet|AspectSupportPsg|BM25Psg|ECNRun).");
                System.exit(-1);
        }
    }
}

