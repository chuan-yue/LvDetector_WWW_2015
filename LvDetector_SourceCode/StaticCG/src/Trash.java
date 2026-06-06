public class Trash {
	/*
	public static final int DEFAULT_TIMEOUT = 120;

	public static class CGBuilderResult {
		public long construction_time;

		public JSCFABuilder builder;

		public PointerAnalysis pa;

		public CallGraph cg;
	}

	public static void try0() throws IllegalArgumentException, IOException, CancelException, WalaException {
		// use Rhino to parse JavaScript
		JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());

		// build a class hierarchy, for access to code info
		IClassHierarchy cha = JSCallGraphUtil.makeHierarchyForScripts(BASE_PATH + "cg1.js", BASE_PATH + "cg2.js");

		// for constructing IRs
		for (IClass klass : cha) {
			// ignore models of built-in JavaScript methods
			if (!klass.getName().toString().startsWith("Lprologue.js")) {
				// get the IMethod representing the code (the ¡®do¡¯ method)
				IMethod m = klass.getMethod(AstMethodReference.fnSelector);
				if (m != null) {
					String funcName = m.getDeclaringClass().getName().toString();
					funcName = funcName.substring(funcName.lastIndexOf("/") + 1);
					System.out.println(funcName);

					MethodReference mr = m.getReference();
					System.out.println(mr.getDeclaringClass().getName().toString());

				}
			}
		}
	}

	public static void try2() throws IllegalArgumentException, IOException, CancelException, WalaException {
		String src = BASE_PATH + "cg2.js";

		// if src is a JS file, build trivial wrapper HTML file
		if (src.endsWith(".js")) {
			File tmpFile = new File(BASE_PATH + "HTMLCGBuilder.html");
			FileUtil.writeFile(tmpFile, "<html>" + "<head>" + "<title></title>" + "<script src=\"" + src + "\" type='text/javascript'></script>"
					+ "</head>" + "<body>" + "</body>" + "</html>");
			src = tmpFile.getAbsolutePath();
			System.out.println(src);
		}

		int timeout = DEFAULT_TIMEOUT;

		// suppress debug output
		JavaScriptFunctionDotCallTargetSelector.WARN_ABOUT_IMPRECISE_CALLGRAPH = false;

		// build call graph
		CGBuilderResult res = buildHTMLCG(src, timeout, CGBuilderType.ONE_CFA);

		if (res.construction_time == -1)
			System.out.println("TIMED OUT!");
		else
			System.out.println("Call graph construction took " + res.construction_time / 1000.0 + " seconds!");

		dumpCG(res.cg);
		System.out.println("\n\n\n");

		for (CGNode node : res.cg) {
			IClass klass = node.getMethod().getDeclaringClass();
			if (!klass.getName().toString().startsWith("Lprologue.js") && !klass.getName().toString().startsWith("Lpreamble.js")) {
				IMethod m = klass.getMethod(AstMethodReference.fnSelector);
				if (m != null) {
					String funcName = m.getDeclaringClass().getName().toString();
					//funcName = funcName.substring(funcName.lastIndexOf("/") + 1);
					System.out.println(funcName);
				}
			}
		}
	}

	public static CGBuilderResult buildHTMLCG(String src, int timeout, CGBuilderType builderType) throws ClassHierarchyException, IOException {
		CGBuilderResult res = new CGBuilderResult();
		URL url = null;
		try {
			url = toUrl(src);
		} catch (MalformedURLException e1) {
			System.out.println("Could not find page to analyse: " + src);
		}
		com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		JSCFABuilder builder = null;
		try {
			builder = JSCallGraphBuilderUtil.makeHTMLCGBuilder(url, builderType);
			// TODO we need to find a better way to do this ContextSelector delegation;
			// the code below belongs somewhere else!!!
			// the bound of 4 is what is needed to pass our current framework tests
			builder.setContextSelector(new RecursionCheckContextSelector(builder.getContextSelector()));
			ProgressMaster master = ProgressMaster.make(new NullProgressMonitor());
			if (timeout > 0) {
				master.setMillisPerWorkItem(timeout * 1000);
				master.beginTask("runSolver", 1);
			}
			long start = System.currentTimeMillis();
			CallGraph cg = timeout > 0 ? builder.makeCallGraph(builder.getOptions(), master) : builder.makeCallGraph(builder.getOptions());
			long end = System.currentTimeMillis();
			master.done();
			res.construction_time = (end - start);
			res.cg = cg;
			res.pa = builder.getPointerAnalysis();
			res.builder = builder;
			return res;
		} catch (CallGraphBuilderCancelException e) {
			res.construction_time = -1;
			res.cg = e.getPartialCallGraph();
			res.pa = e.getPartialPointerAnalysis();
			res.builder = builder;
			return res;
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	private static URL toUrl(String src) throws MalformedURLException {
		// first try interpreting as local file name, if that doesn't work just
		// assume it's a URL
		try {
			File f = (new FileProvider()).getFileFromClassLoader(src, HTMLCGBuilder.class.getClassLoader());
			URL url = f.toURI().toURL();
			return url;
		} catch (FileNotFoundException fnfe) {
			return new URL(src);
		}
	}
	*/
}
