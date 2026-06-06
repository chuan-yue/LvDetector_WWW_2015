import java.io.File;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public class JJTest {

	public static void main(String[] args) {
		ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        File f = new File("test.txt");
        // expose File object as variable to script
        engine.put("file", f);

        // evaluate a script string. The script accesses "file" 
        // variable and calls method on it
        try {
			engine.eval("print(file.getAbsolutePath())");
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
