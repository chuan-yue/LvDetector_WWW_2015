package extanalysis.ssaparser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;

import extanalysis.AnalysisUtil;
import extanalysis.SinkVar;
import extanalysis.SourceVar;
import extanalysis.callgraph.CallGraph;
import extanalysis.ssaparser.SSAVarUseGraph.VarUseEdge;

public class SSAParser {

	private String _js_source_path;
	private CallGraph _call_graph;

	private ArrayList<SSAFunc> ALL_FUNCTIONS = new ArrayList<SSAFunc>();

	private ArrayList<SSAVarUseGraph> ALL_VARIABLE_USE_GRAPHS = new ArrayList<SSAVarUseGraph>();

	public SSAParser(String sourcePath, CallGraph cg) {
		_js_source_path = sourcePath;
		_call_graph = cg;
	}

	public void parse() {
		parsePath(_js_source_path);
		computeVariableUseGraphs();
	}

	public ArrayList<SSAVarUseGraph> getVariableUseGraphs() {
		return ALL_VARIABLE_USE_GRAPHS;
	}

	public SSAVarUseGraph findSSAVarUseGraph(String fileName, String funcName) {
		for (SSAVarUseGraph funcGraph : ALL_VARIABLE_USE_GRAPHS) {
			String tmpFileName = funcGraph.getFileName();
			String tmpFuncName = funcGraph.getFuncName();
			if (tmpFuncName.equals(funcName) && tmpFileName.equals(fileName)) {
				return funcGraph;
			}
		}
		return null;
	}

	public List<SourceVar> searchSourceVar() {
		List<SourceVar> ret = new ArrayList<SourceVar>();
		for (String[] var : AnalysisUtil.getInstance()._init_vars.keySet()) {
			// for each sensitive variable
			List<SourceVar> srcVars = searchValueNumber(var[0], var[1], var[2], var[3], AnalysisUtil.getInstance()._init_vars.get(var));
			if (srcVars == null) {
				continue;
			}

			ret.addAll(srcVars);
		}
		return ret;
	}

	public List<SinkVar> searchSinkedVar() {
		List<SinkVar> ret = new ArrayList<SinkVar>();
		for (SSAVarUseGraph g : ALL_VARIABLE_USE_GRAPHS) {
			for (VarUseEdge e : g.getFlowEdges().values()) {
				String op = null;
				String toStr = g.getLocalName(e.getTo());
				if (e.isCall()) {
					op = e.getOp();
				} else {
					op = e.getOpProto();
					if (op == null) {
						op = e.getOp();
					}
				}
				if (op == null) {
					continue;
				}
				for (String sinkCandicates : AnalysisUtil.getInstance().JS_INHERI_SINK_FUNCTION_NETWORK) {
					if (sinkCandicates.contains("()") || sinkCandicates.contains("[]")) {
						String sinkPrefix = sinkCandicates.substring(sinkCandicates.length() - 2, sinkCandicates.length());
						if (sinkPrefix.equals("[]")) {
							// put into a field
							String sinkTo = sinkCandicates.substring(0, sinkCandicates.length() - 2);
							if (sinkTo.equals(toStr) && op.contains("PUT_VALUE_TO_FIELD")) {
								// this left hand side var is a sinked var
								String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
								ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String.valueOf(e
										.getFrom()), sinkVar, sinkCandicates, true));
							}
						} else if (sinkPrefix.equals("()")) {
							// function/method
							if (op.equals(sinkCandicates)) {
								// this left hand side var is a sinked var
								String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
								ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String.valueOf(e
										.getFrom()), sinkVar, sinkCandicates, true));
							} else {
								if (sinkCandicates.contains("?") && sinkCandicates.contains(".") && op.contains(".")) {
									String lastMethod = sinkCandicates.substring(sinkCandicates.lastIndexOf(".") + 1, sinkCandicates.length());
									String lastOp = op.substring(op.lastIndexOf(".") + 1, op.length());
									if (lastOp.equals(lastMethod)) {
										// this left hand side var is a sinked var
										String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
										ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String
												.valueOf(e.getFrom()), sinkVar, sinkCandicates, true));
									}
								}
							}
						}
					} else {
						// function/method
						if (op.equals(sinkCandicates)) {
							// this left hand side var is a sinked var
							String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
							ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String.valueOf(e
									.getFrom()), sinkVar, sinkCandicates, true));
						} else {
							if (sinkCandicates.contains("?") && sinkCandicates.contains(".") && op.contains(".")) {
								String lastMethod = sinkCandicates.substring(sinkCandicates.lastIndexOf(".") + 1, sinkCandicates.length());
								String lastOp = op.substring(op.lastIndexOf(".") + 1, op.length());
								if (lastOp.equals(lastMethod)) {
									// this left hand side var is a sinked var
									String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
									ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String
											.valueOf(e.getFrom()), sinkVar, sinkCandicates, true));
								}
							}
						}
					}
				}
				for (String sinkCandicates : AnalysisUtil.getInstance().JS_INHERI_SINK_FUNCTION_LOCAL) {
					if (sinkCandicates.contains("()") || sinkCandicates.contains("[]")) {
						String sinkPrefix = sinkCandicates.substring(sinkCandicates.length() - 2, sinkCandicates.length());
						if (sinkPrefix.equals("[]")) {
							// put into a field
							String sinkTo = sinkCandicates.substring(0, sinkCandicates.length() - 2);
							if (sinkTo.equals(toStr) && op.contains("PUT_VALUE_TO_FIELD")) {
								// this left hand side var is a sinked var
								String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
								ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String.valueOf(e
										.getFrom()), sinkVar, sinkCandicates, false));
							}
						} else if (sinkPrefix.equals("()")) {
							// function/method
							if (op.equals(sinkCandicates)) {
								// this left hand side var is a sinked var
								String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
								ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String.valueOf(e
										.getFrom()), sinkVar, sinkCandicates, false));
							} else {
								if (sinkCandicates.contains("?") && sinkCandicates.contains(".") && op.contains(".")) {
									String lastMethod = sinkCandicates.substring(sinkCandicates.lastIndexOf(".") + 1, sinkCandicates.length());
									String lastOp = op.substring(op.lastIndexOf(".") + 1, op.length());
									if (lastOp.equals(lastMethod)) {
										// this left hand side var is a sinked var
										String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
										ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String
												.valueOf(e.getFrom()), sinkVar, sinkCandicates, false));
									}
								}
							}
						}
					} else {
						// function/method
						if (op.equals(sinkCandicates)) {
							// this left hand side var is a sinked var
							String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
							ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String.valueOf(e
									.getFrom()), sinkVar, sinkCandicates, false));
						} else {
							if (sinkCandicates.contains("?") && sinkCandicates.contains(".") && op.contains(".")) {
								String lastMethod = sinkCandicates.substring(sinkCandicates.lastIndexOf(".") + 1, sinkCandicates.length());
								String lastOp = op.substring(op.lastIndexOf(".") + 1, op.length());
								if (lastOp.equals(lastMethod)) {
									// this left hand side var is a sinked var
									String sinkVar = g.getFunc().getVarLocalNameMap().get(e.getFrom());
									ret.add(new SinkVar(e.getContainedFunction().getFileName(), e.getContainedFunction().getFuncName(), String
											.valueOf(e.getFrom()), sinkVar, sinkCandicates, false));
								}
							}
						}
					}
				}
			}
		}
		return ret;
	}

	public List<SourceVar> searchValueNumber(String file, String function, String line, String var, String annotate) {
		List<SourceVar> ret = new ArrayList<SourceVar>();
		if (var.contains(".")) {
			var = var.substring(0, var.indexOf(".")); // we only need the object name
		}
		for (SSAVarUseGraph g : ALL_VARIABLE_USE_GRAPHS) {
			if (g.getFileName().equals(file) && g.getFuncName().equals(function)) {
				for (SSAInst inst : g.getFunc().getInstructions().values()) {
					if (inst.getLineNo().equals(line)) {
						// First look at left hand side var
						int lhsVn = inst.getLhsVar();
						if (inst.getVarName(lhsVn) != null && inst.getVarName(lhsVn).equals(var)) {
							String lhsVar = g.getFunc().getVarLocalNameMap().get(lhsVn);
							ret.add(new SourceVar(file, function, String.valueOf(lhsVn), lhsVar, annotate));
						}
						// Then if it is empty, look at right hand side vars
						if (ret.size() == 0 && inst.getRhsVars() != null) {
							for (int vn : inst.getRhsVars()) {
								if (inst.getVarName(vn) != null && inst.getVarName(vn).equals(var)) {
									String vnVar = g.getFunc().getVarLocalNameMap().get(vn);
									ret.add(new SourceVar(file, function, String.valueOf(vn), vnVar, annotate));
								}
							}
						}
					}
				}
			}
		}

		return ret;
	}

	public String searchOriginalVar(String file, String function, String vn) {
		int valuenumber = Integer.valueOf(vn);
		for (SSAVarUseGraph g : ALL_VARIABLE_USE_GRAPHS) {
			if (g.getFileName().equals(file) && g.getFuncName().equals(function)) {
				SSAFunc func = g.getFunc();
				if (func.getVarLocalNameMap().containsKey(valuenumber)) {
					return func.getVarLocalNameMap().get(valuenumber);
				}
			}
		}

		return null;
	}

	private void recompute(ArrayList<SSAVarUseGraph> all) {
		// save for later implementation if necessary
		for (SSAVarUseGraph vug : all) {
			vug.recompute();
		}
	}

	private void dumpVariableUseGraph(SSAVarUseGraph vug, String path) {
		SSAUtil.getInstance().dumpVarUseGraph(path, vug);
	}

	private void dumpValueFlowGraph(SSAVarUseGraph vug, String path) {
		SSAUtil.getInstance().dumpVarFlowGraph(path, vug);
	}

	private void dumpFuncIR(SSAFunc func, String path) {
		SSAUtil.getInstance().dumpIR(path, func.getFuncName(), func.getIR());
	}

	private void parsePath(String path) {
		Calendar c1 = Calendar.getInstance();
		c1.setTime(new Date());

		// The input arg is the files' folder
		File dir = new File(path);
		String[] files = dir.list();
		for (String fileName : files) {
			try {
				if (_call_graph.isFileAccessed(fileName)) {
					parseFile(path, fileName);
				} else {
					File toDel = new File(path + fileName);
					toDel.delete();
				}
			} catch (ClassHierarchyException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
		sb.append("SSAParser:").append(diff).append(",").append(diffSec).append(",").append(diffMin).append(",").append(diffHours).append(",")
				.append(diffDays).append("\n");
		AnalysisUtil.getInstance().writeToFile(AnalysisUtil.getInstance().PERFORMANCE_FILE, sb.toString(), true);

		System.out.println("SSA parse: DONE!");
	}

	private void parseFile(String path, String filename) throws ClassHierarchyException, IOException {
		// use Rhino to parse JavaScript
		JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());

		// build a class hierarchy, for access to code info
		IClassHierarchy cha = JSCallGraphUtil.makeHierarchyForScripts(path + filename);

		// for constructing IRs
		IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();
		for (IClass klass : cha) {
			// ignore models of built-in JavaScript methods
			if (!klass.getName().toString().startsWith("Lprologue.js")) {
				// get the IMethod representing the code (the ¡®do¡¯ method)
				IMethod m = klass.getMethod(AstMethodReference.fnSelector);
				if (m != null) {
					String funcName = m.getDeclaringClass().getName().toString();
					funcName = funcName.substring(funcName.lastIndexOf("/") + 1);
					if (_call_graph.isProrotypeAccessed(filename, funcName)) {
						try {
							IR ir = factory.makeIR(m, Everywhere.EVERYWHERE, new SSAOptions());
							String funcname = ir.getMethod().getDeclaringClass().getName().toString();
							funcname = funcname.substring(funcname.lastIndexOf("/") + 1);
							// 1231 new added filter to improve performance
							if (_call_graph.isProrotypeAccessed(filename, funcname)) {
								ALL_FUNCTIONS.add(new SSAFunc(this._call_graph, filename, ir));
							}
						} catch (Exception e) {

						}
					}
				}
			}
		}
	}

	private void computeVariableUseGraphs() {
		for (SSAFunc func : ALL_FUNCTIONS) {
			ALL_VARIABLE_USE_GRAPHS.add(func.getVarUseGraph());
		}

		// now recompute after all var use graphs are ready
		recompute(ALL_VARIABLE_USE_GRAPHS);

		// this is for debug use
		if (AnalysisUtil.getInstance().IS_DUMP) {
			for (SSAVarUseGraph vug : ALL_VARIABLE_USE_GRAPHS) {
				dumpVariableUseGraph(vug, AnalysisUtil.getInstance().VARUSE_GRAPH_PATH);
				dumpValueFlowGraph(vug, AnalysisUtil.getInstance().VARFLOW_GRAPH_PATH);
				dumpFuncIR(vug.getFunc(), AnalysisUtil.getInstance().IR_PATH);
			}
		}
	}
}
