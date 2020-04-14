import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PmVersion {

    public static final String PATH="/home/ubuntu/bak/package-version/P-pm-list";
    public static final String OUTPUT_PATH="/home/ubuntu/bak/package-version/Q-pm-versions";
    public static final String NULL_STRING="NULL";

    public static void main(String[] args) {
		List<String> list = readSource(PATH);
		if(list==null || list.size()==0){
			System.out.println("sourceFile not exist or is empty ");
			return;
		}
		for(int i=0;i<list.size();i++){
			String cmd = generateCmd(list.get(i),true);
			//System.out.println("cmd ="+cmd);
			String output=execCmd(cmd,true);
			//System.out.println("output ="+output);
			String out = filterOutput(output);
			System.out.println(list.get(i)+"="+out);
		}
    }

    
    public static String filterOutput(String in){
        if(in==null){
            return NULL_STRING;
        }
        String inTrimed = in.trim();
        if("".equals(inTrimed)){
            return NULL_STRING;
        } else{
            String[] split=inTrimed.split("=");
            if(split!=null && split.length==2 && "versionName".equals(split[0])){
                //System.out.println(split[1]);
                return split[1];
            }
        }
        return NULL_STRING;
    }


    public static List<String> readSource(String sourceFilePath){
        File sourceFile = new File(sourceFilePath);
        if(!sourceFile.exists()) {
            return null;
        }
        BufferedReader br =null;
        List<String> list =null;
        try {
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile)));
                list = new ArrayList<String>();
                String line="";
                while((line=br.readLine())!=null) {
                    //System.out.println(line);
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



    public static String generateCmd(String target, boolean isLinux) {
        if(isLinux) {
            return "adb shell dumpsys package "+target+" | grep versionName";
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