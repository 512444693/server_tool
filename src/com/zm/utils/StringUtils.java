package com.zm.utils;

import java.awt.*;
import java.util.Date;

/**
 * Created by Administrator on 2015/12/13.
 */
public class StringUtils {
    public static String coverWithTime(String str){
        String time = "============" + new Date() + "============\r\n";
        return time + str;
    }

    public static String eachLineAddPrefix(String str, char cPrefix, int times){
        String prefix = "";
        for(int i=0; i<times; i++)
            prefix += cPrefix;
        String strs[] = str.split("\\r\\n");

        StringBuilder ret = new StringBuilder("");
        for(int i=0; i<strs.length; i++){
            ret.append(prefix);
            ret.append(strs[i]);
            ret.append(System.getProperty("line.separator"));
        }
        return ret.toString();
    }
    public static Color randomColor(){
        return new Color((int)(Math.random() * 255), (int)(Math.random() * 255), (int)(Math.random() * 255));
    }

    public static void main(String[] args){
        String a = "1111111111111111111111111111111111111111111111111111111111";
        String b = BU.bytes2HexGoodLook(a.getBytes());
        b = StringUtils.coverWithTime(b);
        String c = StringUtils.eachLineAddPrefix(b, '\t', 6);
        System.out.println(b);
        System.out.println(c);
    }
}
