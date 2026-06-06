package extanalysis.summarize.global;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import extanalysis.AnalysisUtil;
import extanalysis.RelationEdgesSet;
import extanalysis.Func;
import extanalysis.SinkVar;
import extanalysis.SourceVar;
import extanalysis.Var;
import extanalysis.VarPair;
import extanalysis.Func.SumCallsite;
import extanalysis.summarize.SummarizeUtil;

public class GlobalSummarize {
	private ArrayList<Func> _sum_func_list;

	private HashMap<String, VarPair> _global_summaries;

	public GlobalSummarize(ArrayList<Func> functionSummaries) {
		_sum_func_list = functionSummaries;
	}

	public void summarize(List<SourceVar> sourceVars, List<SinkVar> sinkedVars) {
		System.out.print("Global analysis: ");

		Calendar c1 = Calendar.getInstance();
		c1.setTime(new Date());

		GlobalRelationTransitiveAnalysis rta = buildRTA(_sum_func_list);
		for (SinkVar svar : sinkedVars) {
			for (Var oVar : sourceVars) {
				rta.computeRelation(oVar.toString(), svar.toString());
			}

			//if (rta.containsVertex(svar.toString() + "_twin")) {
			//	rta.computeRelation(svar.toString() + "_twin");
			//}
			//rta.computeRelation(svar.toString());

			System.out.print(".");
		}
		_global_summaries = rta.getFinalResult();

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
		sb.append("InterProcedure:").append(diff).append(",").append(diffSec).append(",").append(diffMin).append(",").append(diffHours).append(",")
				.append(diffDays).append("\n");
		AnalysisUtil.getInstance().writeToFile(AnalysisUtil.getInstance().PERFORMANCE_FILE, sb.toString(), true);

		System.out.print("DONE!\n");

		if (AnalysisUtil.getInstance().IS_DUMP) {
			dumpGolbalSummary();
			RelationEdgesSet.getInstance().dumpExtRelationElementArray();
		}
	}

	public HashMap<String, VarPair> getGlobalSummaries() {
		return _global_summaries;
	}

	private void dumpGolbalSummary() {
		StringBuffer sb = new StringBuffer();
		// HashMap<String, List<SumVarPair>> _global_summaries
		for (String funcPair : _global_summaries.keySet()) {
			sb.append(funcPair).append("\n");
			VarPair sum = _global_summaries.get(funcPair);
			sb.append(sum.toString()).append(":").append(sum._enc_relation).append("\n");
		}

		AnalysisUtil.getInstance().writeToFile(AnalysisUtil.getInstance().VAR_GLOBAL_TRANSI_FILE, sb.toString(), false);
	}

	private GlobalRelationTransitiveAnalysis buildRTA(ArrayList<Func> functionSums) {
		GlobalRelationTransitiveAnalysis rta = new GlobalRelationTransitiveAnalysis();
		for (Func func : functionSums) {

			// Add this function's summary
			List<VarPair> functionSum = func._function_sum;
			if (functionSum.size() > 0) {
				for (VarPair vpair : functionSum) {
					rta.addVertex(vpair._from.toString());
					rta.addVertex(vpair._to.toString());
					rta.addEdge(vpair._from.toString(), vpair._to.toString(), vpair);
				}
			}

			// Add this function's callsites
			for (SumCallsite scs : func._callsites.values()) {
				// Here maybe the callee
				String calleeFuncName = scs._to_func;

				for (Func tmp : functionSums) {
					if (tmp._function_name.equals(calleeFuncName)) {
						// the callee is in analysis scope
						// then add this callee, otherwise do nothing

						// this is for edge processing
						// sf -scs-> tmp
						List<VarPair> callPathR = computeCallPath(func, tmp, scs);
						for (VarPair vpair : callPathR) {
							rta.addVertex(vpair._from.toString());
							rta.addVertex(vpair._to.toString());
							rta.addEdge(vpair._from.toString(), vpair._to.toString(), vpair);
						}

						List<VarPair> returnPathR = computeReturnPath(func, tmp, scs);
						if (null != returnPathR) {
							for (VarPair vpair : returnPathR) {
								rta.addVertex(vpair._from.toString());
								rta.addVertex(vpair._to.toString());
								rta.addEdge(vpair._from.toString(), vpair._to.toString(), vpair);
							}
						}
						break;
					}
				}
			}

			// Add this function's  lexical information
			HashMap<Integer, String[]> var2lexical = func._var_lexical_use_map;
			for (int vn : var2lexical.keySet()) {
				// For each used lexical we get its lexical information: file, func, varname
				String[] lexicalUseInfo = var2lexical.get(vn);
				String defFuncName = lexicalUseInfo[1];

				for (Func tmp : functionSums) {
					if (tmp._function_name.equals(defFuncName)) {
						int a = tmp.isLexicalDefined(lexicalUseInfo);
						if (a != -1) {
							List<VarPair> lexicalPathR = computeExtraPath(tmp, a, func, vn, "LEXICAL");
							for (VarPair vpair : lexicalPathR) {
								rta.addVertex(vpair._from.toString());
								rta.addVertex(vpair._to.toString());
								rta.addEdge(vpair._from.toString(), vpair._to.toString(), vpair);
							}
						}
						break;
					}
				}
			}

			// Add this function's global information
			HashMap<Integer, String[]> globalUse = func._var_global_use_map;
			for (int vn : globalUse.keySet()) {
				// For each used global var we get its information: file, func, varname
				String[] globalUseInfo = globalUse.get(vn);

				for (Func tmp : functionSums) {
					// See if this global var is defined in function tmp
					int a = tmp.isGlobalDefined(globalUseInfo);
					if (a != -1) {
						rta.addVertex(tmp._function_name);
						List<VarPair> globalPathR = computeExtraPath(tmp, a, func, vn, "GLOBAL");
						for (VarPair vpair : globalPathR) {
							rta.addVertex(vpair._from.toString());
							rta.addVertex(vpair._to.toString());
							rta.addEdge(vpair._from.toString(), vpair._to.toString(), vpair);
						}
					}
				}
			}
		}

		rta.preprocess();
		return rta;
	}

	private List<VarPair> computeCallPath(Func caller, Func callee, SumCallsite callsite) {
		List<VarPair> ret = new ArrayList<VarPair>();

		// all args passed into this callsite
		int[] args = callsite._arguments;

		// for each arg
		for (int index = 0; index < args.length; index++) {

			// get corresponding para for this arg
			// it is safe for apply function calls
			int correspondingPara;
			try {
				correspondingPara = callee._parameters[index];
			} catch (Exception e) {
				continue;
			}

			// connect arg->para
			String argStr = caller._ssa_func.getVarLocalNameMap().get(args[index]);
			String paraStr = callee._ssa_func.getVarLocalNameMap().get(correspondingPara);
			Var arg = new Var(caller._file_name, caller._function_name, String.valueOf(args[index]), argStr);
			Var para = new Var(callee._file_name, callee._function_name, String.valueOf(correspondingPara), paraStr);
			List<VarPair> update0 = new ArrayList<VarPair>();
			List<String> tmpR = new ArrayList<String>();
			int recIndex = RelationEdgesSet.getInstance().addRelationElement(caller._file_name, caller._function_name, String.valueOf(args[index]),
					argStr, callee._file_name, callee._function_name, String.valueOf(correspondingPara), paraStr, "CALL", null, null);
			tmpR.add("(" + String.valueOf(recIndex) + ")");
			update0.add(new VarPair(arg, para, tmpR));
			ret = SummarizeUtil.getInstance().union(ret, update0);
		}

		return ret;
	}

	private List<VarPair> computeReturnPath(Func caller, Func callee, SumCallsite callsite) {
		// in this function we only compute relations between caller and callee through callsite
		List<VarPair> ret = new ArrayList<VarPair>();

		if (!callee.isReturnedFunc()) {
			return ret;
		}

		// get corresponding assigned
		int assign = callsite._assigned;
		// all returns in this callsite
		int[] returns = callee._returns;

		// for each return
		for (int index = 0; index < returns.length; index++) {

			// consider return->assign
			String retStr = callee._ssa_func.getVarLocalNameMap().get(returns[index]);
			String assgnStr = caller._ssa_func.getVarLocalNameMap().get(assign);
			Var retn = new Var(callee._file_name, callee._function_name, String.valueOf(returns[index]), retStr);
			Var assgn = new Var(caller._file_name, caller._function_name, String.valueOf(assign), assgnStr);
			List<VarPair> update0 = new ArrayList<VarPair>();
			List<String> tmpR = new ArrayList<String>();
			int recIndex = RelationEdgesSet.getInstance().addRelationElement(callee._file_name, callee._function_name,
					String.valueOf(returns[index]), retStr, caller._file_name, caller._function_name, String.valueOf(assign), assgnStr, "RETURN",
					null, null);
			tmpR.add("(" + String.valueOf(recIndex) + ")");
			update0.add(new VarPair(retn, assgn, tmpR));
			ret = SummarizeUtil.getInstance().union(ret, update0);
		}

		return ret;
	}

	private List<VarPair> computeExtraPath(Func fFunc, int fV, Func tFunc, int tV, String type) {
		List<VarPair> ret = new ArrayList<VarPair>();

		// now only consider fV->tV
		String fsvStr = fFunc._ssa_func.getVarLocalNameMap().get(fV);
		String tsvStr = tFunc._ssa_func.getVarLocalNameMap().get(tV);
		Var fsv = new Var(fFunc._file_name, fFunc._function_name, String.valueOf(fV), fsvStr);
		Var tsv = new Var(tFunc._file_name, tFunc._function_name, String.valueOf(tV), tsvStr);
		List<VarPair> update0 = new ArrayList<VarPair>();
		List<String> tmpR = new ArrayList<String>();
		int recIndex = RelationEdgesSet.getInstance().addRelationElement(fFunc._file_name, fFunc._function_name, String.valueOf(fV), fsvStr,
				tFunc._file_name, tFunc._function_name, String.valueOf(tV), tsvStr, type, null, null);
		tmpR.add("(" + String.valueOf(recIndex) + ")");
		update0.add(new VarPair(fsv, tsv, tmpR));
		ret = SummarizeUtil.getInstance().union(ret, update0);

		return ret;
	}
}
