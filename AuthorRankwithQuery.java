import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.FactoryUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import edu.uci.ics.jung.algorithms.scoring.PageRankWithPriors;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.PajekNetReader;

public class AuthorRankwithQuery {

	Map<Integer, Float> map = new HashMap<Integer, Float>();
	Map<String, String> authorMap = new HashMap<String, String>();
	PajekNetReader graphReader;
	
	public static void main(String[] args) throws ParseException, IOException {
		AuthorRankwithQuery search = new AuthorRankwithQuery();
		search.searchTopics("Data Mining");
		System.out.println("");
		System.out.println("");
		search.searchTopics("Information Retrieval");
	}
	
	public void searchTopics(String queryString) throws ParseException, IOException{	
		
		System.out.println("Searching for Query: " + queryString);
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File("./data/author_index")));
		IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new BM25Similarity());
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("content",analyzer);
		Query query = parser.parse(queryString); 
		TopScoreDocCollector collector = TopScoreDocCollector.create(300, true);
		searcher.search(query, collector);
		ScoreDoc[] docs = collector.topDocs().scoreDocs;
		
		float totalProb = 0;
		for(int i = 0; i < docs.length; i++){
			Document doc = searcher.doc(docs[i].doc);
			//System.out.println(doc.get("authorid") + " " + docs[i].score);
			authorMap.put(doc.get("authorid"), doc.get("authorName"));
			if(map.get(Integer.parseInt(doc.get("authorid"))) == null){
				map.put(Integer.parseInt(doc.get("authorid")), docs[i].score);
			}else{
				float value = map.get(Integer.parseInt(doc.get("authorid"))) + docs[i].score;
				map.put(Integer.parseInt(doc.get("authorid")), value);
			}
			totalProb += docs[i].score; 
		}
		Iterator it = map.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        float value = (float) pair.getValue()/totalProb;
	        map.put((Integer) pair.getKey(), value);
	    }
	    graphReader = new PajekNetReader(FactoryUtils.instantiateFactory(Object.class));
		Graph<Integer, Integer> g = new UndirectedSparseGraph<>();
	    g = graphReader.load("./data/author.net", g);
	    PageRankWithPriors<Integer, Integer> ranker = new PageRankWithPriors<Integer, Integer>(g, vertex_prior, 0.85);
	    ranker.getVertexPriors();
	    ranker.evaluate();
	    Map<Integer, Double> resultMap = new HashMap<Integer, Double>();
	    for(Integer v : g.getVertices()){
	    	resultMap.put(v, ranker.getVertexScore(v));
	    }
	    Map<Integer, Double> sortedMap = sortByValue(resultMap);
	    printMap(sortedMap, 10, graphReader, authorMap);
		reader.close();
	}

	Transformer<Integer, Double> vertex_prior = new Transformer<Integer, Double>(){
		@Override
         public Double transform(Integer v){
			String authorid = (String) graphReader.getVertexLabeller().transform(v);
			if(map.containsKey(Integer.parseInt(authorid))){
				return (double) map.get(Integer.parseInt(authorid));
			}else return (double) 0;
         }           
	};

	private static Map<Integer, Double> sortByValue(Map<Integer, Double> unsortMap) {
	    List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(unsortMap.entrySet());
	    Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
	        public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
	            return (o2.getValue()).compareTo(o1.getValue());
	        }
	    });
	
	    Map<Integer, Double> sortedMap = new LinkedHashMap<Integer, Double>();
	    for (Map.Entry<Integer, Double> entry : list) {
	        sortedMap.put(entry.getKey(), entry.getValue());
	    }
	    return sortedMap;
	}
	
	public static <K, V> void printMap(Map<K, V> map, int n, PajekNetReader reader, Map authorMap) {
		int i = 0;
		System.out.println("Rank   Author                  Score");
	    for (Map.Entry<K, V> entry : map.entrySet()) {
	    	System.out.println(i+1 + "      " + authorMap.get(reader.getVertexLabeller().transform(entry.getKey())) + "           " + entry.getValue());
	        i++;
	        if(i == n){
	        	return;
	        }
	    }
	    System.out.println();
	    System.out.println();
	}
}
