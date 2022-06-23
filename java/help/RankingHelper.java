package help;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Contains code to rank documents for query using query expansion with RM3.
 *
 * @version 11/17/2021
 * @author Shubham Chatterjee
 */

public class RankingHelper  {

    public static class ScoredDocument {
        private final String docId;
        private final Document document;
        private final double score;

        public ScoredDocument(String docId, Document document, double score) {
            this.docId = docId;
            this.document = document;
            this.score = score;
        }

        public String getDocId() {
            return docId;
        }

        public Document getDocument() {
            return document;
        }

        public double getScore() {
            return score;
        }

        @Override
        public String toString() {
            return "ScoredDoc{" +
                    "docId='" + docId + '\'' +
                    ", document=" + document +
                    ", score=" + score +
                    '}';
        }
    }

    /**
     * Convert a QueryString to a BooleanQuery with RM3 terms.
     * @param queryStr QueryString
     * @param topKDocs Feedback set of documents
     * @param stopWords List of stop words
     * @return BooleanQuery
     */


    public static BooleanQuery toBooleanQuery(String queryStr,
                                        List<ScoredDocument> topKDocs,
                                        List<String> stopWords) {


        //Get the term distribution
        Map<String, Double> termDist = getTermDistribution(topKDocs, stopWords);

        // Convert the query to an expanded BooleanQuery
        BooleanQuery booleanQuery = null;
        List<Map.Entry<String, Double>> allWordFreqList = new ArrayList<>(termDist.entrySet());
        List<Map.Entry<String, Double>> expansionTerms = allWordFreqList.subList(0,
                Math.min(20, allWordFreqList.size()));
        try {
            booleanQuery = toRm3Query(queryStr, expansionTerms, false, "Text", new EnglishAnalyzer());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return booleanQuery;

    }

    /**
     * Convert a QueryString to a BooleanQuery without RM3 terms.
     * @param queryStr QueryString
     * @return BooleanQuery
     */

    public static BooleanQuery toBooleanQuery(String queryStr, String entityStr)  {
        List<String> tokens = new ArrayList<>();
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        try {
            tokens.addAll(tokenizeQuery(queryStr, "Text", new EnglishAnalyzer()));
            tokens.addAll(tokenizeQuery(entityStr, "Text", new EnglishAnalyzer()));
            for (String token : tokens) {
                booleanQuery.add(new BoostQuery(new TermQuery(new Term("Text", token)), 1.0f),
                        BooleanClause.Occur.SHOULD);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return booleanQuery.build();
    }

    /**
     * Converts a QueryString to a BooleanQuery with RM3 terms using Pseudo-Relevance Feedback.
     * @param queryStr QueryString
     * @param luceneDocList Initial set of documents
     * @param stopWords List of stop words
     * @return BooleanQuery
     */

    @Nullable
    public static BooleanQuery toBooleanQueryWithPRF(String queryStr,
                                                     String entityStr,
                                                     List<Document> luceneDocList,
                                                     List<String> stopWords) {
        // 1. Convert the QueryString to a BooleanQuery with only the query terms
        BooleanQuery booleanQueryWithoutExpansionTerms = toBooleanQuery(queryStr, entityStr);

        // 2. Rank the documents
        List<ScoredDocument> rankedDocList = rankDocuments(booleanQueryWithoutExpansionTerms, luceneDocList, 100);

        if (rankedDocList.isEmpty()) {
            return null;
        }

        // 3. Convert to BooleanQuery with RM3 terms

        return toBooleanQuery(queryStr, rankedDocList, stopWords);



    }

    @NotNull
    public static List<String> tokenizeQuery(String queryStr, String searchField, @NotNull Analyzer analyzer) throws IOException {
        TokenStream tokenStream = analyzer.tokenStream(searchField, new StringReader(queryStr));
        tokenStream.reset();
        List<String> tokens = new ArrayList<>();
        while (tokenStream.incrementToken() && tokens.size() < 64) {
            final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
            tokens.add(token);
        }
        tokenStream.end();
        tokenStream.close();
        return tokens;
    }
    public static BooleanQuery toRm3Query(String queryStr,
                                    List<Map.Entry<String, Double>> relevanceModel,
                                    boolean omitQueryTerms,
                                    String searchField,
                                    Analyzer analyzer) throws IOException {
        List<String> tokens = new ArrayList<>();
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        if (!omitQueryTerms) {
            tokens.addAll(tokenizeQuery(queryStr, searchField, analyzer));
            for (String token : tokens) {
                booleanQuery.add(new BoostQuery(new TermQuery(new Term(searchField, token)), 1.0f),
                        BooleanClause.Occur.SHOULD);
            }
        }

        // add RM3 terms
        for (Map.Entry<String, Double> stringFloatEntry : relevanceModel.subList(0, Math.min(relevanceModel.size(), (64 - tokens.size())))) {
            String token = stringFloatEntry.getKey();
            double weight = stringFloatEntry.getValue();
            booleanQuery.add(new BoostQuery(new TermQuery(new Term("Text", token)), (float) weight), BooleanClause.Occur.SHOULD);
        }
        return booleanQuery.build();
    }

    @NotNull
    public static Map<String, Double> getTermDistribution(@NotNull List<ScoredDocument> topKDocs, List<String> stopWords) {
        Map<String, Double> freqDist = new HashMap<>();

        // compute score normalizer
        float normalizer = 0.0f;
        for (ScoredDocument scoredDocument: topKDocs) {
            normalizer += scoredDocument.getScore();
        }

        for (ScoredDocument scoredDocument : topKDocs) {
            double weight = scoredDocument.getScore() / normalizer;
            String processedDocText = getProcessedDocText(scoredDocument.getDocument(), stopWords);
            try {
                addTokens(processedDocText, weight, freqDist);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return sortByValueDescending(freqDist);
    }


    public static  void addTokens(String content,
                            double weight,
                            Map<String,Double> wordFreq) throws IOException {

        TokenStream tokenStream = new EnglishAnalyzer().tokenStream("Text", new StringReader(content));
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
            wordFreq.compute(token, (t, oldV) -> (oldV == null) ? weight : oldV + weight);
        }
        tokenStream.end();
        tokenStream.close();
    }


    /**
     * Returns the list of words in the paragraph after preprocessing.
     * @param document Lucene Document
     * @param stopWords List of stop words
     * @return Pre-processed text
     */

    @NotNull
    public static String getProcessedDocText(@NotNull Document document, List<String> stopWords) {
        // Get the document corresponding to the paragraph from the lucene index
        String docContents = document.get("Text");
        List<String> words = preProcess(docContents, stopWords);
        return String.join(" ", words);
    }
    /**
     * Pre-process the text.
     * (1) Lowercase words.
     * (2) Remove all spaces.
     * (3) Remove special characters.
     * (4) Remove stop words.
     * @param text String Text to pre-process
     * @return List of words from the text after pre-processing.
     */

    @NotNull
    public static List<String> preProcess(String text, @NotNull List<String> stopWords) {

        // Convert all words to lowercase
        text = text.toLowerCase();

        // Remove all spaces
        text = text.replace("\n", " ").replace("\r", " ");

        // Remove all special characters such as - + ^ . : , ( )
        text = text.replaceAll("[\\-+.^*:,;=(){}\\[\\]\"]","");

        // Get all words
        List<String> words = new ArrayList<>(Arrays.asList(text.split(" ")));
        words.removeAll(Collections.singleton(null));
        words.removeAll(Collections.singleton(""));

        // Remove all stop words
        words.removeIf(stopWords::contains);

        return words;
    }

    /**
     * Ranks a list of Lucene Documents for the given query using BM25.
     * @param query Lucene Query
     * @param documentList Documents to rank
     * @param numDocs Number of documents to rank
     * @return List of ranked documents with scores.
     */

    @NotNull
    @Contract("_, _, _ -> new")
    public static List<ScoredDocument>  rankDocuments(Query query,
                                                      List<Document> documentList,
                                                      int numDocs) {


        List<ScoredDocument> rankedDocList = new ArrayList<>();
        try {

            // 1. Create the IndexWriter
            IndexWriter iw = LuceneHelper.RAMIndex.createWriter(new EnglishAnalyzer());

            // 2. Create the index
            LuceneHelper.RAMIndex.createIndex(documentList, iw);

            // 3. Create the IndexSearcher
            IndexSearcher is = LuceneHelper.RAMIndex.createSearcher(new LMJelinekMercerSimilarity(0.4f), iw);

            // 4. Search the index
            TopDocs topDocs = is.search(query, numDocs);

            if (topDocs.totalHits.value == 0) {
                // If no documents found, then return empty list
                return new ArrayList<>();
            }

            // 5. Score the docs
            ScoreDoc[] retDocs = topDocs.scoreDocs;

            for (int i = 0; i < retDocs.length; i++) {
                rankedDocList.add(new ScoredDocument(is.doc(retDocs[i].doc).get("Id"), is.doc(retDocs[i].doc), topDocs.scoreDocs[i].score));
            }
            // 6. Close the index
            //LuceneHelper.RAMIndex.close(iw);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return rankedDocList;
    }

    @NotNull
    public static <K, V>LinkedHashMap<K, V> sortByValueDescending(@NotNull Map<K, V> map) {
        LinkedHashMap<K, V> reverseSortedMap = new LinkedHashMap<>();
        map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue((Comparator<? super V>) Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
        return reverseSortedMap;
    }

}
