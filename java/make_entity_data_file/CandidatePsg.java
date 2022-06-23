package make_entity_data_file;

import help.LuceneHelper;
import help.RankingHelper;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;


public abstract class CandidatePsg extends MakeEntityData {
    protected final Map<String, String> queryIdToNameMap;
    protected final Map<String, String> entityIdToNameMap;
    protected Map<String, String> entityParaMap;
    protected List<String> stopWords;
    public Map<String, LinkedHashMap<String, Double>> entityRunMap = new HashMap<>();

    public CandidatePsg(String index,
                        String entityParaFile,
                        String queriesFile,
                        String entitiesFile,
                        String stopWordsFile,
                        boolean parallel){

        super(index, parallel);

        System.out.print("Loading queries file....");
        queryIdToNameMap = readTsvFile(queriesFile);
        System.out.println("[Done].");

        System.out.print("Loading entities file....");
        entityIdToNameMap = readTsvFile(entitiesFile);
        System.out.println("[Done].");

        System.out.print("Loading entity to passage mappings...");
        entityParaMap = readTsvFile(entityParaFile);
        System.out.println("[Done].");

        System.out.print("Loading stop words...");
        stopWords = getStopWords(stopWordsFile);
        System.out.println("[Done].");
    }


    protected abstract void getEntityData(String queryId, @NotNull String mode);


    @Nullable
    protected Document getTopDocForEntity(String queryId, String entityId) throws IOException {

        if (queryIdToNameMap.containsKey(queryId) && entityIdToNameMap.containsKey(entityId) && entityParaMap.containsKey(entityId)) {

            String queryStr = queryIdToNameMap.get(queryId);
            String entityStr = entityIdToNameMap.get(entityId);
            try {
                List<String> paraList = JSONArrayToList(new JSONObject(entityParaMap.get(entityId))
                        .getJSONArray("paragraphs"));
                // Rank these paragraphs for the query
                List<RankingHelper.ScoredDocument> rankedParaList = rankParasForQuery(queryStr, entityStr, paraList);

                if (!rankedParaList.isEmpty()) {
                    return getEntityDescription(queryId, entityId, rankedParaList);
                }else {
                    System.err.println("No ranked paragraphs found for entity: " + entityId);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("No paragraphs found for entity: " + entityId);
        }

        return null;
    }

    protected abstract Document getEntityDescription(String queryId, String entityId, List<RankingHelper.ScoredDocument> rankedParaList);

    @NotNull
    protected List<String> JSONArrayToList(@NotNull JSONArray paragraphs) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < paragraphs.length(); i++) {
            try {
                result.add(paragraphs.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @NotNull
    protected List<RankingHelper.ScoredDocument> rankParasForQuery(String queryStr,
                                                                   String entityStr,
                                                                   List<String> paraList) {

        // Get the Lucene documents
        List<Document> luceneDocList = LuceneHelper.toLuceneDocList(paraList, indexSearcher);

        // Convert to BooleanQuery
        BooleanQuery booleanQuery = RankingHelper.toBooleanQueryWithPRF(
                queryStr,
                entityStr,
                luceneDocList,
                stopWords
        );

        // Rank the Lucene Documents using the BooleanQuery

        if (booleanQuery == null) {
            return new ArrayList<>();
        }

        return RankingHelper.rankDocuments(booleanQuery, luceneDocList, 1000);
    }

}
