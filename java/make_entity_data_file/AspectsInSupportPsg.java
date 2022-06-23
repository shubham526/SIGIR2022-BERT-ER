package make_entity_data_file;

import help.LuceneHelper;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * We use the aspect of the entity from the top support passage of the entity as the entity's description.
 * @author Shubham Chatterjee
 * @version 8/30/2021
 */
public class AspectsInSupportPsg extends SupportPsg {

    private final IndexSearcher catalogSearcher;

    public AspectsInSupportPsg(String paraIndex,
                               String catalogIndex,
                               String entityPassageFile,
                               String entityRunFile,
                               String entityFile,
                               String queryIdToNameFile,
                               String entityIdToNameFile,
                               String stopWordsFile,
                               boolean parallel) {

        super(paraIndex, entityPassageFile, entityRunFile, entityFile, queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);

        System.out.print("Setting up catalog index...");
        this.catalogSearcher = LuceneHelper.createSearcher(catalogIndex, "bm25");
        System.out.println("[Done].");

    }

    public AspectsInSupportPsg(String paraIndex,
                               String catalogIndex,
                               String entityPassageFile,
                               String entityRunFile,
                               String queryIdToNameFile,
                               String entityIdToNameFile,
                               String stopWordsFile,
                               boolean parallel) {

        super(paraIndex, entityPassageFile, entityRunFile, queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);

        System.out.print("Setting up catalog index...");
        this.catalogSearcher = LuceneHelper.createSearcher(catalogIndex, "bm25");
        System.out.println("[Done].");

    }

    protected void getEntityData(String entityId,
                                   @NotNull Map<String, Double> scoreMap,
                                   Map<String, String> result) {

        Map.Entry<String, Double> topSupportPsgForEntity = new ArrayList<>(scoreMap.entrySet()).get(0);
        String topSupportPsgId = topSupportPsgForEntity.getKey();
        double topSupportPsgScore = topSupportPsgForEntity.getValue();

        try {

            // Get the entities in the support passage
            Document doc = LuceneHelper.searchIndex("Id", topSupportPsgId, indexSearcher);
            if (doc != null) {
                String paraEntities = doc.get("Entities");

                // Get the aspect of the entity from the support passage
                String entityAspectId = getEntityAspectId(entityId, paraEntities);

                if (entityAspectId != null && !entityAspectId.isEmpty()) {

                    // entityAspectId may be null if the entity is not found in the passage.
                    // entityAspectId may be empty if the entity is found in the passage but it has no associated aspect.
                    // This can happen because the entity aspect linker is not perfect and may not be able to aspect link all entities in the passage.
                    String aspectText = idToText(entityAspectId, "Text", catalogSearcher);
                    String data = aspectText.isEmpty()
                            ? ""
                            : toJSONString(topSupportPsgId, entityAspectId, aspectText, topSupportPsgScore);
                    if (! data.isEmpty()) {
                        result.put(entityId, data);
                    }
                }
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }


    }

    @Nullable
    private String getEntityAspectId(String entityId, @NotNull String paraEntities) {
        String[] aspectList = paraEntities.split("\n");
        for (String aspectStr : aspectList) {
            if (! aspectStr.isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(aspectStr);
                    String linkedEntityId = jsonObject.getString("linkPageId");
                    if (linkedEntityId.equalsIgnoreCase(entityId)) {
                        return jsonObject.getString("aspect");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void main(@NotNull String[] args) {
        String mode = args[0];

        if (mode.equals("train")) {
            String paraIndex = args[1];
            String catalogIndex = args[2];
            String entityPassageFile = args[3];
            String entityRunFile = args[4];
            String entityFile = args[5];
            String queryIdToNameFile = args[6];
            String entityIdToNameFile = args[7];
            String stopWordsFile = args[8];
            String outFile = args[9];
            boolean parallel = args[10].equals("true");


            AspectsInSupportPsg ob = new AspectsInSupportPsg(paraIndex, catalogIndex, entityPassageFile,
                    entityRunFile, entityFile, queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
            ob.doTask(ob.entities.keySet(), mode);
            System.out.print("Writing to file....");
            ob.writeToFile(outFile, ob.entityDataMap);
            System.out.println("[Done].");

        } else if (mode.equals("dev") || mode.equals("test")) {
            String paraIndex = args[1];
            String catalogIndex = args[2];
            String entityPassageFile = args[3];
            String entityRunFile = args[4];
            String queryIdToNameFile = args[5];
            String entityIdToNameFile = args[6];
            String stopWordsFile = args[7];
            String outFile = args[8];
            boolean parallel = args[9].equals("true");


            AspectsInSupportPsg ob = new AspectsInSupportPsg(paraIndex, catalogIndex, entityPassageFile,
                    entityRunFile, queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
            ob.doTask(ob.entityRunMap.keySet(), mode);

            System.out.print("Writing to file....");
            ob.writeToFile(outFile, ob.entityDataMap);
            System.out.println("[Done].");

        }



    }

}
