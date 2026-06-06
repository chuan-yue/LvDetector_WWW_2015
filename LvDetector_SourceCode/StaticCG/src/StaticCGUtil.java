import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.StringEdgeNameProvider;
import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;

public class StaticCGUtil {
	public static void writeToFile(String file, String content, boolean isAppend) {
		try {
			RandomAccessFile rf = new RandomAccessFile(file, "rw");
			if (isAppend) {
				rf.seek(rf.length());
			} else {
				rf.seek(0);
			}
			rf.writeBytes(content);
			rf.close();
		} catch (IOException e) {
			System.out.println("Append to file failes: " + file);
		}
	}

	public static String readFromFile(String fileName) {
		File file = new File(fileName);
		return readFromFile(file);
	}

	public static String readFromFile(File file) {
		String encoding = "ISO-8859-1";
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

	public static void exportToDotFile(DefaultDirectedGraph<String, String> g, String fileName) {
		DOTExporter<String, String> exporter = new DOTExporter<String, String>(new StringNameProvider<String>(), new StringNameProvider<String>(),
				new StringEdgeNameProvider<String>());
		StringWriter sw = new StringWriter();
		exporter.export(sw, g);

		// digraph G { \n rankdir=LR; overlap=false; overlap=scale; splines=true; \n node [label=\"\\N\", color=lightblue2, style=filled]; \n graph [bb=\"0, 0, 1.6447e+005, 3276\"];
		String toWrite = sw.toString();
		toWrite = toWrite
				.replace(
						"digraph G {",
						"digraph G { \n rankdir=LR; overlap=false; overlap=scale; splines=true; \n node [label=\"\\N\", color=lightblue2, style=filled]; \n graph [bb=\"0, 0, 1.6447e+005, 3276\"]");

		writeToFile(fileName, toWrite, false);
	}

	public static String getFunctionString(String[] function_info) {
		String ret = "";
		ret += function_info[1] != "" ? function_info[1] : "";
		ret += function_info[2] != "" ? "=" + function_info[2] : "";
		ret += "@" + function_info[0];
		return ret.replace("\"", "'");
	}

	public static String getDynamicFunctionString(String func_name, String file_name) {
		String ret = func_name + "@" + file_name;
		return ret.replace("\"", "'");
	}

	public static String getCallsiteString(String[] callsites_info) {
		String ret = "(" + callsites_info[3] + "@" + callsites_info[2] + "@" + callsites_info[1] + "@" + callsites_info[0] + ")";
		return ret.replace("\"", "'");
	}

	public static String getDynamicCallsiteString(String callsite_str, String line_no, String caller_name, String file_name) {
		String ret = "(" + callsite_str + "@" + line_no + "@" + caller_name + "@" + file_name + ")";
		return ret.replace("\"", "'");
	}

	public static void printFunction(String[] function_info, DefaultDirectedGraph<String, String> graph) {
		String function_str = getFunctionString(function_info);
		if (graph.containsVertex(function_str)) {
			return;
		}

		//System.out.println(function_str);
		graph.addVertex(function_str);
	}

	public static void printDynamicFunction(String[] function_info, DefaultDirectedGraph<String, String> graph) {
		String function_str = StaticCGUtil.getDynamicFunctionString(function_info[1], function_info[0]);
		if (graph.containsVertex(function_str)) {
			return;
		}

		//System.out.println(function_str);
		graph.addVertex(function_str);
	}

	public static void printCall(String[] caller_info, String[] callee_info, String[] callsites, DefaultDirectedGraph<String, String> graph) {
		String caller_str = getFunctionString(caller_info);
		String callsite_str = getCallsiteString(callsites);
		String callee_str = getFunctionString(callee_info);
		String pnt = caller_str + "==" + callsite_str + "==>" + callee_str;
		if (graph.containsEdge(callsite_str)) {
			return;
		}
		
		//System.out.println(pnt);
		graph.addEdge(caller_str, callee_str, callsite_str);
	}

	public static void printDynamicCall(String[] all, DefaultDirectedGraph<String, String> graph) {
		String caller_str = getDynamicFunctionString(all[1], all[0]);
		String callsite_str = getDynamicCallsiteString(all[7], all[5], all[6], all[4]);
		String callee_str = getDynamicFunctionString(all[3], all[2]);
		String pnt = caller_str + "==" + callsite_str + "==>" + callee_str;

		//System.out.println(pnt);
		graph.addEdge(caller_str, callee_str, callsite_str);
	}

	public static void analyzeGraph(DefaultDirectedGraph<String, String> graph) {
		int vertex_number = graph.vertexSet().size();
		int edge_number = graph.edgeSet().size();

		int scc_number = 0;
		StrongConnectivityInspector sci = new StrongConnectivityInspector(graph);
		Iterator<DirectedSubgraph<String, DefaultEdge>> it = sci.stronglyConnectedSubgraphs().iterator();
		while (it.hasNext()) {
			DirectedSubgraph<String, DefaultEdge> scc = it.next();
			if (scc.vertexSet().size() > 1) {
				scc_number++;
				System.out.println("SCC " + scc_number + ": " + scc.vertexSet().size() + " nodes");
			}
		}

		String toPrint = "Vertex number: " + vertex_number + "; Edge number: " + edge_number + "; SCC number: " + scc_number + ".";
		System.out.println(toPrint);
	}
}
