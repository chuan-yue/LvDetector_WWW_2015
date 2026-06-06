package extanalysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AnalysisUtil {
	private static AnalysisUtil _ext_util_instance = null;

	public AnalysisUtil() {
	}

	public static AnalysisUtil getInstance() {
		if (_ext_util_instance == null) {
			_ext_util_instance = new AnalysisUtil();
		}
		return _ext_util_instance;
	}

	/* flag */
	public final boolean IS_DUMP = true;
	public final boolean IS_SAFE_COMPUTE_TOPO_ORDER = true;
	public final boolean IS_VARUSEGRAPH_RECOMPUTE = true;
	public final boolean IS_FILER_WRONG_FIELD = true;

	/* Input files */
	// this is for call graph
	public String SUM_FUNC_LOG;
	public String SUM_CALLSITE_LOG;
	public String SUM_RUMTIME_LOG;
	// this is JS source code path
	public String JS_SOURCE_PATH;
	// this is sensitive variable configure file
	public String INIT_VAR_CONF_FILE;
	// this is encryption and decryption function configure file
	public String ENCDEC_FUNC_CONF_FILE;
	// this is browser type configure file
	public String BORWSER_TYPE_CONF_FILE;
	// this is skip function configure file
	public String SKIP_FUNC_CONF_FILE;
	// this is message receiver configure file
	public String MESS_REC_CONF_FILE;

	/* Output files */
	// this is for call graph
	public String CALL_DOT_FILE;
	public String CALL_RESULT_FILE;
	// this is for SSA parser
	public String VARUSE_GRAPH_PATH;
	public String VARFLOW_GRAPH_PATH;
	public String IR_PATH;
	// this is for function transitive analysis
	public String VAR_LOCAL_TRANSI_GRAPH_PATH;
	// this is for global transitive analysis
	public String VAR_GLOBAL_TRANSI_FILE;
	public String VAR_GLOBAL_RELATION_ELEM;
	// this is for the whole program result
	public String FINAL_RESULT_FILE;
	public String PERFORMANCE_FILE;

	// Store sensitive variables
	// file, function, line, var
	public HashMap<String[], String> _init_vars = new HashMap<String[], String>();
	// Store encryption decryption functions
	public HashMap<String[], String> _enc_funcs = new HashMap<String[], String>();
	// Store browser type
	public String _browser_type;
	// Store function not need to process
	public List<String[]> _skip_funcs = new ArrayList<String[]>();

	// Store some functions need to process additionally
	public List<String[]> _need_further_process_funcs = new ArrayList<String[]>();

	// Store message receiver in JS code
	public List<String[]> _message_receiver = new ArrayList<String[]>();

	public void setupPaths(String path) {
		/* Input */
		// call graph
		SUM_FUNC_LOG = path + "2_CallgraphInput" + File.separator + "sum_func.log";
		SUM_CALLSITE_LOG = path + "2_CallgraphInput" + File.separator + "sum_callsite.log";
		SUM_RUMTIME_LOG = path + "2_CallgraphInput" + File.separator + "sum_runtime.log";
		// JS source path
		JS_SOURCE_PATH = path + "1_CallgraphRewriting" + File.separator + "3_Format2" + File.separator;
		// sensitive var
		INIT_VAR_CONF_FILE = path + "0_Configure" + File.separator + "sensitivevar.conf";
		// enc dec function
		ENCDEC_FUNC_CONF_FILE = path + "0_Configure" + File.separator + "cryptofunc.conf";
		// browser type
		BORWSER_TYPE_CONF_FILE = path + "0_Configure" + File.separator + "browsertype.conf";
		// skip functions
		SKIP_FUNC_CONF_FILE = path + "0_Configure" + File.separator + "skipfunc.conf";
		// message receiver
		MESS_REC_CONF_FILE = path + "0_Configure" + File.separator + "messagerec.conf";

		/* Output */
		// call graph
		createFolder(path + "3_CallgraphOutput");
		CALL_DOT_FILE = path + "3_CallgraphOutput" + File.separator + "callgraph.dot";
		CALL_RESULT_FILE = path + "3_CallgraphOutput" + File.separator + "call_result.csv";
		// SSA parser results
		createFolder(path + "4_VarUseGraph");
		VARUSE_GRAPH_PATH = path + "4_VarUseGraph" + File.separator;
		createFolder(path + "4_VarFlowGraph");
		VARFLOW_GRAPH_PATH = path + "4_VarFlowGraph" + File.separator;
		createFolder(path + "4_Ir");
		IR_PATH = path + "4_Ir" + File.separator;
		// local transitive analysis results
		createFolder(path + "5_VarTransiGraph");
		VAR_LOCAL_TRANSI_GRAPH_PATH = path + "5_VarTransiGraph" + File.separator;
		// global transitive analysis result
		createFolder(path + "6_GlobalTransitive");
		VAR_GLOBAL_TRANSI_FILE = path + "6_GlobalTransitive" + File.separator + "GlobalSum.txt";
		VAR_GLOBAL_RELATION_ELEM = path + "6_GlobalTransitive" + File.separator + "RelationElem.txt";
		// final whole program result
		createFolder(path + "7_FinalResult");
		FINAL_RESULT_FILE = path + "7_FinalResult" + File.separator + "FinalResult.txt";
		PERFORMANCE_FILE = path + "7_FinalResult" + File.separator + "Performance.txt";

		parseVarConf();
		parseCryptoFuncConf();
		parseBrowserConf();
		parseSkipFuncConf();
		parserMessageReceiverConf();
	}

	private void parseVarConf() {
		String content = readToString(INIT_VAR_CONF_FILE).trim();
		String[] vars = content.split(";");
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].length() > 0) {
				String[] tmp = vars[i].trim().split(":");
				String[] strs = tmp[1].split(",");
				_init_vars.put(strs, tmp[0]);
			}
		}
	}

	private void parseCryptoFuncConf() {
		String content = readToString(ENCDEC_FUNC_CONF_FILE).trim();
		String[] callsiteList = content.split(";");
		for (int i = 0; i < callsiteList.length; i++) {
			if (callsiteList[i].length() > 0) {
				String[] strs = callsiteList[i].trim().split(":");
				String[] tmp = strs[0].split(",");
				if (tmp.length == 2) {
					_enc_funcs.put(tmp, strs[1]);
				} else if (tmp.length == 3) {
					_enc_funcs.put(new String[] { tmp[0], tmp[2] }, strs[1]);
					_need_further_process_funcs.add(new String[] { tmp[1], tmp[2] });
				}
			}
		}

		for (String[] strs : this.JS_INHERI_ENCRYPTION_FUNCTION) {
			if (strs.length == 2) {
				_enc_funcs.put(strs, "ENCRYPTION");
			} else if (strs.length == 3) {
				_enc_funcs.put(new String[] { strs[0], strs[2] }, "ENCRYPTION");
				_need_further_process_funcs.add(new String[] { strs[1], strs[2] });
			}
		}

		for (String[] strs : this.JS_INHERI_DECRYPTION_FUNCTION) {
			if (strs.length == 2) {
				_enc_funcs.put(strs, "DECRYPTION");
			} else if (strs.length == 3) {
				_enc_funcs.put(new String[] { strs[0], strs[2] }, "DECRYPTION");
				_need_further_process_funcs.add(new String[] { strs[1], strs[2] });
			}
		}
	}

	private void parseBrowserConf() {
		_browser_type = readToString(BORWSER_TYPE_CONF_FILE).trim();
	}

	private void parseSkipFuncConf() {
		String content = readToString(SKIP_FUNC_CONF_FILE).trim();
		String[] skipfuncs = content.split(";");
		for (int i = 0; i < skipfuncs.length; i++) {
			if (skipfuncs[i].length() > 0) {
				String tmp = skipfuncs[i].trim();
				if (tmp.contains(",")) {
					String[] strs = skipfuncs[i].trim().split(",");
					// file, func
					_skip_funcs.add(strs);
				} else {
					// func
					_skip_funcs.add(new String[] { tmp });
				}
			}
		}
	}

	private void parserMessageReceiverConf() {
		String content = readToString(MESS_REC_CONF_FILE).trim();
		String[] messrecs = content.split(";");
		for (int i = 0; i < messrecs.length; i++) {
			if (messrecs[i].length() > 0) {
				String[] strs = messrecs[i].trim().split(",");
				_message_receiver.add(strs);
			}
		}
	}

	public String readToString(String fileName) {
		String encoding = "ISO-8859-1";
		File file = new File(fileName);
		Long filelength = file.length();
		byte[] filecontent = new byte[filelength.intValue()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(filecontent);
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			return new String(filecontent, encoding);
		} catch (UnsupportedEncodingException e) {
			System.err.println("The OS does not support " + encoding);
			e.printStackTrace();
			return null;
		}
	}

	public void writeToFile(String filename, String content, boolean isAppend) {
		try {
			RandomAccessFile rf = new RandomAccessFile(filename, "rw");
			if (isAppend) {
				rf.seek(rf.length());
			} else {
				rf.seek(0);
			}
			rf.writeBytes(content);
			rf.close();
		} catch (IOException e) {
			System.out.println("Append to file failes: " + filename);
		}
	}

	public void serializeObject(String fileName, Object obj) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(obj);

		fos.close();
	}

	public Object unserializeObject(String fileName) throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(fileName);
		ObjectInputStream ois = new ObjectInputStream(fis);
		Object obj = ois.readObject();
		ois.close();
		return obj;
	}

	public void createFolder(String path) {
		new File(path).mkdir();
	}

	public final String[] JS_INHERI_FUNCTION_WITH_FLOW = new String[] { "parseInt", "encodeURIComponent()",
			"Cc[@mozilla.org/intl/scriptableunicodeconverter].createInstance().convertToByteArray()", "escape()", "btoa()", "JSON.stringify()",
			"Components[classes][@mozilla.org/network/io-service;1].getService().newURI()", "Lazarus[JSON]encode()", "zdkj()" };

	public final String[] JS_INHERI_FUNCTION_WITHOUT_FLOW = new String[] { "window.openDialog()" };

	/* this is for find sinked vars */
	public final String[] JS_INHERI_SINK_FUNCTION_NETWORK = new String[] {
			// Browser
			"XMLHttpRequest.open()", "XMLHttpRequest.send()", "$.ajax()", "$.post()", "$().append()", "document.getElementById().loadURI()",
			"window.open()", "$http()",
			// Specific	
			"self[syncInst].authorizedRequest()", "putBookmarksList()", "tasks.create()" };
	public final String[] JS_INHERI_SINK_FUNCTION_LOCAL = new String[] {
			// Firefox
			"Components[classes][@mozilla.org/intl/converter-output-stream;1].createInstance().writeString()",
			"Components[classes][this.foutstreamCID].createInstance().write()",
			"Cc[@mozilla.org/network/file-output-stream;1].createInstance().write()",
			"Cc[@mozilla.org/intl/converter-output-stream;1].createInstance().writeString()",
			"Components[classes][@mozilla.org/network/file-output-stream;1].createInstance().write()",
			"Components[classes][@mozilla.org/passwordmanager;1].getService().addUser()",
			"Components[classes][@mozilla.org/login-manager;1].getService().addLogin()",
			"Components[classes][@mozilla.org/login-manager;1].getService().modifyLogin()",
			"Components[classes][@mozilla.org/browser/nav-bookmarks-service;1].getService().insertBookmark()",
			// Chrome
			"chrome.storage.local.set()", "chrome[storage][local].set()", "chrome.extension.sendRequest()", "localStorage[]",
			"null[data].postMessage()", "window[localStorage].setItem()",
			// Specific
			"localStorage.setItem()", "Lazarus[db].exe()", "archivefbUtils.writeFile()", "this[cache].unshift()",
			"setCookie()",// "this.InsertNote()",
			"PriceFinder[storage].setItem()", "worker[port].emit()", "this._createAccount()", "db.transaction().objectStore().put()",
			"this[prefs].setCharPref()", "loginManager.addLogin()", "passwordManager.addUser()", "LinkPassword[FireFox].SetURL()", "initHandler" };

	public final String[][] JS_INHERI_ENCRYPTION_FUNCTION = new String[][] {
			{ "file", "Cc[@mozilla.org/security/hash;1].createInstance().update()", "Cc[@mozilla.org/security/hash;1].createInstance().finish()" },
			{ "file", "Cc[@mozilla.org/security/hmac;1].createInstance().update()", "Cc[@mozilla.org/security/hmac;1].createInstance().finish()" },
			{ "file", "Components.classes[@mozilla.org/security/sdr;1].getService().encryptString()" },
			{ "file", "Components.classes[@mozilla.org/login-manager/crypto/SDR;1].getService().encrypt()" } };

	public final String[][] JS_INHERI_DECRYPTION_FUNCTION = new String[][] { { "file",
			"Components.classes[@mozilla.org/security/sdr;1].getService().decryptString()" } };

	// remember here we donot need "()"
	public final String[] JS_INHERI_EVEN_MESSAGE = new String[] { "chrome[extension].sendMessage" };
}
