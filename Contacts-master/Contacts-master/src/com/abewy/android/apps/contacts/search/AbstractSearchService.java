package com.abewy.android.apps.contacts.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.abewy.android.apps.contacts.search.analysis.NGramAnalyzer;
import com.abewy.android.apps.contacts.search.analysis.PinyinAnalyzer;
import com.abewy.android.apps.contacts.search.analysis.T9Analyzer;
import com.abewy.android.apps.contacts.search.utils.StringUtils;
import com.abewy.android.apps.contacts.search.utils.T9Converter;


public abstract class AbstractSearchService {
    protected static final String TAG = AbstractSearchService.class.getSimpleName();
    public static final Pattern PHONE_STRIP_PATTERN = Pattern.compile("[^+\\d]");
    public static final String FIELD_NAME = "name";
    public static final String FIELD_PINYIN = "pinyin";
    public static final String FIELD_NUMBER = "number";
    public static final String FIELD_HIGHLIGHTED_NUMBER = "hl_number";
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String INDEX_TYPE_CALLLOG = "CALLLOG";
    public static final String INDEX_TYPE_CONTACT = "CONTACT";
    public static final long ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
    public static final long THIRTY_DAYS_IN_MILLISECONDS = 30 * ONE_DAY_IN_MILLISECONDS;// 30 days

    protected static final FieldType TYPE_STORED_WITH_TERM_VECTORS = new FieldType();

    static {
        TYPE_STORED_WITH_TERM_VECTORS.setIndexed(true);
        TYPE_STORED_WITH_TERM_VECTORS.setTokenized(true);
        TYPE_STORED_WITH_TERM_VECTORS.setStored(true);
        TYPE_STORED_WITH_TERM_VECTORS.setStoreTermVectors(true);
        TYPE_STORED_WITH_TERM_VECTORS.setStoreTermVectorPositions(true);
        TYPE_STORED_WITH_TERM_VECTORS.setStoreTermVectorOffsets(true);
        TYPE_STORED_WITH_TERM_VECTORS.freeze();
    }

    protected static final String PRE_TAG = "<font color='#0099FF'>";
    protected static final String POST_TAG = "</font>";


    protected IndexWriter indexWriter = null;
    protected IndexWriterConfig indexWriterConfig = null;
    protected Analyzer indexAnalyzer = null;
    protected Analyzer searchAnalyzer = null;
    protected ThreadPoolExecutor searchThreadPool = null;
    protected ThreadPoolExecutor rebuildThreadPool = null;
    protected String preTag = PRE_TAG;
    protected String postTag = POST_TAG;

    protected AbstractSearchService() {
        init(new RAMDirectory());
    }

    protected AbstractSearchService(File directory) {
        try {
            init(new MMapDirectory(directory));
        } catch (IOException e) {
            log(TAG, e.toString());
            init(new RAMDirectory());
        }
    }

    private void init(Directory directory) {
        try {
            long start = System.currentTimeMillis();
            Thread.currentThread().setContextClassLoader(
                    getClass().getClassLoader());
            searchThreadPool = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(1),
                    new ThreadPoolExecutor.DiscardOldestPolicy());
            rebuildThreadPool = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(1),
                    new ThreadPoolExecutor.DiscardOldestPolicy());
            Map<String, Analyzer> indexAnalyzers = new HashMap<String, Analyzer>();
            indexAnalyzers.put(FIELD_NUMBER, new NGramAnalyzer(
                    Version.LUCENE_40, 1, 7));

            indexAnalyzers.put(FIELD_PINYIN, new PinyinAnalyzer(
                    Version.LUCENE_40, true));

            Map<String, Analyzer> searchAnalyzers = new HashMap<String, Analyzer>();
            searchAnalyzers.put(FIELD_PINYIN, new T9Analyzer(Version.LUCENE_40));

            indexAnalyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer(),
                    indexAnalyzers);
            searchAnalyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer(), searchAnalyzers);
            indexWriterConfig = new IndexWriterConfig(Version.LUCENE_40,
                    indexAnalyzer);
            indexWriterConfig
                    .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            TieredMergePolicy mergePolicy = (TieredMergePolicy) indexWriterConfig.getMergePolicy();
            mergePolicy.setUseCompoundFile(false);
            indexWriterConfig.setRAMBufferSizeMB(2.0);
            indexWriter = new IndexWriter(directory, indexWriterConfig);
            long end = System.currentTimeMillis();
            log(TAG, "init time used:" + (end - start) + ",numDocs:" + indexWriter.numDocs());
        } catch (IOException e) {
            log(TAG, e.toString());
        }
    }

    protected String stripNumber(String number) {
        return PHONE_STRIP_PATTERN.matcher(number).replaceAll("");
    }

//    protected abstract ThreadPoolExecutor getSearchThreadPool();

    protected abstract void log(String tag, String msg);

    /**
     * rebuild contacts in the callers' thread
     *
     * @param urgent
     * @return
     */
    protected abstract long rebuildContacts(boolean urgent);

    /**
     * rebuild calllogs in the callers' thread
     *
     * @param urgent
     * @return
     */
    protected abstract long rebuildCalllog(boolean urgent);


    public String getPreTag() {
        return preTag;
    }

    public void setPreTag(String preTag) {
        this.preTag = preTag;
    }

    public String getPostTag() {
        return postTag;
    }

    public void setPostTag(String postTag) {
        this.postTag = postTag;
    }

    public void query(String query, int maxHits, boolean highlight,
                      SearchCallback searchCallback) {
        searchThreadPool.execute(new SearchRunnable(query, maxHits, highlight,
                searchCallback));
    }

    public void destroy() {
        try {
            searchThreadPool.shutdown();
            searchThreadPool.awaitTermination(1, TimeUnit.SECONDS);
            rebuildThreadPool.shutdown();
            rebuildThreadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log(TAG, e.toString());
        } finally {
            try {
                indexWriter.close();
            } catch (IOException e) {
                log(TAG, e.toString());
            }
            log(TAG, "index writer closed");
        }
    }

    /**
     * rebuild contacts and calllogs in a new thread
     *
     * @param urgent
     */
    public void asyncRebuild(final boolean urgent) {
        rebuildThreadPool.execute(new Runnable() {
            public void run() {
                rebuildContacts(urgent);
            }
        });
        rebuildThreadPool.execute(new Runnable() {
            public void run() {
                rebuildCalllog(urgent);
            }
        });
    }


    private class SearchRunnable implements Runnable {

        private String mQuery;
        private int mMaxHits;
        private boolean mHighlight;
        private SearchCallback mSearchCallback;

        public SearchRunnable(String query, int maxHits, boolean highlight,
                              SearchCallback searchCallback) {
            mQuery = query;
            mMaxHits = maxHits;
            mHighlight = highlight;
            mSearchCallback = searchCallback;
        }

        @Override
        public void run() {
            doQuery(mQuery, mMaxHits, mHighlight, mSearchCallback);
        }
    }

    protected void doQuery(String mQuery, int mMaxHits, boolean mHighlight, SearchCallback mSearchCallback) {
        long start = System.currentTimeMillis();
        Map<String, Float> boosts = new HashMap<String, Float>();
        if (!StringUtils.isBlank(mQuery)) {
            if (mQuery.indexOf('0') != -1 || mQuery.indexOf('1') != -1) {
                boosts.put(FIELD_NUMBER, 1.0F);
            } else if (Character.isLetter(mQuery.charAt(0))) {
                boosts.put(FIELD_PINYIN, 4.0F);
            } else {
                boosts.put(FIELD_PINYIN, 4.0F);
                if (mQuery.length() >= 2) {
                    boosts.put(FIELD_NUMBER, 1.0F);
                }
            }
        } else {
            mHighlight = false;
        }
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(
                Version.LUCENE_40, boosts.keySet().toArray(new String[0]),
                searchAnalyzer, boosts);
        multiFieldQueryParser.setAllowLeadingWildcard(false);
        multiFieldQueryParser.setDefaultOperator(QueryParser.Operator.AND);
        long highlightedTimeUsed = 0;
        try {
            Query q = boosts.isEmpty() ? new MatchAllDocsQuery()
                    : multiFieldQueryParser.parse(mQuery);
            IndexReader indexReader = DirectoryReader.open(indexWriter,
                    false);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            TopDocs td = indexSearcher.search(q, mMaxHits);
            long hits = td.totalHits;
            ScoreDoc[] scoreDocs = td.scoreDocs;
            List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>(
                    mMaxHits);
            for (ScoreDoc scoreDoc : scoreDocs) {
                Map<String, Object> doc = new HashMap<String, Object>();
                Document document = indexReader.document(scoreDoc.doc);
                String name = document.get(FIELD_NAME);
                String number = document.get(FIELD_NUMBER);
                String pinyin = document.get(FIELD_PINYIN);
                if (null != name) {
                    doc.put(FIELD_NAME, document.get(FIELD_NAME));
                }
                doc.put(FIELD_NUMBER, number);
                long begin = System.currentTimeMillis();
                if (mHighlight) {
                    String highlightedNumber = highlightNumber(number, mQuery);
                    String highlightedPinyin = null;
                    if (null != highlightedNumber) {
                        doc.put(FIELD_HIGHLIGHTED_NUMBER, highlightedNumber);
                    }
                    if (null != pinyin) {//highlight pinyin
                        highlightedPinyin = highlightPinyin(pinyin, mQuery);
                        if (null != highlightedPinyin) {
                            if (pinyin.equals(name)) { //Ã§ÂºÂ¯Ã¨â€¹Â±Ã¦â€“â€¡
                                doc.put(FIELD_NAME, highlightedPinyin);
                            } else {
                                doc.put(FIELD_PINYIN, highlightedPinyin);
                            }
                        } else {
                            if (!pinyin.equals(name)) {
                                int index = pinyin.lastIndexOf('|');
                                doc.put(FIELD_PINYIN, index == -1 ? pinyin : pinyin.substring(0, index));
                            }
                        }
                    }
                    if (null == highlightedNumber && null == highlightedPinyin) {
                        continue;
                    }
                }
                long end = System.currentTimeMillis();
                highlightedTimeUsed += (end - begin);
                doc.put(FIELD_TYPE, document.get(FIELD_TYPE));
                docs.add(doc);
            }
            indexReader.close();
            long end = System.currentTimeMillis();
            log(TAG, q.toString() + "\t" + hits + "\t" + (end - start) + "\t" + highlightedTimeUsed);
            mSearchCallback.onSearchResult(mQuery, hits, docs);
        } catch (Exception e) {
            e.printStackTrace();
            log(TAG, e.toString());
        }
    }

    /**
     * @param pinyin Ã¦â€¹Â¼Ã©Å¸Â³Ã¨Â¡Â¨Ã§Â¤ÂºÃ¯Â¼Å’Ã¦Â¯â€�Ã¥Â¦â€šWangWeiWei,Ã¦Â³Â¨Ã¦â€žï¿½Ã¦Â­Â¤Ã¥Â¤â€žÃ©Â¦â€“Ã¥Â­â€”Ã¦Â¯ï¿½Ã¦ËœÂ¯Ã¥Â¤Â§Ã¥â€ â„¢Ã§Å¡â€ž
     * @param query  Ã¦Å¸Â¥Ã¨Â¯Â¢Ã¯Â¼Å’Ã¥ï¿½Â¯Ã¨Æ’Â½Ã¤Â¸Âºt9,Ã¤Â¹Å¸Ã¥ï¿½Â¯Ã¨Æ’Â½Ã¦ËœÂ¯alphaÃ¥Â­â€”Ã¦Â¯ï¿½
     * @return Ã¨Â¿â€�Ã¥â€ºÅ¾Ã©Â«ËœÃ¤ÂºÂ®Ã¤Â¹â€¹Ã¥ï¿½Å½Ã§Å¡â€žÃ§Â»â€œÃ¦Å¾Å“Ã¦Ë†â€“Ã¨â‚¬â€¦Ã¥Å“Â¨Ã¦Â²Â¡Ã¦Å“â€°Ã©Â«ËœÃ¤ÂºÂ®Ã§Å¡â€žÃ¦Æ’â€¦Ã¥â€ ÂµÃ¤Â¸â€¹Ã¨Â¿â€�Ã¥â€ºÅ¾Ã¥Å½Â»Ã¦Å½â€°Ã¦â€¹Â¼Ã©Å¸Â³Ã©Â¦â€“Ã¥Â­â€”Ã¦Â¯ï¿½Ã§Å¡â€žÃ©Æ’Â¨Ã¥Ë†â€ 
     * @throws java.io.IOException
     */
    protected String highlightPinyin(String pinyin, String query) throws IOException {
        if (StringUtils.isEmpty(query)) {
            return null;
        }
        int index = pinyin.lastIndexOf('|');
        String full = index > -1 ? pinyin.substring(0, index) : pinyin;
        //Ã©Æ’Â½Ã¨Â½Â¬Ã¦ï¿½Â¢Ã¤Â¸Âºt9Ã¥ï¿½Å½Ã¥â€ ï¿½Ã¥Å’Â¹Ã©â€¦ï¿½
        String t9 = pinyin.toLowerCase();
        String t9Query = query.toLowerCase();
        if (!Character.isLetter(query.charAt(0)))//t9 match
        {
            t9 = T9Converter.convert(t9);
            t9Query = T9Converter.convert(query);
        }
        int start = t9.lastIndexOf(t9Query);
        if (index > -1 && start > index)//Ã©Â¦â€“Ã¥Â­â€”Ã¦Â¯ï¿½Ã¥Å’Â¹Ã©â€¦ï¿½
        {
            String match = pinyin.substring(start, start + t9Query.length());
            StringBuilder stringBuilder = new StringBuilder(pinyin.length() + match.length() * (1 + preTag.length() + postTag.length()));
            for (int i = 0, j = 0; i < full.length(); i++) {//Ã¥Â¾ÂªÃ§Å½Â¯Ã©Â«ËœÃ¤ÂºÂ®Ã©Â¦â€“Ã¥Â­â€”Ã¦Â¯ï¿½
                char c = full.charAt(i);
                if (j < match.length()) {
                    if (c != match.charAt(j)) {
                        stringBuilder.append(c);
                    } else {
                        stringBuilder.append(preTag).append(c).append(postTag);
                        j++;
                    }
                } else {
                    stringBuilder.append(c);
                }
            }
            return stringBuilder.toString();
        } else if (start > -1) {   //Ã©ï¿½Å¾Ã©Â¦â€“Ã¥Â­â€”Ã¦Â¯ï¿½Ã¥Å’Â¹Ã©â€¦ï¿½
            start = t9.indexOf(t9Query);
            StringBuilder stringBuilder = new StringBuilder(pinyin.length() + preTag.length() + postTag.length());
            stringBuilder.append(full.substring(0, start))
                    .append(preTag)
                    .append(full.substring(start, start + t9Query.length()))
                    .append(postTag)
                    .append(full.substring(start + t9Query.length()));
            return stringBuilder.toString();
        }
        return null;
    }

    /**
     * Ã¤Â¸ï¿½Ã¨Æ’Â½Ã¤Â½Â¿Ã§â€�Â¨luceneÃ§Å¡â€žhighlight,Ã¥â€ºÂ Ã¤Â¸ÂºÃ¥ï¿½Â·Ã§Â ï¿½Ã¤Â½Â¿Ã§â€�Â¨Ã§Å¡â€žÃ¦ËœÂ¯NGramÃ¥Ë†â€ Ã¨Â¯ï¿½Ã¯Â¼Å’Ã¦â€°â‚¬Ã¤Â»Â¥tokenÃ©Â¡Â¹Ã¥Â¾Ë†Ã¥Â¤Å¡Ã¯Â¼Å’Ã¨Â¿â„¢Ã¥Â°Â±Ã¥Â¯Â¼Ã¨â€¡Â´Ã©Â«ËœÃ¤ÂºÂ®Ã¦Å¾Å¡Ã¤Â¸Â¾tokenÃ§â€°Â¹Ã¥Ë†Â«Ã¨â‚¬â€”Ã¦â€”Â¶
     *
     * @param number
     * @param query
     * @return
     * @throws java.io.IOException
     */
    protected String highlightNumber(String number, String query) throws IOException {
        if (StringUtils.isEmpty(query)) {
            return null;
        }
        int start = number.indexOf(query);
        if (start != -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(number.substring(0, start));
            stringBuilder.append(preTag).append(query).append(postTag).append(number.substring(start + query.length()));
            return stringBuilder.toString();
        }
        return null;
    }

    protected Field createStringField(String field, String value) {
        return new StringField(field, value, Field.Store.YES);
    }

    protected Field createTextField(String field, String value) {
        return new TextField(field, value, Field.Store.YES);
    }

    protected Field createHighlightedField(String field, String value) {
        return new Field(field, value,
                TYPE_STORED_WITH_TERM_VECTORS);
    }

    protected final void yieldInterrupt() throws InterruptedException {
        Thread.yield();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }
}
