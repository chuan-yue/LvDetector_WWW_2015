import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MakeScript {

	private static void writeFileContent(String filename, String content) throws IOException {
		File file = new File(filename);

		BufferedWriter bw = new BufferedWriter(new FileWriter(file));

		bw.write(content);
		bw.close();
	}

	private static void checkAndCreateFolder(String folderPath) {
		File folder = new File(folderPath);
		if (folder.exists() && folder.isDirectory()) {
			return;
		}
		folder.mkdirs();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// args[0] should be the folder containing In and Out
		String fPath = System.getProperty("user.dir");
		String in = fPath + File.separator + "0_In" + File.separator;
		String format1 = fPath + File.separator + "1_Format1" + File.separator;
		String funcExp = fPath + File.separator + "2_InsertFunExp" + File.separator;
		String format2 = fPath + File.separator + "3_Format2" + File.separator;
		String clstPrt = fPath + File.separator + "4_GetAndInsertCallsitePrint" + File.separator;
		String format3 = fPath + File.separator + "5_Format3" + File.separator;
		String funcPrt = fPath + File.separator + "6_InsertFunPrint" + File.separator;
		String finalTest = fPath + File.separator + "7_FinalTest" + File.separator;

		// Prepare the folders
		checkAndCreateFolder(format1);
		checkAndCreateFolder(funcExp);
		checkAndCreateFolder(format2);
		checkAndCreateFolder(clstPrt);
		checkAndCreateFolder(format3);
		checkAndCreateFolder(funcPrt);
		checkAndCreateFolder(finalTest);

		// Get js files under folderIn
		File folder = new File(in);

		if (!folder.isDirectory()) {
			return;
		}

		StringBuffer sFormat1 = new StringBuffer();
		StringBuffer sFuncExp = new StringBuffer();
		StringBuffer sFormat2 = new StringBuffer();
		StringBuffer sClstPrt = new StringBuffer();
		StringBuffer sFormat3 = new StringBuffer();
		StringBuffer sFuncPrt = new StringBuffer();
		StringBuffer sFinalTest = new StringBuffer();

		String[] files = folder.list();
		for (int i = 0; i < files.length; i++) {

			// Filter out other files, left js files
			String suf = files[i].substring(files[i].lastIndexOf(".") + 1, files[i].length());
			if (!(suf.equals("js") || suf.equals("jsm"))) {
				continue;
			}

			// 1
			sFormat1.append("java -jar compiler.jar --compilation_level=WHITESPACE_ONLY --formatting=pretty_print --language_in=ECMASCRIPT5 " + in
					+ files[i] + " > " + format1 + files[i] + "\n");
			// --formatting=pretty_print
			// --language_in=ECMASCRIPT5
			// --compilation_level=WHITESPACE_ONLY
			// --summary_detail_level=1

			// 2
			sFuncExp.append("java -jar CodeRewriting.jar " + format1 + files[i] + " " + funcExp + files[i] + " 2\n");

			// 3
			sFormat2.append("java -jar compiler.jar --compilation_level=WHITESPACE_ONLY --formatting=pretty_print --language_in=ECMASCRIPT5 "
					+ funcExp + files[i] + " > " + format2 + files[i] + "\n");

			// 4
			sClstPrt.append("java -jar CodeRewriting.jar " + format2 + files[i] + " " + clstPrt + files[i] + " 4\n");

			// 5
			sFormat3.append("java -jar compiler.jar --compilation_level=WHITESPACE_ONLY --formatting=pretty_print --language_in=ECMASCRIPT5 "
					+ clstPrt + files[i] + " > " + format3 + files[i] + "\n");

			// 6
			sFuncPrt.append("java -jar CodeRewriting.jar " + format3 + files[i] + " " + funcPrt + files[i] + " 5\n");

			// 7
			sFinalTest.append("java -jar compiler.jar --compilation_level=WHITESPACE_ONLY --formatting=pretty_print --language_in=ECMASCRIPT5 "
					+ funcPrt + files[i] + " > " + finalTest + files[i] + "\n");
		}
		try {
			writeFileContent("1_Format1.bat", sFormat1.toString());
			writeFileContent("2_FuncExp.bat", sFuncExp.toString());
			writeFileContent("3_Format2.bat", sFormat2.toString());
			writeFileContent("4_ClstPrt.bat", sClstPrt.toString());
			writeFileContent("5_Format3.bat", sFormat3.toString());
			writeFileContent("6_FuncPrt.bat", sFuncPrt.toString());
			writeFileContent("7_FinalTest.bat", sFinalTest.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
