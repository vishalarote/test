package com.jazzyapps.android.apps.contacts.search;

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

import com.jazzyapps.android.apps.contacts.search.analysis.NGramAnalyzer;
import com.jazzyapps.android.apps.contacts.search.analysis.PinyinAnalyzer;
import com.jazzyapps.android.apps.contacts.search.analysis.T9Analyzer;
import com.jazzyapps.android.apps.contacts.search.utils.StringUtils;
import com.jazzyapps.android.apps.contacts.search.utils.T9Converter;


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
                            if (pinyin.equals(name)) { //ÃƒÂ§Ã‚ÂºÃ‚Â¯ÃƒÂ¨Ã¢â‚¬Â¹Ã‚Â±ÃƒÂ¦Ã¢â‚¬â€œÃ¢â‚¬Â¡
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
     * @param pinyin ÃƒÂ¦Ã¢â‚¬Â¹Ã‚Â¼ÃƒÂ©Ã…Â¸Ã‚Â³ÃƒÂ¨Ã‚Â¡Ã‚Â¨ÃƒÂ§Ã‚Â¤Ã‚ÂºÃƒÂ¯Ã‚Â¼Ã…â€™ÃƒÂ¦Ã‚Â¯Ã¢â‚¬ï¿½ÃƒÂ¥Ã‚Â¦Ã¢â‚¬Å¡WangWeiWei,ÃƒÂ¦Ã‚Â³Ã‚Â¨ÃƒÂ¦Ã¢â‚¬Å¾Ã¯Â¿Â½ÃƒÂ¦Ã‚Â­Ã‚Â¤ÃƒÂ¥Ã‚Â¤Ã¢â‚¬Å¾ÃƒÂ©Ã‚Â¦Ã¢â‚¬â€œÃƒÂ¥Ã‚Â­Ã¢â‚¬â€�ÃƒÂ¦Ã‚Â¯Ã¯Â¿Â½ÃƒÂ¦Ã‹Å“Ã‚Â¯ÃƒÂ¥Ã‚Â¤Ã‚Â§ÃƒÂ¥Ã¢â‚¬Â Ã¢â€žÂ¢ÃƒÂ§Ã…Â¡Ã¢â‚¬Å¾
     * @param query  ÃƒÂ¦Ã…Â¸Ã‚Â¥ÃƒÂ¨Ã‚Â¯Ã‚Â¢ÃƒÂ¯Ã‚Â¼Ã…â€™ÃƒÂ¥Ã¯Â¿Â½Ã‚Â¯ÃƒÂ¨Ã†â€™Ã‚Â½ÃƒÂ¤Ã‚Â¸Ã‚Âºt9,ÃƒÂ¤Ã‚Â¹Ã…Â¸ÃƒÂ¥Ã¯Â¿Â½Ã‚Â¯ÃƒÂ¨Ã†â€™Ã‚Â½ÃƒÂ¦Ã‹Å“Ã‚Â¯alphaÃƒÂ¥Ã‚Â­Ã¢â‚¬â€�ÃƒÂ¦Ã‚Â¯Ã¯Â¿Â½
     * @return ÃƒÂ¨Ã‚Â¿Ã¢â‚¬ï¿½ÃƒÂ¥Ã¢â‚¬ÂºÃ…Â¾ÃƒÂ©Ã‚Â«Ã‹Å“ÃƒÂ¤Ã‚ÂºÃ‚Â®ÃƒÂ¤Ã‚Â¹Ã¢â‚¬Â¹ÃƒÂ¥Ã¯Â¿Â½Ã…Â½ÃƒÂ§Ã…Â¡Ã¢â‚¬Å¾ÃƒÂ§Ã‚Â»Ã¢â‚¬Å“ÃƒÂ¦Ã…Â¾Ã…â€œÃƒÂ¦Ã‹â€ Ã¢â‚¬â€œÃƒÂ¨Ã¢â€šÂ¬Ã¢â‚¬Â¦ÃƒÂ¥Ã…â€œÃ‚Â¨ÃƒÂ¦Ã‚Â²Ã‚Â¡ÃƒÂ¦Ã…â€œÃ¢â‚¬Â°ÃƒÂ©Ã‚Â«Ã‹Å“ÃƒÂ¤Ã‚ÂºÃ‚Â®ÃƒÂ§Ã…Â¡Ã¢â‚¬Å¾ÃƒÂ¦Ã†â€™Ã¢â‚¬Â¦ÃƒÂ¥Ã¢â‚¬Â Ã‚ÂµÃƒÂ¤Ã‚Â¸Ã¢â‚¬Â¹ÃƒÂ¨Ã‚Â¿Ã¢â‚¬ï¿½ÃƒÂ¥Ã¢â‚¬ÂºÃ…Â¾ÃƒÂ¥Ã…Â½Ã‚Â»ÃƒÂ¦Ã…Â½Ã¢â‚¬Â°ÃƒÂ¦Ã¢â‚¬Â¹Ã‚Â¼ÃƒÂ©Ã…Â¸Ã‚Â³ÃƒÂ©Ã‚Â¦Ã¢â‚¬â€œÃƒÂ¥Ã‚Â­Ã¢â‚¬â€�ÃƒÂ¦Ã‚Â¯Ã¯Â¿Â½ÃƒÂ§Ã…Â¡Ã¢â‚¬Å¾ÃƒÂ©Ã†â€™Ã‚Â¨ÃƒÂ¥Ã‹â€ Ã¢â‚¬Â 
     * @throws java.io.IOException
     */
    protected String highlightPinyin(String pinyin, String query) throws IOException {
        if (StringUtils.isEmpty(query)) {
            return null;
        }
        int index = pinyin.lastIndexOf('|');
        String full = index > -1 ? pinyin.substring(0, index) : pinyin;
        //ÃƒÂ©Ã†â€™Ã‚Â½ÃƒÂ¨Ã‚Â½Ã‚Â¬ÃƒÂ¦Ã¯Â¿Â½Ã‚Â¢ÃƒÂ¤Ã‚Â¸Ã‚Âºt9ÃƒÂ¥Ã¯Â¿Â½Ã…Â½ÃƒÂ¥Ã¢â‚¬Â Ã¯Â¿Â½ÃƒÂ¥Ã…â€™Ã‚Â¹ÃƒÂ©Ã¢â‚¬Â¦Ã¯Â¿Â½
        String t9 = pinyin.toLowerCase();
        String t9Query = query.toLowerCase();
        if (!Character.isLetter(query.charAt(0)))//t9 match
        {
            t9 = T9Converter.convert(t9);
            t9Query = T9Converter.convert(query);
        }
        int start = t9.lastIndexOf(t9Query);
        if (index > -1 && start > index)//ÃƒÂ©Ã‚Â¦Ã¢â‚¬â€œÃƒÂ¥Ã‚Â­Ã¢â‚¬â€�ÃƒÂ¦Ã‚Â¯Ã¯Â¿Â½ÃƒÂ¥Ã…â€™Ã‚Â¹ÃƒÂ©Ã¢â‚¬Â¦Ã¯Â¿Â½
        {
            String match = pinyin.substring(start, start + t9Query.length());
            StringBuilder stringBuilder = new StringBuilder(pinyin.length() + match.length() * (1 + preTag.length() + postTag.length()));
            for (int i = 0, j = 0; i < full.length(); i++) {//ÃƒÂ¥Ã‚Â¾Ã‚ÂªÃƒÂ§Ã…Â½Ã‚Â¯ÃƒÂ©Ã‚Â«Ã‹Å“ÃƒÂ¤Ã‚ÂºÃ‚Â®ÃƒÂ©Ã‚Â¦Ã¢â‚¬â€œÃƒÂ¥Ã‚Â­Ã¢â‚¬â€�ÃƒÂ¦Ã‚Â¯Ã¯Â¿Â½
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
        } else if (start > -1) {   //ÃƒÂ©Ã¯Â¿Â½Ã…Â¾ÃƒÂ©Ã‚Â¦Ã¢â‚¬â€œÃƒÂ¥Ã‚Â­Ã¢â‚¬â€�ÃƒÂ¦Ã‚Â¯Ã¯Â¿Â½ÃƒÂ¥Ã…â€™Ã‚Â¹ÃƒÂ©Ã¢â‚¬Â¦Ã¯Â¿Â½
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
     * ÃƒÂ¤Ã‚Â¸Ã¯Â¿Â½ÃƒÂ¨Ã†â€™Ã‚Â½ÃƒÂ¤Ã‚Â½Ã‚Â¿ÃƒÂ§Ã¢â‚¬ï¿½Ã‚Â¨luceneÃƒÂ§Ã…Â¡Ã¢â‚¬Å¾highlight,ÃƒÂ¥Ã¢â‚¬ÂºÃ‚Â ÃƒÂ¤Ã‚Â¸Ã‚ÂºÃƒÂ¥Ã¯Â¿Â½Ã‚Â·ÃƒÂ§Ã‚Â Ã¯Â¿Â½ÃƒÂ¤Ã‚Â½Ã‚Â¿ÃƒÂ§Ã¢â‚¬ï¿½Ã‚Â¨ÃƒÂ§Ã…Â¡Ã¢â‚¬Å¾ÃƒÂ¦Ã‹Å“Ã‚Â¯NGramÃƒÂ¥Ã‹â€ Ã¢â‚¬Â ÃƒÂ¨Ã‚Â¯Ã¯Â¿Â½ÃƒÂ¯Ã‚Â¼Ã…â€™ÃƒÂ¦Ã¢â‚¬Â°Ã¢â€šÂ¬ÃƒÂ¤Ã‚Â»Ã‚Â¥tokenÃƒÂ©Ã‚Â¡Ã‚Â¹ÃƒÂ¥Ã‚Â¾Ã‹â€ ÃƒÂ¥Ã‚Â¤Ã…Â¡ÃƒÂ¯Ã‚Â¼Ã…â€™ÃƒÂ¨Ã‚Â¿Ã¢â€žÂ¢ÃƒÂ¥Ã‚Â°Ã‚Â±ÃƒÂ¥Ã‚Â¯Ã‚Â¼ÃƒÂ¨Ã¢â‚¬Â¡Ã‚Â´ÃƒÂ©Ã‚Â«Ã‹Å“ÃƒÂ¤Ã‚ÂºÃ‚Â®ÃƒÂ¦Ã…Â¾Ã…Â¡ÃƒÂ¤Ã‚Â¸Ã‚Â¾tokenÃƒÂ§Ã¢â‚¬Â°Ã‚Â¹ÃƒÂ¥Ã‹â€ Ã‚Â«ÃƒÂ¨Ã¢â€šÂ¬Ã¢â‚¬â€�ÃƒÂ¦Ã¢â‚¬â€�Ã‚Â¶
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
