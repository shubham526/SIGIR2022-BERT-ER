package help;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LuceneHelper {

    @Nullable
    public static Similarity getSimilarity(@NotNull String similarityStr) {

        if (similarityStr.equalsIgnoreCase("bm25")) {
            return new BM25Similarity();
        } else if (similarityStr.equalsIgnoreCase("lmds")) {
            return new LMDirichletSimilarity(1500);
        } else if (similarityStr.equalsIgnoreCase("lmjm")) {
            return new LMJelinekMercerSimilarity(0.5f);
        }
        return null;

    }

    @NotNull
    public static IndexSearcher createSearcher(String indexDir, @NotNull String similarityStr) {
        Similarity similarity = getSimilarity(similarityStr);

        Directory dir = null;
        try {
            dir = FSDirectory.open((new File(indexDir).toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        IndexReader reader = null;
        try {
            reader = DirectoryReader.open(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert reader != null;
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);
        return searcher;
    }

    @Nullable
    public static Document searchIndex(String field, String query, @NotNull IndexSearcher searcher)throws IOException, ParseException {
        Term term = new Term(field,query);
        Query q = new TermQuery(term);
        TopDocs tds = searcher.search(q,1);

        ScoreDoc[] retDocs = tds.scoreDocs;
        if(retDocs.length != 0) {
            return searcher.doc(retDocs[0].doc);
        }
        return null;
    }

    @NotNull
    public static List<Document> toLuceneDocList(@NotNull List<String> paraList, IndexSearcher indexSearcher) {
        List<Document> documentList = new ArrayList<>();
        for (String paraId : paraList) {
            try {
                Document d = searchIndex("Id", paraId, indexSearcher);
                if (d != null) {
                    documentList.add(d);
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

        }
        return documentList;
    }

    /**
     * Class to make a RAM index.
     * This class uses the Lucene 7.7.0 RAMDirectory to create in-memory indices.
     * NOTE: Use caution in the use of this class! The RAMDirectory class has been marked as deprecated by the developers.
     * @author Shubham Chatterjee
     * @version 03/11/2019
     */
    public static class RAMIndex {

        /**
         * Get the IndexWriter.
         * This method uses the lucene RAMDirectory which has been marked deprecated.
         * The reason for its use is that we want to maintain an in-memory index of relevant documents for
         * every query and there was no other tool in Lucene 7.7.0 that I am aware of that does this.
         * The problem with using RAMDirectory is that I cannot use parraelStreams in Java since this method
         * has been marked as not thread-safe by the developers.
         * @return IndexWriter
         */
        public static IndexWriter createWriter(Analyzer analyzer) {
            //Directory dir = new RAMDirectory();
            Directory dir = new ByteBuffersDirectory();
            IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter iw = null;
            try {
                iw = new IndexWriter(dir, conf);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return iw;

        }

        /**
         * Get the IndexSearcher.
         * @return IndexSearcher
         * @throws IOException
         */
        @NotNull
        public static IndexSearcher createSearcher(Similarity similarity, @NotNull IndexWriter iw) throws IOException {
            Directory dir = iw.getDirectory();
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(similarity);
            return searcher;
        }
        /**
         * Build an in-memory index of documents passed as parameters.
         * @param documents The documents to index
         */
        public static void createIndex(@NotNull List<Document> documents, IndexWriter iw) throws IOException {
            for (Document d : documents) {
                if (d != null) {
                    try {
                        iw.addDocument(d);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            iw.commit();
        }

        /**
         * Close the directory to release the associated memory.
         * @param iw IndexWriter
         * @throws IOException
         */
        public static void close(@NotNull IndexWriter iw) throws IOException {
            iw.getDirectory().close();

        }
    }



}
