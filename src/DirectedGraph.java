import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 
 * the directed graph needs to implement iterable, so that we can iterate over the elements
 *
 */
class DirectedGraph implements Iterable<ClauseValue> {
	private Map<ClauseValue, Set<ClauseValue>> graph = new HashMap<ClauseValue, Set<ClauseValue>>();
	//the elements in the directed graph are saved as a mapping from clausevalue to an array of clausevalues.
	
	/**
	 * 
	 * @return returns the mapping used in the directed graph
	 */
	public Map<ClauseValue, Set<ClauseValue>> getGraph(){
		return graph;
	}
	
	public void addNode(ClauseValue node){
		graph.put(node, new HashSet<ClauseValue>());// find out why this does not work
	}
	
	public void addEdge(ClauseValue start, ClauseValue end) {
        graph.get(start).add(end);
    }
	
	public Set<ClauseValue> edgesFrom(ClauseValue node) {
		return graph.get(node);
    }
	
	public Iterator<ClauseValue> iterator() {
		//for iteration to work, the ClauseValue needs a hashcode and an equals function.
		return graph.keySet().iterator();
	}
}
