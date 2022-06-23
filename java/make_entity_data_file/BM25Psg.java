package make_entity_data_file;

import help.RankingHelper;
import org.apache.lucene.document.Document;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * We use the text of the top BM25 passage which mentions an entity as the entity's description.
 * @author Shubham Chatterjee
 * @version 2/26/2022
 */

public class BM25Psg extends CandidatePsg {

    public BM25Psg(String paraIndex,
                   String entityParaFile,
                   String entityFile,
                   String queriesFile,
                   String entitiesFile,
                   String stopWordsFile,
                   @NotNull String mode,
                   boolean parallel) {
        super(paraIndex, entityParaFile, queriesFile, entitiesFile, stopWordsFile,  parallel);

        if (mode.equals("train")) {
            System.out.print("Loading pos/neg entity file...");
            entities = readEntityFile(entityFile);
        } else {
            System.out.print("Loading entity run...");
            entityRunMap = readRunFile(entityFile);
        }
        System.out.println("[Done].");
    }



    @Override
    protected void getEntityData(String queryId, @NotNull String mode) {
        Map<String, String> res = new HashMap<>();

        if (mode.equals("train")) {
            Set<String> entitySet = entities.get(queryId);
            for (String entityId : entitySet) {
                getEntityData(queryId, entityId, 0.0, res);
            }
        } else {
            Map<String, Double> retEntityMap = entityRunMap.get(queryId);
            for (String entityId : retEntityMap.keySet()) {
                getEntityData(queryId, entityId, retEntityMap.get(entityId), res);
            }
        }
        entityDataMap.put(queryId, res);
        if (parallel) {
            count.getAndIncrement();
            System.out.println("Done: " + queryId + " ( " + count + "/" + total + " ).");
        }
    }

    private void getEntityData(String queryId, String entityId, double entityScore, Map<String, String> res) {
        try {
            Document doc = getTopDocForEntity(queryId, entityId);
            if (doc != null) {
                String paraId = doc.get("Id");
                String paraText = doc
                        .get("Text")
                        .replaceAll("\n", " ")
                        .replaceAll("\r", " ");
                String data = toJSONString(paraId, " ", paraText, entityScore);
                res.put(entityId, data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected Document getEntityDescription(String queryId, String entityId, @NotNull List<RankingHelper.ScoredDocument> rankedParaList) {
        return rankedParaList.get(0).getDocument();
    }

    public static void main(@NotNull String[] args) {
        String  paraIndex = args[0];
        String entityParaFile = args[1];
        String entityFile = args[2]; // If mode != train, then this file is entity run file, else pos/neg entity file.
        String queriesFile = args[3];
        String entitiesFile = args[4];
        String stopWordsFile = args[5];
        String outFile = args[6];
        String mode = args[7];
        boolean parallel = args[8].equals("true");

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


    }

}
