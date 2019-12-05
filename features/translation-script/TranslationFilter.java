import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * filter translation utils
 * 
 * @author ever_who
 *
 */
public class TranslationFilter {

	// private static final String UNKNOWN_OS="unknown os";

	// private static boolean linuxOS;
	
	private static class Translation{
		String module;
		String modulePath;
		String locale;
		String wordsInEnglish;
		String wordsInTargetLocale;
	
		
		private Translation(String module, String modulePath, String locale, String wordsInEnglish,
				String wordsInTargetLocale) {
			super();
			this.module = module;
			this.modulePath = modulePath;
			this.locale = locale;
			this.wordsInEnglish = wordsInEnglish;
			this.wordsInTargetLocale = wordsInTargetLocale;
		}
		public String getModule() {
			return module;
		}
		public void setModule(String module) {
			this.module = module;
		}
		public String getModulePath() {
			return modulePath;
		}
		public void setModulePath(String modulePath) {
			this.modulePath = modulePath;
		}
		public String getLocale() {
			return locale;
		}
		public void setLocale(String locale) {
			this.locale = locale;
		}
		public String getWordsInEnglish() {
			return wordsInEnglish;
		}
		public void setWordsInEnglish(String wordsInEnglish) {
			this.wordsInEnglish = wordsInEnglish;
		}
		public String getWordsInTargetLocale() {
			return wordsInTargetLocale;
		}
		public void setWordsInTargetLocale(String wordsInTargetLocale) {
			this.wordsInTargetLocale = wordsInTargetLocale;
		}
		@Override
		public String toString() {
			return "Translation [module=" + module + ", modulePath=" + modulePath + ", locale=" + locale
					+ ", wordsInEnglish=" + wordsInEnglish + ", wordsInTargetLocale=" + wordsInTargetLocale + "]";
		}
		
		
		
	}

	private static final String ROOT_PATH = "/media/ubuntu/disk/code/P_ZAL1670-hmd/";
	
	private static final String LOCALE_FILE_PATH="G:\\test.csv";
	
	private static final String SOURCE_FILE_PATH="G:\\src.csv";
	
	private static final String REGEX_TRANS=">\"[^>]+\"<";
	
	//packages/apps/Contacts/res/values-nb/strings.xml:    <string name="applicationLabel" msgid="3906689777043645443">"Kontakter"</string>
	
	//private static Map<String,String> moduleAndStrings;
	
	
	public static void main(String[] args) {
		
		boolean linuxOS = getIfLinuxOs();
		System.out.println("linuxOS :" + linuxOS);
		/*List<String> localeList = readTargetLocales(LOCALE_FILE_PATH);
		if(localeList==null) {
			System.out.println("localeList==null exit");
			return;
		}*/
		//System.out.println(localeList);//locale list
		List<Translation> sourceList= readSourceMaps(SOURCE_FILE_PATH);
		if(sourceList==null) {
			System.out.println("sourceList==null exit");
			return;
		}
		System.out.println(sourceList);
		/*for(int i=0;i<localeList.size();i++) {
			String locale="af";
			System.out.println("locale = "+locale);
			for(int j=0;j<sourceList.size();j++) {
				Translation t=sourceList.get(j);
				String modulePath=t.getModulePath();
				String source=t.getWordsInEnglish();
				String cmd=generateCmd(source, getSourcePath(modulePath, locale), linuxOS);
				String cmdLog=execCmd(cmd, linuxOS);
				System.out.println(cmdLog);
				getTargetWords(cmdLog);
			}
		}*/
		
		
//		String cmdLog=execCmd("grep -rn all_apps_loading_message /home/hjc/code/aosp/packages/apps/Launcher3/res/values-gl",linuxOS);
//		System.out.println(cmdLog);
//		getTargetWords(cmdLog);
	}
	
	
	public static List<String> readTargetLocales(String localesFilePath){
		File localesFile = new File(localesFilePath);
		if(!localesFile.exists()) {
			return null;
		}
		BufferedReader br =null;
		List<String> list =null;
		try {
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(localesFile)));
				list = new ArrayList<String>();
				String line="";
				while((line=br.readLine())!=null) {
					list.add(line);
				}
			}finally {
				br.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	
	
	public static List<Translation> readSourceMaps(String sourceFilePath){
		File sourceFile = new File(sourceFilePath);
		if(!sourceFile.exists()) {
			return null;
		}
		BufferedReader br =null;
		List<Translation> list =null;
		try {
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile)));
				list = new ArrayList<Translation>();
				String line="";
				while((line=br.readLine())!=null) {
					String[] str=line.split(" ");
					if(str==null || str.length!=2) {
						System.out.println("SourceMap not formatted");
						return null;
					}
					Translation tr=new Translation(str[0],getSourceModule(str[0]),null,str[1],null);
					list.add(tr);
				}
			}finally {
				br.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	
	
	
	public static String getTargetWords(String line) {
		Pattern pattern = Pattern.compile(REGEX_TRANS);
		Matcher matcher=pattern.matcher(line);
		
		int index =0;
		while(matcher.find()) {
			index++;
			String out=matcher.group();
			int length = out.length();
			out = out.substring(2,length-2);
			System.out.println(index+" "+out);
		}
		return null;
	}

	/**
	 * get source module path from module-name
	 * 
	 * @param module
	 * @return
	 */
	public static String getSourceModule(String module) {
		switch (module) {
		case "Settings":
			return "packages/apps/Settings";
		case "Launcher":
			return "packages/apps/Launcher3";
		case "MtkSettings":
			return "vendor/mediatek/proprietary/packages/apps/MtkSettings";
		case "SettingsIntelligence":
			return "packages/apps/SettingsIntelligence";
		case "Telephony":
			return "packages/services/Telephony";
		case "Launcher3":
			return "vendor/mediatek/proprietary/packages/apps/Launcher3";
		case "Telephony":
			return "packages/services/Telephony";
		default:
			return null;
		}
	}

	/**
	 * get specific res dir of the locale 获取指定语言res文件夹
	 * 
	 * @param module
	 * @param locale
	 * @return res dir path
	 */
	public static String getSourcePath(String module, String locale) {
		String modulePath = getSourceModule(module);
		if (modulePath == null) {
			System.out.println(" --- modulePath not exists --- ");
			return null;
		}
		String resDirPath = ROOT_PATH + modulePath + "/res/value-" + locale;
		return resDirPath;
	}

	public static String getTargetTranslation(String target, String path, boolean isLinux) {
		if (target == null || path == null) {
			return null;
		}
		if (isLinux) {

		}

		return null;
	}

	/**
	 * judge if it is linux os
	 * 
	 * @return
	 */
	public static boolean getIfLinuxOs() {
		String osName = System.getProperty("os.name");
		if (osName != null && osName.contains("Windows")) {
			return false;
		}
		return true;
	}

	public static String generateCmd(String target,String path, boolean isLinux) {
		if(isLinux) {
			return "grep -rn "+target+" "+path;
		}
		return null;
	}

	
	/**
	 * execute command
	 * 
	 * @param cmd
	 * @return cmd result
	 */
	public static String execCmd(String cmd,boolean isLinux) {
		if (cmd == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		Process process = null;
		BufferedReader normalReader = null;
		BufferedReader errorReader = null;
		try {
			String[] commands = {"/bin/sh","-c",cmd};
			process = Runtime.getRuntime().exec(commands, null, null);
			process.waitFor();
			normalReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"));
			errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "utf-8"));
			String line = null;
			while ((line = normalReader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			line = null;
			while ((line = errorReader.readLine()) != null) {
				builder.append(line).append("\n");
			}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (errorReader != null) {
					errorReader.close();
				}
				if (normalReader != null) {
					normalReader.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (process != null) {
				process.destroy();
				process = null;
			}
		}
		return builder.toString();
	}

}
