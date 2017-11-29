import com.alibaba.fastjson.JSON;
import pojo.AppCardInfo;

import java.util.concurrent.ConcurrentHashMap;

public class StringTest {

    public static void main(String []args){

        String str="{\"AppInfoEntity\":{\"addtnlDataPrivate\":\"ASAO00215\",\"cardNo\":\"6236430910100000052\",\"ccy\":\"CNY\",\"channel\":\"11\",\"checkOption\":\"00110\",\"cupAreaCode\":\"92010000\",\"cupDate\":\"20211121\",\"integrateOption\":\"1000\",\"merchNo\":\"123456789012345\",\"reserved\":\"000006000600000000004012000\",\"retCode\":\"000000\",\"retMsg\":\"\",\"track2\":\"30122200000000661F\"},\"sysHeader\":{\"sysHeaderServiceCode\":\"SVR_CARD\",\"seqNo\":\"ECI2021091700071683\"}}";
        int i=str.indexOf("组包报文");
        Object appCardInfo= JSON.parseObject(str).get("AppInfoEntity");
        AppCardInfo cardInf=JSON.parseObject(String.valueOf(appCardInfo), AppCardInfo.class);
        System.out.println( cardInf);

         ConcurrentHashMap<String,Object> concurrentHashMap=new ConcurrentHashMap<String, Object>();
        concurrentHashMap.put("ksy","100");

        System.out.println(concurrentHashMap.get("ksy"));


    }
}
