package extanalysis.callgraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import extanalysis.AnalysisUtil;
import extanalysis.callgraph.logparser.CallGraphPrototype;

public class CallGraphUtil {

	private static CallGraphUtil _callgraph_instance = null;

	private ArrayList<CallGraphPrototype> PROTOTYPE = new ArrayList<CallGraphPrototype>();
	//private ArrayList<CallGraphCallsite> CALLSITE = new ArrayList<CallGraphCallsite>();
	
	public CallGraphUtil() {
	}

	public static CallGraphUtil getInstance() {
		if (_callgraph_instance == null) {
			_callgraph_instance = new CallGraphUtil();
		}
		return _callgraph_instance;
	}

	// Pre process all functions' prototypes declared in extension from static analysis
	public void parsePrototypeSum(String staticPath) {
		String content = readToString(staticPath);
		String[] funList = content.split("\n");
		for (int i = 0; i < funList.length; i++) {
			if (funList[i].length() > 0) {
				PROTOTYPE.add(new CallGraphPrototype(funList[i]));
			}
		}
	}	

	public boolean isPrototypeExisted(String prototype) {
		for (CallGraphPrototype f : PROTOTYPE) {
			String fT = f.getPrototype();
			if (fT.equals(prototype)) {
				return true;
			}
		}
		return false;
	}

	/*
	// Pre process all found callsites
	public void parseCallsiteSum(String staticPath) {
		String content = readToString(staticPath);
		String[] callsiteList = content.split("\n");
		for (int i = 0; i < callsiteList.length; i++) {
			if (callsiteList[i].length() > 0) {
				String[] strs = callsiteList[i].split("@@");
				CALLSITE.add(new CallGraphCallsite(strs[0], strs[1], strs[2], strs[3]));
			}
		}
	}
	*/

	public String readToString(String fileName) {
		String encoding = "ISO-8859-1";
		File file = new File(fileName);
		Long filelength = file.length();
		byte[] filecontent = new byte[filelength.intValue()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(filecontent);
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			return new String(filecontent, encoding);
		} catch (UnsupportedEncodingException e) {
			System.err.println("The OS does not support " + encoding);
			e.printStackTrace();
			return null;
		}
	}

	public void printArrayString(ArrayList<String> array, String path) {
		StringBuffer sb = new StringBuffer();
		for (String v : array) {
			sb.append(v);
			sb.append("\n");
		}
		AnalysisUtil.getInstance().writeToFile(path, sb.toString(), false);
	}

	public boolean isArgConstant(String arg) {
		return arg.contains(":");
	}


	public void parseCallResultFile(ArrayList<String[]> nodes, ArrayList<CallGraphCall> edges) {
		File callResults = new File(AnalysisUtil.getInstance().CALL_RESULT_FILE);
		if (callResults.exists()) {
			String content = readToString(AnalysisUtil.getInstance().CALL_RESULT_FILE);
			String[] calls = content.split("\n");
			for (String call : calls) {
				String[] tmp = call.split(",");
				if (tmp.length == 8) {
					edges.add(new CallGraphCall(call));
				} else if (tmp.length == 2) {
					nodes.add(tmp);
				}
			}
		}
	}

	public void printCallResult(ArrayList<String[]> nodes, ArrayList<CallGraphCall> edges, String path) {
		StringBuffer sb = new StringBuffer();
		for (String[] func : nodes) {
			sb.append(func[0]);
			sb.append(",");
			sb.append(func[1]);
			sb.append("\n");
		}
		for (CallGraphCall call : edges) {
			sb.append(call.toString());
		}
		AnalysisUtil.getInstance().writeToFile(path, sb.toString(), false);
	}

	public void exportCallGraph(ArrayList<String[]> nodes, ArrayList<CallGraphCall> edges, String exportPath) {
		StringBuffer all = new StringBuffer();

		all.append("digraph AST {\n");
		all.append("rankdir=LR;overlap=false;overlap=scale;splines=true;\n");
		all.append("node [label=\"\\N\", color=lightblue2, style=filled];\n");
		all.append("graph [bb=\"0,0,1.6447e+005,3276\"];\n");

		HashMap<String, String> addedNodes = new HashMap<String, String>();

		for (String[] node : nodes) {
			String tmp = node[1] + "@" + node[0];
			String id = "node" + String.valueOf(addedNodes.size());
			String toAdd = id + " [label=\"" + tmp + "\"];\n";
			all.append(toAdd);
			addedNodes.put(tmp, id);
		}

		for (CallGraphCall edge : edges) {
			String from = edge.getCallerFuncProto() + "@" + edge.getCallerFile();
			String idFrom = addedNodes.get(from);
			String to = edge.getCalleeFuncProto() + "@" + edge.getCalleeFile();
			String idTo = addedNodes.get(to);

			String edgeLabel = edge.getCallsiteFrom() + "->" + edge.getCallsiteStr() + "@" + edge.getCallsiteFile() + ":" + edge.getCallsiteLine();
			String toAdd = idFrom + "->" + idTo + " [label=\"" + edgeLabel + "\"];\n";
			all.append(toAdd);
		}

		all.append("}");

		AnalysisUtil.getInstance().writeToFile(exportPath, all.toString(), false);
	}
}
