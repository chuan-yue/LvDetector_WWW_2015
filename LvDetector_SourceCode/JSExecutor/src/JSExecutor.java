import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JSExecutor {

	static String js_file = "C:\\Users\\rzhao\\Downloads\\T1\\test1.js";

	public static void main(String[] args) {
		// create a script engine manager
		ScriptEngineManager factory = new ScriptEngineManager();
		// create JavaScript engine
		ScriptEngine engine = factory.getEngineByName("JavaScript");

		String content = readFileContent(js_file);

		String str = "print(" + content + ")";

		// evaluate JavaScript code from given file - specified by first argument
		try {
			System.out.println(engine.eval(str));
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String readFileContent(String fileName) {
		File file = new File(fileName);
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String content = "";
		StringBuilder sb = new StringBuilder();
		while (content != null) {
			try {
				content = bf.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (content == null) {
				break;
			}
			sb.append(content.trim());
		}
		try {
			bf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}
}
