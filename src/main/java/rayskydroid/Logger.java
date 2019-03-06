package rayskydroid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.logging.FileHandler;
import java.util.logging.Level;

/**
 * @author Rakesh.Jha
 * Date - 07/10/2013
 * Definition - Logger file use to keep Log info to external SD with the simple method
 */

public class Logger {
	public static boolean isEnabled = false;
    public static  FileHandler logger = null;
    private static String proj_name = "BingoAppServer";
    private static String filename = proj_name + "_Log";
    private static String path = proj_name;//for windows: "C:\\" + proj_name;

    static boolean isExternalStorageAvailable = false;
    static boolean isExternalStorageWriteable = false;

    public static void clearLogs(){
        File dir = new File(path);
        if (!dir.exists()) {
            java.util.logging.Logger.getGlobal().log(Level.FINE, "Dir created ");
            dir.mkdirs();
        }

        // for windows: File logFile = new File(path + "\\" + filename + ".txt");

        File logFile = new File(path + "/" + filename + ".txt");
        if (logFile.exists()) {
            logFile.delete();
        }
    }
    public static void addRecordToLog(String message) {
    	if( isEnabled ){
    		final File dir = new File(path);
            if(!dir.exists()) {
            	java.util.logging.Logger.getGlobal().log(Level.FINE,"Dir created ");
                dir.mkdirs();
            }

            //for windows: final File logFile = new File(path + "\\"+filename+".txt");
            final File logFile = new File(path + "/"+filename+".txt");
            if (!logFile.exists())  {
                try  {
                	java.util.logging.Logger.getGlobal().log(Level.FINE, "File created ");
                    logFile.createNewFile();
                    final FileWriter fw = new FileWriter(logFile, true);
                    //write header format:
                    final BufferedWriter buf = new BufferedWriter(fw);
                    buf.write( "description|timestamp|memorysize|threadid");
                    buf.newLine();
                    buf.flush();
                    buf.close();
                    fw.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                final FileWriter fw = new FileWriter(logFile, true);
                //BufferedWriter for performance, true to set append to file flag
                final BufferedWriter buf = new BufferedWriter(fw);
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                buf.write( message + "|" + timestamp.toString() + "|" + Runtime.getRuntime().totalMemory() + "|" + Thread.currentThread().getId());
                //buf.append(message);
                buf.newLine();
                buf.flush();
                buf.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    	}        
    }
}