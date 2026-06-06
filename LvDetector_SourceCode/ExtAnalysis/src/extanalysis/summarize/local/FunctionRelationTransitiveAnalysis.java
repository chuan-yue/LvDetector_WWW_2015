package extanalysis.summarize.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;

import extanalysis.summarize.SummarizeUtil;

public class FunctionRelationTransitiveAnalysis {

	private DefaultDirectedGraph<String, DefaultEdge> G = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

	private HashMap<String, List<String>> _proprocess_result = new HashMap<String, List<String>>();

	private HashMap<String, List<String>> _final_result = new HashMap<String, List<String>>();

	private List<DirectedSubgraph<String, DefaultEdge>> SCC;

	private HashMap<DirectedSubgraph<String, DefaultEdge>, List<Object>> _revised_SCC = new HashMap<DirectedSubgraph<String, DefaultEdge>, List<Object>>();

	private boolean DEBUG = false;

	public void addVertex(String v) {
		G.addVertex(v);
	}

	public boolean containsVertex(String v) {
		return G.containsVertex(v);
	}

	public void addEdge(String f, String t, String r) {
		makeEdge(G, f, t, r);
	}

	public void addEdge(String f, String t, List<String> r) {
		makeEdge(G, f, t, r);
	}

	private void makeEdge(DefaultDirectedGraph<String, DefaultEdge> g, String A, String B, String r) {
		List<String> R = new ArrayList<String>();
		R.add(r);
		makeEdge(g, A, B, R);
	}

	private void makeEdge(DefaultDirectedGraph<String, DefaultEdge> g, String A, String B, List<String> r) {
		DefaultEdge e = null;
		if (g.containsEdge(A, B)) {
			e = g.getEdge(A, B);
		} else {
			e = g.addEdge(A, B);
		}
		_proprocess_result.put(e.toString(), SummarizeUtil.getInstance().baseUnion(_proprocess_result.get(e.toString()), r));
	}

	private void putTo(HashMap<String, List<String>> result, String v1, String v2, List<String> value) {
		if (value != null && value.size() > 0) {
			result.put(SummarizeUtil.getInstance().composeEdge(v1, v2), value);
		}
	}

	private List<String> getFrom(HashMap<String, List<String>> result, String v1, String v2) {
		return result.get(SummarizeUtil.getInstance().composeEdge(v1, v2));
	}

	public void resetFinalResults() {
		_final_result.clear();
		clone(_final_result, _proprocess_result);
	}

	public void preprocess() {
		preprocess_cycle(G, _proprocess_result);
		clone(_final_result, _proprocess_result);
	}

	public HashMap<String, List<String>> getFinalResult() {
		HashMap<String, List<String>> ret = new HashMap<String, List<String>>();

		for (String key : _final_result.keySet()) {
			List<String> update = _final_result.get(key);

			// Now remove wrong field operations
			List<String> finalize = new ArrayList<String>();
			for (String str : update) {
				if (SummarizeUtil.getInstance().hasWrongFieldRef(str) == false) {
					finalize.add(str);
				}
			}

			String keyTrim = key.substring(1, key.length() - 1).replace(" ", "");
			String[] keyContents = keyTrim.split(":");
			if (keyContents[0].contains("_twin")) {
				keyContents[0] = keyContents[0].substring(0, keyContents[0].indexOf("_twin"));
			}
			if (keyContents[1].contains("_twin")) {
				keyContents[1] = keyContents[1].substring(0, keyContents[1].indexOf("_twin"));
			}

			if (finalize.size() > 0) {
				String edge = SummarizeUtil.getInstance().composeEdge(keyContents[0], keyContents[1]);
				ret.put(edge, SummarizeUtil.getInstance().baseUnion(ret.get(edge), finalize));
			}
		}
		return ret;
	}

	public void computeRelation(String v) {
		compute_path_summaries(G, v, _final_result);
	}

	// Transforming G into DAG
	@SuppressWarnings("unchecked")
	private void preprocess_cycle(DefaultDirectedGraph<String, DefaultEdge> g, HashMap<String, List<String>> result) {
		@SuppressWarnings("rawtypes")
		StrongConnectivityInspector sci = new StrongConnectivityInspector(g);
		SCC = sci.stronglyConnectedSubgraphs();

		// Find all SCCs
		Iterator<DirectedSubgraph<String, DefaultEdge>> it = SCC.iterator();
		while (it.hasNext()) {
			DirectedSubgraph<String, DefaultEdge> scc = it.next();

			if (scc.vertexSet().size() < 2) {
				continue;
			}

			List<Object> revised = new ArrayList<Object>();

			for (Object vi : scc.vertexSet().toArray()) {
				// New vertex to receive edge
				String newVertex = vi + "_twin";
				// if there are incoming edges
				Object[] incomings = scc.incomingEdgesOf(vi.toString()).toArray(); //g
				if (incomings.length == 0) {
					continue;
				} else {
					// Record that this vertex was changed
					if (!revised.contains(vi)) {
						revised.add(vi);
					}
					// Add new vertex
					if (!g.vertexSet().contains(newVertex)) {
						g.addVertex(newVertex);
					}
					if (!scc.vertexSet().contains(newVertex)) {
						scc.addVertex(newVertex);
					}
				}

				// Search backedge, and change
				for (Object edge : incomings) {
					String source = g.getEdgeSource((DefaultEdge) edge);

					List<String> r = getFrom(result, source, vi.toString()); // relation

					// Remove current edge from g
					g.removeEdge((DefaultEdge) edge);
					// Make new edge in g
					@SuppressWarnings("unused")
					DefaultEdge newEdge = g.addEdge(source, newVertex);

					// scc may contain that edge in g
					if (scc.containsEdge((DefaultEdge) edge)) {
						// Remove current edge from scc
						scc.removeEdge((DefaultEdge) edge);
						// Make new edge in scc
						scc.addEdge(source, newVertex);
					}

					// Remove relation
					result.remove(edge.toString());
					// Make new relation
					putTo(result, source, newVertex, r);
				}
			}

			if (DEBUG)
				print("1.0 scc: " + scc);
			if (DEBUG)
				print("1.0 T: " + result);
			if (DEBUG)
				print("");

			if (revised.size() == 0) {
				continue;
			} else {
				_revised_SCC.put(scc, revised);
			}

			// In this scc compute cycle info
			for (int i = 0; i < revised.size(); i++) {
				String vi = revised.get(i).toString();
				String vi_twin = vi + "_twin";

				if (DEBUG)
					print("1.1 i: " + i + "; vi: " + vi + "; vi_twin: " + vi_twin);

				for (Object p : scc.vertexSet().toArray()) {
					// p vi1
					compute_path_summary_on_dag(g, p.toString(), vi_twin, result);
				}

				if (DEBUG)
					print("1.1 T: " + result);

				for (int k = 0; k < i + 1; k++) { // i+1 or i
					String vk = revised.get(k).toString();
					String vk_twin = vk + "_twin";

					if (DEBUG)
						print("1.2 k: " + k + "; vk: " + vk + "; vk_twin: " + vk_twin);

					for (Object p : scc.vertexSet().toArray()) {
						// p vi1
						List<String> p_vi_twin = getFrom(result, p.toString(), vi_twin);
						List<String> p_vk_twin = getFrom(result, p.toString(), vk_twin);
						List<String> vk_vi_twin = getFrom(result, vk, vi_twin);

						List<String> tmp = SummarizeUtil.getInstance().baseConcatenate(p_vk_twin, vk_vi_twin);

						putTo(result, p.toString(), vi_twin, SummarizeUtil.getInstance().baseUnion(p_vi_twin, tmp));
					}
				}

				List<String> vi_vi_twin = getFrom(result, vi.toString(), vi_twin);
				putTo(result, vi, vi_twin, SummarizeUtil.getInstance().baseClosure(vi_vi_twin));
			}

			if (DEBUG)
				print("1.3 scc: " + scc);
			if (DEBUG)
				print("1.3 T: " + result);
		}
	}

	private void print(String content) {
		System.out.println(content);
	}

	public void printT() {
		System.out.println(_proprocess_result);
		System.out.println("");
	}

	private void compute_path_summaries(DefaultDirectedGraph<String, DefaultEdge> g, String v, HashMap<String, List<String>> result) {

		// Now it is a DAG so we can sort
		if (SCC != null && SCC.size() > 0) {
			SummarizeUtil.getInstance().sortSccInReverseOrder(g, SCC);
		}

		for (DirectedSubgraph<String, DefaultEdge> scc : SCC) {

			// For each SCC
			for (Object p : scc.vertexSet().toArray()) {
				compute_path_summary_on_dag(g, p.toString(), v, result);
			}

			List<Object> sccRv = _revised_SCC.get(scc);
			if (sccRv == null || sccRv.size() == 0) {
				continue;
			}

			String vk = null;
			String vk_twin = null;
			for (int k = 0; k < sccRv.size(); k++) {
				vk = sccRv.get(k).toString();
				vk_twin = vk + "_twin";
				for (Object p : scc.vertexSet().toArray()) {
					List<String> p_vk_twin = getFrom(result, p.toString(), vk_twin);
					List<String> vk_v = getFrom(result, vk, v);
					List<String> p_v = getFrom(result, p.toString(), v);
				
					List<String> tmp = SummarizeUtil.getInstance().baseConcatenate(p_vk_twin, vk_v);
					putTo(result, p.toString(), v, SummarizeUtil.getInstance().baseUnion(p_v, tmp));
				}
			}
		}
	}

	private void compute_path_summary_on_dag(DefaultDirectedGraph<String, DefaultEdge> g, String p, String v, HashMap<String, List<String>> result) {

		if (DEBUG)
			print("2 compute_path_summary_on_dag(" + p + "->" + v + ")");

		// There is no self-cycles for srcV
		// and self loop

		if (p.equals(v)) {
			return;
		}

		// If srcV is before dstV in g
		if (SummarizeUtil.getInstance().isPBeforeVInTopoOrder(g, p, v)) {

			Object[] allEdgesFromsrcV = g.outgoingEdgesOf(p).toArray();

			for (Object outgoing : allEdgesFromsrcV) {
				// For each outgoing edge from srcV
				String q = g.getEdgeTarget((DefaultEdge) outgoing);

				// String edge_q_v = SummarizeUtil.getInstance().composeEdge(q, v);
				// if (!T.containsKey(edge_q_v)) {
				compute_path_summary_on_dag(g, q, v, result);
				// }

				//
				List<String> p_v = getFrom(result, p, v);
				List<String> p_q = getFrom(result, p, q);
				List<String> q_v = getFrom(result, q, v);
				List<String> tmp = SummarizeUtil.getInstance().baseConcatenate(p_q, q_v);
				putTo(result, p, v, SummarizeUtil.getInstance().baseUnion(p_v, tmp)); // ?
			}
		}
		//
		List<String> p_p = getFrom(result, p, p);
		List<String> p_v = getFrom(result, p, v);
		putTo(result, p, v, SummarizeUtil.getInstance().baseConcatenate(p_p, p_v));
	}

	private void clone(HashMap<String, List<String>> dst, HashMap<String, List<String>> src) {
		for (String key : src.keySet()) {
			List<String> srdList = src.get(key);

			List<String> dstList = new ArrayList<String>();
			for (String str : srdList) {
				dstList.add(str);
			}

			dst.put(key, dstList);
		}
	}
}