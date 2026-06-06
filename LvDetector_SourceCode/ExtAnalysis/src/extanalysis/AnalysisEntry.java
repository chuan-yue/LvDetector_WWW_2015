package extanalysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import extanalysis.callgraph.CallGraph;
import extanalysis.ssaparser.SSAParser;
import extanalysis.ssaparser.SSAVarUseGraph;
import extanalysis.summarize.global.GlobalSummarize;
import extanalysis.summarize.local.IterativeFunctionsSummarize;
import extanalysis.vulanalysis.VulnerabilityAnalysis;

public class AnalysisEntry {

	private static final boolean SPECIFIC_FOLDER = true; //***

	private static final String folderName = "75_SCG";

	public static void main(String[] args) {
		String rootPath;
		if (SPECIFIC_FOLDER) {
			rootPath = "C:" + File.separator + "TestBench" + File.separator + folderName + File.separator;
		} else {
			rootPath = System.getProperty("user.dir") + File.separator;
		}
		AnalysisUtil.getInstance().setupPaths(rootPath);

		CallGraph cg = new CallGraph();
		cg.compute();

		SSAParser parser = new SSAParser(AnalysisUtil.getInstance().JS_SOURCE_PATH, cg);
		parser.parse();
		ArrayList<SSAVarUseGraph> variableUseGraphs = parser.getVariableUseGraphs();
		List<SourceVar> sourceVars = parser.searchSourceVar();
		List<SinkVar> sinkedVars = parser.searchSinkedVar();
		if(sourceVars.size() == 0 || sinkedVars.size() == 0){
			System.out.println("NO source or sink vars!");
			return;
		}

		IterativeFunctionsSummarize localSummarize = new IterativeFunctionsSummarize(variableUseGraphs, parser, cg);
		localSummarize.iterativeSummarize();
		ArrayList<Func> functionSummaries = localSummarize.getFunctionSummaries();

		GlobalSummarize globalSummarize = new GlobalSummarize(functionSummaries);
		globalSummarize.summarize(sourceVars, sinkedVars);
		HashMap<String, VarPair> globalSummaries = globalSummarize.getGlobalSummaries();

		VulnerabilityAnalysis vanalysis = new VulnerabilityAnalysis(globalSummaries, sourceVars, sinkedVars);
		vanalysis.analyze();
	}
}