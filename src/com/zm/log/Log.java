package com.zm.log;

import com.zm.utils.BU;
import com.zm.utils.SU;

/**
 * Created by Administrator on 2015/12/13.
 */
public class Log {
    private Log(){}

    public static void send(byte[] data){
        String sData = BU.bytes2HexGoodLook(data);
        sData = SU.coverWithTime(sData);
        sData = SU.eachLineAddPrefix(sData, '\t', 6);
        System.out.println(sData);
    }

    public static void rec(byte[] data){
        String sData = BU.bytes2HexGoodLook(data);
        sData = SU.coverWithTime(sData);
        System.out.println(sData);
    }

    public static void main(String[] args){
        String a = "123asgasdfasdfasefasefasdfasdfasdfasdfasdf";
        rec(a.getBytes());
        send(a.getBytes());
    }
}
