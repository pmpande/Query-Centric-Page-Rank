import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.FactoryUtils;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.PajekNetReader;

public class AuthorRank {

	public static void main(String[] args) throws IOException {
		PajekNetReader reader = new PajekNetReader(FactoryUtils.instantiateFactory(Object.class));
		Graph<Integer, Integer> g = new UndirectedSparseGraph<>();
	    g = reader.load("./data/author.net", g);
	    PageRank<Integer, Integer> ranker = new PageRank<Integer, Integer>(g, 0.85);
	    ranker.evaluate();
	    Map<Integer, Double> resultMap = new HashMap<Integer, Double>();
	    for(Integer v : g.getVertices()){
	    	resultMap.put(v, ranker.getVertexScore(v));
	    }
	    Map<Integer, Double> sortedMap = sortByValue(resultMap);
	    printMap(sortedMap, 10, reader);
	}
	
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

    public static <K, V> void printMap(Map<K, V> map, int n, PajekNetReader reader) {
    	int i = 0;
    	System.out.println("Rank   Author    Score");
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.println(i+1 + "      " + reader.getVertexLabeller().transform(entry.getKey()) + "      " + entry.getValue());
            i++;
            if(i == n){
            	return;
            }
        }
    }
}
