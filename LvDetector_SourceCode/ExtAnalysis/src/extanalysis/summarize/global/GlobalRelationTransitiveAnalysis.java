package extanalysis.summarize.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;

import extanalysis.VarPair;
import extanalysis.summarize.SummarizeUtil;

public class GlobalRelationTransitiveAnalysis {
	private DefaultDirectedGraph<String, DefaultEdge> G = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

	private HashMap<String, VarPair> _proprocess_result = new HashMap<String, VarPair>();

	private HashMap<String, VarPair> _final_result = new HashMap<String, VarPair>();

	private List<DirectedSubgraph<String, DefaultEdge>> SCC;

	private HashMap<DirectedSubgraph<String, DefaultEdge>, List<Object>> _revised_SCC = new HashMap<DirectedSubgraph<String, DefaultEdge>, List<Object>>();

	private boolean DEBUG = false;

	public void addVertex(String v) {
		if (!G.containsVertex(v)) {
			G.addVertex(v);
		}
	}

	public boolean containsVertex(String v) {
		return G.containsVertex(v);
	}

	public void addEdge(String f, String t, VarPair r) {
		makeEdge(G, f, t, r);
	}

	private void makeEdge(DefaultDirectedGraph<String, DefaultEdge> g, String A, String B, VarPair r) {
		DefaultEdge e = null;
		if (g.containsEdge(A, B)) {
			e = g.getEdge(A, B);
		} else {
			e = g.addEdge(A, B);
		}
		_proprocess_result.put(e.toString(), SummarizeUtil.getInstance().union(_proprocess_result.get(e.toString()), r));
	}

	private void putTo(HashMap<String, VarPair> result, String f1, String f2, VarPair value) {
		if (value != null) {
			result.put(SummarizeUtil.getInstance().composeEdge(f1, f2), value);
		}
	}

	private VarPair getFrom(HashMap<String, VarPair> result, String f1, String f2) {
		return result.get(SummarizeUtil.getInstance().composeEdge(f1, f2));
	}

	public void preprocess() {
		//preprocess_cycle(G, _proprocess_result);
		clone(_final_result, _proprocess_result);
	}

	public HashMap<String, VarPair> getFinalResult() {
		HashMap<String, VarPair> ret = new HashMap<String, VarPair>();

		for (String key : _final_result.keySet()) {
			VarPair tmp = _final_result.get(key);

			String keyTrim = key.substring(1, key.length() - 1).replace(" ", "");
			String[] keyContents = keyTrim.split(":");
			if (keyContents[0].contains("_twin")) {
				keyContents[0] = keyContents[0].substring(0, keyContents[0].indexOf("_twin"));
			}
			if (keyContents[1].contains("_twin")) {
				keyContents[1] = keyContents[1].substring(0, keyContents[1].indexOf("_twin"));
			}

			String edge = SummarizeUtil.getInstance().composeEdge(keyContents[0], keyContents[1]);
			ret.put(edge, SummarizeUtil.getInstance().union(ret.get(edge), tmp));
		}
		return ret;
	}

	public void computeRelation(String v) {
		compute_path_summaries(G, v, _final_result);
	}

	public void computeRelation(String s, String d) {
		compute_path_summary_on_dag(G, s, d, _final_result);
	}

	// Transforming G into DAG
	@SuppressWarnings({ "unused", "unchecked" })
	private void preprocess_cycle(DefaultDirectedGraph<String, DefaultEdge> g, HashMap<String, VarPair> result) {
		@SuppressWarnings({ "rawtypes" })
		StrongConnectivityInspector sci = new StrongConnectivityInspector(g);
		SCC = sci.stronglyConnectedSubgraphs();

		// Find all SCCs
		Iterator<DirectedSubgraph<String, DefaultEdge>> it = SCC.iterator();
		while (it.hasNext()) {
			DirectedSubgraph<String, DefaultEdge> scc = it.next();

			if (scc.vertexSet().size() < 2) {
				continue;
			}

			if (isSCCWithinOneFunction(scc)) {
				it.remove();
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

					// Remove current edge from g
					g.removeEdge((DefaultEdge) edge);
					// Make new edge in g
					DefaultEdge newEdge = g.addEdge(source, newVertex);

					// scc may contain that edge in g
					if (scc.containsEdge((DefaultEdge) edge)) {
						// Remove current edge from scc
						scc.removeEdge((DefaultEdge) edge);
						// Make new edge in scc
						scc.addEdge(source, newVertex);
					}

					// Do the similar thing on _proprocess_result
					VarPair old = _proprocess_result.get(edge);
					_proprocess_result.remove(edge);
					_proprocess_result.put(newEdge.toString(), old);
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
						VarPair p_vi_twin = getFrom(result, p.toString(), vi_twin);
						VarPair p_vk_twin = getFrom(result, p.toString(), vk_twin);
						VarPair vk_vi_twin = getFrom(result, vk, vi_twin);

						VarPair tmp = SummarizeUtil.getInstance().concatenate(p_vk_twin, vk_vi_twin);

						putTo(result, p.toString(), vi_twin, SummarizeUtil.getInstance().union(p_vi_twin, tmp));
					}
				}

				VarPair vi_vi_twin = getFrom(result, vi.toString(), vi_twin);
				putTo(result, vi, vi_twin, SummarizeUtil.getInstance().closure(vi_vi_twin));
			}

			if (DEBUG)
				print("1.3 scc: " + scc);
			if (DEBUG)
				print("1.3 T: " + result);
		}
	}

	private boolean isSCCWithinOneFunction(DirectedSubgraph<String, DefaultEdge> scc) {
		String function = null;
		for (Object vi : scc.vertexSet().toArray()) {
			String node = (String) vi;
			// Abstract the function info
			String[] tmp = node.split("@");
			if (null == function) {
				function = tmp[1];
			} else if (tmp[1].equalsIgnoreCase(function)) {
				continue;
			} else if (!tmp[1].equalsIgnoreCase(function)) {
				return false;
			}
		}
		return true;
	}

	private void print(String content) {
		System.out.println(content);
	}

	public void printT() {
		System.out.println(_proprocess_result);
		System.out.println("");
	}

	private void compute_path_summaries(DefaultDirectedGraph<String, DefaultEdge> g, String v, HashMap<String, VarPair> result) {

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

			for (int k = 0; k < sccRv.size(); k++) {
				String vk = sccRv.get(k).toString();
				String vk_twin = vk + "_twin";
				for (Object p : scc.vertexSet().toArray()) {
					VarPair p_vk_twin = getFrom(result, p.toString(), vk_twin);
					VarPair vk_v = getFrom(result, vk, v);
					VarPair p_v = getFrom(result, p.toString(), v);

					VarPair tmp = SummarizeUtil.getInstance().concatenate(p_vk_twin, vk_v);
					putTo(result, p.toString(), v, SummarizeUtil.getInstance().union(p_v, tmp));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void compute_path_summary_on_dag(DefaultDirectedGraph<String, DefaultEdge> g, String p, String v, HashMap<String, VarPair> result) {

		if (DEBUG)
			print("2 compute_path_summary_on_dag(" + p + "->" + v + ")");

		// There is no self-cycles for srcV
		// and self loop

		if (p.equals(v)) {
			return;
		}

		@SuppressWarnings("rawtypes")
		ConnectivityInspector cin = new ConnectivityInspector(g);
		if (!cin.pathExists(p, v)) {
			return;
		}

		// If srcV is before dstV in g
		//if (SummarizeUtil.getInstance().isPBeforeVInTopoOrder(g, p, v)) { // Ray May
		Object[] allEdgesFromsrcV = g.outgoingEdgesOf(p).toArray();

		for (Object outgoing : allEdgesFromsrcV) {
			// For each outgoing edge from srcV
			String q = g.getEdgeTarget((DefaultEdge) outgoing);

			//String edge_q_v = SummarizeUtil.getInstance().composeEdge(q, v);
			VarPair tmp_q_v = getFrom(result, q, v);
			if (tmp_q_v == null || tmp_q_v._enc_relation.size() == 0) {
				compute_path_summary_on_dag(g, q, v, result);
			}

			//
			VarPair p_v = getFrom(result, p, v);
			VarPair p_q = getFrom(result, p, q);
			VarPair q_v = getFrom(result, q, v);
			VarPair tmp = SummarizeUtil.getInstance().concatenate(p_q, q_v);
			putTo(result, p, v, SummarizeUtil.getInstance().union(p_v, tmp));
		}
		//	}
		//
		VarPair p_p = getFrom(result, p, p);
		VarPair p_v = getFrom(result, p, v);
		putTo(result, p, v, SummarizeUtil.getInstance().concatenate(p_p, p_v));
	}

	private void clone(HashMap<String, VarPair> dst, HashMap<String, VarPair> src) {
		for (String key : src.keySet()) {
			VarPair srcVarPair = src.get(key);
			dst.put(key, srcVarPair);
		}
	}

	@SuppressWarnings("unused")
	private List<String> clone(List<String> src) {
		List<String> dst = new ArrayList<String>();
		for (String str : src) {
			dst.add(str);
		}
		return dst;
	}
}
