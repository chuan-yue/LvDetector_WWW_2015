package extanalysis.ssaparser;

import java.util.ArrayList;

import extanalysis.AnalysisUtil;
import extanalysis.ssaparser.SSAVarUseGraph.VarUseEdge;

public class SSAUtil {

	private static SSAUtil _ssautil_instance = null;

	public SSAUtil() {

	}

	public static SSAUtil getInstance() {
		if (_ssautil_instance == null) {
			_ssautil_instance = new SSAUtil();
		}
		return _ssautil_instance;
	}

	// file.js [0->55] (line 1)
	public String get_fileName(String callsite) {
		return callsite.substring(0, callsite.indexOf("[") - 1);
	}

	// file.js [0->55] (line 1)
	public String get_lineNumber(String callsite) {
		return callsite.substring(callsite.indexOf("line") + 5, callsite.length() - 1);
	}

	public void dumpVarUseGraph(String path, SSAVarUseGraph g) {
		String exportPath = path + g.getFuncName() + ".dot";
		StringBuffer sb = new StringBuffer();

		sb.append("digraph AST {\n");
		sb.append("rankdir=LR;overlap=false;overlap=scale;splines=true;\n");
		sb.append("node [label=\"\\N\", color=lightblue2, style=filled];\n");
		sb.append("graph [bb=\"0,0,1.6447e+005,3276\"];\n");

		ArrayList<String> nodes = new ArrayList<String>();

		for (Integer vertex : g.getVertexes()) {
			if (vertex == -1) {
				continue;
			}
			String v = String.valueOf(vertex);
			if (!nodes.contains(v)) {
				String id = "node" + v;
				nodes.add(v);
				String gName = g.getFunc().getVarLocalNameMap().get(vertex);
				if (gName != null) {
					v = v + "=" + gName;
				}
				String node = id + " [label=\"" + v + "\"];\n";
				sb.append(node);
			}
		}

		for (VarUseEdge edge : g.getOperationEdges().values()) {
			if (edge.getFrom() == -1 || edge.getTo() == -1) {
				continue;
			}
			String label = edge.getOp();
			if (edge.getOpProto() != null) {
				label = label + ":" + edge.getOpProto();
			}
			String e = null;
			if (edge.getOp().equals("PROTOTYPE") || edge.getOp().equals("CONSTRUCT")) {
				e = "node" + edge.getFrom() + "->" + "node" + edge.getTo() + " [style=dotted label=\"" + label + "\"];\n";
			} else if (edge.getOp().contains("GET_FIELD") || edge.getOp().contains("PUT_FIELD") || edge.getOp().contains("GET_VALUE_FROM_FIELD")
					|| edge.getOp().contains("PUT_VALUE_TO_FIELD")) {
				e = "node" + edge.getFrom() + "->" + "node" + edge.getTo() + " [color=red label=\"" + label + "\"];\n";
			} else {
				e = "node" + edge.getFrom() + "->" + "node" + edge.getTo() + " [label=\"" + label + "\"];\n";
			}
			sb.append(e);
		}

		sb.append("}");
		AnalysisUtil.getInstance().writeToFile(exportPath, sb.toString(), false);
	}

	public void dumpVarFlowGraph(String path, SSAVarUseGraph g) {
		String exportPath = path + g.getFuncName() + ".dot";
		StringBuffer sb = new StringBuffer();

		sb.append("digraph AST {\n");
		sb.append("rankdir=LR;overlap=false;overlap=scale;splines=true;\n");
		sb.append("node [label=\"\\N\", color=lightblue2, style=filled];\n");
		sb.append("graph [bb=\"0,0,1.6447e+005,3276\"];\n");

		ArrayList<String> nodes = new ArrayList<String>();

		for (Integer vertex : g.getVertexes()) {
			if (vertex == -1) {
				continue;
			}
			String v = String.valueOf(vertex);
			if (!nodes.contains(v)) {
				String id = "node" + v;
				nodes.add(v);
				String gName = g.getFunc().getVarLocalNameMap().get(vertex);
				if (gName != null) {
					v = v + "=" + gName;
				}
				String node = id + " [label=\"" + v + "\"];\n";
				sb.append(node);
			}
		}

		for (VarUseEdge edge : g.getFlowEdges().values()) {
			if (edge.getFrom() == -1 || edge.getTo() == -1) {
				continue;
			}
			String label = edge.getOp();
			if (edge.getOpProto() != null) {
				label = label + ":" + edge.getOpProto();
			}

			String e = null;
			if (edge.getOp().equals("PROTOTYPE") || edge.getOp().equals("CONSTRUCT")) {
				e = "node" + edge.getFrom() + "->" + "node" + edge.getTo() + " [style=dotted label=\"" + label + "\"];\n";
			} else if (edge.getOp().contains("GET_FIELD") || edge.getOp().contains("PUT_FIELD") || edge.getOp().contains("GET_VALUE_FROM_FIELD")
					|| edge.getOp().contains("PUT_VALUE_TO_FIELD")) {
				e = "node" + edge.getFrom() + "->" + "node" + edge.getTo() + " [color=red label=\"" + label + "\"];\n";
			} else {
				e = "node" + edge.getFrom() + "->" + "node" + edge.getTo() + " [label=\"" + label + "\"];\n";
			}
			sb.append(e);
		}

		sb.append("}");
		AnalysisUtil.getInstance().writeToFile(exportPath, sb.toString(), false);
	}

	public void dumpIR(String path, String funcName, String ir) {
		String exportPath = path + funcName + ".ir";
		AnalysisUtil.getInstance().writeToFile(exportPath, ir, false);
	}
}
