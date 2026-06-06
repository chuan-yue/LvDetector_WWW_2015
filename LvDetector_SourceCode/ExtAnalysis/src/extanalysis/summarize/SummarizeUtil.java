package extanalysis.summarize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import extanalysis.AnalysisUtil;
import extanalysis.RelationEdge;
import extanalysis.RelationEdgesSet;
import extanalysis.VarPair;

public class SummarizeUtil {
	private static SummarizeUtil _instance = null;

	public SummarizeUtil() {
	}

	public static SummarizeUtil getInstance() {
		if (_instance == null) {
			_instance = new SummarizeUtil();
		}
		return _instance;
	}

	public String composeEdge(String f, String t) {
		return "(" + f + " : " + t + ")";
	}

	public synchronized VarPair concatenate(VarPair l1, VarPair l2) {
		if (l1 == null || l2 == null) {
			return null;
		}
		return new VarPair(l1._from, l2._to, baseConcatenate(l1._enc_relation, l2._enc_relation));
	}

	public synchronized List<VarPair> concatenate(List<VarPair> l1, List<VarPair> l2) {
		if (l1 == null || l2 == null) {
			return null;
		}
		List<VarPair> ret = new ArrayList<VarPair>();

		for (VarPair s1 : l1) {
			for (VarPair s2 : l2) {
				if (s1.isFollowedBy(s2)) {
					ret.add(new VarPair(s1._from, s2._to, baseConcatenate(s1._enc_relation, s2._enc_relation)));
				}
			}
		}
		for (VarPair s2 : l2) {
			for (VarPair s1 : l1) {
				if (s2.isFollowedBy(s1)) {
					ret.add(new VarPair(s2._from, s1._to, baseConcatenate(s2._enc_relation, s1._enc_relation)));
				}
			}
		}

		return ret;
	}

	public synchronized VarPair union(VarPair l1, VarPair l2) {
		if (l1 == null && l2 == null) {
			return null;
		} else if (l1 != null && l2 == null) {
			return l1;
		} else if (l1 == null && l2 != null) {
			return l2;
		} else {
			return new VarPair(l1._from, l1._to, baseUnion(l1._enc_relation, l2._enc_relation));
		}
	}

	public synchronized List<VarPair> union(List<VarPair> l1, List<VarPair> l2) {
		if (l1 == null && l2 == null) {
			return null;
		} else if (l1 != null && l2 == null) {
			return l1;
		} else if (l1 == null && l2 != null) {
			return l2;
		} else {
			List<VarPair> ret = new ArrayList<VarPair>();

			for (VarPair s1 : l1) {
				// add s1 to ret
				boolean isProced = false;
				for (VarPair r : ret) {
					if (r.equals(s1)) {
						// update r in ret
						ret.remove(s1);
						r._enc_relation = baseUnion(r._enc_relation, s1._enc_relation);
						isProced = true;
					}
				}
				if (isProced == false) {
					ret.add(s1);
				}
			}
			for (VarPair s2 : l2) {
				// add s1 to ret
				boolean isProced = false;
				for (VarPair r : ret) {
					if (r.equals(s2)) {
						// update r in ret
						ret.remove(s2);
						r._enc_relation = baseUnion(r._enc_relation, s2._enc_relation);
						isProced = true;
					}
				}
				if (isProced == false) {
					ret.add(s2);
				}
			}

			return ret;
		}
	}

	public synchronized VarPair closure(VarPair l) {
		if (l == null) {
			return null;
		}
		return l;
	}

	public synchronized List<VarPair> closure(List<VarPair> l) {
		if (l == null) {
			return null;
		}
		List<VarPair> ret = new ArrayList<VarPair>();
		for (VarPair s : l) {
			ret.add(s);
			//ret.add(concatenate(s, s));
		}
		return ret;
	}

	public synchronized List<String> baseConcatenate(List<String> l1, List<String> l2) {
		if (l1 == null || l2 == null) {
			return null;
		}
		List<String> ret = new ArrayList<String>();
		for (String s1 : l1) {
			for (String s2 : l2) {
				String s = s1 + s2;
				if (!containsRepeat(ret, s)) {
					ret.add(s);
				}
			}
		}
		return ret;
	}

	public synchronized List<String> baseUnion(List<String> l1, List<String> l2) {
		if (l1 == null && l2 == null) {
			return null;
		}
		List<String> ret = new ArrayList<String>();
		if (l1 != null) {
			for (String s1 : l1) {
				if (!containsRepeat(ret, s1)) {
					ret.add(s1);
				}
			}
		}
		if (l2 != null) {
			for (String s2 : l2) {
				if (!containsRepeat(ret, s2)) {
					ret.add(s2);
				}
			}
		}
		return ret;
	}

	public synchronized List<String> baseClosure(List<String> l) {
		if (l == null) {
			return null;
		}
		List<String> ret = new ArrayList<String>();
		for (String s : l) {
			if (!containsRepeat(ret, s)) {
				ret.add(s);
			}
			//ret.add(s + s);
		}
		return ret;
	}

	public synchronized boolean containsRepeat(List<String> set, String s) {
		for (String c : set) {
			if (s.equals(c)) {
				return true;
			}
		}
		if (isSelfRepeated(s)) {
			return true;
		}
		return false;
	}

	private boolean isSelfRepeated(String s) {
		s = s.substring(1, s.length() - 1);
		if (!s.contains(")(")) {
			return false;
		}
		String[] elements = s.split("\\)\\(");

		for (int i = 0; i < elements.length; i++) {
			int lastIndex = i;
			String lastStr = null;
			int k;
			while ((k = nextIndexFrom(elements, lastIndex)) != -1) {
				String innerStr = "";
				for (int m = lastIndex + 1; m < k; m++) {
					innerStr += elements[m];
				}
				if (lastStr != null && innerStr.equalsIgnoreCase(lastStr)) {
					return true;
				}
				lastStr = innerStr;
				lastIndex = k;
			}
		}
		return false;
	}

	private int nextIndexFrom(String[] elements, int startPos) {
		String startValue = elements[startPos];
		for (int i = startPos + 1; i < elements.length; i++) {
			if (elements[i].equalsIgnoreCase(startValue)) {
				return i;
			}
		}
		return -1;
	}

	public boolean isPBeforeVInTopoOrder(DefaultDirectedGraph<String, DefaultEdge> g, String p, String v) {
		TopologicalOrderIterator<String, DefaultEdge> orderIterator = new TopologicalOrderIterator<String, DefaultEdge>(g);
		boolean isPThere = false;
		List<String> visited = new ArrayList<String>();
		while (orderIterator.hasNext()) {
			try {
				String tmp = orderIterator.next();

				// new added for stack overflow error : 1230
				if (AnalysisUtil.getInstance().IS_SAFE_COMPUTE_TOPO_ORDER) {
					if (visited.contains(tmp)) {
						return false;
					} else {
						visited.add(tmp);
					}
				}

				if (tmp.equals(p)) {
					isPThere = true;
				}
				if (tmp.equals(v)) {
					if (isPThere) {
						return true;
					}
				}
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	public void sortSccInReverseOrder(DefaultDirectedGraph<String, DefaultEdge> g, List<DirectedSubgraph<String, DefaultEdge>> SCC) {
		// we operation on the cloned gcpy
		@SuppressWarnings("unchecked")
		DefaultDirectedGraph<String, DefaultEdge> gcpy = (DefaultDirectedGraph<String, DefaultEdge>) g.clone();

		// replace all scc with a single node in gcpy
		for (int i = 0; i < SCC.size(); i++) {
			DirectedSubgraph<String, DefaultEdge> scc = SCC.get(i);

			String newSccVertex = "scc" + i;

			// add new vertex
			gcpy.addVertex(newSccVertex);

			// remove all scc's edges
			for (Object vertex : scc.vertexSet()) {
				// for all edges related to this vertex
				Object[] outgoings = gcpy.outgoingEdgesOf(vertex.toString()).toArray();
				for (Object edge : outgoings) {
					// check target vertex
					String target = gcpy.getEdgeTarget((DefaultEdge) edge);
					if (!scc.vertexSet().contains(target)) {
						// if target is not in this scc
						// change source of this edge to newSccVertex						
						gcpy.addEdge(newSccVertex, target);
					}
					gcpy.removeEdge((DefaultEdge) edge);
				}

				Object[] incomings = gcpy.incomingEdgesOf(vertex.toString()).toArray();
				for (Object edge : incomings) {
					// check source vertex
					String source = gcpy.getEdgeSource((DefaultEdge) edge);
					if (!scc.vertexSet().contains(source)) {
						// if source is not in this scc
						// change target of this edge to newSccVertex						
						gcpy.addEdge(source, newSccVertex);
					}
					gcpy.removeEdge((DefaultEdge) edge);
				}
			}

			// remove vertexes
			for (Object vertex : scc.vertexSet()) {
				// remove this vertex
				gcpy.removeVertex(vertex.toString());
			}
		}

		// now graph g does not contain any scc and it is a DAG
		for (int i = 0; i < SCC.size(); i++) {
			String scci = "scc" + i;
			for (int j = i + 1; j < SCC.size(); j++) {
				String sccj = "scc" + j;

				if (isPBeforeVInTopoOrder(gcpy, scci, sccj)) {
					// revert those two SCC in scc
					Collections.swap(SCC, i, j);
				}
			}
		}
	}

	public boolean hasWrongFieldRef(String str) {
		if (AnalysisUtil.getInstance().IS_FILER_WRONG_FIELD) {
			Stack<String> stack = new Stack<String>();

			String[] segs = str.split("\\)\\(");
			for (int i = 0; i < segs.length; i++) {
				String seg = segs[i];
				seg = seg.replace("(", "").replace(")", "");

				RelationEdge ere = RelationEdgesSet.getInstance().getRelationElement(Integer.valueOf(seg));
				if (ere._operation.equals("PUT_VALUE_TO_FIELD") || ere._operation.equals("PUT_FIELD")) {
					stack.push(ere._field);
				} else if (ere._operation.equals("GET_VALUE_FROM_FIELD") || ere._operation.equals("GET_FIELD")) {
					if (stack.size() > 0) {
						String poped = stack.peek();
						if (poped.equals(ere._field)) {
							stack.pop();
						} else {
							return true;
						}
					}
				}
			}
			return false;
		} else {
			return false;
		}
	}

	public boolean isFirstGetField(String str, String var) {
		String field = var.substring(var.indexOf(".") + 1, var.length());

		String[] segs = str.split("\\)\\(");
		for (int i = 0; i < segs.length; i++) {
			String seg = segs[i];
			seg = seg.replace("(", "").replace(")", "");

			RelationEdge ere = RelationEdgesSet.getInstance().getRelationElement(Integer.valueOf(seg));
			if (ere._operation.equals("PUT_VALUE_TO_FIELD") || ere._operation.equals("PUT_FIELD")) {
				return false;
			} else if (ere._operation.equals("GET_VALUE_FROM_FIELD") || ere._operation.equals("GET_FIELD")) {
				if (ere._field.equals(field)) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	public String[] getOpAndField(String op) {
		String[] ret = new String[2];
		if (op.contains("[")) {
			ret[0] = op.substring(0, op.indexOf("["));
			ret[1] = op.substring(op.indexOf("[") + 1, op.indexOf("]"));
		} else {
			ret[0] = op;
			ret[1] = null;
		}
		return ret;

	}
}
