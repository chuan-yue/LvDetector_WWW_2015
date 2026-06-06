package extanalysis.summarize.local;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import extanalysis.AnalysisUtil;
import extanalysis.RelationEdgesSet;
import extanalysis.Func;
import extanalysis.callgraph.CallGraph;
import extanalysis.ssaparser.SSAParser;
import extanalysis.ssaparser.SSAVarUseGraph;
import extanalysis.ssaparser.SSAVarUseGraph.VarUseEdge;
import extanalysis.summarize.SummarizeUtil;

public class IterativeFunctionsSummarize {
	private CallGraph _call_graph;

	private ArrayList<SSAVarUseGraph> _variable_use_graphs;

	private SSAParser _ssa_parser;

	private HashMap<SSAVarUseGraph, FunctionRelationTransitiveAnalysis> _vug_rta_map = new HashMap<SSAVarUseGraph, FunctionRelationTransitiveAnalysis>();

	private HashMap<SSAVarUseGraph, HashMap<String, List<String>>> _middle_function_summaries = new HashMap<SSAVarUseGraph, HashMap<String, List<String>>>();

	private ArrayList<Func> _final_function_summaries = new ArrayList<Func>();

	public IterativeFunctionsSummarize(ArrayList<SSAVarUseGraph> variableUseGraphs, SSAParser parser, CallGraph cg) {
		_variable_use_graphs = variableUseGraphs;
		_ssa_parser = parser;
		_call_graph = cg;
	}

	public void iterativeSummarize() {
		Calendar c1 = Calendar.getInstance();
		c1.setTime(new Date());

		boolean stop = false;
		while (stop == false) {
			stop = true;
			// For each function summarize
			for (SSAVarUseGraph vug : this._variable_use_graphs) {
				// 1231 new added filter to improve performance
				if (!_call_graph.isProrotypeAccessed(vug.getFileName(), vug.getFuncName())) {
					continue;
				}

				System.out.print("Local analysis: " + vug.getFuncName() + " ");

				if (isJsFunctionNeedSkip(vug.getFileName(), vug.getFuncName())) {
					System.out.print("SKIP!\n");
					continue;
				}

				HashMap<String, List<String>> lastSum = _middle_function_summaries.get(vug);
				computeSummary(vug);
				HashMap<String, List<String>> curSum = _middle_function_summaries.get(vug);
				if (isSumEqual(lastSum, curSum) == false) {
					stop = false;
				}
				System.out.print("DONE!\n");
			}
			System.out.println("Local analysis: ONE ROUND DONE!");
		}

		// this is for debug use
		if (AnalysisUtil.getInstance().IS_DUMP) {
			dumpLocalTransiGraphs(AnalysisUtil.getInstance().VAR_LOCAL_TRANSI_GRAPH_PATH);
		}

		Calendar c2 = Calendar.getInstance();
		c2.setTime(new Date());
		long time1 = c1.getTimeInMillis();
		long time2 = c2.getTimeInMillis();
		long diff = time2 - time1;
		// Difference in seconds
		long diffSec = diff / 1000;
		// Difference in minutes
		long diffMin = diff / (60 * 1000);
		// Difference in hours
		long diffHours = diff / (60 * 60 * 1000);
		// Difference in days
		long diffDays = diff / (24 * 60 * 60 * 1000);
		StringBuffer sb = new StringBuffer();
		sb.append("IntraProcedure:").append(diff).append(",").append(diffSec).append(",").append(diffMin).append(",").append(diffHours).append(",")
				.append(diffDays).append("\n");
		AnalysisUtil.getInstance().writeToFile(AnalysisUtil.getInstance().PERFORMANCE_FILE, sb.toString(), true);

		for (SSAVarUseGraph vug : _middle_function_summaries.keySet()) {
			_final_function_summaries.add(new Func(vug, _middle_function_summaries.get(vug)));
		}
	}

	public ArrayList<Func> getFunctionSummaries() {
		return _final_function_summaries;
	}

	private boolean isSumEqual(HashMap<String, List<String>> s1, HashMap<String, List<String>> s2) {
		if (s1 == null) {
			// first round s1 is null
			return false;
		} else {
			if (s1.size() != s2.size()) {
				// add new edge
				return false;
			} else {
				for (String str1 : s1.keySet()) {
					List<String> list1 = s1.get(str1);
					List<String> list2 = s2.get(str1);
					// list2 is updated based on list1
					if (list1.size() != list2.size()) {
						// add new summary to an edge
						return false;
					} else {
						for (String str2 : list2) {
							if (!list1.contains(str2)) {
								return false;
							}
						}
					}
				}
				return true;
			}
		}
	}

	// summarize a function
	private void computeSummary(SSAVarUseGraph vug) {
		FunctionRelationTransitiveAnalysis rta = null;
		if (_vug_rta_map.containsKey(vug)) {
			rta = _vug_rta_map.get(vug);
			rta.resetFinalResults();
		} else {
			rta = buildRTA(vug);
			_vug_rta_map.put(vug, rta);
		}

		for (int vertex : vug.getVertexes()) {
			if (rta.containsVertex(String.valueOf(vertex) + "_twin")) {
				rta.computeRelation(String.valueOf(vertex) + "_twin");
			}
			rta.computeRelation(String.valueOf(vertex));
			System.out.print(".");
		}
		this._middle_function_summaries.put(vug, rta.getFinalResult());
	}

	private FunctionRelationTransitiveAnalysis buildRTA(SSAVarUseGraph vug) {
		FunctionRelationTransitiveAnalysis rta = new FunctionRelationTransitiveAnalysis();

		for (int vertex : vug.getVertexes()) {
			rta.addVertex(String.valueOf(vertex));
		}
		for (VarUseEdge edge : vug.getFlowEdges().values()) {
			// Here for edge we convert operation to relation
			List<String> rel = getEncRelation(edge);
			if (rel != null && rel.size() > 0) {
				rta.addEdge(String.valueOf(edge.getFrom()), String.valueOf(edge.getTo()), rel);
			}
		}
		rta.preprocess();
		return rta;
	}

	private List<String> getEncRelation(VarUseEdge edge) {
		String funcName = edge.getContainedFunction().getFuncName();
		String fileName = edge.getContainedFunction().getFileName();
		String op = edge.getOp();
		String opproto = edge.getOpProto();
		String from = String.valueOf(edge.getFrom());
		String to = String.valueOf(edge.getTo());
		String fromVar = edge.getContainedFunction().getVarLocalNameMap().get(edge.getFrom());
		String toVar = edge.getContainedFunction().getVarLocalNameMap().get(edge.getTo());

		List<String> ret = new ArrayList<String>();
		if (isJsFunctionWithFlow(op) || isJsFunctionWithFlow(opproto)) {
			// check if it is defined in ExtAnalysisConst.JS_INHERI_FUNCTION
			int index = RelationEdgesSet.getInstance().addRelationElement(fileName, funcName, from, fromVar, fileName, funcName, to, toVar, "ENCODE",
					op, null);
			ret.add("(" + String.valueOf(index) + ")");
		} else if (isJsFunctionWithoutFlow(op) || isJsFunctionWithoutFlow(opproto)) {
			// do nothing
		} else if (isJsFunctionEncryption(op) || isJsFunctionEncryption(opproto)) {
			int index = RelationEdgesSet.getInstance().addRelationElement(fileName, funcName, from, fromVar, fileName, funcName, to, toVar,
					"ENCRYPT", op, null);
			ret.add("(" + String.valueOf(index) + ")");
		} else if (isJsFunctionDecryption(op) || isJsFunctionDecryption(opproto)) {
			int index = RelationEdgesSet.getInstance().addRelationElement(fileName, funcName, from, fromVar, fileName, funcName, to, toVar,
					"DECRYPT", op, null);
			ret.add("(" + String.valueOf(index) + ")");
		} else {
			if (edge.isCall()) {
				// now find corresponding function summary
				ret = findFunctionSummary(edge);
			} else {
				String[] tmp = SummarizeUtil.getInstance().getOpAndField(edge.getOp());

				//0125 to get rid of not-accessed also not in call graph functions
				if (tmp[0].contains("()")) {
					return null;
				}

				// add a normal edge
				int index = RelationEdgesSet.getInstance().addRelationElement(fileName, funcName, from, fromVar, fileName, funcName, to, toVar,
						tmp[0], null, tmp[1]);
				ret.add("(" + String.valueOf(index) + ")");
			}
		}
		return ret;
	}

	private List<String> findFunctionSummary(VarUseEdge edge) {
		// First find that function's SSAVarUseGraph from  _func_var_use_graph_list
		String funcName = edge.getCall().getCalleeFuncProto();
		String fileName = edge.getCall().getCalleeFile();

		SSAVarUseGraph theRightFunction = _ssa_parser.findSSAVarUseGraph(fileName, funcName);
		// This function has related call info in the whole program run
		// But do not have this function in our analysis 
		// Or this invoked function is not a returned function
		if (theRightFunction == null || !(theRightFunction.getFunc().isReturnedFunc())) {
			return null;
		}

		// Then definitely there should be an summary for this edge
		// This is for corresponding index of argument
		int argIndex = edge.getInstruction().getArgIndex(edge.getFrom());
		// Find parameter in that function's definition
		int parameter = theRightFunction.getFunc().getIthParameter(argIndex);
		if (parameter == -1) {
			return null;
		}
		// Find returned value in that function
		ArrayList<Integer> returns = theRightFunction.getFunc().getReturnedValueNumber();

		// find summary from parameter to returns in that function
		return findSummaryBetweenVars(edge, theRightFunction, parameter, returns);
	}

	private boolean isJsFunctionWithFlow(String funcName) {
		for (String str : AnalysisUtil.getInstance().JS_INHERI_FUNCTION_WITH_FLOW) {
			if (str.equals(funcName)) {
				return true;
			}
		}
		return false;
	}

	private boolean isJsFunctionWithoutFlow(String funcName) {
		for (String str : AnalysisUtil.getInstance().JS_INHERI_FUNCTION_WITHOUT_FLOW) {
			if (str.equals(funcName)) {
				return true;
			}
		}
		return false;
	}

	private boolean isJsFunctionEncryption(String funcName) {
		Iterator<String[]> it = AnalysisUtil.getInstance()._enc_funcs.keySet().iterator();
		while (it.hasNext()) {
			String[] name = it.next();
			if (name[1].equals(funcName)) {
				String type = AnalysisUtil.getInstance()._enc_funcs.get(name);
				if (type.equals("ENCRYPTION")) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isJsFunctionDecryption(String funcName) {
		Iterator<String[]> it = AnalysisUtil.getInstance()._enc_funcs.keySet().iterator();
		while (it.hasNext()) {
			String[] name = it.next();
			if (name[1].equals(funcName)) {
				String type = AnalysisUtil.getInstance()._enc_funcs.get(name);
				if (type.equals("DECRYPTION")) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isJsFunctionNeedSkip(String fileName, String funcName) {
		for (String[] skip : AnalysisUtil.getInstance()._skip_funcs) {
			if (skip.length == 2) {
				if (skip[0].equals(fileName) && skip[1].equals(funcName)) {
					return true;
				}
			} else if (skip.length == 1) {
				if (skip[0].equals(funcName)) {
					return true;
				}
			}
		}
		return false;
	}

	private List<String> findSummaryBetweenVars(VarUseEdge srcEdge, SSAVarUseGraph calledFunc, int from, ArrayList<Integer> tos) {
		List<String> ret = new ArrayList<String>();
		HashMap<String, List<String>> functionSummary = this._middle_function_summaries.get(calledFunc);
		// if this function is summarized
		if (functionSummary != null) {
			// find summary from parameter to each return in that function
			for (int to : tos) {
				String key = composeEdge(from, to);
				List<String> tmpSum = functionSummary.get(key);
				if (tmpSum == null || tmpSum.size() == 0) {
					continue;
				}
				// Here we need to add an extra call and return relation
				String callerFile = srcEdge.getContainedFunction().getFileName();
				String callerFunc = srcEdge.getContainedFunction().getFuncName();
				String calleeFile = calledFunc.getFileName();
				String calleeFunc = calledFunc.getFuncName();

				String argVar = srcEdge.getContainedFunction().getVarLocalNameMap().get(srcEdge.getFrom());
				String fromVar = calledFunc.getFunc().getVarLocalNameMap().get(from);
				String toVar = calledFunc.getFunc().getVarLocalNameMap().get(to);
				String recVar = srcEdge.getContainedFunction().getVarLocalNameMap().get(srcEdge.getTo());

				int callElem = RelationEdgesSet.getInstance().addRelationElement(callerFile, callerFunc, String.valueOf(srcEdge.getFrom()), argVar,
						calleeFile, calleeFunc, String.valueOf(from), fromVar, "CALL", null, null);
				List<String> callEdge = new ArrayList<String>();
				callEdge.add("(" + callElem + ")");
				int returnElem = RelationEdgesSet.getInstance().addRelationElement(calleeFile, calleeFunc, String.valueOf(to), toVar, callerFile,
						callerFunc, String.valueOf(srcEdge.getTo()), recVar, "RETURN", null, null);
				List<String> returnEdge = new ArrayList<String>();
				returnEdge.add("(" + returnElem + ")");
				tmpSum = SummarizeUtil.getInstance().baseConcatenate(callEdge, tmpSum);
				tmpSum = SummarizeUtil.getInstance().baseConcatenate(tmpSum, returnEdge);

				ret = SummarizeUtil.getInstance().baseUnion(ret, tmpSum);

			}
		} else {
			// this function is not summarized
			// give a temporary value UNKNOWN
			String fileName = srcEdge.getContainedFunction().getFileName();
			String funcName = srcEdge.getContainedFunction().getFuncName();
			String argVar = srcEdge.getContainedFunction().getVarLocalNameMap().get(srcEdge.getFrom());
			String recVar = srcEdge.getContainedFunction().getVarLocalNameMap().get(srcEdge.getTo());
			int index = RelationEdgesSet.getInstance().addRelationElement(fileName, funcName, String.valueOf(srcEdge.getFrom()), argVar, fileName,
					funcName, String.valueOf(srcEdge.getTo()), recVar, "UNKNOWN", null, null);
			ret.add("(" + String.valueOf(index) + ")");
		}
		if (ret.size() > 0) {
			return ret;
		} else {
			return null;
		}
	}

	private String composeEdge(int f, int t) {
		return "(" + f + " : " + t + ")";
	}

	/*
	private String hashedSummary() {
		String hashRet = null;
		for (HashMap<String, List<String>> map : this._func_summary.values()) {
			for (List<String> list : map.values()) {
				hashRet += list.toArray();
			}
		}
		return hashRet;
	}
	*/

	private void dumpLocalTransiGraphs(String path) {
		for (SSAVarUseGraph vug : this._middle_function_summaries.keySet()) {
			HashMap<String, List<String>> summaries = this._middle_function_summaries.get(vug);
			String exportDotPath = path + vug.getFuncName() + ".dot";
			String exportTxtPath = path + vug.getFuncName() + ".txt";
			dumpDot(exportDotPath, summaries, vug);
			dumpTxt(exportTxtPath, summaries);
		}
	}

	private void dumpDot(String exportPath, HashMap<String, List<String>> summaries, SSAVarUseGraph vug) {
		StringBuffer sb = new StringBuffer();

		sb.append("digraph AST {\n");
		sb.append("rankdir=LR;overlap=false;overlap=scale;splines=true;\n");
		sb.append("node [label=\"\\N\", color=lightblue2, style=filled];\n");
		sb.append("graph [bb=\"0,0,1.6447e+005,3276\"];\n");

		ArrayList<Integer> nodes = new ArrayList<Integer>();

		for (int vertex : vug.getVertexes()) {
			String id = "node" + vertex;
			nodes.add(vertex);
			String label = String.valueOf(vertex);
			String gName = vug.getFunc().getVarLocalNameMap().get(vertex);
			if (gName != null) {
				label = label + "=" + gName;
			}
			String node = id + " [label=\"" + label + "\"];\n";
			sb.append(node);
		}

		for (String str0 : summaries.keySet()) {
			String str = str0.substring(1, str0.length() - 1).replace(" ", "");
			String[] vars = str.split(":");
			String e = "node" + vars[0] + "->" + "node" + vars[1] + " [label=\"" + summaries.get(str0) + "\"];\n";
			sb.append(e);
		}

		sb.append("}");
		AnalysisUtil.getInstance().writeToFile(exportPath, sb.toString(), false);
	}

	private void dumpTxt(String exportPath, HashMap<String, List<String>> summaries) {
		StringBuffer sb = new StringBuffer();
		for (String str0 : summaries.keySet()) {
			sb.append(str0);
			sb.append(":");
			sb.append(summaries.get(str0));
			sb.append("\n");
		}
		AnalysisUtil.getInstance().writeToFile(exportPath, sb.toString(), false);
	}
}
