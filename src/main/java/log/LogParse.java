package log;

import com.alibaba.fastjson.JSON;
import kafka.SimpleProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class LogParse implements Runnable{

    static Logger logger= LoggerFactory.getLogger(LogParse.class);
    private File logFile = null;
    private long lastTimeFileSize = 0; // ??????
    private static SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    public LogParse(File logFile) {
        this.logFile = logFile;
        lastTimeFileSize = logFile.length();
    }

    /**
     *
     */
    public void run() {
        while (true) {
            try {
                long len = logFile.length();
                if (len < lastTimeFileSize) {
                    System.out.println("Log file was reset. Restarting logging from start of file.");
                    lastTimeFileSize = len;
                } else if(len > lastTimeFileSize) {
                    RandomAccessFile randomFile = new RandomAccessFile(logFile, "r");
                    randomFile.seek(lastTimeFileSize);
                    String tmp = null;
                    while ((tmp = randomFile.readLine()) != null) {
                        String str=new String(tmp.getBytes("ISO8859-1"));
                        int i=str.indexOf("组包报文");
                        if(-1!=i){
                            String subStr=str.substring(i+5,str.length()).trim();
                            logger.info(dateFormat.format(new Date()) + "\t" + subStr);
                            Map<String, Object> jsonHashMap=ParseXML.parseAcceptXML(subStr);
                            SimpleProducer.sedMessage(JSON.toJSONString(jsonHashMap));
                            logger.info(dateFormat.format(new Date()) + "\t" + JSON.toJSONString(jsonHashMap));
                        }
                        System.out.println(dateFormat.format(new Date()) + "\t" + str);
                    }
                    lastTimeFileSize = randomFile.length();
                    randomFile.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                logger.info(e.getMessage());
                e.printStackTrace();
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void main(String []args){
        File logFile = new File("C:\\Users\\user\\Desktop\\BOD\\ECIS_LOGS\\out.log");
        Thread wthread = new Thread(new LogParse(logFile));
        wthread.start();
    }
}
