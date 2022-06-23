package make_entity_data_file;

import help.LuceneHelper;
import help.RankingHelper;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

/**
 * We use the top support passage for an entity as the entity's description.
 * This class implements method Entity Context Neighbour (ECN) from Chatterjee et al., 2019.
 * @author Shubham Chatterjee
 * @version 2/26/2022
 */

public class SupportPsg extends CandidatePsg {

    protected final DecimalFormat df;

    /**
     * Class to represent an Entity Context Document for an entity.
     * @author Shubham Chatterjee
     * @version 05/31/2020
     */
    public static class EntityContextDocument {

        private final List<Document> documentList;
        private final String entity;
        private final List<String> contextEntities;

        /**
         * Constructor.
         * @param documentList List of documents in the pseudo-document
         * @param entity The entity for which the pseudo-document is made
         * @param contextEntities The list of entities in the pseudo-document
         */
        @Contract(pure = true)
        public EntityContextDocument(List<Document> documentList,
                                     String entity,
                                     List<String> contextEntities) {
            this.documentList = documentList;
            this.entity = entity;
            this.contextEntities = contextEntities;
        }

        /**
         * Method to get the list of documents in the ECD.
         * @return String
         */
        public List<Document> getDocumentList() {
            return this.documentList;
        }

        /**
         * Method to get the entity of the ECD.
         * @return String
         */
        public String getEntity() {
            return this.entity;
        }

        /**
         * Method to get the list of context entities in the ECD.
         * @return ArrayList
         */
        public List<String> getEntityList() {
            return this.contextEntities;
        }
    }



    public SupportPsg(String paraIndex,
                      String entityParaFile,
                      String entityRunFile,
                      String posOrNegEntityFile,
                      String queryIdToNameFile,
                      String entityIdToNameFile,
                      String stopWordsFile,
                      boolean parallel) {

        super(paraIndex, entityParaFile, queryIdToNameFile, entityIdToNameFile, stopWordsFile,  parallel);

        System.out.print("Loading entity run...");
        entityRunMap = readRunFile(entityRunFile);
        System.out.println("[Done].");

        System.out.print("Loading pos/neg entity file...");
        entities = readEntityFile(posOrNegEntityFile);
        System.out.println("[Done].");

        df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);

    }

    public SupportPsg(String paraIndex,
                      String entityParaFile,
                      String entityRunFile,
                      String queryIdToNameFile,
                      String entityIdToNameFile,
                      String stopWordsFile,
                      boolean parallel) {

        super(paraIndex, entityParaFile, queryIdToNameFile, entityIdToNameFile, stopWordsFile,  parallel);

        df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);

        System.out.print("Loading entity run...");
        entityRunMap = readRunFile(entityRunFile);
        System.out.println("[Done].");


    }

    @Override
    public void getEntityData(@NotNull String queryId, @NotNull String mode) {

        if (entityRunMap.containsKey(queryId)) {

            Set<String> retEntitySet = entityRunMap.get(queryId).keySet();

            Map<String, String> res;

            if (mode.equals("train")) {

                // Get the pos/neg entities
                // Set<String> entitySet = entities.get(queryId).stream().limit(10).collect(Collectors.toSet());
                Set<String> entitySet = entities.get(queryId);

                // Get the text of the top support passage/text of the aspect from top support passage for these entities
                res = getEntityData(queryId, entitySet);
            } else {
                res = getEntityData(queryId, retEntitySet);
            }
            // Save
            entityDataMap.put(queryId, res);
            if (parallel) {
                count.getAndIncrement();
                System.out.println("Done: " + queryId + " ( " + count + "/" + total + " ).");
            }
        }
    }

    @NotNull
    protected Map<String, String> getEntityData(String queryId, @NotNull Set<String> candidateEntitySet) {
        Map<String, String> result = new HashMap<>();

        for (String entityId : candidateEntitySet) {
            if (entityParaMap.containsKey(entityId)) {
                try {
                    Document doc = getTopDocForEntity(queryId, entityId);
                    double entityScore = entityRunMap.get(queryId).get(entityId);
                    if (doc != null) {
                        String paraId = doc.get("Id");
                        String paraText = doc
                                .get("Text")
                                .replaceAll("\n", " ")
                                .replaceAll("\r", " ");
                        String data = toJSONString(paraId, " ", paraText, entityScore);
                        result.put(entityId, data);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }


    @Nullable
    @Override
    protected Document getEntityDescription(String queryId, String entityId, @NotNull List<RankingHelper.ScoredDocument> rankedParaList) {

        Set<String> retEntitySet = entityRunMap.get(queryId).keySet();

        // Create the ECD using the ranked paragraphs
        EntityContextDocument d = createECD(entityId, rankedParaList);
        if (d != null) {
            List<String> contextEntityList = d.getEntityList();
            Map<String, Double> freqDist = getDistribution(contextEntityList, retEntitySet);
            freqDist.remove(entityId);
            return getTopDocForEntity(d, freqDist);
        }
        return null;
    }

    @NotNull
    protected List<RankingHelper.ScoredDocument> rankParasForQuery(String queryId, String entityId, List<String> paraList) {

        // Get the Lucene documents
        List<Document> luceneDocList = LuceneHelper.toLuceneDocList(paraList, indexSearcher);

        // Convert to BooleanQuery
        BooleanQuery booleanQuery = RankingHelper.toBooleanQueryWithPRF(queryIdToNameMap.get(queryId),
                entityIdToNameMap.get(entityId), luceneDocList, stopWords);

        // Rank the Lucene Documents using the BooleanQuery

        if (booleanQuery == null) {
            return new ArrayList<>();
        }

        return RankingHelper.rankDocuments(booleanQuery, luceneDocList, 1000);

    }

    @Nullable
    protected EntityContextDocument createECD(String entityId,
                                            @NotNull List<RankingHelper.ScoredDocument> paraList) {
        List<Document> documentList = new ArrayList<>();
        List<String> contextEntityList = new ArrayList<>();
        for (RankingHelper.ScoredDocument scoredDocument : paraList) {
            Document doc = scoredDocument.getDocument();
            List<String> entityList = getEntitiesInPara(doc);
            if (entityList.isEmpty()) {
                // If the document does not have any entities then ignore
                continue;
            }
            if (entityList.contains(entityId)) {
                documentList.add(doc);
                contextEntityList.addAll(entityList);
            } else{
                System.out.println("Target entity not in document.");
            }
        }

        // If there are no documents in the pseudo-document
        if (documentList.size() == 0) {
            return null;
        }
        return new EntityContextDocument(documentList, entityId, contextEntityList);
    }

    @NotNull
    protected List<String> getEntitiesInPara(@NotNull Document doc) {
        List<String> entityList = new ArrayList<>();
        String[] paraEntities = doc.get("Entities").split("\n");

        for (String entity : paraEntities) {
            if (! entity.isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(entity);
                    String linkedEntityId = jsonObject.getString("linkPageId");
                    entityList.add(linkedEntityId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        return entityList;
    }

    @NotNull
    protected Map<String, Double> getDistribution(@NotNull List<String> contextEntityList,
                                                Set<String> retEntitySet) {

        HashMap<String, Integer> freqMap = new HashMap<>();

        // For every co-occurring entity do
        for (String entityID : contextEntityList) {
            // If the entity also occurs in the list of entities retrieved for the query then
            if ( retEntitySet.contains(entityID)) {
                freqMap.compute(entityID, (t, oldV) -> (oldV == null) ? 1 : oldV + 1);
            }
        }
        return  toDistribution(freqMap);
    }

    @NotNull
    protected Map<String, Double> toDistribution (@NotNull Map<String, Integer> freqMap) {
        Map<String, Double> dist = new HashMap<>();

        // Calculate the normalizer
        int norm = 0;
        for (int val : freqMap.values()) {
            norm += val;
        }

        // Normalize the map
        for (String word: freqMap.keySet()) {
            int freq = freqMap.get(word);
            double normFreq = (double) freq / norm;
            normFreq = Double.parseDouble(df.format(normFreq));
            if (! (normFreq < 0.0d) ) {
                dist.put(word, normFreq);
            }
        }
        return dist;
    }


    private Document getTopDocForEntity(@NotNull EntityContextDocument d, Map<String, Double> freqMap) {

        double topScore = 0.0d;
        Document topDoc = null;

        // Get the list of documents in the pseudo-document corresponding to the entity
        List<Document> documents = d.getDocumentList();

        // For every document do
        for (Document doc : documents) {

            // Get the score of the document
            double score = getParaScore(doc, freqMap);

            if (score > topScore) {
                topScore = score;
                topDoc = doc;
            }
        }
        return topDoc;
    }

    /**
     * Method to find the score of a paragraph.
     * This method looks at all the entities in the paragraph and calculates the score from them.
     * For every entity in the paragraph, if the entity has a score from the entity context pseudo-document,
     * then sum over the entity scores and store the score in a HashMap.
     *
     * @param doc  Document
     * @param freqMap HashMap where Key = entity id and Value = score
     * @return Integer
     */

    protected double getParaScore(@NotNull Document doc, Map<String, Double> freqMap) {

        double entityScore, paraScore = 0;
        // Get the entities in the paragraph
        // Make an ArrayList from the String array

        List<String> entityList = getEntitiesInPara(doc);
        /* For every entity in the paragraph do */
        for (String e : entityList) {
            // Lookup this entity in the HashMap of frequencies for the entities
            // Sum over the scores of the entities to get the score for the passage
            // Store the passage score in the HashMap
            if (freqMap.containsKey(e)) {
                entityScore = freqMap.get(e);
                paraScore += entityScore;
            }

        }
        return paraScore;
    }



    public static void main(@NotNull String[] args) {
        String mode = args[0];

        if (mode.equals("train")) {
            String paraIndex = args[1];
            String entityPassageFile = args[2];
            String entityRunFile = args[3];
            String posOrNegEntityFile = args[4];
            String outFile = args[5];
            String queryIdToNameFile = args[6];
            String entityIdToNameFile = args[7];
            String stopWordsFile = args[8];
            boolean parallel = args[9].equals("true");


            SupportPsg ob = new SupportPsg(paraIndex, entityPassageFile, entityRunFile, posOrNegEntityFile,
                    queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
            ob.doTask(ob.entities.keySet(), mode);
            System.out.print("Writing to file....");
            ob.writeToFile(outFile, ob.entityDataMap);
            System.out.println("[Done].");


        } else if (mode.equals("dev") || mode.equals("test")) {
            String paraIndex = args[1];
            String entityPassageFile = args[2];
            String entityRunFile = args[3];
            String outFile = args[4];
            String queryIdToNameFile = args[5];
            String entityIdToNameFile = args[6];
            String stopWordsFile = args[7];
            boolean parallel = args[8].equals("true");


            SupportPsg ob = new SupportPsg(paraIndex, entityPassageFile, entityRunFile,
                    queryIdToNameFile, entityIdToNameFile, stopWordsFile, parallel);
            ob.doTask(ob.entityRunMap.keySet(), mode);
            System.out.print("Writing to file....");
            ob.writeToFile(outFile, ob.entityDataMap);
            System.out.println("[Done].");

        }

    }
}
