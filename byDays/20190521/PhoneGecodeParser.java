import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;

public class PhoneGecodeParser {

	public static void main(String[] args) {
		File file = new File("/home/hjc/scripts/20190521/177.csv");
		File fileresult = new File("/home/hjc/scripts/20190521/177-out.txt");
		readTxtFile(file,fileresult,177);
		// System.out.println("loadstring-----"+loadstring);

	}
	
	
	private static String[] generateRootNumbers(int root) {
		String[] rootNums=new String[10];
		for(int i=0;i<10;i++) {
			rootNums[i]=String.valueOf(root*10+i);
		}
		return rootNums;
	}
	
	private static String keepThreeDigits(int num) {
		if(num >999 || num < 0) {
			throw new NumberFormatException("not a 3-digits number");
		}else if(num>99){
			return String.valueOf(num);
		}else if(num>9) {
			return "0"+String.valueOf(num);
		}else {
			return "00"+String.valueOf(num);
		}
	}

	public static String readTxtFile(File file,File fileresult,int root) {
		// ArrayList newList=new ArrayList<String>();
		String line = "";
		StringBuffer str = new StringBuffer();
		System.out.println("file-----" + file);
		// ���path�Ǵ��ݹ����Ĳ�����������һ����Ŀ¼���ж�
		if (file.isDirectory()) {
			System.out.println("The File doesn't not exist.");
			return null;
		} else {
			try {
				InputStream instream = new FileInputStream(file);
				if (instream != null) {
					System.out.println("1");
					Writer out = null;
					System.out.println("2");
					out = new OutputStreamWriter(new FileOutputStream(fileresult), "utf-8");
					System.out.println("3");
					InputStreamReader inputreader = new InputStreamReader(instream, "gbk");
					System.out.println("4");
					BufferedReader buffreader = new BufferedReader(inputreader);
					System.out.println("5");
					// BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new
					// FileOutputStream("/system/guishudiresult.txt"),"GBK"));

					String[] arrays =generateRootNumbers(root);
					//
					while ((line = buffreader.readLine()) != null) {
						System.out.println("line ---" + line);

						String[] buff = line.split(",");
						System.out.println("buff ---" + Arrays.toString(buff));
						for (int i = 3; i < buff.length; i++) {
							System.out.println("buff[i] ---" + buff[i] + ",i=" + i);
							if (buff[i].length() <= 2)
								continue;
							else {
								String[] aa = buff[i].split("、");
								if (aa.length == 1) {
									if (!aa[0].contains("-")) {
										str = str.append(arrays[i - 3]).append(aa[0]).append("\t").append(buff[0])
												.append("\t").append(buff[1]).append("\n");
										// System.out.println("str---"+str);
										// int len = reader.read(str);
										// out.write(str);
										// out.newLine();
									} else {
										String[] bb = aa[0].split("-");
//										boolean isStartZero = false;
//										boolean isDoubleStartZero = false;
//										if (bb[0].startsWith("00")) {
//											isDoubleStartZero = true;
//										} else if (bb[0].startsWith("0")) {
//											isStartZero = true;
//										}
										for (int x = Integer.parseInt(bb[0]); x <= Integer.parseInt(bb[1]); x++) {
											// if(isStartZero){
											str = str.append(arrays[i - 3])
													.append(keepThreeDigits(x))
													.append("\t").append(buff[0]).append("\t").append(buff[1])
													.append("\n");
											// }else{
											// str =arrays[i-3]+""+x+"\t"+buff[0]+"\t"+buff[1]+"\n";
											// }
											// System.out.println("str---"+str);

											// int len = reader.read(str);
											// out.write(str);
											// out.newLine();
										}
									}
								} else {
									for (int j = 0; j <= aa.length - 1; j++) {
										String cc[] = aa[j].split("-");
										if (cc.length == 1) {
											str = str.append(arrays[i - 3]).append(cc[0]).append("\t").append(buff[0])
													.append("\t").append(buff[1]).append("\n");
											// System.out.println("str---"+str);
											// int len = reader.read(str);
											// out.write(str);
											// out.newLine();
										} else {
											for (int y = Integer.parseInt(cc[0]); y <= Integer.parseInt(cc[1]); y++) {
												str = str.append(arrays[i - 3])
														.append(keepThreeDigits(y)).append("\t").append(buff[0]).append("\t")
														.append(buff[1]).append("\n");
											}
										}
									}
								}
							}
						}

						// newList.add(line+"\n");

					}
					instream.close();
					out.write(str.toString());
					out.flush();
					out.close();

				}
			} catch (FileNotFoundException e) {
				System.out.println("filenotfountexception");
				System.out.println("The File doesn't not exist.");
			} catch (IOException e) {
				System.out.println("ioexception");
				System.out.println(e.getMessage());
			}
		}
		return str.toString();
	}

}
