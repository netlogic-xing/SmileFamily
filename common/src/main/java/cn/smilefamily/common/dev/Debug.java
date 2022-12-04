package cn.smilefamily.common.dev;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;

public class Debug {
    private static int deep = 0;
    public static boolean enabled = true;

    private static PrintStream debugOut;
    static {
        try {

            String logFileName = System.getProperty("smile.internal.dev.debug.file","smile-internal-debug.log");
            debugOut = new PrintStream(new FileOutputStream(logFileName, true));
            debugOut.println("-".repeat(30)+new Date()+"-".repeat(30));
        } catch (FileNotFoundException e) {
            System.err.println("create internal");
        }
    }

    public static void debug(String content) {
        if(enabled) {
            debugOut.println(" ".repeat(4 * Debug.deep) + content);
        }
    }
    public static void enter(String content){
        if(enabled) {
            debugOut.println(" ".repeat(4 * Debug.deep++) + content);
        }
    }
    public static void leave(){
        if(enabled){
            deep--;
        }
    }
}
