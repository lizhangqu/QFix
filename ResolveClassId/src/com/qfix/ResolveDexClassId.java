package com.qfix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javassist.ClassPool;
import javassist.CtClass;

import org.apache.commons.io.FileUtils;

public class ResolveDexClassId {
	
	public static class ClassIdMap {
		String className;
		int dexId;
		long classId;
		
		public ClassIdMap(String classname, int dexid, long classid) {
			className = classname;
			dexId = dexid;
			classId = classid;
		}
		
		@Override
		public String toString() {
			return new StringBuilder(className).append("-").append(dexId).append("-").append(classId).toString();
		}
	}
	
	private static class LogErrorRunnable implements Runnable {
		InputStream inputStream;

		public LogErrorRunnable(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		public void run() {
			InputStreamReader isReader = null;
			BufferedReader reader = null;
			try {
				isReader = new InputStreamReader(this.inputStream);
				reader = new BufferedReader(isReader);
				String line = null;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
			} catch (Exception e) {
				System.out.println("LogErrorRunnable run exception=" + e);
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception e) {
						System.out.println("LogErrorRunnable close BufferedReader exception=" + e);
						e.printStackTrace();
					}
				}
				if (isReader != null) {
					try {
						isReader.close();
					} catch (Exception e) {
						System.out.println("LogErrorRunnable close InputStreamReader exception=" + e);
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private static class ProcessRunnable implements Runnable {
		InputStream mInputStream;
		int mDexIndex;
		HashSet<String> mPatchSet;
		public ArrayList<ClassIdMap> mClassIdMapList;
		
		public ProcessRunnable(InputStream inputStream, int dexIndex, HashSet<String> patchSet) {
			mInputStream = inputStream;
			mDexIndex = dexIndex;
			mPatchSet = patchSet;
			mClassIdMapList = new ArrayList<ClassIdMap>();
		}

		public void run() {
			InputStreamReader isReader = null;
			BufferedReader reader = null;
			try {
				isReader = new InputStreamReader(mInputStream);
				reader = new BufferedReader(isReader);
				String line = null;
				boolean findHead = false;
				boolean findClass = false;
				int classIndex = -1;
				long classIdx = -1;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("Class #") && line.endsWith(" header:") && !findHead && classIndex < 0) {
						findHead = true;
						classIndex = Integer.parseInt(line.substring("Class #".length(), line.indexOf(" header:")));
					} else if (line.startsWith("class_idx") && findHead && classIndex >= 0 && classIdx < 0) {
						classIdx = Integer.parseInt(line.substring(line.indexOf(": ") + 2));
					} else if (line.startsWith("Class #") && findHead && classIndex >= 0
							&& line.contains(String.valueOf(classIndex)) && classIdx > 0) {
						findClass = true;
					} else if (line.startsWith("  Class descriptor") && findHead && findClass && classIndex >= 0 && classIdx > 0) {
						String className = line.substring(line.indexOf("'L") + 2, line.indexOf(";'"));
						className = className.replace("/", ".");
						if (mPatchSet.contains(className)) {
							ClassIdMap item = new ClassIdMap(className, mDexIndex, classIdx);
							mClassIdMapList.add(item);
						}
						findHead = false;
						findClass = false;
						classIndex = -1;
						classIdx = -1;
					}
				}
			} catch (Exception e) {
				System.out.println("ProcessRunnable run exception=" + e);
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception e) {
						System.out.println("ProcessRunnable close BufferedReader exception=" + e);
						e.printStackTrace();
					}
				}
				if (isReader != null) {
					try {
						isReader.close();
					} catch (Exception e) {
						System.out.println("ProcessRunnable close InputStreamReader exception=" + e);
						e.printStackTrace();
					}
				}
			}
		}
	}
	
    public static void main(String[] args) throws IOException, Exception {
		String dexDumpPath = args[0];
		String dexFilePath = args[1];
		String patchClassPath = args[2];
		String outputPath = args[3];
        try {
			HashSet<String> patchClassSet = new HashSet<String>();
			ClassPool classPool = ClassPool.getDefault();
			classPool.appendClassPath(patchClassPath);
			Collection<File> classFiles = FileUtils.listFiles(new File(patchClassPath), new String[]{"class"}, true);
			System.out.println("ResolveDexClassId patch class files size=" + classFiles.size());
			for (File file : classFiles) {
	            CtClass ctClass = classPool.makeClassIfNew(new FileInputStream(file));
	            String ctClassName = ctClass.getName();
	            System.out.println("ResolveDexClassId patch class name=" + ctClassName);
	            if (!patchClassSet.contains(ctClassName)) {
	            	patchClassSet.add(ctClassName);
	            }
			}
			System.out.println("ResolveDexClassId patchClassSet size=" + patchClassSet.size());
			
			File dexDir = new File(dexFilePath);
        	File[] dexFileList = dexDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isFile() && file.getName().startsWith("classes") && file.getName().endsWith(".dex");
				}
			});
        	System.out.println("ResolveDexClassId dexFileList size=" + dexFileList.length);
        	if (dexFileList != null && dexFileList.length > 0) {
        		for (File tempFile : dexFileList) {
        			System.out.println("ResolveDexClassId dexFile path=" + tempFile.getAbsolutePath());
        		}
        	}
        	
        	StringBuilder outputStr = new StringBuilder("");
        	StringBuilder patchConfig = new StringBuilder("");
        	boolean isFirst = true;
        	for (File file : dexFileList) {
        		String command = dexDumpPath + " -h " + file.getAbsolutePath();
        		int dexIndex = getDexIndex(file.getName());
    			Process process = Runtime.getRuntime().exec(command);
    			ProcessRunnable procRunnable = new ProcessRunnable(process.getInputStream(), dexIndex, patchClassSet);
    			new Thread(procRunnable).start();
    			new Thread(new LogErrorRunnable(process.getErrorStream())).start();
    			process.waitFor();
    			for (ClassIdMap item : procRunnable.mClassIdMapList) {
    				System.out.println("ResolveDexClassId ClassIdMap item=" + item.toString());
    				if (isFirst) {
    					patchConfig.append(item.dexId).append("-").append(item.classId);
    					isFirst = false;
    				} else {
    					patchConfig.append(";").append(item.dexId).append("-").append(item.classId);
    				}
    				outputStr.append(item.toString()).append("\n");
    			}
        	}
        	outputStr.append(patchConfig.toString()).append("\n");
			
			File outputFile = new File(outputPath);
			if (outputFile.exists()) {
				outputFile.delete();
			}
			FileWriter writer = null;
			BufferedWriter bw = null;
			try {
				writer = new FileWriter(outputPath);
				bw = new BufferedWriter(writer);
				bw.write(outputStr.toString());
			} catch (Exception e) {
				System.out.println("ResolveDexClassId write output exception=" + e);
				e.printStackTrace();
			} finally {
				if (bw != null) {
					try {
						bw.close();
					} catch (Exception e) {
						System.out.println("ResolveDexClassId close BufferedWriter exception=" + e);
						e.printStackTrace();
					}
				}
				if (writer != null) {
					try {
						writer.close();
					} catch (Exception e) {
						System.out.println("ResolveDexClassId close FileWriter exception=" + e);
						e.printStackTrace();
					}
				}
			}
			
		} catch (IOException e) {
			System.out.println("ResolveDexClassId main IOException=" + e);
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("ResolveDexClassId main Exception=" + e);
			e.printStackTrace();
		}
    }
    
    private static int getDexIndex(String dexName) {
    	if ("classes.dex".equals(dexName)) {
    		return 1;
    	} else if (dexName != null && dexName.startsWith("classes") && dexName.endsWith(".dex")) {
    		String str = dexName.substring(7);
    		str = str.substring(0, str.indexOf(".dex"));
    		return Integer.parseInt(str);
    	}
    	return 0;
    }
}