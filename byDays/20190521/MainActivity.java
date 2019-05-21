import java.io.* ;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream ;
import java.io.OutputStreamWriter ;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity{



    public static void main(String[] args) {

        ReadTxtFile();
        //System.out.println("loadstring-----"+loadstring);



    }


 public static String ReadTxtFile()  
    {  
            //ArrayList newList=new ArrayList<String>();
            String line = "";  
		StringBuffer str = new StringBuffer();
            //打开文件  
            File file = new File("/home/zjk/bak/geocode/149.csv");  
            File fileresult = new File("/home/zjk/bak/geocode/149-out.txt");  
            System.out.println("file-----"+file);
            //如果path是传递过来的参数，可以做一个非目录的判断  
            if (file.isDirectory())  
            {  
                System.out.println( "The File doesn't not exist.");  
            }  
            else  
            {  
                try {  
                    InputStream instream = new FileInputStream(file);   
                    if (instream != null)   
                    {  
                        System.out.println("1");
                        Writer out = null ;
                        System.out.println("2");
                        out = new OutputStreamWriter(new FileOutputStream(fileresult),"utf-8") ;
                        System.out.println("3");
                        InputStreamReader inputreader = new InputStreamReader(instream,"gbk");  
                        System.out.println("4");
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        System.out.println("5");
                        //BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/system/guishudiresult.txt"),"GBK"));
                        
                        
                        String[] arrays= {"1490","1491","1492","1493","1494","1495","1496","1497","1498","1499"};
                        //分行读取  
                        while (( line = buffreader.readLine()) != null) {  
                            System.out.println("line ---"+line); 
			
                            String [] buff = line.split(",");
			    System.out.println("buff ---"+Arrays.toString(buff));
                            for (int i = 3;i < buff.length ;i++){
				 System.out.println("buff[i] ---"+buff[i]+",i="+i); 	
                                if(buff[i].length() <= 2) continue;
                                else {
                                    String[] aa = buff[i].split("、");
                                    if(aa.length == 1){
                                        if(!aa[0].contains("-")){
                                            str=str.append(arrays[i-3]).append(aa[0]).append("\t").append(buff[0]).append("\t").append(buff[1]).append("\n");
                                            //System.out.println("str---"+str);
                                            //int len = reader.read(str);
                                            //out.write(str);
                                            //out.newLine();
                                        }
                                        else{
                                            String[] bb = aa[0].split("-");
					    boolean isStartZero=false;
					    boolean isDoubleStartZero=false;
					    if(bb[0].startsWith("00")){
						isDoubleStartZero=true;
					    }else if(bb[0].startsWith("0")){
						isStartZero=true;
					    }
                                            for(int x = Integer.parseInt(bb[0]);x<=Integer.parseInt(bb[1]);x++){
						//if(isStartZero){
						str =str.append(arrays[i-3]).append(isDoubleStartZero?"00":isStartZero?"0":"").append(x).append("\t").append(buff[0]).append("\t").append(buff[1]).append("\n");
						//}else{
                                                	//str =arrays[i-3]+""+x+"\t"+buff[0]+"\t"+buff[1]+"\n";
						//}
                                                //System.out.println("str---"+str);

                                                //int len = reader.read(str);
                                                //out.write(str);
                                                //out.newLine();
                                            }
                                        }
                                    }
                                    else {
                                        for(int j = 0;j<=aa.length - 1;j++){
                                            String cc[] = aa[j].split("-");
                                            if(cc.length == 1 ){
                                                str =str.append(arrays[i-3]).append(cc[0]).append("\t").append(buff[0]).append("\t").append(buff[1]).append("\n");
                                                //System.out.println("str---"+str);
                                                //int len = reader.read(str);
                                                //out.write(str);
                                                //out.newLine();
                                            }else {
					    	boolean isStartZero=false;
					    	boolean isDoubleStartZero=false;
					    	if(cc[0].startsWith("00")){
							isDoubleStartZero=true;
					    	}else if(cc[0].startsWith("0")){
							isStartZero=true;
					    	}
                                                for(int y = Integer.parseInt(cc[0]);y<=Integer.parseInt(cc[1]);y++){
							str =str.append(arrays[i-3]).append(isDoubleStartZero?"00":isStartZero?"0":"").append(y).append("\t").append(buff[0]).append("\t").append(buff[1]).append("\n");
                                                	//if(isStartZero){
								//str =arrays[i-3]+"0"+y+"\t"+buff[0]+"\t"+buff[1]+"\n";
							//}else{

								//str =arrays[i-3]+""+y+"\t"+buff[0]+"\t"+buff[1]+"\n";
							//}   
							
                                                    //System.out.println("str---"+str);
                                                    //int len = reader.read(str);
                                                    //out.write(str);
                                                    //out.newLine();
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                
                            //newList.add(line+"\n");
                            
				
                        }
				instream.close(); 
				out.write(str.toString());
				out.flush();
				out.close();            
                         
                    }  
                }  
                catch (java.io.FileNotFoundException e)   
                {  
                    System.out.println("filenotfountexception");
                    System.out.println( "The File doesn't not exist.");  
                }   
                catch (IOException e)   
                {  
                    System.out.println("ioexception");
                     System.out.println( e.getMessage());  
                }  
            }  
            return str.toString();  
    } 


}
