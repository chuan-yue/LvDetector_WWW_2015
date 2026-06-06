/**
 * All rights reserved
 * @author rzhao
 * CS Department
 * UCCS 2013
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

import com.google.javascript.jscomp.CallGraph;
import com.google.javascript.jscomp.CodePrinter;
import com.google.javascript.jscomp.CallGraph.Callsite;
import com.google.javascript.jscomp.CallGraph.Function;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

public class Util {

	private static Util _instance = null;

	private final static int INSERT_ENTERFUNC = 0;
	private final static int INSERT_DECLARATION = 1;
	private final static int INSERT_CALLSITE = 2;

	private final static String MY_SEP = "@@";

	// Used to randomly generate function name
	private static final String LETTER_CHAR = "abcdefghijklmnopqrstuvwxyz";

	private String BROWSER_TYPE;

	/**
	 * 
	 */
	private Util() {

	}

	/**
	 * 
	 * @return
	 */
	public static synchronized Util getInstance() {
		if (_instance == null) {
			_instance = new Util();
		}
		return _instance;
	}

	public String getFileName(String filePath) throws IOException {
		File file = new File(filePath);
		return file.getName();
	}

	/**
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public String readFileContent(String filePath) throws IOException {

		File file = new File(filePath);

		BufferedReader bf = new BufferedReader(new FileReader(file));

		String content = "";
		String ls = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();

		while (content != null) {
			content = bf.readLine();

			if (content == null) {
				break;
			}

			sb.append(content);
			sb.append(ls);
		}

		bf.close();
		return sb.toString();
	}

	/**
	 * 
	 * @param filename
	 * @param content
	 * @throws IOException
	 */
	public void writeFileContent(String filename, String content) throws IOException {
		File file = new File(filename);

		BufferedWriter bw = new BufferedWriter(new FileWriter(file));

		bw.write(content);
		bw.close();
	}

	/**
	 * 
	 * @param filename
	 * @param content
	 */
	public void appendToFile(String filename, String content) {
		try {
			RandomAccessFile rf = new RandomAccessFile(filename, "rw");
			rf.seek(rf.length());
			rf.writeBytes(content);
			rf.close();
		} catch (IOException e) {
			System.out.println("Append to file failes: " + filename);
		}
	}

	/**
	 * 
	 * @param length
	 * @return
	 */
	private String generateRandomString(int length) {
		StringBuffer sb = new StringBuffer();
		Random random = new Random();
		for (int i = 0; i < length; i++) {
			sb.append(LETTER_CHAR.charAt(random.nextInt(LETTER_CHAR.length())));
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	private Collection<Function> getAllFunctionsFromSource(String source) {
		MyGraph g = new MyGraph();
		CallGraph callgraph = g.compileAndRunForward(source);
		return callgraph.getAllFunctions();
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	private ArrayList<NameOffsetPair> getCallsitePosition(String source, String fileName) {
		// To save callsite name and offsets
		ArrayList<NameOffsetPair> offsets = new ArrayList<NameOffsetPair>();

		// To save static call analysis result
		StringBuffer sb = new StringBuffer();

		Collection<Function> functions = getAllFunctionsFromSource(source);
		Iterator<Function> itF = functions.iterator();
		while (itF.hasNext()) {

			// Get function
			Function tmpF = itF.next();
			if (tmpF.isMain()) {
				continue;
			}

			// For each function
			String fName = tmpF.getName();
			String fPara = tmpF.getParameters().toString();
			// Get callsites
			Collection<Callsite> callsites = tmpF.getCallsitesInFunction();
			Iterator<Callsite> itC = callsites.iterator();
			while (itC.hasNext()) {
				// For each callsite
				Callsite tmpC = itC.next();

				// First we need to relate each callsite to a unique name
				String callsiteName = tmpC.getName();
				// String callsitePara = tmpC.getPara();

				// Second we need to get this callsite's position
				// to print this callsite just before it is invoked
				Node tmpNode = tmpC.getAstNode();
				while (tmpNode.getParent().getType() != Token.BLOCK) {
					tmpNode = tmpNode.getParent();
				}
				int offset = tmpNode.getSourceOffset();
				int lineno = tmpNode.getLineno();

				if (callsiteName == null || fName == null) {
					continue;
				}

				// Generate a string array for this callsite
				String[] paraSet = { fName, callsiteName, fileName, String.valueOf(lineno) };

				// System.out.println(uniqueName + ":" + offset);
				offsets.add(new NameOffsetPair(paraSet, offset, lineno));

				// Here to organize static call analysis result
				// callerName[callerPara]->calleeName[calleePara]:uniqueName:possibleTargets
				// possibleTarget is
				// functionName[functionPara]|functionName[functionPara]...

				sb.append(fName);
				// sb.append(fPara);
				sb.append(MY_SEP);
				sb.append(callsiteName);
				// sb.append(callsitePara);
				sb.append(MY_SEP);
				sb.append(fileName);
				sb.append(MY_SEP);
				sb.append(lineno);

				// For eachcall site get all possible target functions
				// Collection<Function> targets = tmpC.getPossibleTargets();
				// Iterator<Function> it = targets.iterator();
				// while (it.hasNext()) {
				// Function tmpT = it.next();
				// String tmpTName = tmpT.getName();
				// String tmpTPara = tmpT.getParameters().toString();

				// sb.append(tmpTName);
				// sb.append(tmpTPara);

				// if (it.hasNext()) {
				// sb.append("|");
				// }
				// }

				sb.append("\n");
			}
		}

		// Write static call analysis result to file
		String fPath = System.getProperty("user.dir");
		fPath = fPath.substring(0, fPath.lastIndexOf(File.separator));
		String fullPath = fPath + File.separator + "2_CallgraphInput" + File.separator + "sum_callsite.log";
		appendToFile(fullPath, sb.toString());

		return offsets;
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	private ArrayList<NameOffsetPair> getFunBodyPosition(String source, String fileName) {
		ArrayList<NameOffsetPair> offsets = new ArrayList<NameOffsetPair>();

		Collection<Function> functions = getAllFunctionsFromSource(source);
		Iterator<Function> itF = functions.iterator();
		while (itF.hasNext()) {

			// Get function
			Function tmpF = itF.next();
			if (tmpF.isMain()) {
				continue;
			}
			if (tmpF.getName() == null || tmpF.getName().length() == 0) {
				continue;
			}

			// Get function body's position
			int start = tmpF.getBodyNode().getSourceOffset() + 1;
			// System.out.println(tmpF.getName() + ":" + start);
			// String nStr = new
			// CodePrinter.Builder(tmpF.getBodyNode()).build();
			// int end = start + nStr.length();
			// System.out.println(tmpF.getName() + ":" + end);

			String[] paraSet = { tmpF.getName(), fileName };

			offsets.add(new NameOffsetPair(paraSet, start, -1));
		}

		return offsets;
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	private ArrayList<NameOffsetPair> getFuncNamePosition(String source, String fileName) {
		ArrayList<NameOffsetPair> offsets = new ArrayList<NameOffsetPair>();

		StringBuffer sb = new StringBuffer();

		Collection<Function> functions = getAllFunctionsFromSource(source);
		Iterator<Function> itF = functions.iterator();
		while (itF.hasNext()) {

			// Get function
			Function tmpF = itF.next();
			if (tmpF.isMain()) {
				continue;
			}

			// Get function node
			Node func = tmpF.getAstNode();

			// Get functin's parent
			// Node para = func.getParent();
			// if (para.getType() != Token.NAME && para.getType() !=
			// Token.ASSIGN
			// && para.getType() != Token.STRING_KEY) {
			// continue;
			// }

			String funName = tmpF.getName();

			// if (funName == null || funName.length() == 0) {
			// funName = generateRandomString(8);
			// } else {
			// funName = tmpF.getName();
			// }
			// funName = ((funName != null) ? transformName(funName) : null);

			// If already has such name as declaration like "function XXX (){}",
			// skip, else generate a random name
			if (func.getChildAtIndex(0).getString().length() > 0) {
				sb.append(funName);
				sb.append(MY_SEP);
				sb.append(fileName);
				sb.append("\n");
				continue;
			} else {
				funName = generateRandomString(10);
				sb.append(funName);
				sb.append(MY_SEP);
				sb.append(fileName);
				sb.append("\n");
			}

			// Get function parameter's position
			int offset = func.getFirstChild().getNext().getSourceOffset();

			// System.out.println(funName + ":" + offset);

			String[] paraSet = { funName };
			offsets.add(new NameOffsetPair(paraSet, offset, -1));
		}

		// Write function analysis result to file
		String fPath = System.getProperty("user.dir");
		fPath = fPath.substring(0, fPath.lastIndexOf(File.separator));
		String sum_func = fPath + File.separator + "2_CallgraphInput" + File.separator + "sum_func.log";
		appendToFile(sum_func, sb.toString());

		return offsets;
	}

	/**
	 * 
	 * @param offsets
	 */
	private void sort(ArrayList<NameOffsetPair> offsets) {
		Comparator<NameOffsetPair> comp = new Comparator<NameOffsetPair>() {
			public int compare(NameOffsetPair o1, NameOffsetPair o2) {
				return (int) (o2.getOffset() - o1.getOffset());
			}
		};
		Collections.sort(offsets, comp);
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	private String transformName(String name) {
		return name.replace(" ", "\\ ").replace("+", "\\+").replace("-", "\\-").replace("@", "\\@").replace("=", "\\=").replace("\"", "\\\"")
				.replace("(", "\\(").replace(")", "\\)").replace("{", "\\{").replace("}", "\\}").replace("[", "\\[").replace("]", "\\]");
		/*
		 * return name.replace(".", "DOT").replace(" ", "BLANK") .replace("+",
		 * "PLUS").replace("-", "MINUS").replace("@", "AT") .replace("=",
		 * "EQUAL").replace("_", "UNDER").replace("\"", "\\\"");
		 */
	}

	public String transformNameBack(String name) {
		return name.replace("\\ ", " ").replace("\\+", "+").replace("\\-", "-").replace("\\@", "@").replace("\\=", "=").replace("\\\"", "\"")
				.replace("\\(", "(").replace("\\)", ")").replace("\\{", "{").replace("\\}", "}").replace("\\[", "[").replace("\\]", "]");
		/*
		 * return name.replace("DOT", ".").replace("BLANK", " ")
		 * .replace("PLUS", "+").replace("MINUS", "-").replace("AT", "@")
		 * .replace("EQUAL", "=").replace("UNDER", "_").replace("\\\"", "\"");
		 */
	}

	public void insertStringInFile(File inFile, int lineno, String lineToBeInserted) {
		try {
			// Tmp file
			File outFile = File.createTempFile("name", ".tmp");
			// Input
			FileInputStream fis = new FileInputStream(inFile);
			BufferedReader in = new BufferedReader(new InputStreamReader(fis));
			// Output
			FileOutputStream fos = new FileOutputStream(outFile);
			PrintWriter out = new PrintWriter(fos);
			// Save a line
			String thisLine;
			// Start from first line
			int i = 1;
			while ((thisLine = in.readLine()) != null) {
				// Insert
				if (i == lineno) {
					out.println(lineToBeInserted);
				}
				// Output
				out.println(thisLine);
				//
				i++;
			}
			out.flush();
			out.close();
			in.close();
			// Delete input file
			inFile.delete();
			// Rename temp file to input file
			outFile.renameTo(inFile);
		} catch (Exception e) {
			System.out.println("Inserting error:" + inFile.getName());
		}
	}

	/**
	 * 
	 * @param source
	 * @param offsets
	 * @param type
	 * @return
	 */
	private String insert(String source, ArrayList<NameOffsetPair> offsets, int type) {
		StringBuffer tmpS = new StringBuffer(source);

		sort(offsets);

		Iterator<NameOffsetPair> it = offsets.iterator();
		while (it.hasNext()) {
			NameOffsetPair tmp = it.next();

			int o = tmp.getOffset();
			int l = tmp.getLineNo();
			// System.out.println(tmp.getName() + ":" + o);
			String s = null;
			String[] paraSet = tmp.getParaSet();

			switch (type) {
				case INSERT_ENTERFUNC: {
					// paraSet : { tmpF.getName(), fileName };
					if (this.BROWSER_TYPE.equals("Firefox")) {
						s = "try{var callerProto = " + paraSet[0] + ".caller.prototype;" + "var calleeProto = " + paraSet[0] + ".prototype;"
								+ "if(callerProto != null && calleeProto != null){" + "dump(\"PROTO" + MY_SEP + "\" + " + paraSet[0]
								+ ".caller.name + \"" + MY_SEP + "\" + callerProto.constructor.name + \"\\n\");" + "dump(\"PROTO" + MY_SEP
								+ paraSet[0] + MY_SEP + "\" + calleeProto.constructor.name + \"\\n\");" + "dump(" + "\"FUNC" + MY_SEP + "\" + "
								+ paraSet[0] + ".caller.name+" + "\"" + MY_SEP + "\"+" + paraSet[0] + ".name + \"" + MY_SEP + "" + paraSet[1]
								+ "\\n\");" + "}}catch(error){}";
					} else if (this.BROWSER_TYPE.equals("Chrome")) {
						s = "try{var callerProto = " + paraSet[0] + ".caller.prototype;" + "var calleeProto = " + paraSet[0] + ".prototype;"
								+ "if(callerProto != null && calleeProto != null){" + "console.log(\"PROTO" + MY_SEP + "\" + " + paraSet[0]
								+ ".caller.name + \"" + MY_SEP + "\" + callerProto.constructor.name);" + "console.log(\"PROTO" + MY_SEP
								+ paraSet[0] + MY_SEP + "\" + calleeProto.constructor.name);" + "console.log(" + "\"FUNC" + MY_SEP
								+ "\" + " + paraSet[0] + ".caller.name+" + "\"" + MY_SEP + "\"+" + paraSet[0] + ".name + \"" + MY_SEP + ""
								+ paraSet[1] + "\");" + "}}catch(error){}";
					}
					tmpS.insert(o, s);
				}
					break;
				case INSERT_DECLARATION: {
					s = " " + paraSet[0];
					tmpS.insert(o, s);
				}
					break;
				case INSERT_CALLSITE: {
					// paraSet = { fName, callsiteName, fileName, lineno};
					if (this.BROWSER_TYPE.equals("Firefox")) {
						s = "try{var callerProto = " + paraSet[0] + ".prototype;" + "var calleeProto = " + paraSet[1] + ".prototype;"
								+ "if(callerProto != null && calleeProto != null){" + "dump(\"PROTO" + MY_SEP + paraSet[0] + MY_SEP
								+ "\" + callerProto.constructor.name + \"\\n\");" + "dump(\"PROTO" + MY_SEP + transformName(paraSet[1]) + MY_SEP
								+ "\" + calleeProto.constructor.name + \"\\n\");" + "dump(\"CALLSITE" + MY_SEP + paraSet[0] + MY_SEP
								+ transformName(paraSet[1]) + MY_SEP + paraSet[2] + MY_SEP + paraSet[3] + "\\n\");" + "}}catch(error){}";
					} else if (this.BROWSER_TYPE.equals("Chrome")) {
						s = "try{var callerProto = " + paraSet[0] + ".prototype;" + "var calleeProto = " + paraSet[1] + ".prototype;"
								+ "if(callerProto != null && calleeProto != null){" + "console.log(\"PROTO" + MY_SEP + paraSet[0] + MY_SEP
								+ "\" + callerProto.constructor.name);" + "console.log(\"PROTO" + MY_SEP + transformName(paraSet[1])
								+ MY_SEP + "\" + calleeProto.constructor.name);" + "console.log(\"CALLSITE" + MY_SEP + paraSet[0] + MY_SEP
								+ transformName(paraSet[1]) + MY_SEP + paraSet[2] + MY_SEP + paraSet[3] + "\");" + "}}catch(error){}";
					}
					tmpS.insert(o, s);
				}
					break;
				default:
					break;
			}

		}

		return tmpS.toString();
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	public String rewriteExpression(String source, String fileName) {
		// Function Name Analysis
		ArrayList<NameOffsetPair> offsets = getFuncNamePosition(source, fileName);

		// Insert Name
		return insert(source, offsets, INSERT_DECLARATION);
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	public String insertFuncPrinting(String source, String fileName) {
		// Function Body Analysis
		ArrayList<NameOffsetPair> offsets = getFunBodyPosition(source, fileName);

		// Insert Code
		return insert(source, offsets, INSERT_ENTERFUNC);
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	public String reFormatSource(String source) {
		String ret = null;
		MyGraph g = new MyGraph();
		CallGraph callgraph = g.compileAndRunForward(source);
		Collection<Function> functions = callgraph.getAllFunctions();

		Iterator<Function> itF = functions.iterator();
		while (itF.hasNext()) {

			// Get function
			Function tmpF = itF.next();
			if (tmpF.isMain()) {
				ret = new CodePrinter.Builder(tmpF.getAstNode()).build();
				break;
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	public String insertCallsitePrinting(String source, String fileName) {
		// Callsite analysis
		ArrayList<NameOffsetPair> offsets = getCallsitePosition(source, fileName);

		// Insert Code
		return insert(source, offsets, INSERT_CALLSITE);
	}

	public void getBrowserType() {
		String path = System.getProperty("user.dir");
		path = path.substring(0, path.lastIndexOf(File.separator));
		String fullPath = path + File.separator + "0_Configure" + File.separator + "browsertype.conf";
		try {
			this.BROWSER_TYPE = readFileContent(fullPath).trim();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
