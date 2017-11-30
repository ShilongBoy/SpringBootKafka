package log;

import com.alibaba.fastjson.JSON;
import model.*;
import pojo.AppCardInfo;
import utils.JaxbUtil;
import utils.JaxbUtil.*;

import java.util.*;

public class ParseXML {


    public static Map<String,Object> parseAcceptXML(String xml) {

        Map<String,Object> resultMap= new HashMap<String,Object>();
        Map<String,String> sysHeaderMap = new HashMap<String,String>();

        AppCardInfo appCardInfo = new AppCardInfo();

        String retCode = "000000";
        StringBuffer retMsgBuffer = new StringBuffer();

        JaxbUtil acceptBinder = new JaxbUtil(AcceptXMLService.class, CollectionWrapper.class);
        AcceptXMLService acceptXMLService = acceptBinder.fromXml(xml);

        if (acceptXMLService != null) {
            // 解析sys-header信息
            if(acceptXMLService.getAcceptSysHeadData()!=null){
                List<AcceptSysHeadData> sysHeadDataList = acceptXMLService.getAcceptSysHeadData();
                for (int i = 0; i < sysHeadDataList.size(); i++) {
                    AcceptSysHeadData tmpSysHeadData = sysHeadDataList.get(i);
                    if (tmpSysHeadData.getName().equals("SYS_HEAD")) {
                        List<AcceptSysHeadStructData> sysHeadStructDataList = tmpSysHeadData.getSysHeadStructData();
                        for(int j = 0; j < sysHeadStructDataList.size(); j++){
                            AcceptSysHeadStructData tmpAcceptSysHeadStructData = sysHeadStructDataList.get(j);
                            String tmpName = tmpAcceptSysHeadStructData.getName();
                            String tmpValue = tmpAcceptSysHeadStructData.getField().getValue();
                            if(tmpName.equals("SERVICE_CODE")){
                                sysHeaderMap.put("sysHeaderServiceCode", tmpValue);
                            }else if(tmpName.equals("SEQ_NO")){
                                sysHeaderMap.put("seqNo", tmpValue);
                            }
                        }
                    }
                }
            }else{
                retCode = "000001";
                retMsgBuffer.append("sys-header 为空;");
            }
            // 解析body信息
            if(acceptXMLService.getBodyData()!=null){
                List<BodyData> bodyDataList = acceptXMLService.getBodyData();
                for(int j=0;j<bodyDataList.size();j++) {
                    BodyData tmpBodyData = bodyDataList.get(j);

                    //CODE,PID,USER_ID,APPLY_TIME 是必填值
                    if (tmpBodyData.getName().equals("CARD_NO")) {
                        String value = tmpBodyData.getField().getValue();
                        if (value != null && !value.equals("")) {
                            appCardInfo.setCardNo(value);
                        } else {
                            retCode = "000001";
                            retMsgBuffer.append("CARD_NO 为空;");
                        }
                    } else if (tmpBodyData.getName().equals("CHECK_OPTION")) {
                        String value = tmpBodyData.getField().getValue();
                        if (value != null && !value.equals("")) {
                            appCardInfo.setCheckOption(value);
                        } else {
                            retCode = "000001";
                            retMsgBuffer.append("CHECK_OPTION 为空;");
                        }
                    } else if (tmpBodyData.getName().equals("TRACK2")) {
                        String value = tmpBodyData.getField().getValue();
                        if (value != null && !value.equals("")) {
                            appCardInfo.setTrack2(value);
                        } else {
                            retCode = "000001";
                            retMsgBuffer.append("TRACK2 为空;");
                        }
                    } else if (tmpBodyData.getName().equals("CCY")) {
                        String value = tmpBodyData.getField().getValue();
                        appCardInfo.setCcy(value);
                    } else if (tmpBodyData.getName().equals("CHANNEL")) {
                        String value = tmpBodyData.getField().getValue();
                        appCardInfo.setChannel(value);
                    } else if (tmpBodyData.getName().equals("CUP_DATE")) {
                        String value = tmpBodyData.getField().getValue();
                        appCardInfo.setCupDate(value);
                    } else if (tmpBodyData.getName().equals("CUP_AREA_CODE")) {
                        String value = tmpBodyData.getField().getValue();
                        appCardInfo.setCupAreaCode(value);
                    } else if (tmpBodyData.getName().equals("MERCH_NO")) {
                        String value = tmpBodyData.getField().getValue();
                        appCardInfo.setMerchNo(value);
                    } else if (tmpBodyData.getName().equals("ADDTNL_DATA_PRIVATE")) {
                        String value = tmpBodyData.getField().getValue();
                        appCardInfo.setAddtnlDataPrivate(value);
                    } else if (tmpBodyData.getName().equals("RESERVED")) {
                        String value = tmpBodyData.getField().getValue();
                        appCardInfo.setReserved(value);
                    } else if (tmpBodyData.getName().equals("INTEGRATE_OPTION")) {
                        String value = tmpBodyData.getField().getValue();
                        appCardInfo.setIntegrateOption(value);
                    } else if (tmpBodyData.getName().equals("TRAN_AMT")) {
                        String value = tmpBodyData.getField().getValue();
                        appCardInfo.setTranAmt(Double.valueOf(value));
                    }

                }
            }else{
                retCode = "000001";
                retMsgBuffer.append("body 为空;");
            }
        }else{
            retCode = "000001";
            retMsgBuffer.append("xml无法解析");
        }
        appCardInfo.setRetCode(retCode);
        appCardInfo.setRetMsg(retMsgBuffer.toString());
        resultMap.put("AppCardInfo", appCardInfo);
        resultMap.put("sysHeader", sysHeaderMap);
        return resultMap;
    }

    public static void main(String []args){

        String str="(com.dc.governance.metadata.impls.interpretive.InterpretivePacker.invoke:147) - 组包报文：<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<service>\n" +
                "    <sys-header>\n" +
                "        <data name=\"SYS_HEAD\">\n" +
                "            <struct>\n" +
                "                <data name=\"SERVICE_CODE\">\n" +
                "                    <field length=\"30\" type=\"string\">SVR_CARD</field>\n" +
                "                </data>\n" +
                "                <data name=\"TRAN_MODE\">\n" +
                "                    <field length=\"10\" type=\"string\">ONLINE</field>\n" +
                "                </data>\n" +
                "                <data name=\"SOURCE_TYPE\">\n" +
                "                    <field length=\"2\" type=\"string\">UC</field>\n" +
                "                </data>\n" +
                "                <data name=\"BRANCH_ID\">\n" +
                "                    <field length=\"6\" type=\"string\">027001</field>\n" +
                "                </data>\n" +
                "                <data name=\"USER_ID\">\n" +
                "                    <field length=\"30\" type=\"string\">EC0001</field>\n" +
                "                </data>\n" +
                "                <data name=\"TRAN_DATE\">\n" +
                "                    <field length=\"8\" type=\"string\">20210917</field>\n" +
                "                </data>\n" +
                "                <data name=\"TRAN_TIMESTAMP\">\n" +
                "                    <field length=\"9\" type=\"string\">102648</field>\n" +
                "                </data>\n" +
                "                <data name=\"SERVER_ID\">\n" +
                "                    <field length=\"30\" type=\"string\">127.0.0.1</field>\n" +
                "                </data>\n" +
                "                <data name=\"USER_LANG\">\n" +
                "                    <field length=\"20\" type=\"string\">CHINESE</field>\n" +
                "                </data>\n" +
                "                <data name=\"SEQ_NO\">\n" +
                "                    <field length=\"50\" type=\"string\">ECI2021091700071683</field>\n" +
                "                </data>\n" +
                "                <data name=\"AUTH_FLAG\">\n" +
                "                    <field length=\"1\" type=\"string\">M</field>\n" +
                "                </data>\n" +
                "                <data name=\"SOURCE_BRANCH_NO\">\n" +
                "                    <field length=\"10\" type=\"string\">cups.gaps.zpk</field>\n" +
                "                </data>\n" +
                "                <data name=\"MODULE_ID\">\n" +
                "                    <field length=\"2\" type=\"string\">RB</field>\n" +
                "                </data>\n" +
                "                <data name=\"MESSAGE_TYPE\">\n" +
                "                    <field length=\"4\" type=\"string\">1200</field>\n" +
                "                </data>\n" +
                "                <data name=\"MESSAGE_CODE\">\n" +
                "                    <field length=\"6\" type=\"string\">6077</field>\n" +
                "                </data>\n" +
                "            </struct>\n" +
                "        </data>\n" +
                "    </sys-header>\n" +
                "    <body>\n" +
                "        <data name=\"CARD_NO\">\n" +
                "            <field type=\"string\" length=\"19\" scale=\"0\">6236430910100000052</field>\n" +
                "        </data>\n" +
                "        <data name=\"CHECK_OPTION\">\n" +
                "            <field type=\"string\" length=\"4\" scale=\"0\">00110</field>\n" +
                "        </data>\n" +
                "        <data name=\"TRACK2\">\n" +
                "            <field type=\"string\" length=\"37\" scale=\"0\">30122200000000661F</field>\n" +
                "        </data>\n" +
                "        <data name=\"CCY\">\n" +
                "            <field type=\"string\" length=\"3\" scale=\"0\">CNY</field>\n" +
                "        </data>\n" +
                "        <data name=\"CHANNEL\">\n" +
                "            <field type=\"string\" length=\"2\" scale=\"0\">11</field>\n" +
                "        </data>\n" +
                "        <data name=\"CUP_DATE\">\n" +
                "            <field type=\"string\" length=\"8\" scale=\"0\">20211121</field>\n" +
                "        </data>\n" +
                "        <data name=\"CUP_AREA_CODE\">\n" +
                "            <field type=\"string\" length=\"8\" scale=\"0\">92010000</field>\n" +
                "        </data>\n" +
                "        <data name=\"MERCH_NO\">\n" +
                "            <field type=\"string\" length=\"15\" scale=\"0\">123456789012345</field>\n" +
                "        </data>\n" +
                "        <data name=\"ADDTNL_DATA_PRIVATE\">\n" +
                "            <field type=\"string\" length=\"512\" scale=\"0\">ASAO00215</field>\n" +
                "        </data>\n" +
                "        <data name=\"RESERVED\">\n" +
                "            <field type=\"string\" length=\"100\" scale=\"0\">000006000600000000004012000</field>\n" +
                "        </data>\n" +
                "        <data name=\"INTEGRATE_OPTION\">\n" +
                "            <field type=\"string\" length=\"20\" scale=\"0\">1000</field>\n" +
                "        </data>\n" +
                "        <data name=\"TRAN_AMT\">\n" +
                "            <field type=\"string\" length=\"20\" scale=\"0\">000000016100</field>\n" +
                "        </data>\n" +
                "    </body>\n" +
                "</service>";


        ParseXML parseXML=new ParseXML();

        String subStr=str.substring(str.indexOf("组包报文")+5,str.length()).trim();
        System.out.println("teh message is:"+subStr);

        HashMap<String,Object> hashMap= (HashMap<String, Object>) parseXML.parseAcceptXML(subStr);

        System.out.println(JSON.toJSONString(hashMap));


    }
}
