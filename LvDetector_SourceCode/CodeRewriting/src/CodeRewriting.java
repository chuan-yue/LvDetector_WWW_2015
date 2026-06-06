/**
 * All rights reserved
 * @author rzhao
 * CS Department
 * UCCS 2013
 */

import java.io.File;
import java.io.IOException;

public class CodeRewriting {
	@Deprecated
	static String folderIn = "C:\\Users\\rzhao\\Downloads\\roboform_lite_password_manager-2.1.0-fx\\chrome\\xpirftoolbar\\content\\";
	@Deprecated
	static String folderOut = "C:\\Users\\rzhao\\Downloads\\roboform_lite_password_manager-2.1.0-fx\\chrome\\xpirftoolbar\\output\\";

	private final static int REWRITE_FUNCTION_EXPRESSION = 2;
	private final static int GET_AND_INSERT_CALLSITE_PRINTING = 4;
	private final static int INSERT_FUNCTION_PRINTING = 5;

	/**
	 * @param tmp
	 * @param type
	 * @return
	 */
	private static String processString(String tmp, String fileName, int type) {
		// Eliminate unnecessary whitespces
		// String source = utility.getInstance().reFormatSource(tmp);
		// System.out.println(source);

		String output = null;
		switch (type) {
		case REWRITE_FUNCTION_EXPRESSION: {
			// Rewrite all function expressions
			tmp = tmp.replace("const ", "var ");
			output = Util.getInstance().rewriteExpression(tmp, fileName); // source
		}
			break;
		case GET_AND_INSERT_CALLSITE_PRINTING: {
			// Pure static analysis on call site information

			// Insert printing for each call site in the function
			output = Util.getInstance()
					.insertCallsitePrinting(tmp, fileName);
		}
			break;
		case INSERT_FUNCTION_PRINTING: {
			// Insert printing at the beginning of function body
			output = Util.getInstance().insertFuncPrinting(tmp, fileName);
		}
			break;
		default:
			// System.out.println(output);
			break;
		}

		return output;
	}

	/**
	 * @param source
	 * @param dest
	 * @param type
	 * @throws IOException
	 */
	private static void processFile(String source, String dest, int type)
			throws IOException {
		String tmp = null;
		String fileName = null;
		// Read file
		try {
			tmp = Util.getInstance().readFileContent(source);
			fileName = Util.getInstance().getFileName(source);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		
		// System.out.println(tmp);
		String outputI = processString(tmp, fileName, type);

		// Output to folderOut
		try {
			Util.getInstance().writeFileContent(dest, outputI);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Deprecated
	private static void prcFolder(int type) {

		// Get js files under folderIn
		File folder = new File(folderIn);

		if (!folder.isDirectory()) {
			return;
		}

		String[] files = folder.list();
		for (int i = 0; i < files.length; i++) {

			// Filter out other files, left js files
			if (!(files[i].substring(files[i].length() - 3, files[i].length())
					.equals(".js"))) {
				continue;
			}

			System.out.println("Begin process: " + files[i]);

			try {
				processFile(files[i], "stes", type);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			System.out.println("Success: " + files[i]);
		}
	}

	/**
	 * @param source
	 * @param dest
	 * @param type
	 */
	private static void procFile(String source, String dest, int type) {
		try {
			processFile(source, dest, type);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Util.getInstance().getBrowserType();
		
		int switcher = 0;
		if (switcher == 0) {
			procFile(args[0], args[1], Integer.parseInt(args[2]));
		} else {
			procFile("C:\\Users\\rzhao\\Downloads\\closure\\about.js",
					"C:\\Users\\rzhao\\Downloads\\closure\\out.js", 5);
		}

	}
}
