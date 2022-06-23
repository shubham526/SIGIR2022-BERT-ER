package make_entity_data_file;

import help.LuceneHelper;
import help.RankingHelper;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;



/**
 * Start with a passage ranking.
 * We first collect all the aspects of an entity from the top-K passages of the passage ranking.
 * If there is only 1 aspect found for an entity, then we use it. Otherwise, if more than one aspect is found, we rank
 * the aspects of the entity for the query using BM25 and use the top ranked aspect as the entity's representation.
 * Get the map of (pos_ent, pos_ent_aspect) and (neg_ent, neg_ent_aspect).
 * Make the pos_ent_id_to_text, neg_ent_id_to_text.
 *
 * @version 11/17/2021
 * @author Shubham Chatterjee
 */
public class AspectsInCandidateSet extends MakeEntityData {

    public static class ParaAspect {
        private final Document aspectDoc; // Lucene document corresponding to the aspect
        private final String paraId; // Id of the paragraph containing the aspect

        public ParaAspect(Document aspectDoc, String paraId) {
            this.aspectDoc = aspectDoc;
            this.paraId = paraId;
        }

        public Document getAspectDoc() {
            return aspectDoc;
        }

        public String getParaId() {
            return paraId;
        }

        public String getAspectId() {
            return aspectDoc.get("Id");
        }

        public String getAspectText() {
            return aspectDoc
                    .get("Text")
                    .replaceAll("\n", " ")
                    .replaceAll("\r", " ");
        }
    }


    private final IndexSearcher catalogSearcher;
    private final Map<String, Map<String, String>> posEntityToTextMap = new HashMap<>();
    private final Map<String, Map<String, String>> negEntityToTextMap = new HashMap<>();
    private final Map<String, Map<String, String>> allEntityToTextMap = new HashMap<>();
    private final Map<String, String> queryIdToNameMap;
    private final Map<String, LinkedHashMap<String, Double>> paraRankings;
    private final List<String> stopWords;
    private List<RankingHelper.ScoredDocument> topKDocs;
    private final int takeKDocs;


    public AspectsInCandidateSet(String paraIndex,
                                 String catalogIndex,
                                 String psgRanking,
                                 String entityQrelFile,
                                 String queriesFile,
                                 String stopWordsFile,
                                 String posEntityToTextFile,
                                 String negEntityToTextFile,
                                 int takeKDocs,
                                 String mode,
                                 boolean parallel) {

        super(paraIndex, entityQrelFile, parallel);


        this.takeKDocs = takeKDocs;


        System.out.print("Setting up catalog index...");
        this.catalogSearcher = LuceneHelper.createSearcher(catalogIndex, "bm25");
        System.out.println("[Done].");

        System.out.print("Loading passage run file...");
        paraRankings = readRunFile(psgRanking);
        System.out.println("[Done].");

        System.out.print("Loading queries file....");
        queryIdToNameMap = readTsvFile(queriesFile);
        System.out.println("[Done].");

        System.out.print("Loading stop words....");
        stopWords = getStopWords(stopWordsFile);
        System.out.println("[Done].");

        doTask(paraRankings.keySet(), mode);

        System.out.print("Writing positive entities data to file....");
        writeToFile(posEntityToTextFile, posEntityToTextMap);
        System.out.println("[Done].");

        System.out.print("Writing negative entities data to file....");
        writeToFile(negEntityToTextFile, negEntityToTextMap);
        System.out.println("[Done].");



    }

    public AspectsInCandidateSet(String paraIndex,
                                 String catalogIndex,
                                 String psgRanking,
                                 String queriesFile,
                                 String stopWordsFile,
                                 String outFile,
                                 int takeKDocs,
                                 String mode,
                                 boolean parallel) {
        super(paraIndex, parallel);

        this.takeKDocs = takeKDocs;


        System.out.print("Setting up catalog index...");
        this.catalogSearcher = LuceneHelper.createSearcher(catalogIndex, "bm25");
        System.out.println("[Done].");

        System.out.print("Loading passage run file...");
        paraRankings = readRunFile(psgRanking);
        System.out.println("[Done].");

        System.out.print("Loading queries file....");
        queryIdToNameMap = readTsvFile(queriesFile);
        System.out.println("[Done].");

        System.out.print("Loading stop words....");
        stopWords = getStopWords(stopWordsFile);
        System.out.println("[Done].");

        doTask(paraRankings.keySet(), mode);

        System.out.print("Writing entity data to file....");
        writeToFile(outFile, allEntityToTextMap);
        System.out.println("[Done].");


    }


    /**
     * Get the text corresponding to the entity.
     * @param queryId String
     */

    @Override
    public void getEntityData(String queryId, @NotNull String mode) {

        String queryStr = queryIdToNameMap.get(queryId);

        // Get the top-K passages corresponding to the query from the passage run file
        topKDocs = getTopKDocsForQuery(paraRankings.get(queryId));


        // Get the map of all (entity, aspects) from the passages above
        Map<String, Set<ParaAspect>> allQueryEntities = getEntitiesFromPassages(topKDocs);

        // Get a ranking of entities from the topK passages for the query
        Map<String, Double> entityRankingForQuery = getEntityRankingFromCandidateSet(topKDocs)
                .entrySet()
                .stream()
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);


        if (mode.equals("train")) {


            // Get the relevant entities corresponding to the query from the entity qrel file
            if (entities.containsKey(queryId)) {

                Set<String> relevantEntitySet = entities.get(queryId);


                // Get the negative query entities
                Set<String> negQueryEntitySet = new HashSet<>(allQueryEntities.keySet());
                try {
                    negQueryEntitySet.removeAll(relevantEntitySet);
                } catch (NullPointerException e) {
                    System.out.println("NullPointerException");
                    e.printStackTrace();
                }

                // Get the positive query entities
                Set<String> posQueryEntitySet = new HashSet<>(allQueryEntities.keySet());
                try {
                    posQueryEntitySet.retainAll(relevantEntitySet);
                } catch (NullPointerException e) {
                    System.out.println("NullPointerException");
                    e.printStackTrace();
                }

                // Get the positive entities text
                Map<String, String> posEntityToText = getEntityText(queryStr, posQueryEntitySet, allQueryEntities, entityRankingForQuery);
                posEntityToTextMap.put(queryId, posEntityToText);

                // Get the negative entities text
                Map<String, String> negEntityToText = getEntityText(queryStr, negQueryEntitySet, allQueryEntities, entityRankingForQuery);
                negEntityToTextMap.put(queryId, negEntityToText);
            }
        } else {

            Map<String, String> allEntityToText = getEntityText(queryStr, entityRankingForQuery.keySet(), allQueryEntities, entityRankingForQuery);
            allEntityToTextMap.put(queryId, allEntityToText);
        }
        if (parallel) {
            count.getAndIncrement();
            System.out.println("Done: " + queryId + " ( " + count + "/" + total + " ).");
        }
    }

    /**
     * Returns a list of top-K passages for the query
     * @param psgRankings Passage ranking for the query.
     * @return Top-K passages from the ranking.
     */

    @NotNull
    private List<RankingHelper.ScoredDocument> getTopKDocsForQuery(@NotNull LinkedHashMap<String, Double> psgRankings) {
        List<Map.Entry<String, Double>> allPsgRankings = new ArrayList<>(psgRankings.entrySet());
        List<Map.Entry<String, Double>> subList = allPsgRankings.subList(0, Math.min(takeKDocs, allPsgRankings.size()));
        List<RankingHelper.ScoredDocument> topKDocs = new ArrayList<>();

        for (Map.Entry<String, Double> entry : subList) {
            try {
                String paraId = entry.getKey();
                double paraScore = entry.getValue();
                Document doc = LuceneHelper.searchIndex("Id", paraId, indexSearcher);
                if (doc != null) {
                    topKDocs.add(new RankingHelper.ScoredDocument(paraId, doc, paraScore));
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }

        return topKDocs;

    }

    /**
     * Derive an entity ranking from a passage ranking.
     * Score of entity = sum of scores of passages it appears in
     * @param topKDocs List of top-K passages for the query.
     * @return Entity ranking derived from top-K passages.
     */

    @NotNull
    private Map<String, Double> getEntityRankingFromCandidateSet(List<RankingHelper.ScoredDocument> topKDocs) {


        // 1. Get the map from Entity --> Passage set
        // Passage set = set of passages which contains a link to the entity
        Map<String, Set<String>> entityToPassageMap = getEntityToPassageMap(topKDocs);

        // 2. Rank the entities using the scores of the passages.
        // Score of entity = sum of scores of passages it appears in


        return rankEntities(entityToPassageMap, topKDocs);
    }

    /**
     * Rank entities.
     * @param entityToPassageMap Map from entity to set of passages that the entity appears in.
     * @param topKDocs Top-K passages for the query.
     * @return Ranking of entities derived from the top-K passages.
     */

    @NotNull
    private Map<String, Double> rankEntities(@NotNull Map<String, Set<String>> entityToPassageMap,
                                             List<RankingHelper.ScoredDocument> topKDocs) {

        Map<String, Double> entityRanking = new HashMap<>();

        for (String entityId : entityToPassageMap.keySet()) {
            Set<String> passageSetForEntity = entityToPassageMap.get(entityId);
            double entityScore = getEntityScore(passageSetForEntity, topKDocs);
            entityRanking.put(entityId, entityScore);
        }

        return sortByValueDescending(entityRanking);
    }

    /**
     * Get the score of an entity.
     * @param passageSetForEntity Set of passages in which the entity appears.
     * @param topKDocs Top-K passages for the query.
     * @return Score of the entity.
     */

    private double getEntityScore(@NotNull Set<String> passageSetForEntity,
                                  @NotNull List<RankingHelper.ScoredDocument> topKDocs) {
        double score = 0.0d;

        // We convert the List<Map.Entry<String, Double>> to a Map<String, Double> for easier lookup
        Map<String, Double> rankedDocMap = new LinkedHashMap<>();
        for (RankingHelper.ScoredDocument scoredDocument : topKDocs) {
            rankedDocMap.put(scoredDocument.getDocId(), scoredDocument.getScore());
        }

        for (String paraId : passageSetForEntity) {
            if (rankedDocMap.containsKey(paraId)) {
                score += rankedDocMap.get(paraId);
            }
        }

        return score;
    }

    /**
     * Make a map of entity to set of passages in which the entity occurs.
     * @param topKDocs Top-K passages for the query.
     * @return Map of of {Entity: Set of passages in which entity occurs}
     */

    @NotNull
    private Map<String, Set<String>> getEntityToPassageMap(@NotNull List<RankingHelper.ScoredDocument> topKDocs) {
        Map<String, Set<String>> entityToPassageMap = new HashMap<>();

        for (RankingHelper.ScoredDocument document : topKDocs) {
            // Get the entities in the paragraph
            Set<String> entitySetForPara = getParaEntities(document.getDocument());

            // Populate the entityToPassageMap
            makeEntityToPassageMap(document.getDocId(), entitySetForPara, entityToPassageMap);
        }
        return entityToPassageMap;
    }

    /**
     * Helper method.
     */

    private void makeEntityToPassageMap(String paraId,
                                        @NotNull Set<String> entitySetForPara,
                                        Map<String, Set<String>> entityToPassageMap) {
        for (String entityId : entitySetForPara) {
            Set<String> paraSetForEntity = entityToPassageMap.containsKey(entityId)
                    ? entityToPassageMap.get(entityId)
                    : new HashSet<>();
            paraSetForEntity.add(paraId);
            entityToPassageMap.put(entityId, paraSetForEntity);
        }
    }

    /**
     * Get the entities in the paragraph.
     * @param document Lucene Document
     * @return Set of entities in the paragraph
     */

    @NotNull
    private Set<String> getParaEntities(@NotNull Document document) {
        Set<String> entitySet = new HashSet<>();
        String[] annotations = document.get("Entities").split("\n");
        for (String annotation : annotations) {
            if (!annotation.isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(annotation);
                    String entityId = jsonObject.getString("linkPageId");
                    entitySet.add(entityId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return entitySet;

    }


    @NotNull
    private Map<String, String> getEntityText(String queryStr,
                                              @NotNull Set<String> entitySet,
                                              Map<String, Set<ParaAspect>> entityToAspectMap,
                                              Map<String, Double> entityRankingForQuery) {
        Map<String, String> entityToText = new HashMap<>();

        try {

            for (String entityId : entitySet) {
                if (entityToAspectMap.containsKey(entityId) && entityRankingForQuery.containsKey(entityId)) {
                    String data = getDataForEntity(queryStr, entityToAspectMap.get(entityId), entityRankingForQuery.get(entityId));
                    if (!data.isEmpty()) {
                        entityToText.put(entityId, data);
                    }
                }
            }
        } catch (NullPointerException e) {
            //System.err.println("NullPointerException for query: " + queryStr);
            e.printStackTrace();
        }
        return entityToText;
    }

    private String getDataForEntity(String queryStr,  @NotNull Set<ParaAspect> aspectSet, double entityScore) {

        // If there are more than one aspect of the entity found in the passage then we get the top ranked aspect of
        // the entity for the query
        if (aspectSet.size() > 1) {
            return topAspectTextForQuery(queryStr, aspectSet, entityScore);
        }

        // Otherwise, there is only one aspect, so we grab that from the set
        ParaAspect paraAspect = new ArrayList<>(aspectSet).get(0);
        String aspectId = paraAspect.getAspectId();
        String data = "";
        if (! aspectId.isEmpty()) {
            String aspectText = paraAspect.getAspectText();
            if (! aspectText.isEmpty()) {
                String paraId = paraAspect.getParaId();
                data = toJSONString(paraId, aspectId, aspectText, entityScore);
            }

        }
        return data;
    }

    @NotNull
    private String topAspectTextForQuery(String queryStr, Set<ParaAspect> aspectSet, double entityScore) {



        // Now use the top-K passages to derive expansion terms for the query and convert it to a BooleanQuery
        BooleanQuery booleanQuery = RankingHelper.toBooleanQuery(queryStr, topKDocs, stopWords);

        // Now get the top aspect for the query
        // We score the aspects for the query using BM25
        ParaAspect topAspect = getTopAspectForQuery(booleanQuery, aspectSet);
        if (topAspect == null) {
            return "";
        }
        String topAspectId = topAspect.getAspectId();
        String topAspectText = topAspect.getAspectText();
        String topAspectParaId = topAspect.getParaId();
        return topAspectText.isEmpty()
                ? ""
                : toJSONString(topAspectParaId, topAspectId, topAspectText, entityScore);

    }

    @NotNull
    private Map<String, ParaAspect> toDocParaAspectMap(@NotNull Set<ParaAspect> aspectSet) {
        Map<String, ParaAspect> paraAspectDocumentMap = new HashMap<>();

        for (ParaAspect paraAspect : aspectSet) {
            paraAspectDocumentMap.put(paraAspect.getAspectDoc().get("Id"), paraAspect);
        }

        return paraAspectDocumentMap;
    }

    @Nullable
    private ParaAspect getTopAspectForQuery(BooleanQuery booleanQuery, Set<ParaAspect> paraAspectSet) {
        Map<String, ParaAspect> documentParaAspectMap = toDocParaAspectMap(paraAspectSet);
        List<Document> aspectDocs = toLuceneDoc(paraAspectSet);
        List<RankingHelper.ScoredDocument> rankedDocList = RankingHelper.rankDocuments(booleanQuery, aspectDocs, 100);
        String topDocId = rankedDocList.get(0).getDocId();
        return documentParaAspectMap.get(topDocId);
    }

    @NotNull
    private List<Document> toLuceneDoc(@NotNull Set<ParaAspect> aspects) {
        List<Document> aspectList = new ArrayList<>();
        for (ParaAspect paraAspect : aspects) {
            Document doc = paraAspect.getAspectDoc();
            aspectList.add(doc);
        }
        return aspectList;
    }

    @NotNull
    private Map<String, Set<ParaAspect>> getEntitiesFromPassages(@NotNull List<RankingHelper.ScoredDocument> paraList) {

        Map<String, Set<ParaAspect>> queryEntities = new HashMap<>();

        for (RankingHelper.ScoredDocument document : paraList) {
            Document doc = document.getDocument();
            String paraId = document.getDocId();
            String[] annotations = doc.get("Entities").split("\n");
            for (String annotation : annotations) {
                if (!annotation.isEmpty()) {
                    try {
                        JSONObject jsonObject = new JSONObject(annotation);
                        String aspectId = jsonObject.getString("aspect");
                        String entityId = jsonObject.getString("linkPageId");
                        Document aspectDoc = getAspectDocFromIndex(aspectId);
                        if (aspectDoc != null) {
                            Set<ParaAspect> aspectList = queryEntities.containsKey(entityId)
                                    ? queryEntities.get(entityId)
                                    : new HashSet<>();
                            aspectList.add(new ParaAspect(aspectDoc, paraId));
                            queryEntities.put(entityId, aspectList);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return queryEntities;
    }

    @Nullable
    private Document getAspectDocFromIndex(String aspectId) {
        try {
            Document doc = LuceneHelper.searchIndex("Id", aspectId, catalogSearcher);
            if (doc != null) {
                return doc;

            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(@NotNull String[] args) {
        String mode = args[0];
        if (mode.equals("train")) {
            String paraIndex = args[1];
            String catalogIndex = args[2];
            String psgRanking = args[3];
            String entityQrelFile = args[4];
            String queriesFile = args[5];
            String stopWordsFile = args[6];
            String posEntityToTextFile = args[7];
            String negEntityToTextFile = args[8];
            int takeKDocs = Integer.parseInt(args[9]);
            boolean parallel = args[10].equals("true");

            new AspectsInCandidateSet(paraIndex, catalogIndex, psgRanking, entityQrelFile, queriesFile, stopWordsFile,
                    posEntityToTextFile, negEntityToTextFile,  takeKDocs, mode, parallel);


        } else if (mode.equals("dev") || mode.equals("test")) {
            String paraIndex = args[1];
            String catalogIndex = args[2];
            String psgRanking = args[3];
            String queriesFile = args[4];
            String stopWordsFile = args[5];
            String outFile = args[6];
            int takeKDocs = Integer.parseInt(args[7]);
            boolean parallel = args[8].equals("true");

            new AspectsInCandidateSet(paraIndex, catalogIndex, psgRanking, queriesFile, stopWordsFile,
                    outFile, takeKDocs, mode, parallel);
        }
    }


}
