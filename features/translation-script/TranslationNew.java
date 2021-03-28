import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * filter translations and insert
 *
 * 20210310
 *  Attention:  when string contains ":" , it won't work !
 * 
 * @author hujingcheng
 *
 */
public class TranslationNew {
	static List<String[]> targetWords;

	public static void main(String[] args) {

		// boolean linuxOS = getIfLinuxOs();
		// System.out.println("linuxOS :" + linuxOS);
		/*
		 * List<String> localeList = readTargetLocales(LOCALE_FILE_PATH);
		 * if(localeList==null) { System.out.println("localeList==null exit"); return; }
		 */
		// System.out.println(localeList);//locale list

		// List<String[]> sourceList= readSource(SOURCE_FILE_PATH);
		// if(sourceList==null) {
		// System.out.println("sourceList==null exit");
		// return;
		// }
		// System.out.println(sourceList);
		List<String> target = readTargetLocales(SOURCE_FILE_PATH);// scf grep 出来的源字符串
		// System.out.println(target);
		targetWords = new ArrayList<String[]>();
		for (int i = 0; i < target.size(); i++) {
			getTargetWords(target.get(i));
		}

		insertTranslation(targetWords);
		// insertTranslation(sourceList);
		/*
		 * for(int i=0;i<localeList.size();i++) { String locale="af";
		 * System.out.println("locale = "+locale); for(int j=0;j<sourceList.size();j++)
		 * { Translation t=sourceList.get(j); String modulePath=t.getModulePath();
		 * String source=t.getWordsInEnglish(); String cmd=generateCmd(source,
		 * getSourcePath(modulePath, locale), linuxOS); String cmdLog=execCmd(cmd,
		 * linuxOS); System.out.println(cmdLog); getTargetWords(cmdLog); } }
		 */

		// String cmdLog=execCmd("grep -rn all_apps_loading_message
		// /home/hjc/code/aosp/packages/apps/Launcher3/res/values-gl",linuxOS);
		// System.out.println(cmdLog);
		// getTargetWords(cmdLog);
	}

	public static List<String> readTargetLocales(String localesFilePath) {
		File localesFile = new File(localesFilePath);
		if (!localesFile.exists()) {
			return null;
		}
		BufferedReader br = null;
		List<String> list = null;
		try {
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(localesFile)));
				list = new ArrayList<String>();
				String line = "";
				while ((line = br.readLine()) != null) {
					list.add(line);
				}
			} finally {
				br.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	// go to source res dir to : grep -rn "\"test\"" > path.log 
	private static final String SOURCE_FILE_PATH = "/work/hujingcheng/test/res-aftersalesservice/oppo_after_service_title.log";

	private static final String ROOT_PATH = "/work/hujingcheng/Riox/RIOX-sp9863a-20210203/packages/apps/Settings/res/";// scf
																														// 目的资源路径

	private static final String ENDINGS = "</resources>";

	private static final String REGEX_TRANS = ">\"*[^>]+\"*<";

	private static final String TARGET_STRING_KEY = "oppo_after_service_title";

	public static void getTargetWords(String line) {
		String[] splitWords = line.split(":");
		if (splitWords.length != 3) {
			System.out.println("getTargetWords split error");
			return;
		}

		Pattern pattern = Pattern.compile(REGEX_TRANS);
		Matcher matcher = pattern.matcher(splitWords[2]);

		int index = 0;
		if (matcher.find()) {
			index++;
			String out = matcher.group();
			// int length = out.length();
			// out = out.substring(2,length-2);
			out = out.replaceAll("\"", "");
			out = out.replaceAll("<", "");
			out = out.replaceAll(">", "");
			targetWords.add(new String[] { splitWords[0], out });
			System.out.println(out);
		} else {
			System.out.println("null");
		}
		// return null;
	}

	public static List<String[]> readSource(String localesFilePath) {
		File localesFile = new File(localesFilePath);
		if (!localesFile.exists()) {
			return null;
		}
		BufferedReader br = null;
		List<String[]> list = null;
		try {
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(localesFile)));
				list = new ArrayList<String[]>();
				String line = "";
				while ((line = br.readLine()) != null) {
					String[] pathContent = line.split(":");
					list.add(pathContent);
				}
			} finally {
				br.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	// public static String getTargetDir(String sourceRePath){
	// sourceRePath =sourceRePath.replace("/strings.xml","");
	// System.out.println(transMaps[0]+" still not exist , pass ------------------
	// !");
	// return ROOT_PATH+sourceRePath;
	// }

	public static void insertTranslation(List<String[]> pathAndTrans) {
		for (int i = 0; i < pathAndTrans.size(); i++) {
			String[] transMaps = pathAndTrans.get(i);
			File path = new File(ROOT_PATH + transMaps[0]);
			boolean newStringFile = false;
			try {
				if (!path.getParentFile().exists()) {
					System.out.println(transMaps[0] + " target dir not exist , mkdir --------------- !");
					path.getParentFile().mkdirs();
				}
				if (!path.exists()) {
					System.out.println(transMaps[0] + " target path not exist , touch --------------- !");
					path.createNewFile();
					newStringFile = true;
				}
			} catch (IOException e) {
				e.printStackTrace();
				newStringFile = false;
			}
			if (!path.exists()) {
				System.out.println(transMaps[0] + " still not exist , pass ------------------ !");
				newStringFile = false;
				continue;
			} else {
				String trans = transMaps[1];
				System.out.println("string --- " + transMaps[1]);
				StringBuilder sb;
				BufferedReader br = null;
				BufferedWriter bw = null;

				try {
					try {
						sb = new StringBuilder();
						if (newStringFile) {
							System.out
									.println(transMaps[0] + " init file , and add target string ------------------ !");
							sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>").append("\n");
							sb.append("<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">").append("\n");
							sb.append("    <string name=\"").append(TARGET_STRING_KEY).append("\">\"").append(trans)
									.append("\"</string>\n");
							sb.append(ENDINGS).append("\n");
						} else {
							br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
							String line = "";
							int count = 0;
							while ((line = br.readLine()) != null) {
								if (line.contains("<string name=\"" + TARGET_STRING_KEY + "\">")) {
									System.out.println(TARGET_STRING_KEY + " found , replace ------------------ !");
									sb.append("    <string name=\"").append(TARGET_STRING_KEY).append("\">\"")
											.append(trans).append("\"</string>\n");
									count++;
								} else if (line.contains(ENDINGS) && count == 0) {
									// sb.append(" <!-- Huaqin add by hujingcheng ANDYSEVEN-247 20201216
									// begin-->").append("\n");
									sb.append("    <string name=\"").append(TARGET_STRING_KEY).append("\">\"")
											.append(trans).append("\"</string>\n");
									// sb.append(" <!-- Huaqin add by hujingcheng ANDYSEVEN-247 20191216
									// end-->").append("\n");
									sb.append(ENDINGS).append("\n");
								} else {
									sb.append(line).append("\n");
								}
							}
						}
						bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
						bw.write(sb.toString());
						bw.flush();
					} finally {
						if (br != null) {
							br.close();
						}
						if (bw != null) {
							bw.close();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}