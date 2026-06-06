package extanalysis.callgraph;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import extanalysis.AnalysisUtil;
import extanalysis.callgraph.logparser.CallGraphFunctionPrototype;

public class CallGraph {
	private ArrayList<CallGraphFunctionPrototype> FUNC_PROTOTYPE = new ArrayList<CallGraphFunctionPrototype>();

	private ArrayList<String> _filtered_callsite = new ArrayList<String>();
	private ArrayList<String> _filtered_func = new ArrayList<String>();

	private ArrayList<String[]> _callgraph_nodes = new ArrayList<String[]>();
	private ArrayList<CallGraphCall> _callgraph_edges = new ArrayList<CallGraphCall>();

	public CallGraph() {
	}

	public void compute() {
		Calendar c1 = Calendar.getInstance();
		c1.setTime(new Date());

		computeCallGraph();
		shrinkCallGraphWithCrypto();
		CallGraphUtil.getInstance().printCallResult(_callgraph_nodes, _callgraph_edges, AnalysisUtil.getInstance().CALL_RESULT_FILE);
		CallGraphUtil.getInstance().exportCallGraph(_callgraph_nodes, _callgraph_edges, AnalysisUtil.getInstance().CALL_DOT_FILE);

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
		sb.append("CallGraph:").append(diff).append(",").append(diffSec).append(",").append(diffMin).append(",").append(diffHours).append(",")
				.append(diffDays).append("\n");
		AnalysisUtil.getInstance().writeToFile(AnalysisUtil.getInstance().PERFORMANCE_FILE, sb.toString(), true);
	}

	// exposed to out
	public ArrayList<CallGraphCall> getCallGraphEdges() {
		return this._callgraph_edges;
	}

	// exposed to out
	public ArrayList<String[]> getCallGraphNodes() {
		return this._callgraph_nodes;
	}

	// exposed to out
	public CallGraphCall getReferenceCall(String file, String resideFunc, String lineno, String fullInvoke) {
		for (CallGraphCall c : _callgraph_edges) {
			if (c.getCallsiteFile().equals(file) && c.getCallerFuncProto().equals(resideFunc) && c.getCallsiteLine().equals(lineno)) {
				// here we process a.b in c.getCallsiteStr() equals a[b] in fullInvoke
				String str = c.getCallsiteStr();
				// we need to convert "[]" to "."
				fullInvoke = fullInvoke.replace("][", ".");
				fullInvoke = fullInvoke.replace("[", ".");
				fullInvoke = fullInvoke.replace("]", "");
				str = str.replace("][", ".");
				str = str.replace("[", ".");
				str = str.replace("]", "");
				if (str.equals(fullInvoke)) {
					return c;
				}
			}
		}
		for (String str : AnalysisUtil.getInstance().JS_INHERI_EVEN_MESSAGE) {
			// Here if this call is messaging related
			if (str.equals(fullInvoke)) {
				// Get pointto function, there should be only one
				String[] pointto = AnalysisUtil.getInstance()._message_receiver.get(0);
				// Create a new CallGraphCall and return it
				return new CallGraphCall(file, resideFunc, pointto[0], pointto[1], file, resideFunc, lineno, fullInvoke);
			}
		}

		return null;
	}

	// exposed to out
	public boolean isFileAccessed(String file) {
		for (String[] c : _callgraph_nodes) {
			if (c[0].equals(file)) {
				return true;
			}
		}
		return false;
	}

	// exposed to out
	public boolean isProrotypeAccessed(String file, String proto) {
		for (String[] c : _callgraph_nodes) {
			if (c[0].equals(file) && c[1].equals(proto)) {
				return true;
			}
		}
		return false;
	}

	private void computeCallGraph() {
		CallGraphUtil.getInstance().parseCallResultFile(_callgraph_nodes, _callgraph_edges);
		if (_callgraph_edges.size() > 0 && _callgraph_nodes.size() > 0) {
			return;
		}

		CallGraphUtil.getInstance().parsePrototypeSum(AnalysisUtil.getInstance().SUM_FUNC_LOG);
		filterRuntimeLog(AnalysisUtil.getInstance().SUM_RUMTIME_LOG);
		try {
			match();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Call graph: DONE!");
	}

	private void filterRuntimeLog(String logPath) {
		String content = CallGraphUtil.getInstance().readToString(logPath);
		String[] logList = null;
		if (AnalysisUtil.getInstance()._browser_type.equals("Firefox")) {
			logList = content.split("\r\n");
		} else if (AnalysisUtil.getInstance()._browser_type.equals("Chrome")) {
			logList = content.split("\n");
		}

		for (int i = 0; i < logList.length; i++) {
			if (logList[i].length() > 0 && logList[i].contains("@@")) {
				if (AnalysisUtil.getInstance()._browser_type.equals("Firefox")) {
					filterPrototypeInFirefox(logList[i]);
				} else if (AnalysisUtil.getInstance()._browser_type.equals("Chrome")) {
					filterPrototypeInChrome(logList[i]);
				}
			}
		}

		for (int i = 0; i < logList.length; i++) {
			if (logList[i].length() > 0 && logList[i].contains("@@")) {
				if (AnalysisUtil.getInstance()._browser_type.equals("Firefox")) {
					filterCallInFirefox(logList[i]);
				} else if (AnalysisUtil.getInstance()._browser_type.equals("Chrome")) {
					filterCallInChrome(logList[i]);
				}
			}
		}
	}

	private void filterPrototypeInFirefox(String s1) {
		String[] tmp = s1.split("@@");
		int len = tmp.length;

		if (len == 3 && tmp[0].equals("PROTO") && CallGraphUtil.getInstance().isPrototypeExisted(tmp[2])) {
			// PROTO@@funcName@@funcProto
			addFunctionPrototype(tmp[1], tmp[2]);
		}
	}

	public void addFunctionPrototype(String func, String proto) {
		if (CallGraphUtil.getInstance().isPrototypeExisted(proto)) {
			for (CallGraphFunctionPrototype p : FUNC_PROTOTYPE) {
				if (p.getFunctionName().equals(func) && p.getPrototype().equals(proto)) {
					return;
				}
			}
			FUNC_PROTOTYPE.add(new CallGraphFunctionPrototype(func, proto));
		}
	}

	private void filterCallInFirefox(String s1) {
		String[] tmp = s1.split("@@");
		int len = tmp.length;

		if (tmp[0].equals("CALLSITE") && len == 5) {
			// CALLSITE@@callerName@@callsiteString@@callerFile@@lineNo
			//if (containsFuncInFunctionPrototype(tmp[1]) && containsFuncInFunctionPrototype(tmp[2])) {
			if (containsFuncInFunctionPrototype(tmp[1])) {
				if (!_filtered_callsite.contains(s1)) {
					_filtered_callsite.add(s1);
				}
			}
		} else if (tmp[0].equals("FUNC") && len == 4) {
			// FUNC@@callerProto@@calleeProto@@calleeFile
			if (containsProtoInFunctionPrototype(tmp[2])) {
				if (!_filtered_func.contains(s1)) {
					_filtered_func.add(s1);
				}
			}
		}
	}

	public boolean containsFuncInFunctionPrototype(String func) {
		for (CallGraphFunctionPrototype p : FUNC_PROTOTYPE) {
			if (p.getFunctionName().equals(func)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsProtoInFunctionPrototype(String proto) {
		for (CallGraphFunctionPrototype p : FUNC_PROTOTYPE) {
			if (p.getPrototype().equals(proto)) {
				return true;
			}
		}
		return false;
	}

	private void filterPrototypeInChrome(String s1) {
		// [8176:7424:1205/132738:INFO:CONSOLE(43)] "TestTestTestTestTestTest", source: chrome-extension://mdabdmaifljpfpgoflecjclgdgkkpdlo/options.js (43)
		if (s1.contains("CONSOLE")) {
			// we take strs[0]
			String s2 = s1.split(",")[0];
			String s3 = s2.split(" ")[1];
			String s4 = s3.substring(1, s3.length() - 1);

			// now s4 is in format of PROTO@@wufrnkvnjg@@wufrnkvnjg
			filterPrototypeInFirefox(s4);
		}
	}

	private void filterCallInChrome(String s1) {
		// [8176:7424:1205/132738:INFO:CONSOLE(43)] "TestTestTestTestTestTest", source: chrome-extension://mdabdmaifljpfpgoflecjclgdgkkpdlo/options.js (43)
		if (s1.contains("CONSOLE")) {
			// we take strs[0]
			String s2 = s1.split(",")[0];
			String s3 = s2.split(" ")[1];
			String s4 = s3.substring(1, s3.length() - 1);

			// now s4 is in format of PROTO@@wufrnkvnjg@@wufrnkvnjg
			filterCallInFirefox(s4);
		}
	}

	private void match() {
		for (String func : _filtered_func) {
			// FUNC@@callerProto@@calleeProto@@calleeFile	
			String[] funcPars = func.split("@@");
			addNode(funcPars[3], funcPars[2]);
		}
		for (String callsite : _filtered_callsite) {
			// CALLSITE@@callerName@@callsiteString@@callerFile@@lineNo
			String[] callsitePars = callsite.split("@@");

			String callerProto = findPrototypeFromFunction(callsitePars[1]);
			if (callerProto != null) {
				addNode(callsitePars[3], callerProto);
			}

			String calleeProto = findPrototypeFromFunction(callsitePars[2]);
			String[] callerNode = getNode(callerProto);
			String[] calleeNode = getNode(calleeProto);
			if (callerNode == null || calleeNode == null) {
				continue;
			}

			if (callerProto.equals(callerNode[1]) && calleeProto.equals(calleeNode[1])) {
				addEdge(callsitePars[3], callerProto, calleeNode[0], calleeProto, callsitePars[3], callsitePars[4], callsitePars[1], callsitePars[2]);
			}
		}
	}

	private void addNode(String file, String prototype) {
		String[] tmp = new String[] { file, prototype };
		for (String[] cgn : _callgraph_nodes) {
			if (cgn[0].equals(file) && cgn[1].equals(prototype)) {
				return;
			}
		}
		_callgraph_nodes.add(tmp);
	}

	private void addEdge(String callerFile, String callerProto, String calleeFile, String calleeProto, String callsiteFile, String callsiteLine,
			String callsiteFunc, String callsiteStr) {
		CallGraphCall tmp = new CallGraphCall(callerFile, callerProto, calleeFile, calleeProto, callsiteFile, callsiteLine, callsiteFunc, callsiteStr);
		for (CallGraphCall cgc : _callgraph_edges) {
			if (cgc.equals(tmp)) {
				return;
			}
		}
		_callgraph_edges.add(tmp);
	}

	public String findPrototypeFromFunction(String func) {
		for (CallGraphFunctionPrototype p : FUNC_PROTOTYPE) {
			if (p.getFunctionName().equals(func)) {
				return p.getPrototype();
			}
		}
		return null;
	}

	private String[] getNode(String proto) {
		for (String[] strs : _callgraph_nodes) {
			if (strs[1].equals(proto)) {
				return strs;
			}
		}
		return null;
	}

	private void shrinkCallGraphWithCrypto() {
		List<CallGraphCall> toDelete = new ArrayList<CallGraphCall>();
		List<String> toDelFromList = new ArrayList<String>();

		// first find callsites to crypto functions
		for (String[] encFunc : AnalysisUtil.getInstance()._enc_funcs.keySet()) {
			toDelFromList.add(encFunc[1]);
			findCallsiteTo(encFunc[1], toDelete);
		}

		// second find all callsites from crypto functions 
		for (String toDelFrom : toDelFromList) {
			findCallsiteFrom(toDelFrom, toDelete);
		}

		// last delete those callsites
		Iterator<CallGraphCall> it = _callgraph_edges.iterator();
		while (it.hasNext()) {
			CallGraphCall cur = it.next();
			if (toDelete.contains(cur)) {
				it.remove();
			}
		}
	}

	private void findCallsiteTo(String toDelTo, List<CallGraphCall> toDelete) {
		for (CallGraphCall cgc : _callgraph_edges) {
			if (cgc.getCalleeFuncProto() != null && cgc.getCalleeFuncProto().equals(toDelTo)) {
				// delete ?->cryptoFunc
				toDelete.add(cgc);
			}
		}
	}

	private void findCallsiteFrom(String toDelFrom, List<CallGraphCall> toDelete) {
		for (CallGraphCall cgc : _callgraph_edges) {
			if (cgc.getCallerFuncProto() != null && cgc.getCallerFuncProto().equals(toDelFrom)) {
				toDelete.add(cgc);
				findCallsiteFrom(cgc.getCalleeFuncProto(), toDelete);
			}
		}
	}
}
