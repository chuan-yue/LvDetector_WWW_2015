import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedGraph;

import com.google.javascript.jscomp.CallGraph;
import com.google.javascript.jscomp.CallGraph.Callsite;
import com.google.javascript.jscomp.CallGraph.Function;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

public class StaticCGClosure {

	static HashMap<String, String> file_name_map = new HashMap<String, String>();

	public static DefaultDirectedGraph<String, String> getCG(String[] scripts) {
		DefaultDirectedGraph<String, String> graph = new DefaultDirectedGraph<String, String>(String.class);

		ClosureSuite suite = new ClosureSuite();
		CallGraph callgraph = suite.compileAndRunForward(scripts);

		//DiGraph<Function, Callsite> tmp = callgraph.getForwardDirectedGraph();
		//String str = DotFormatter.toDot((GraphvizGraph)tmp);
		//System.out.println(str);

		Collection<CallGraph.Function> callers = callgraph.getAllFunctions();
		Iterator<Function> itCaller = callers.iterator();
		while (itCaller.hasNext()) {
			// Get caller
			Function caller = itCaller.next();
			if (caller.isMain()) {
				continue;
			}
			String callerName = caller.getName();
			String callerFileName = file_name_map.get(caller.getAstNode().getSourceFileName());
			String callerPrototypeName = caller.getAstNode().getFirstChild().getQualifiedName();
			String[] callerStrs = new String[] { callerFileName, callerName, callerPrototypeName };
			StaticCGUtil.printFunction(callerStrs, graph);

			// Get callsites
			Collection<Callsite> callsites = caller.getCallsitesInFunction();
			Iterator<Callsite> itCallsite = callsites.iterator();
			while (itCallsite.hasNext()) {
				// Get callsite
				Callsite callsite = itCallsite.next();
				String callsiteName = callsite.getName();

				// Callsite's position
				Node tmpNode = callsite.getAstNode();
				while (tmpNode.getParent().getType() != Token.BLOCK) {
					tmpNode = tmpNode.getParent();
				}
				int callsiteLineno = tmpNode.getLineno();

				Collection<CallGraph.Function> callees = callsite.getPossibleTargets();
				Iterator<Function> itCallee = callees.iterator();
				while (itCallee.hasNext()) {
					// Get callee
					Function callee = itCallee.next();
					if (callee.isMain()) {
						continue;
					}
					String calleeName = callee.getName();
					String calleeFileName = file_name_map.get(callee.getAstNode().getSourceFileName());
					String calleePrototypeName = callee.getAstNode().getFirstChild().getQualifiedName();
					String[] calleeStrs = new String[] { calleeFileName, calleeName, calleePrototypeName };
					StaticCGUtil.printFunction(calleeStrs, graph);

					StaticCGUtil.printCall(callerStrs, calleeStrs, new String[] { callerFileName, callerName, String.valueOf(callsiteLineno),
							callsiteName }, graph);
				}
			}
		}

		return graph;
	}

	public static DefaultDirectedGraph<String, String> withClosure(String path) {
		File folder = new File(path);
		File[] files = folder.listFiles();

		ArrayList<File> file_filtered = new ArrayList<File>();

		for (int i = 0; i < files.length; i++) {
			String file_name = files[i].getName();
			if (file_name.endsWith(".js")) {
				file_filtered.add(files[i]);
			}
		}

		String[] combined_scripts = new String[file_filtered.size()];
		for (int i = 0; i < file_filtered.size(); i++) {
			File tmpF = file_filtered.get(i);
			combined_scripts[i] = StaticCGUtil.readFromFile(tmpF);
			file_name_map.put("input" + i, tmpF.getName());
			//System.out.println(combined_scripts[i]);
		}

		return getCG(combined_scripts);
	}

	public static DefaultDirectedGraph<String, String> buildCFGWithClosure(String path) {
		DefaultDirectedGraph<String, String> g = withClosure(path);
		StaticCGUtil.exportToDotFile(g, path + "..\\callgraph_closure.dot");
		StaticCGUtil.analyzeGraph(g);
		return g;
	}

	public static void withParse(File file, DefaultDirectedGraph<String, String> graph) {
		String content = StaticCGUtil.readFromFile(file);
		String[] lines = content.split("\n");
		for (String line : lines) {
			String[] tmp = line.trim().split(",");
			if (tmp.length == 8) {
				StaticCGUtil.printDynamicCall(tmp, graph);
			} else if (tmp.length == 2) {
				StaticCGUtil.printDynamicFunction(tmp, graph);
			}
		}
	}

	public static DefaultDirectedGraph<String, String> buildCFGFromLvDetector(String path) {
		DefaultDirectedGraph<String, String> g = withClosure(path);

		File directory = new File(path);
		File[] files = directory.listFiles();
		for (File file : files) {
			withParse(file, g);
		}

		StaticCGUtil.exportToDotFile(g, path + "..\\callgraph_dynamic.dot");
		StaticCGUtil.analyzeGraph(g);

		return g;
	}

	public static void verifyDynamicCG(DefaultDirectedGraph<String, String> staticCG, DefaultDirectedGraph<String, String> dynamicCG) {
		Set<String> dynamic_vs = dynamicCG.vertexSet();
		Set<String> static_vs = staticCG.vertexSet();

		int node_not_match_count = 0;
		int edge_not_match_count = 0;

		for (String dynamic_v : dynamic_vs) {
			boolean match = false;

			for (String static_v : static_vs) {

				if (static_v.contains(dynamic_v)) {
					//System.out.println("Node Match!");
					match = true;
				}
			}

			if (!match) {
				System.out.println("Dynamic Node Not Found In Static: " + dynamic_v);
				node_not_match_count++;
			}
		}

		System.out.println();

		Set<String> dynamic_es = dynamicCG.edgeSet();
		Set<String> static_es = staticCG.edgeSet();

		for (String dynamic_e : dynamic_es) {
			String dynamic_caller_e = dynamicCG.getEdgeSource(dynamic_e);
			String dynamic_callee_e = dynamicCG.getEdgeTarget(dynamic_e);
			boolean match = false;

			for (String static_e : static_es) {
				String static_caller_e = staticCG.getEdgeSource(static_e);
				String static_callee_e = staticCG.getEdgeTarget(static_e);

				if (dynamic_e.equals(static_e)) {
					if (static_caller_e.contains(dynamic_caller_e) && static_callee_e.contains(dynamic_callee_e)) {
						//System.out.println("Edge Match!");
						match = true;
					}
				}
			}

			if (!match) {
				System.out.println("Dynamic Edge Not Found In Static: " + dynamic_caller_e + "==" + dynamic_e + "==>" + dynamic_callee_e);
				edge_not_match_count++;
			}
		}

		System.out.println();

		System.out.println("Node Not Match: " + node_not_match_count + "; Edge Not Match: " + edge_not_match_count + ".");
	}

	public static void randomSelectEdges(DefaultDirectedGraph<String, String> callgraph, int sample_size) {
		Object[] edge_set = callgraph.edgeSet().toArray();
		int population = edge_set.length;

		ArrayList<String> samples = new ArrayList<String>();
		while (samples.size() < sample_size) {
			int rnd = new Random().nextInt(population);
			String edge = edge_set[rnd].toString();
			if (!samples.contains(edge)) {
				samples.add(edge);
			}
		}

		for (String sample : samples) {
			String caller = callgraph.getEdgeSource(sample);
			String callee = callgraph.getEdgeTarget(sample);
			System.out.println("Static Sample: " + caller + "==" + sample + "==>" + callee);
		}
	}

	public static void generateCGFilesForLV(DefaultDirectedGraph<String, String> callgraph, String path) {
		StringBuffer sb = new StringBuffer();

		Set<String> node_set = callgraph.vertexSet();
		for (String node : node_set) {
			String[] tmp1 = node.split("@");
			String file_name = tmp1[1];
			String[] tmp2 = tmp1[0].split("=");
			String node_prototype = tmp2[1];

			sb.append(file_name).append(",");
			sb.append(node_prototype).append("\n");
		}

		Set<String> edge_set = callgraph.edgeSet();
		for (String edge : edge_set) {
			String caller = callgraph.getEdgeSource(edge);
			String[] caller_tmp1 = caller.split("@");
			String caller_file_name = caller_tmp1[1];
			String[] caller_tmp2 = caller_tmp1[0].split("=");
			String caller_prototype = caller_tmp2[1];

			String callee = callgraph.getEdgeTarget(edge);
			String[] callee_tmp1 = callee.split("@");
			String callee_file_name = callee_tmp1[1];
			String[] callee_tmp2 = callee_tmp1[0].split("=");
			String callee_prototype = callee_tmp2[1];
			
			edge = edge.substring(1, edge.length() - 1);
			String[] edge_tmp = edge.split("@");
			
			sb.append(caller_file_name).append(",");
			sb.append(caller_prototype).append(",");
			sb.append(callee_file_name).append(",");
			sb.append(callee_prototype).append(",");
			
			sb.append(edge_tmp[3]).append(",");
			sb.append(edge_tmp[1]).append(",");
			sb.append(edge_tmp[2]).append(",");
			sb.append(edge_tmp[0]).append("\n");			
		}

		StaticCGUtil.writeToFile(path + "..\\call_result.csv", sb.toString(), false);
	}

	public static void main(String[] args) {
		String js_path = "C:\\Users\\rzhao\\Downloads\\T1\\75\\js\\";
		DefaultDirectedGraph<String, String> static_callgraph = buildCFGWithClosure(js_path);

		System.out.println();

		/*
		String lv_callgraphs_path = "C:\\Users\\rzhao\\Downloads\\T1\\roboform\\lv_callgraphs\\";
		DefaultDirectedGraph<String, String> dynamic_callgraph = buildCFGFromLvDetector(lv_callgraphs_path);

		System.out.println();

		verifyDynamicCG(static_callgraph, dynamic_callgraph);

		System.out.println();

		// randomly select 357 edges from all edges in static_callgraph
		int static_sample_size = 357;
		randomSelectEdges(static_callgraph, static_sample_size);
		*/
		
		// generate call graph files LV Detector can use
		generateCGFilesForLV(static_callgraph, js_path);		
	}
}
