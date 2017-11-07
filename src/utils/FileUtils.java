package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
	/**
     * �����ļ�
     * @param fromFile
     * @param toFile
     * <br/>
     * 2016��12��19��  ����3:31:50
     * @throws IOException 
     */
    public static void copyFile(File fromFile,File toFile) throws IOException{
        FileInputStream ins = new FileInputStream(fromFile);
        FileOutputStream out = new FileOutputStream(toFile);
        byte[] b = new byte[1024];
        int n=0;
        while((n=ins.read(b))!=-1){
            out.write(b, 0, n);
            out.flush();
        }
       
        ins.close();
        out.close();
    }
}
