/*
package log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import net.shuzun.zbank.datacenter.source.internal.mock.AppInfoMockDataSource;
import net.shuzun.zbank.datacenter.source.internal.mock.entity.AppInfoEntity;
import net.shuzun.zbank.datacenter.source.internal.mock.entity.SysLogEntity;
import net.shuzun.zbank.datacenter.source.internal.mock.entity.SysTestEntity;
import net.shuzun.zbank.datacenter.source.internal.mock.entity.XDCodeEngineMappingEntity;
import net.shuzun.zbank.decision.server.decision.RuleInfo;
import net.shuzun.zbank.decision.server.decision.ZBankResult;
import net.shuzun.zbank.decision.server.decision.ZBankUtil;
import net.shuzun.zbank.decision.server.model.AcceptSysHeadData;
import net.shuzun.zbank.decision.server.model.AcceptSysHeadStructData;
import net.shuzun.zbank.decision.server.model.AcceptXMLService;
import net.shuzun.zbank.decision.server.model.BodyData;
import net.shuzun.zbank.decision.server.model.BodyDataArray;
import net.shuzun.zbank.decision.server.model.BodyDataArrayData;
import net.shuzun.zbank.decision.server.model.BodyDataArrayDataField;
import net.shuzun.zbank.decision.server.model.BodyDataField;
import net.shuzun.zbank.decision.server.model.ReturnAppHeadData;
import net.shuzun.zbank.decision.server.model.ReturnLocalHeadData;
import net.shuzun.zbank.decision.server.model.ReturnSysHeadData;
import net.shuzun.zbank.decision.server.model.ReturnSysHeadStructData;
import net.shuzun.zbank.decision.server.model.ReturnXMLService;
import net.shuzun.zbank.decision.server.model.SubOrder;
import net.shuzun.zbank.decision.server.model.SysHeadStructDataArray;
import net.shuzun.zbank.decision.server.model.SysHeadStructDataArrayData;
import net.shuzun.zbank.decision.server.model.SysHeadStructDataArrayDataField;
import net.shuzun.zbank.decision.server.model.SysHeadStructDataField;
import net.shuzun.zbank.decision.server.model.Traveler;
import net.shuzun.zbank.decision.server.utils.HttpPostUtils;
import net.shuzun.zbank.decision.server.utils.JaxbUtil;
import net.shuzun.zbank.decision.server.utils.JaxbUtil.CollectionWrapper;
import net.shuzun.zbank.decision.server.utils.SpringUtil;

public class Handler extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(Handler.class);

	private Socket socket = null;

	private OutputStream os = null;

	private InputStream is = null;

	String sRequestMessage = "";

	String sResponseMessage = "";

	public Handler(Socket socket) {
		this.socket = socket;
	}

	private OutputStream getOutputStream(Socket socket) {
		try {
			os = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return os;
	}

	private InputStream getInputStream(Socket socket) {
		try {
			is = new BufferedInputStream(new DataInputStream(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return is;
	}

	public void run() {
		logger.info("新连接建立:" + socket.getInetAddress() + ":" + socket.getPort());

		is = getInputStream(socket);
		os = getOutputStream(socket);

		final long start = System.currentTimeMillis();
		logger.info("开始时间戳："+start);

		try {
			byte[] b1 = new byte[10];
			is.read(b1, 0, 10);
			String s1 = new String(b1, "UTF-8");
			int length = Integer.parseInt(s1) + 10;

			byte[] b = new byte[1024 * 10000];
//			is.read(b, 0, length+10000);

			int readBytes = 0;
			while (readBytes < length) {
                int read = is.read(b, readBytes, length - readBytes);
               //判断是不是读到了数据流的末尾 ，防止出现死循环。
                if (read == -1) {
                    break;
                }
                readBytes += read;
            }

			// 请求报文按着utf8解析
			sRequestMessage = new String(b, "UTF-8");
			sRequestMessage = sRequestMessage.trim();

			// 完整报文
			sRequestMessage = s1 + sRequestMessage;
			try {
				logger.info("请求报文："+sRequestMessage);
				// 截掉10位报文头长
				sRequestMessage = sRequestMessage.substring(10);

				AppInfoMockDataSource appInfoDatasource = SpringUtil.getBean(AppInfoMockDataSource.class);
				String bodUrl = appInfoDatasource.getBodUrl();
				logger.info("塔URL"+bodUrl);

				// 信贷请求数据落地
				SysLogEntity sysLogXdRequest = new SysLogEntity();
				sysLogXdRequest.setBody(sRequestMessage);
				logger.info("信贷请求数据落地开始");
				appInfoDatasource.saveLogData(sysLogXdRequest);
				logger.info("信贷请求数据落地结束");

				// 1、 解析请求的xml
				logger.info("解析请求的xml开始");
				Map<String,Object> resultMap = parseAcceptXML(sRequestMessage);
				logger.info("解析请求的xml结束");
				if(resultMap.get("AppInfoEntity")!=null){
					AppInfoEntity appInfoEntity = (AppInfoEntity) resultMap.get("AppInfoEntity");
					String retCode = "";
					if(appInfoEntity.getRetCode()!=null){
						retCode = appInfoEntity.getRetCode();
					}

					Map<String,String> sysHeadMap = new HashMap<String,String>();
					Map<String,String> sysHeadRetMap = new HashMap<String,String>();
					Map<String,Object> bodyMap = new HashMap<String,Object>();
					Map<String,Object> returnMap = new HashMap<String,Object>();

					String retStatus = "S";
					String retMsg = "";

					if (retCode.equals("000001")) {//解析关键信息缺失导致失败
						logger.info("解析请求的xml的结果：失败");
						retStatus = "F";
						retMsg = appInfoEntity.getRetMsg();
						sysHeadRetMap.put("ret_status", retStatus);
						sysHeadRetMap.put("ret_code", retCode);
						sysHeadRetMap.put("ret_msg", retMsg);
						returnMap.put("syshead", sysHeadMap);
						returnMap.put("syshead-ret", sysHeadRetMap);
						returnMap.put("body", bodyMap);
						sResponseMessage = buildReturnXML(returnMap);
					}else if(retCode.equals("000000")){//解析成功开始处理
						logger.info("解析请求的xml的结果：成功");
						String errCode = "";
						// 2、根据xdcode找映射关系（1、engineCode 2、是否申请数据，申请数据落地app_info，跑批数据落地到app_info_batch）
						String engineCode = "";
						String fqzFlag = "";
						String xdCode = "";
						String xyedFieldEn = "";
						String lvFieldEn = "";
						if(appInfoEntity.getXdCode()!=null){
							xdCode = appInfoEntity.getXdCode();
							XDCodeEngineMappingEntity xdEntity = appInfoDatasource.getXDEntity(xdCode);
							if(xdEntity!=null){
								engineCode = xdEntity.getEngineCode();
								fqzFlag = xdEntity.getFqzFlag();
								xyedFieldEn = xdEntity.getXyedFieldEn();
								lvFieldEn = xdEntity.getLvFieldEn();
								logger.info("获取xdcode映射关系ok");
							}else{
								logger.info("找不到xdcode的映射关系");
								errCode = "XDCODE_NOTEXISTS";
							}
						}
						if(fqzFlag.equals("1")){ //申请数据
							logger.info("申请数据落地开始");
							int i = (appInfoDatasource).saveData(appInfoEntity);
							if(i==1){
								// 3、数据异步落地 (贷前申请与贷中贷后跑批在反欺诈次数统计上分开处理)
								   // app_info_address app_info_gps app_info_ip app_info_device
								appInfoDatasource.saveFQZ(appInfoEntity);
							}else if(i==0){ // 申请数据pid重复
								logger.info("申请数据pid重复");
								errCode = "DAIQIAN_DUP";
							}
							logger.info("申请数据落地结束");
						}else if(fqzFlag.equals("0")){ //贷中、贷后跑批数据
							logger.info("贷中、贷后跑批数据落地");
							int i = appInfoDatasource.saveBatch(appInfoEntity);
							if (i!=1){
								logger.info("贷中、贷后跑批数据落地失败");
								errCode = "DAIHOU_SAVEERROR";
							}
						}

						if(errCode.equals("")){
							// 4、 调用数尊塔系统
							String pid = "";
							String uid = "";
							if(appInfoEntity.getPid()!=null){
								pid = appInfoEntity.getPid();
							}
							if(appInfoEntity.getUid()!=null){
								uid = appInfoEntity.getUid();
							}
							Map<String,String> reqMap = new HashMap<String,String>();
							reqMap.put("engineCode", engineCode);
							reqMap.put("pid", pid);
							reqMap.put("uid", uid);
							reqMap.put("reqType", appInfoDatasource.getReqType());
							reqMap.put("organId", appInfoDatasource.getOrganId());
							logger.info("开始调用塔:");

							//调用塔 返回数据串落地 解析对象
							Map<String,Object> zbankResultMap = new HashMap<String,Object>();
							zbankResultMap = reqDecision(reqMap,bodUrl);
							String taRespStr = "";
							logger.info("塔返回对象转string:"+zbankResultMap.toString());
							if(zbankResultMap.size()==0){
								retStatus = "F";
								retCode = "000001";
								retMsg = "塔返回结果异常";
							}
							if(zbankResultMap.get("json")!=null){
								SysLogEntity sysLogTaResponse = new SysLogEntity();
								sysLogTaResponse.setPid(pid);
								sysLogTaResponse.setUid(uid);
								sysLogTaResponse.setXdCode(xdCode);
								taRespStr = zbankResultMap.get("json").toString();
								sysLogTaResponse.setBody(taRespStr);
								logger.info("塔返回数据落地开始");
								logger.info("塔返回串:"+taRespStr);
								appInfoDatasource.saveLogData(sysLogTaResponse);
								logger.info("塔返回数据落地结束");
							}
							String loadSum = "";
							String interestRate = "";
							ZBankResult zBankResult = new ZBankResult();
							if(zbankResultMap.get("obj")!=null){
								zBankResult = (ZBankResult) zbankResultMap.get("obj");
								if(zBankResult.getDecisionOpt()!=null){
									Map<String,String> outMap = zBankResult.getDecisionOpt().getOutMap();
									if(outMap!=null){
										if(xyedFieldEn!=null&&!xyedFieldEn.equals("")){
											loadSum = outMap.get(xyedFieldEn);
										}
										if(lvFieldEn!=null&&!lvFieldEn.equals("")){
											interestRate = outMap.get(lvFieldEn);
										}
									}
								}
								logger.info("塔返回串转对象:" + zBankResult.getResultString());
							}

							// 6、封装xml（带长度）返回给信贷
							String resultIndex = "";
							if(zBankResult.getResultIndex()!=null){
								resultIndex = zBankResult.getResultIndex();
								if(resultIndex.equals("-1")){
									retStatus = "F";
									retCode = "000001";
									retMsg = "业务异常";
								}else if(resultIndex.equals("1")){ //把塔返回决策结果为“通过”的异步落地， 包含 身份证、 手机号码、 银行卡、账户历史累计申请成功次数
									appInfoDatasource.saveAppSucc(appInfoEntity);
								}
							}else{
								retStatus = "F";
								retCode = "000001";
								retMsg = "业务异常";
							}
							sysHeadRetMap.put("ret_status", retStatus);
							sysHeadRetMap.put("ret_code", retCode);
							sysHeadRetMap.put("ret_msg", retMsg);
							returnMap.put("syshead", sysHeadMap);
							returnMap.put("syshead-ret", sysHeadRetMap);
							bodyMap.put("PASS_STATUS", resultIndex);
							bodyMap.put("LOAN_SUM", loadSum);//临时赋值
							bodyMap.put("CREDIT_SCORE", "");
							bodyMap.put("INTEREST_RATE", interestRate);

							// 一个节点加减分总分值
	 						String ruleScore = "";
							List<String> strList = new ArrayList<String>();
							if(zBankResult.getRuleInfos()!=null){
								List<RuleInfo> ruleInfoList = zBankResult.getRuleInfos();
								for(int i=0;i<ruleInfoList.size();i++){
									RuleInfo ruleInfo = ruleInfoList.get(i);
									if(ruleInfo.getScoreIndex()!=null){
										ruleScore = ruleInfo.getScoreIndex();
									}
									if(ruleInfo.getReasons()!=null&&ruleInfo.getReasons().size()>0){
										strList = ruleInfo.getReasons(); //就取第一个
									}
								}
							}
							bodyMap.put("PLUSMINUS_SCORE", ruleScore);
							bodyMap.put("RR",strList);
							returnMap.put("body", bodyMap);
							sResponseMessage = buildReturnXML(returnMap);

							// 异步落地返回给信贷的数据
							SysLogEntity sysLogXdResponse = new SysLogEntity();
							sysLogXdResponse.setPid(pid);
							sysLogXdResponse.setUid(uid);
							sysLogXdResponse.setXdCode(xdCode);
							sysLogXdResponse.setBody(sResponseMessage);
							appInfoDatasource.saveLogData(sysLogXdResponse);

							// 异步落地验证测试结果数据
							SysTestEntity sysTestEntity = new SysTestEntity();
							sysTestEntity.setPid(pid);
							sysTestEntity.setUid(uid);
							sysTestEntity.setXdCode(xdCode);
							sysTestEntity.setResultIndex(resultIndex);
							sysTestEntity.setRuleScore(ruleScore);
							String refuseReason = "";
							for(int k=0;k<strList.size();k++){
								refuseReason += strList.get(k);
							}
							sysTestEntity.setRefuseReason(refuseReason);
							sysTestEntity.setTaResp(taRespStr);
							sysTestEntity.setResultResp(sResponseMessage);
							sysTestEntity.setCostTime(System.currentTimeMillis()-start);
							appInfoDatasource.saveTestData(sysTestEntity);
						}else{

							sysHeadRetMap.put("ret_status", "F");
							sysHeadRetMap.put("ret_code", "000001");

							if(errCode.equals("XDCODE_NOTEXISTS")){
								sysHeadRetMap.put("ret_msg", "找不到xdcode与引擎的映射关系");
							}else if(errCode.equals("DAIQIAN_DUP")){
								sysHeadRetMap.put("ret_msg", "申请数据PID重复");
							}else if(errCode.equals("DAIHOU_SAVEERROR")){
								sysHeadRetMap.put("ret_msg", "贷后数据保存失败");
							}

							returnMap.put("syshead", sysHeadMap);
							returnMap.put("syshead-ret", sysHeadRetMap);
							returnMap.put("body", bodyMap);
							sResponseMessage = buildReturnXML(returnMap);
						}

					}
				}else{
					Map<String,String> sysHeadMap = new HashMap<String,String>();
					Map<String,String> sysHeadRetMap = new HashMap<String,String>();
					Map<String,Object> bodyMap = new HashMap<String,Object>();
					Map<String,Object> returnMap = new HashMap<String,Object>();
					sysHeadRetMap.put("ret_status", "F");
					sysHeadRetMap.put("ret_code", "000001");
					sysHeadRetMap.put("ret_msg", "业务异常：信贷发送的报文无法解析");
					returnMap.put("syshead", sysHeadMap);
					returnMap.put("syshead-ret", sysHeadRetMap);
					returnMap.put("body", bodyMap);
					sResponseMessage = buildReturnXML(returnMap);
				}
			} catch (Exception e) {
				logger.info("something wrong occur");

				Map<String,String> sysHeadMap = new HashMap<String,String>();
				Map<String,String> sysHeadRetMap = new HashMap<String,String>();
				Map<String,Object> bodyMap = new HashMap<String,Object>();
				Map<String,Object> returnMap = new HashMap<String,Object>();
				sysHeadRetMap.put("ret_status", "F");
				sysHeadRetMap.put("ret_code", "000001");
				sysHeadRetMap.put("ret_msg", "数据中心执行失败");
				returnMap.put("syshead", sysHeadMap);
				returnMap.put("syshead-ret", sysHeadRetMap);
				returnMap.put("body", bodyMap);
				sResponseMessage = buildReturnXML(returnMap);

				e.printStackTrace();
			}

			String respLength = String.format("%010d", sResponseMessage.getBytes().length);
			sResponseMessage = respLength + "" + sResponseMessage;
			os.write(sResponseMessage.getBytes("UTF-8"));
			os.flush();

			String cost = System.currentTimeMillis()-start + "";
			logger.info("服务器端发送成功！"+ "耗时：" + cost + "ms , 返回串xml：" + sResponseMessage);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private Map<String,Object> parseAcceptXML(String xml) {

		Map<String,Object> resultMap= new HashMap<String,Object>();
		Map<String,String> sysHeaderMap = new HashMap<String,String>();

		AppInfoEntity appInfoEntity = new AppInfoEntity();

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
							}else if(tmpName.equals("TRAN_CODE")){
								sysHeaderMap.put("sysHeaderTranCode", tmpValue);
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
				for(int j=0;j<bodyDataList.size();j++){
					BodyData tmpBodyData = bodyDataList.get(j);

					//CODE,PID,USER_ID,APPLY_TIME 是必填值
					if(tmpBodyData.getName().equals("CODE")){
						String value = tmpBodyData.getField().getValue();
						if(value!=null&&!value.equals("")){
							appInfoEntity.setXdCode(value);
						}else{
							retCode = "000001";
							retMsgBuffer.append("CODE 为空;");
						}
					}else if(tmpBodyData.getName().equals("PID")){
						String value = tmpBodyData.getField().getValue();
						if(value!=null&&!value.equals("")){
							appInfoEntity.setPid(value);
						}else{
							retCode = "000001";
							retMsgBuffer.append("PID 为空;");
						}
					}else if(tmpBodyData.getName().equals("USER_ID")){
						String value = tmpBodyData.getField().getValue();
						if(value!=null&&!value.equals("")){
							appInfoEntity.setUid(value);
						}else{
							retCode = "000001";
							retMsgBuffer.append("USER_ID 为空;");
						}
					}else if(tmpBodyData.getName().equals("APPLY_TIME")){
						String value = tmpBodyData.getField().getValue();
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date date = null;
						   try {
						    date = format.parse(value);
						   } catch (ParseException e) {
						    e.printStackTrace();
						   }
						if(value!=null&&!value.equals("")){
							appInfoEntity.setDatetime(date);
						}else{
							retCode = "000001";
							retMsgBuffer.append("APPLY_TIME 为空;");
						}
					}else if(tmpBodyData.getName().equals("APPLY_NAME")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setName(value);
					}else if(tmpBodyData.getName().equals("APPLY_CARD_TYPE")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setApplyCardType(value);
					}else if(tmpBodyData.getName().equals("APPLY_CARD_NO")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setIdNumber(value);
					}else if(tmpBodyData.getName().equals("APPLY_PHONE_NO")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setMobile(value);
					}else if(tmpBodyData.getName().equals("APPLY_BANK_NO")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setBankNumber(value);
					}else if(tmpBodyData.getName().equals("APPLY_ADDR_PROV")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setContactProv(value);
					}else if(tmpBodyData.getName().equals("APPLY_ADDR_CITY")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setContactCity(value);
					}else if(tmpBodyData.getName().equals("APPLY_ADDR_ZONE")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setContactZone(value);
					}else if(tmpBodyData.getName().equals("APPLY_ADDR_OTHERS")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setContactOther(value);
					}else if(tmpBodyData.getName().equals("CONTACT_NAME1")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setFirstContactName(value);
					}else if(tmpBodyData.getName().equals("CONTACT_PHONE1")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setFirstContactPhone(value);
					}else if(tmpBodyData.getName().equals("CONTACT_NAME2")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setSecondContactName(value);
					}else if(tmpBodyData.getName().equals("CONTACT_PHONE2")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setSecondContactPhone(value);
					}else if(tmpBodyData.getName().equals("TRAVEL_DATE")){
						String value = tmpBodyData.getField().getValue();
						if(value!=null&&value.equals("")){
							appInfoEntity.setTravelDate(value);
						}
					}else if(tmpBodyData.getName().equals("TRAVEL_NUM")){
						String value = tmpBodyData.getField().getValue();
						if(value.equals("")){
							value = "0";
						}
						appInfoEntity.setTravelNum(value);
					}else if(tmpBodyData.getName().equals("TRAVELERS_ARRAY")){
						//出行人信息列表
						List<BodyDataArray> bodyDataArrayList = new ArrayList<BodyDataArray>();
						bodyDataArrayList = tmpBodyData.getBodyDataArray();
						List<Traveler> travelerList = new ArrayList<Traveler>();
						Gson gson = new Gson();
						for(int m=0;m<bodyDataArrayList.size();m++){
							BodyDataArray bodyDataArray = bodyDataArrayList.get(m);
							if(bodyDataArray!=null){
								List<BodyDataArrayData> bodyDataArrayDataList = bodyDataArray.getBodyDataArrayData();
								Traveler traveler = new Traveler();
								if(bodyDataArrayDataList!=null){
									for(int n=0; n<bodyDataArrayDataList.size();n++){
										BodyDataArrayData bodyDataArrayData = bodyDataArrayDataList.get(n);
										String name = bodyDataArrayData.getName();
										String value = "";
										List<BodyDataArrayDataField> bodyDataArrayDataFieldList = bodyDataArrayData.getBodyDataArrayDataField();
										for(int k=0; k<bodyDataArrayDataFieldList.size();k++){
											BodyDataArrayDataField bodyDataArrayDataField = bodyDataArrayDataFieldList.get(k);
											value = bodyDataArrayDataField.getValue();
										}
										if(name.equals("CLIENT_NAME")&&!value.equals("")){
											traveler.setTravelerName(value);
										}
										if(name.equals("DOCUMENT_TYPE")&&!value.equals("")){
											traveler.setTravelerDocumentType(value);
										}
										if(name.equals("DOCUMENT_ID")&&!value.equals("")){
											traveler.setTravelerDocumentId(value);
										}
										if(name.equals("PHONE_NO")&&!value.equals("")){
											traveler.setTravelerPhone(value);
										}
									}
								}
								if(traveler!=null){
									travelerList.add(traveler);
								}
								if(travelerList.size()>0){
									appInfoEntity.setTravelersArray(gson.toJson(travelerList));
								}
							}
						}
					}else if(tmpBodyData.getName().equals("ORDER_AMT")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setOrderAmt(value);
					}else if(tmpBodyData.getName().equals("ORDER_NAME")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setOrderName(value);
					}else if(tmpBodyData.getName().equals("SUBORDER_ARRAY")){
						//子订单信息列表
						List<BodyDataArray> bodyDataArrayList = new ArrayList<BodyDataArray>();
						bodyDataArrayList = tmpBodyData.getBodyDataArray();
						List<SubOrder> subOrderList = new ArrayList<SubOrder>();
						Gson gson = new Gson();
						for(int m=0;m<bodyDataArrayList.size();m++){
							BodyDataArray bodyDataArray = bodyDataArrayList.get(m);
							List<BodyDataArrayData> bodyDataArrayDataList = bodyDataArray.getBodyDataArrayData();
							SubOrder subOrder = new SubOrder();
							if(bodyDataArrayDataList!=null){
								for(int n=0; n<bodyDataArrayDataList.size();n++){
									BodyDataArrayData bodyDataArrayData = bodyDataArrayDataList.get(n);
									String name = bodyDataArrayData.getName();
									String value = "";
									List<BodyDataArrayDataField> bodyDataArrayDataFieldList = bodyDataArrayData.getBodyDataArrayDataField();
									for(int k=0; k<bodyDataArrayDataFieldList.size();k++){
										BodyDataArrayDataField bodyDataArrayDataField = bodyDataArrayDataFieldList.get(k);
										value = bodyDataArrayDataField.getValue();
									}
									if(name.equals("SUBORDER_TYPE")&&!value.equals("")){
										subOrder.setSubOrderType(value);
									}
									if(name.equals("SUBORDER_AMOUNT")&&!value.equals("")){
										subOrder.setSubOrderAmount(value);
									}
								}
							}
							if(subOrder!=null){
								subOrderList.add(subOrder);
							}
							if(subOrderList.size()>0){
								appInfoEntity.setSubOrderArray(gson.toJson(subOrderList));
							}
						}
					}else if(tmpBodyData.getName().equals("TERM")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setTerm(value);
					}else if(tmpBodyData.getName().equals("TERM_TYPE")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setTermType(value);
					}else if(tmpBodyData.getName().equals("EQMTID")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setDeviceId(value);
					}else if(tmpBodyData.getName().equals("EQMTIP")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setIp(value);
					}else if(tmpBodyData.getName().equals("EQMTGPS")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setGps(value);
					}else if(tmpBodyData.getName().equals("MAC")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setMac(value);
					}else if(tmpBodyData.getName().equals("WIFIMAC")){
						String value = tmpBodyData.getField().getValue();
						appInfoEntity.setWifimac(value);
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
		appInfoEntity.setRetCode(retCode);
		appInfoEntity.setRetMsg(retMsgBuffer.toString());
		resultMap.put("AppInfoEntity", appInfoEntity);
		resultMap.put("sysHeader", sysHeaderMap);
		return resultMap;
	}

	@SuppressWarnings("unchecked")
	private String buildReturnXML(Map<String,Object> resultMap){

		Map<String,String> sysHeadMap = (Map<String, String>) resultMap.get("syshead");
		Map<String,String> sysHeadRetMap = (Map<String, String>) resultMap.get("syshead-ret");
		Map<String,Object> bodyMap = (Map<String, Object>) resultMap.get("body");

		ReturnXMLService rs = new ReturnXMLService();

		//sys-header
		//sys-header:SERVICE_CODE
		ReturnSysHeadStructData serviceCodeData = new ReturnSysHeadStructData();
		serviceCodeData.setName("SERVICE_CODE");

		SysHeadStructDataField serviceCodeField = new SysHeadStructDataField();
		serviceCodeField.setLength("7");
		serviceCodeField.setScale("0");
		serviceCodeField.setType("string");
		serviceCodeField.setValue("SVR_BOD");
		serviceCodeData.setField(serviceCodeField);

		//sys-header:RET_STATUS
		ReturnSysHeadStructData retStatusData = new ReturnSysHeadStructData();
		retStatusData.setName("RET_STATUS");

		SysHeadStructDataField retStatusField = new SysHeadStructDataField();
//		retStatusField.setLength("1");
		String retStatus = sysHeadRetMap.get("ret_status");
		retStatusField.setLength(retStatus.length()+"");
		retStatusField.setScale("0");
		retStatusField.setType("string");
//		retStatusField.setValue("S");
		retStatusField.setValue(retStatus);
		retStatusData.setField(retStatusField);

		//sys-header:RET
		ReturnSysHeadStructData retData = new ReturnSysHeadStructData();
		retData.setName("RET");
		String retCode = sysHeadRetMap.get("ret_code");
		SysHeadStructDataArray retArray = new SysHeadStructDataArray();
		List<SysHeadStructDataArrayData> sysHeadStructDataArrayDataList = new ArrayList<SysHeadStructDataArrayData>();

		SysHeadStructDataArrayData sysHeadStructDataArrayDataCode = new SysHeadStructDataArrayData();
		List<SysHeadStructDataArrayDataField> sysHeadStructDataArrayDataFieldListCode = new ArrayList<SysHeadStructDataArrayDataField>();
		SysHeadStructDataArrayDataField sysHeadStructDataArrayDataFieldCode = new SysHeadStructDataArrayDataField();
		sysHeadStructDataArrayDataFieldCode.setLength(retCode.length()+"");
//		sysHeadStructDataArrayDataFieldCode.setLength("10");
		sysHeadStructDataArrayDataFieldCode.setScale("0");
		sysHeadStructDataArrayDataFieldCode.setType("string");
//		sysHeadStructDataArrayDataFieldCode.setValue("000000");
		sysHeadStructDataArrayDataFieldCode.setValue(retCode);
		sysHeadStructDataArrayDataFieldListCode.add(sysHeadStructDataArrayDataFieldCode);
		sysHeadStructDataArrayDataCode.setName("RET_CODE");
		sysHeadStructDataArrayDataCode.setSysHeadStructDataArrayDataField(sysHeadStructDataArrayDataFieldListCode);
		//sys-header:RET:RET_CODE
		sysHeadStructDataArrayDataList.add(sysHeadStructDataArrayDataCode);

		String retMsg = sysHeadRetMap.get("ret_msg");
		SysHeadStructDataArrayData sysHeadStructDataArrayDataMsg = new SysHeadStructDataArrayData();
		List<SysHeadStructDataArrayDataField> sysHeadStructDataArrayDataFieldListMsg = new ArrayList<SysHeadStructDataArrayDataField>();
		SysHeadStructDataArrayDataField sysHeadStructDataArrayDataFieldMsg = new SysHeadStructDataArrayDataField();
//		sysHeadStructDataArrayDataFieldMsg.setLength("0");
		sysHeadStructDataArrayDataFieldMsg.setLength(retMsg.length()+"");
		sysHeadStructDataArrayDataFieldMsg.setScale("0");
		sysHeadStructDataArrayDataFieldMsg.setType("string");
		sysHeadStructDataArrayDataFieldMsg.setValue(retMsg);
		sysHeadStructDataArrayDataFieldListMsg.add(sysHeadStructDataArrayDataFieldMsg);
		sysHeadStructDataArrayDataMsg.setName("RET_MSG");
		sysHeadStructDataArrayDataMsg.setSysHeadStructDataArrayDataField(sysHeadStructDataArrayDataFieldListMsg);
		//sys-header:RET:RET_MSG
		sysHeadStructDataArrayDataList.add(sysHeadStructDataArrayDataMsg);

		retArray.setSysHeadStructDataArrayData(sysHeadStructDataArrayDataList);
		retData.setSysHeadStructDataArray(retArray);

		List<ReturnSysHeadStructData> SysHeadStructDataList = new ArrayList<ReturnSysHeadStructData>();
		// add SERVICE_CODE
		SysHeadStructDataList.add(serviceCodeData);
		// add RET_STATUS
		SysHeadStructDataList.add(retStatusData);
		// add RET
		SysHeadStructDataList.add(retData);

		ReturnSysHeadData sysHeadData = new ReturnSysHeadData();
		sysHeadData.setName("SYS_HEAD");
		sysHeadData.setSysHeadStructData(SysHeadStructDataList);

		List<ReturnSysHeadData> respSysHeadDataList = new ArrayList<ReturnSysHeadData>();
		respSysHeadDataList.add(sysHeadData);

		//app-header 暂不处理

		//body
		List<BodyData> bodyDataList = new ArrayList<BodyData>();
		if(bodyMap.size()!=0){
			//body:PASS_STATUS
			BodyData bodyDataPassStatus = new BodyData();
			bodyDataPassStatus.setName("PASS_STATUS");

			BodyDataField bodyDataFieldPassStatus = new BodyDataField();
			String passStatus = String.valueOf(bodyMap.get("PASS_STATUS"));
			bodyDataFieldPassStatus.setLength(passStatus.length()+"");
			bodyDataFieldPassStatus.setScale("0");
			bodyDataFieldPassStatus.setType("string");
			bodyDataFieldPassStatus.setValue(passStatus); //1 通过 2 拒绝 3 人工审核
			bodyDataPassStatus.setField(bodyDataFieldPassStatus);

			bodyDataList.add(bodyDataPassStatus);

			//body:LOAN_SUM
			BodyData bodyDataLoanSum = new BodyData();
			bodyDataLoanSum.setName("LOAN_SUM");

			BodyDataField bodyDataFieldLoanSum = new BodyDataField();
			String loanSum = String.valueOf(bodyMap.get("LOAN_SUM"));
			bodyDataFieldLoanSum.setLength(loanSum.length() + "");
			bodyDataFieldLoanSum.setScale("2");
			bodyDataFieldLoanSum.setType("double");
			bodyDataFieldLoanSum.setValue(loanSum); //审核通过的额度
			bodyDataLoanSum.setField(bodyDataFieldLoanSum);

			bodyDataList.add(bodyDataLoanSum);

			//body:CREDIT_SCORE
			BodyData bodyDataCreditScore = new BodyData();
			bodyDataCreditScore.setName("CREDIT_SCORE");

			BodyDataField bodyDataFieldCreditScore = new BodyDataField();
			String creditScore = String.valueOf(bodyMap.get("CREDIT_SCORE"));
			bodyDataFieldCreditScore.setLength(creditScore.length() + "");
			bodyDataFieldCreditScore.setScale("2");
			bodyDataFieldCreditScore.setType("double");
			bodyDataFieldCreditScore.setValue(creditScore);

			bodyDataCreditScore.setField(bodyDataFieldCreditScore);

			bodyDataList.add(bodyDataCreditScore);

			//body:INTEREST_RATE
			BodyData bodyDataInterestRate = new BodyData();
			bodyDataInterestRate.setName("INTEREST_RATE");

			BodyDataField bodyDataFieldInterestRate = new BodyDataField();
			String interestRate = String.valueOf(bodyMap.get("INTEREST_RATE"));
			bodyDataFieldInterestRate.setLength(interestRate.length() + "");
			bodyDataFieldInterestRate.setScale("2");
			bodyDataFieldInterestRate.setType("string");
			bodyDataFieldInterestRate.setValue(interestRate);

			bodyDataInterestRate.setField(bodyDataFieldInterestRate);

			bodyDataList.add(bodyDataInterestRate);

			//body:PLUSMINUS_SCORE
			BodyData bodyDataPlusMinusScore = new BodyData();
			bodyDataPlusMinusScore.setName("PLUSMINUS_SCORE");

			BodyDataField bodyDataFieldPlusMinusScore = new BodyDataField();
			String plusMinusScore = "";
			if(bodyMap.get("PLUSMINUS_SCORE")!=null&&!bodyMap.get("PLUSMINUS_SCORE").equals("")){
				plusMinusScore = (String) bodyMap.get("PLUSMINUS_SCORE");
			}
			bodyDataFieldPlusMinusScore.setLength(plusMinusScore.length() + "");
			bodyDataFieldPlusMinusScore.setScale("0");
			bodyDataFieldPlusMinusScore.setType("string");
			bodyDataFieldPlusMinusScore.setValue(plusMinusScore);

			bodyDataPlusMinusScore.setField(bodyDataFieldPlusMinusScore);

			bodyDataList.add(bodyDataPlusMinusScore);

			//body:RR 失败原因 REFUSE_REASON 数组
			BodyData bodyDataRR = new BodyData();
			bodyDataRR.setName("RR");

//			List<String> reasonList = (List<String>) bodyMap.get("RR");
//			String[] refuseReasonArray = new String[reasonList.size()];
//			for(int i=0;i<reasonList.size();i++){
//				refuseReasonArray[i] = reasonList.get(i);
//			}
//			bodyDataRR.setRefuseReason(refuseReasonArray);

			List<BodyDataArray> bodyDataArrayList = new ArrayList<BodyDataArray>();
			BodyDataArray bodyDataArray = new BodyDataArray();
			List<BodyDataArrayData> bodyDataArrayDataList = new ArrayList<BodyDataArrayData>();

			if(bodyMap.get("RR")!=null){
				List<String> reasonList = (List<String>) bodyMap.get("RR");
				for(int i=0;i<reasonList.size();i++){
					BodyDataArrayData bodyDataArrayData = new BodyDataArrayData();
					bodyDataArrayData.setName("REASON");
					List<BodyDataArrayDataField> bodyDataArrayDataFieldList = new ArrayList<BodyDataArrayDataField>();
					BodyDataArrayDataField bodyDataArrayDataField = new BodyDataArrayDataField();
					String value = reasonList.get(i);
					bodyDataArrayDataField.setLength(value.length()+"");
					bodyDataArrayDataField.setScale("0");
					bodyDataArrayDataField.setType("string");
					bodyDataArrayDataField.setValue(value);
					bodyDataArrayDataFieldList.add(bodyDataArrayDataField);
					bodyDataArrayData.setBodyDataArrayDataField(bodyDataArrayDataFieldList);
					bodyDataArrayDataList.add(bodyDataArrayData);
				}
			}

			bodyDataArray.setBodyDataArrayData(bodyDataArrayDataList);
			bodyDataArrayList.add(bodyDataArray);
			bodyDataRR.setBodyDataArray(bodyDataArrayList);
			bodyDataList.add(bodyDataRR);
		}

		//xml添加sys-header
		rs.setRespSysHeadData(respSysHeadDataList);

		//xml添加app-header
		ReturnAppHeadData respAppHeadData = new ReturnAppHeadData();
		rs.setRespAppHeadData(respAppHeadData);
		//xml添加local-header
		ReturnLocalHeadData respLocalHeadData = new ReturnLocalHeadData();
		rs.setRespLocalHeadData(respLocalHeadData);

		//xml添加body
		rs.setBodyData(bodyDataList);

		JaxbUtil returnBinder = new JaxbUtil(ReturnXMLService.class, CollectionWrapper.class);

		String resXml = returnBinder.toXml(rs, "utf-8");

		return resXml;

	}

	private static Map<String,Object> reqDecision(Map<String,String> reqMap,String bodUrl){

		Map<String, Object> param = new HashMap<String, Object>();
		param.put("organId", reqMap.get("organId")); //公司ID
		param.put("engineCode", reqMap.get("engineCode"));
		param.put("reqType", reqMap.get("reqType"));
		param.put("act", "query");
		param.put("version", "1");
		param.put("subVersion", "0");
		param.put("uid", reqMap.get("uid"));
		param.put("pid", reqMap.get("pid"));
		param.put("dataSource", 1);
		Map<String, Object> tmp = new HashMap<String, Object>();
		tmp.put("tmp", "tmp");
		param.put("inFields", tmp);

		String respStr = "";
		Map<String, Object> resultParam = new HashMap<String, Object>();

		try{
			logger.info("塔调用开始");
			respStr = HttpPostUtils.sendPostJson(bodUrl, param);
			logger.info("塔调用完成"+respStr);
		}catch(Exception e){
			logger.error("ta meet error");
		}finally{
			//信保贷测试拒绝数据
//			respStr = "{\"status\":\"0x0000\",\"msg\":\"加减分规则拒绝\",\"result\":\"拒绝\",\"score\":\"0\",\"data\":[{\"nodeType\":2,\"nodeTypeName\":\"政策规则\",\"resultJson\":[{\"status\":\"0x0000\",\"msg\":\"拒绝\",\"nodeId\":182043654467585,\"nodeName\":\"规则集_1\",\"execOrder\":1,\"refusedScore\":\"10.0\",\"calcScore\":\"160.0\",\"missedRules\":[{\"ruleCode\":\"ZBD_CRR_S008_09\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_欠税名单\",\"state\":2,\"ruleId\":182042280792072,\"desc\":\"无 欠税名单\"},{\"ruleCode\":\"ZBD_CRR_S008_03\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_限制高消费名单\",\"state\":2,\"ruleId\":182042280792078,\"desc\":\"无 限制高消费名单\"},{\"ruleCode\":\"ZBD_CRR_S008_05\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_民商事裁判文书\",\"state\":2,\"ruleId\":182042280804355,\"desc\":\"无 民商事裁判文书 \"},{\"ruleCode\":\"ZBD_CRR_S009_01\",\"ruleType\":1,\"ruleName\":\"众保贷_数尊在网状态校验\",\"state\":2,\"ruleId\":182042280792079,\"desc\":\"在网状态非异常\"},{\"ruleCode\":\"ZBD_CRR_S005_01\",\"ruleType\":1,\"ruleName\":\"众保贷_白骑士风险名单类型校验\",\"state\":2,\"ruleId\":182042280792070,\"desc\":\"审批建议为通过或者审核\"},{\"ruleCode\":\"ZBD_CRR_S008_08\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_行政违法记录\",\"state\":2,\"ruleId\":182042280792065,\"desc\":\"无 行政违法记录 \"},{\"ruleCode\":\"ZBD_CRR_S004_02\",\"ruleType\":1,\"ruleName\":\"众保贷_芝麻申请欺诈评分限制\",\"state\":2,\"ruleId\":182042280792073,\"desc\":\"申请欺诈评分不能低于40分\"},{\"ruleCode\":\"ZBD_CRR_S008_10\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_纳税非正常户\",\"state\":2,\"ruleId\":182042280792071,\"desc\":\"无 纳税非正常户\"},{\"ruleCode\":\"ZBD_CRR_S007_02\",\"ruleType\":1,\"ruleName\":\"众保贷_百融个人不良信息校验\",\"state\":2,\"ruleId\":182042280792074,\"desc\":\"无 个人不良信息\"},{\"ruleCode\":\"ZBD_CRR_S009_02\",\"ruleType\":1,\"ruleName\":\"众保贷_数尊在网时长校验\",\"state\":2,\"ruleId\":182042280792077,\"desc\":\"在网时长在6个月以上\"},{\"ruleCode\":\"ZBD_CRR_S008_01\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_执行公开信息\",\"state\":2,\"ruleId\":182042280804354,\"desc\":\"无 执行公开信息\"},{\"ruleCode\":\"ZBD_CRR_S007_01\",\"ruleType\":1,\"ruleName\":\"众保贷_百融个人信息关联校验\",\"state\":2,\"ruleId\":182042280804352,\"desc\":\"身份证关联的手机号个数<3\"},{\"ruleCode\":\"ZBD_CRR_S008_06\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_民商事审判流程\",\"state\":2,\"ruleId\":182042280792067,\"desc\":\"无 民商事审判流程\"},{\"ruleCode\":\"ZBD_CRR_S002_02\",\"ruleType\":1,\"ruleName\":\"众保贷_各类外部数据源\",\"state\":2,\"ruleId\":182042280792066,\"desc\":\"腾讯芝麻同盾不能均为缺失\"},{\"ruleCode\":\"ZBD_CRR_S008_11\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_欠款欠费名单\",\"state\":2,\"ruleId\":182042280804353,\"desc\":\"无 欠款欠费名单\"},{\"ruleCode\":\"ZBD_CRR_S001_01\",\"ruleType\":1,\"ruleName\":\"众保贷_身份证位数校验\",\"state\":2,\"ruleId\":182042280792075,\"desc\":\"身份证为18位\"},{\"ruleCode\":\"ZBD_CRR_S008_04\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_限制出入境名单\",\"state\":2,\"ruleId\":182042280792068,\"desc\":\"无 限制出入境名单\"},{\"ruleCode\":\"ZBD_CRR_S004_01\",\"ruleType\":1,\"ruleName\":\"众保贷_芝麻申请欺诈风险描述校验\",\"state\":2,\"ruleId\":182042280792076,\"desc\":\"风险描述 不为 “身份证号出现在风险关联网络”\"},{\"ruleCode\":\"ZBD_CRR_S008_02\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_失信老赖名单\",\"state\":2,\"ruleId\":182042280804357,\"desc\":\"无 失信老赖名单\"},{\"ruleCode\":\"ZBD_CRR_S008_07\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_罪犯及嫌疑人名单\",\"state\":2,\"ruleId\":182042280804356,\"desc\":\"无 罪犯及嫌疑人名单\"},{\"ruleCode\":\"ZBD_CRR_S006_01\",\"ruleType\":1,\"ruleName\":\"众保贷_同盾反欺诈分数限制\",\"state\":2,\"ruleId\":182042280792064,\"desc\":\"反欺诈分数不能高于80分\"}],\"refusedRules\":[],\"calcRules\":[{\"ruleId\":182042280804358,\"ruleCode\":\"ZBD_CRR_S001_02\",\"ruleName\":\"众保贷_年龄校验\",\"score\":80,\"state\":1,\"desc\":\"年龄为18到60岁\",\"output\":[{\"code\":\"score\",\"name\":\"信用得分\",\"value\":80}]},{\"ruleId\":182042280792069,\"ruleCode\":\"ZBD_CRR_S003_01\",\"ruleName\":\"众保贷_腾讯天御反欺诈评分限制\",\"score\":80,\"state\":1,\"desc\":\"天御反欺诈评分不能高于70分 \",\"output\":[{\"code\":\"score\",\"name\":\"信用得分\",\"value\":80}]}],\"focusRules\":[]}]}]}";
			//信保贷测试通过数据
//			respStr = "{\"status\":\"0x0000\",\"msg\":\"执行成功\",\"result\":\"通过\",\"score\":\"0\",\"data\":[{\"nodeType\":2,\"nodeTypeName\":\"政策规则\",\"resultJson\":[{\"status\":\"0x0000\",\"msg\":\"通过\",\"nodeId\":182043654467585,\"nodeName\":\"规则集_1\",\"execOrder\":1,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"ZBD_CRR_S003_01\",\"ruleType\":1,\"ruleName\":\"众保贷_腾讯天御反欺诈评分限制\",\"state\":2,\"ruleId\":182042280792069,\"desc\":\"天御反欺诈评分不能高于70分 \"},{\"ruleCode\":\"ZBD_CRR_S001_01\",\"ruleType\":1,\"ruleName\":\"众保贷_身份证位数校验\",\"state\":2,\"ruleId\":182042280792075,\"desc\":\"身份证为18位\"},{\"ruleCode\":\"ZBD_CRR_S009_02\",\"ruleType\":1,\"ruleName\":\"众保贷_数尊在网时长校验\",\"state\":2,\"ruleId\":182042280792077,\"desc\":\"在网时长在6个月以上\"},{\"ruleCode\":\"ZBD_CRR_S009_01\",\"ruleType\":1,\"ruleName\":\"众保贷_数尊在网状态校验\",\"state\":2,\"ruleId\":182042280792079,\"desc\":\"在网状态非异常\"},{\"ruleCode\":\"ZBD_CRR_S008_08\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_行政违法记录\",\"state\":2,\"ruleId\":182042280792065,\"desc\":\"无 行政违法记录 \"},{\"ruleCode\":\"ZBD_CRR_S008_06\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_民商事审判流程\",\"state\":2,\"ruleId\":182042280792067,\"desc\":\"无 民商事审判流程\"},{\"ruleCode\":\"ZBD_CRR_S008_09\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_欠税名单\",\"state\":2,\"ruleId\":182042280792072,\"desc\":\"无 欠税名单\"},{\"ruleCode\":\"ZBD_CRR_S008_11\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_欠款欠费名单\",\"state\":2,\"ruleId\":182042280804353,\"desc\":\"无 欠款欠费名单\"},{\"ruleCode\":\"ZBD_CRR_S004_01\",\"ruleType\":1,\"ruleName\":\"众保贷_芝麻申请欺诈风险描述校验\",\"state\":2,\"ruleId\":182042280792076,\"desc\":\"风险描述 不为 “身份证号出现在风险关联网络”\"},{\"ruleCode\":\"ZBD_CRR_S002_02\",\"ruleType\":1,\"ruleName\":\"众保贷_各类外部数据源\",\"state\":2,\"ruleId\":182042280792066,\"desc\":\"腾讯芝麻同盾不能均为缺失\"},{\"ruleCode\":\"ZBD_CRR_S004_02\",\"ruleType\":1,\"ruleName\":\"众保贷_芝麻申请欺诈评分限制\",\"state\":2,\"ruleId\":182042280792073,\"desc\":\"申请欺诈评分不能低于40分\"},{\"ruleCode\":\"ZBD_CRR_S008_07\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_罪犯及嫌疑人名单\",\"state\":2,\"ruleId\":182042280804356,\"desc\":\"无 罪犯及嫌疑人名单\"},{\"ruleCode\":\"ZBD_CRR_S008_02\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_失信老赖名单\",\"state\":2,\"ruleId\":182042280804357,\"desc\":\"无 失信老赖名单\"},{\"ruleCode\":\"ZBD_CRR_S007_02\",\"ruleType\":1,\"ruleName\":\"众保贷_百融个人不良信息校验\",\"state\":2,\"ruleId\":182042280792074,\"desc\":\"无 个人不良信息\"},{\"ruleCode\":\"ZBD_CRR_S008_05\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_民商事裁判文书\",\"state\":2,\"ruleId\":182042280804355,\"desc\":\"无 民商事裁判文书 \"},{\"ruleCode\":\"ZBD_CRR_S008_10\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_纳税非正常户\",\"state\":2,\"ruleId\":182042280792071,\"desc\":\"无 纳税非正常户\"},{\"ruleCode\":\"ZBD_CRR_S001_02\",\"ruleType\":1,\"ruleName\":\"众保贷_年龄校验\",\"state\":2,\"ruleId\":182042280804358,\"desc\":\"年龄为18到60岁\"},{\"ruleCode\":\"ZBD_CRR_S006_01\",\"ruleType\":1,\"ruleName\":\"众保贷_同盾反欺诈分数限制\",\"state\":2,\"ruleId\":182042280792064,\"desc\":\"反欺诈分数不能高于80分\"},{\"ruleCode\":\"ZBD_CRR_S008_04\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_限制出入境名单\",\"state\":2,\"ruleId\":182042280792068,\"desc\":\"无 限制出入境名单\"},{\"ruleCode\":\"ZBD_CRR_S008_01\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_执行公开信息\",\"state\":2,\"ruleId\":182042280804354,\"desc\":\"无 执行公开信息\"},{\"ruleCode\":\"ZBD_CRR_S008_03\",\"ruleType\":1,\"ruleName\":\"众保贷_汇法网_限制高消费名单\",\"state\":2,\"ruleId\":182042280792078,\"desc\":\"无 限制高消费名单\"},{\"ruleCode\":\"ZBD_CRR_S007_01\",\"ruleType\":1,\"ruleName\":\"众保贷_百融个人信息关联校验\",\"state\":2,\"ruleId\":182042280804352,\"desc\":\"身份证关联的手机号个数<3\"},{\"ruleCode\":\"ZBD_CRR_S005_01\",\"ruleType\":1,\"ruleName\":\"众保贷_白骑士风险名单类型校验\",\"state\":2,\"ruleId\":182042280792070,\"desc\":\"审批建议为通过或者审核\"}],\"refusedRules\":[],\"calcRules\":[],\"focusRules\":[]}]}]}";
			//驴妈妈测试通过数据
//			respStr = "{\"status\":\"0x0000\",\"msg\":\"执行成功\",\"result\":\"通过\",\"score\":\"0\",\"data\":[{\"nodeType\":2,\"nodeTypeName\":\"政策规则\",\"resultJson\":[{\"status\":\"0x0000\",\"msg\":\"通过\",\"nodeId\":183478829539330,\"nodeName\":\"反欺诈规则\",\"execOrder\":1,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"LMM_AH00_0047\",\"ruleType\":0,\"ruleName\":\"联系地址过去30天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689760,\"desc\":\"联系地址过去30天内累计申请身份证号个数<=7\"},{\"ruleCode\":\"LMM_AH00_0053\",\"ruleType\":0,\"ruleName\":\"联系地址过去60天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689745,\"desc\":\"联系地址过去60天内累计申请手机号码个数<=10\"},{\"ruleCode\":\"LMM_AH00_0006\",\"ruleType\":0,\"ruleName\":\"手机号码过去7天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831687713,\"desc\":\"手机号码过去7天内累计申请次数<=5次\"},{\"ruleCode\":\"LMM_AH00_0041\",\"ruleType\":0,\"ruleName\":\"GPS过去7天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689732,\"desc\":\"GPS过去7天内累计申请手机号码个数<=3\"},{\"ruleCode\":\"LMM_AH00_0030\",\"ruleType\":0,\"ruleName\":\"IP过去1天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687705,\"desc\":\"IP过去1天内累计申请的手机号码数<=2\"},{\"ruleCode\":\"LMM_AH00_0038\",\"ruleType\":0,\"ruleName\":\"GPS过去60天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689753,\"desc\":\"GPS过去60天内累计申请的身份证号数<=5\"},{\"ruleCode\":\"LMM_AH00_0020\",\"ruleType\":0,\"ruleName\":\"设备ID过去1天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687714,\"desc\":\"设备ID过去1天内累计申请的手机号码数<=2\"},{\"ruleCode\":\"LMM_AH00_0014\",\"ruleType\":0,\"ruleName\":\"身份证过去90天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689728,\"desc\":\"身份证过去90天内累计申请次数<=30次\"},{\"ruleCode\":\"LMM_AH00_0050\",\"ruleType\":0,\"ruleName\":\"联系地址过去1天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689754,\"desc\":\"联系地址过去1天内累计申请手机号码个数<=3\"},{\"ruleCode\":\"LMM_AH00_0023\",\"ruleType\":0,\"ruleName\":\"设备ID过去60天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689763,\"desc\":\"设备ID过去60天内累计申请的手机号码数<=5\"},{\"ruleCode\":\"LMM_AH00_0004\",\"ruleType\":0,\"ruleName\":\"账户历史累计申请成功次数校验\",\"state\":2,\"ruleId\":183478831687704,\"desc\":\"账户历史累计申请成功次数=1次\"},{\"ruleCode\":\"LMM_AH00_0022\",\"ruleType\":0,\"ruleName\":\"设备ID过去30天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689741,\"desc\":\"设备ID过去30天内累计申请的手机号码数<=3\"},{\"ruleCode\":\"LMM_AH00_0005\",\"ruleType\":0,\"ruleName\":\"手机号码过去1天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831687712,\"desc\":\"手机号码过去1天内累计申请次数<=3次\"},{\"ruleCode\":\"LMM_AH00_0054\",\"ruleType\":0,\"ruleName\":\"联系地址过去90天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689747,\"desc\":\"联系地址过去90天内累计申请手机号码个数<=20\"},{\"ruleCode\":\"LMM_AH00_0042\",\"ruleType\":0,\"ruleName\":\"GPS过去30天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689752,\"desc\":\"GPS过去30天内累计申请手机号码个数<=3\"},{\"ruleCode\":\"LMM_AH00_0002\",\"ruleType\":0,\"ruleName\":\"手机号码历史累计申请成功次数校验\",\"state\":2,\"ruleId\":183478831689731,\"desc\":\"手机号码历史累计申请成功次数=1次\"},{\"ruleCode\":\"LMM_AH00_0035\",\"ruleType\":0,\"ruleName\":\"GPS过去1天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831687718,\"desc\":\"GPS过去1天内累计申请的身份证号数<=2\"},{\"ruleCode\":\"LMM_AH00_0009\",\"ruleType\":0,\"ruleName\":\"手机号码过去90天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689761,\"desc\":\"手机号码过去90天内累计申请次数<=30次\"},{\"ruleCode\":\"LMM_AH00_0051\",\"ruleType\":0,\"ruleName\":\"联系地址过去7天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689742,\"desc\":\"联系地址过去7天内累计申请手机号码个数<=5\"},{\"ruleCode\":\"LMM_AH00_0019\",\"ruleType\":0,\"ruleName\":\"设备ID过去90天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831687720,\"desc\":\"设备ID过去90天内累计申请的身份证号数<=10\"},{\"ruleCode\":\"LMM_AH00_0008\",\"ruleType\":0,\"ruleName\":\"手机号码过去60天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689768,\"desc\":\"手机号码过去60天内累计申请次数<=15次\"},{\"ruleCode\":\"LMM_AH00_0012\",\"ruleType\":0,\"ruleName\":\"身份证过去30天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831687706,\"desc\":\"身份证过去30天内累计申请次数<=10次\"},{\"ruleCode\":\"LMM_AH00_0028\",\"ruleType\":0,\"ruleName\":\"IP过去60天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689749,\"desc\":\"IP过去60天内累计申请的身份证号数<=5\"},{\"ruleCode\":\"LMM_AH00_0010\",\"ruleType\":0,\"ruleName\":\"身份证过去1天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831687709,\"desc\":\"身份证过去1天内累计申请次数<=3次\"},{\"ruleCode\":\"LMM_AH00_0045\",\"ruleType\":0,\"ruleName\":\"联系地址过去1天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689765,\"desc\":\"联系地址过去1天内累计申请身份证号个数<=3\"},{\"ruleCode\":\"LMM_AH00_0011\",\"ruleType\":0,\"ruleName\":\"身份证过去7天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689733,\"desc\":\"身份证过去7天内累计申请次数<=5次\"},{\"ruleCode\":\"LMM_AH00_0025\",\"ruleType\":0,\"ruleName\":\"IP过去1天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689737,\"desc\":\"IP过去1天内累计申请的身份证号数<=2\"},{\"ruleCode\":\"LMM_AH00_0036\",\"ruleType\":0,\"ruleName\":\"GPS过去7天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831687715,\"desc\":\"GPS过去7天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0021\",\"ruleType\":0,\"ruleName\":\"设备ID过去7天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687717,\"desc\":\"设备ID过去7天内累计申请的手机号码数<=3\"},{\"ruleCode\":\"LMM_AH00_0034\",\"ruleType\":0,\"ruleName\":\"IP过去90天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689740,\"desc\":\"IP过去90天内累计申请的手机号码数<=10\"},{\"ruleCode\":\"LMM_AH00_0013\",\"ruleType\":0,\"ruleName\":\"身份证过去60天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689769,\"desc\":\"身份证过去60天内累计申请次数<=15次\"},{\"ruleCode\":\"LMM_AH00_0049\",\"ruleType\":0,\"ruleName\":\"联系地址过去90天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689746,\"desc\":\"联系地址过去90天内累计申请身份证号个数<=20\"},{\"ruleCode\":\"LMM_AH00_0031\",\"ruleType\":0,\"ruleName\":\"IP过去7天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689770,\"desc\":\"IP过去7天内累计申请的手机号码数<=3\"},{\"ruleCode\":\"LMM_AH00_0018\",\"ruleType\":0,\"ruleName\":\"设备ID过去60天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689756,\"desc\":\"设备ID过去60天内累计申请的身份证号数<=5\"},{\"ruleCode\":\"LMM_AH00_0037\",\"ruleType\":0,\"ruleName\":\"GPS过去30天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689744,\"desc\":\"GPS过去30天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0040\",\"ruleType\":0,\"ruleName\":\"GPS过去1天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831687707,\"desc\":\"GPS过去1天内累计申请手机号码个数<=2\"},{\"ruleCode\":\"LMM_AH00_0032\",\"ruleType\":0,\"ruleName\":\"IP过去30天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687708,\"desc\":\"IP过去30天内累计申请的手机号码数<=3\"},{\"ruleCode\":\"LMM_AH00_0024\",\"ruleType\":0,\"ruleName\":\"设备ID过去90天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689729,\"desc\":\"设备ID过去90天内累计申请的手机号码数<=10\"},{\"ruleCode\":\"LMM_AH00_0016\",\"ruleType\":0,\"ruleName\":\"设备ID过去7天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689764,\"desc\":\"设备ID过去7天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0026\",\"ruleType\":0,\"ruleName\":\"IP过去7天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689758,\"desc\":\"IP过去7天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0001\",\"ruleType\":0,\"ruleName\":\"身份证历史累计申请成功次数校验\",\"state\":2,\"ruleId\":183478831689738,\"desc\":\"身份证历史累计申请成功次数=1次\"},{\"ruleCode\":\"LMM_AH00_0039\",\"ruleType\":0,\"ruleName\":\"GPS过去90天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689739,\"desc\":\"GPS过去90天内累计申请的身份证号数<=10\"},{\"ruleCode\":\"LMM_AH00_0003\",\"ruleType\":0,\"ruleName\":\"银行卡历史累计申请成功次数校验\",\"state\":2,\"ruleId\":183478831689750,\"desc\":\"银行卡历史累计申请成功次数=1次\"},{\"ruleCode\":\"LMM_AH00_0007\",\"ruleType\":0,\"ruleName\":\"手机号码过去30天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689755,\"desc\":\"手机号码过去30天内累计申请次数<=10次\"},{\"ruleCode\":\"LMM_AH00_0033\",\"ruleType\":0,\"ruleName\":\"IP过去60天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687711,\"desc\":\"IP过去60天内累计申请的手机号码数<=5\"},{\"ruleCode\":\"LMM_AH00_0044\",\"ruleType\":0,\"ruleName\":\"GPS过去90天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689751,\"desc\":\"GPS过去90天内累计申请手机号码个数<=10\"},{\"ruleCode\":\"LMM_AH00_0048\",\"ruleType\":0,\"ruleName\":\"联系地址过去60天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689759,\"desc\":\"联系地址过去60天内累计申请身份证号个数<=10\"},{\"ruleCode\":\"LMM_AH00_0027\",\"ruleType\":0,\"ruleName\":\"IP过去30天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689730,\"desc\":\"IP过去30天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0052\",\"ruleType\":0,\"ruleName\":\"联系地址过去30天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689743,\"desc\":\"联系地址过去30天内累计申请手机号码个数<=7\"},{\"ruleCode\":\"LMM_AH00_0043\",\"ruleType\":0,\"ruleName\":\"GPS过去60天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831687719,\"desc\":\"GPS过去60天内累计申请手机号码个数<=5\"},{\"ruleCode\":\"LMM_AH00_0046\",\"ruleType\":0,\"ruleName\":\"联系地址过去7天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689767,\"desc\":\"联系地址过去7天内累计申请身份证号个数<=5\"},{\"ruleCode\":\"LMM_AH00_0017\",\"ruleType\":0,\"ruleName\":\"设备ID过去30天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689757,\"desc\":\"设备ID过去30天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0029\",\"ruleType\":0,\"ruleName\":\"IP过去90天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689766,\"desc\":\"IP过去90天内累计申请的身份证号数<=10\"},{\"ruleCode\":\"LMM_AH00_0015\",\"ruleType\":0,\"ruleName\":\"设备ID过去1天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689735,\"desc\":\"设备ID过去1天内累计申请的身份证号数<=2\"}],\"refusedRules\":[],\"calcRules\":[],\"focusRules\":[]},{\"status\":\"0x0000\",\"msg\":\"通过\",\"nodeId\":183478829539334,\"nodeName\":\"基本信息\",\"execOrder\":2,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"LMM_CH00_0004\",\"ruleType\":0,\"ruleName\":\"开放地市校验\",\"state\":2,\"ruleId\":183478831687687,\"desc\":\"手机号归属地为北京上海浙江江苏广东地区\"},{\"ruleCode\":\"LMM_CH00_0003\",\"ruleType\":0,\"ruleName\":\"高位地市校验\",\"state\":2,\"ruleId\":183478831687699,\"desc\":\"身份证归属地不为 福建莆田，福建宁德,，福建龙岩\"},{\"ruleCode\":\"LMM_CH00_0005\",\"ruleType\":0,\"ruleName\":\"申请时间校验\",\"state\":2,\"ruleId\":183478831687688,\"desc\":\"开放时间为6点到24点\"},{\"ruleCode\":\"LMM_CH00_0002\",\"ruleType\":0,\"ruleName\":\"年龄校验\",\"state\":2,\"ruleId\":183478831687681,\"desc\":\"年龄为20到55岁\"},{\"ruleCode\":\"LMM_CH00_0001\",\"ruleType\":0,\"ruleName\":\"身份证位数校验\",\"state\":2,\"ruleId\":183478831687695,\"desc\":\"身份证为18位\"}],\"refusedRules\":[],\"calcRules\":[],\"focusRules\":[]},{\"status\":\"0x0000\",\"msg\":\"通过\",\"nodeId\":183478829539336,\"nodeName\":\"信息有效性\",\"execOrder\":3,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"LMM_CH00_0009\",\"ruleType\":0,\"ruleName\":\"申请人和联系人号码校验\",\"state\":2,\"ruleId\":183478831687685,\"desc\":\"申请人号码、联系人号码不为同一号码\"},{\"ruleCode\":\"LMM_CH00_0006\",\"ruleType\":0,\"ruleName\":\"申请人手机号校验\",\"state\":2,\"ruleId\":183478831687701,\"desc\":\"非：申请人手机号码虚拟号段（170,171）暂不准入\"},{\"ruleCode\":\"LMM_CH00_0007\",\"ruleType\":0,\"ruleName\":\"联系人1手机号码校验\",\"state\":2,\"ruleId\":183478831687684,\"desc\":\"非：联系人1手机号码虚拟号段（170,171）暂不准入\"},{\"ruleCode\":\"LMM_CH00_0008\",\"ruleType\":0,\"ruleName\":\"联系人2手机号码校验\",\"state\":2,\"ruleId\":183478831687680,\"desc\":\"非：联系人2手机号码虚拟号段（170,171）暂不准入\"}],\"refusedRules\":[],\"calcRules\":[],\"focusRules\":[]},{\"status\":\"0x0000\",\"msg\":\"通过\",\"nodeId\":183478829539335,\"nodeName\":\"负面信息\",\"execOrder\":4,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"LMM_CH00_0026\",\"ruleType\":0,\"ruleName\":\"驴妈妈_同盾反欺诈分数限制\",\"state\":2,\"ruleId\":183478831689781,\"desc\":\"反欺诈分数不能高于80分\"},{\"ruleCode\":\"LMM_CH00_0011\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_失信老赖名单\",\"state\":2,\"ruleId\":183478831689748,\"desc\":\"无 失信老赖名单\"},{\"ruleCode\":\"LMM_CH00_0032\",\"ruleType\":0,\"ruleName\":\"同盾手机号命中虚假号码库校验\",\"state\":2,\"ruleId\":183478831689783,\"desc\":\"非：手机号命中虚假号码库\"},{\"ruleCode\":\"LMM_CH00_0036\",\"ruleType\":0,\"ruleName\":\"驴妈妈_百融个人不良信息校验\",\"state\":2,\"ruleId\":183478831689789,\"desc\":\"无 个人不良信息\"},{\"ruleCode\":\"LMM_CH00_0033\",\"ruleType\":0,\"ruleName\":\"同盾第一联系人手机号校验\",\"state\":2,\"ruleId\":183478831689780,\"desc\":\"非：第一联系人手机号命中虚假号码或通信小号库\"},{\"ruleCode\":\"LMM_CH00_0023\",\"ruleType\":0,\"ruleName\":\"驴妈妈_芝麻申请欺诈评分缺失情\",\"state\":2,\"ruleId\":183478831689776,\"desc\":\"申请欺诈评分不能为缺失\"},{\"ruleCode\":\"LMM_CH00_0030\",\"ruleType\":0,\"ruleName\":\"同盾手机号命中高风险关注名单校验\",\"state\":2,\"ruleId\":183478831689782,\"desc\":\"非：手机号命中高风险关注名单\"},{\"ruleCode\":\"LMM_CH00_0012\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_限制高消费名单\",\"state\":2,\"ruleId\":183478831687710,\"desc\":\"无 限制高消费名单\"},{\"ruleCode\":\"LMM_CH00_0027\",\"ruleType\":0,\"ruleName\":\"同盾身份证命中信贷逾期名单校验\",\"state\":2,\"ruleId\":183478831689784,\"desc\":\"非：身份证命中信贷逾期名单\"},{\"ruleCode\":\"LMM_CH00_0039\",\"ruleType\":0,\"ruleName\":\"驴妈妈_在网时长校验\",\"state\":2,\"ruleId\":183478831689791,\"desc\":\"在网时长在6个月以上\"},{\"ruleCode\":\"LMM_CH00_0019\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_纳税非正常户\",\"state\":2,\"ruleId\":183478831689734,\"desc\":\"无 纳税非正常户\"},{\"ruleCode\":\"LMM_CH00_0013\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_限制出入境名单\",\"state\":2,\"ruleId\":183478831689736,\"desc\":\"无 限制出入境名单\"},{\"ruleCode\":\"LMM_CH00_0018\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_欠税名单\",\"state\":2,\"ruleId\":183478831687703,\"desc\":\"无 欠税名单\"},{\"ruleCode\":\"LMM_CH00_0037\",\"ruleType\":0,\"ruleName\":\"白骑士风险名单类型校验\",\"state\":2,\"ruleId\":183478831689790,\"desc\":\"审批建议为 “Accept” or \\\"Review\\\"\"},{\"ruleCode\":\"LMM_CH00_0017\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_行政违法记录\",\"state\":2,\"ruleId\":183478831687716,\"desc\":\"行政违法记录  在2次以内\"},{\"ruleCode\":\"LMM_CH00_0021\",\"ruleType\":0,\"ruleName\":\"驴妈妈_腾讯天御反欺诈评分限制\",\"state\":2,\"ruleId\":183478831689777,\"desc\":\"天御反欺诈评分不能高于70分 \"},{\"ruleCode\":\"LMM_CH00_0028\",\"ruleType\":0,\"ruleName\":\"同盾手机号命中信贷逾期名单校验\",\"state\":2,\"ruleId\":183478831689785,\"desc\":\"非：手机号命中信贷逾期名单\"},{\"ruleCode\":\"LMM_CH00_0029\",\"ruleType\":0,\"ruleName\":\"同盾身份证命中高风险关注名单校验\",\"state\":2,\"ruleId\":183478831689786,\"desc\":\"非：身份证命中高风险关注名单\"},{\"ruleCode\":\"LMM_CH00_0022\",\"ruleType\":0,\"ruleName\":\"腾讯天御反欺诈评分风险类型校验\",\"state\":2,\"ruleId\":183478831689778,\"desc\":\"风险类型 不为 \\\"信贷中介\\\"，\\\"不法分子\\\"，\\\"疑似恶意欺诈\\\"，\\\"失信名单\\\"，\\\"高风险可能性关联度较高”\"},{\"ruleCode\":\"LMM_CH00_0025\",\"ruleType\":0,\"ruleName\":\"驴妈妈_芝麻申请欺诈风险描述校验\",\"state\":2,\"ruleId\":183478831689774,\"desc\":\"风险描述 不为 “身份证号出现在风险关联网络”\"},{\"ruleCode\":\"LMM_CH00_0015\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_民商事审判流程\",\"state\":2,\"ruleId\":183478831689771,\"desc\":\"民商事审判流程 在2次以内\"},{\"ruleCode\":\"LMM_CH00_0024\",\"ruleType\":0,\"ruleName\":\"驴妈妈_芝麻申请欺诈评分限制\",\"state\":2,\"ruleId\":183478831689775,\"desc\":\"申请欺诈评分不能低于40分\"},{\"ruleCode\":\"LMM_CH00_0016\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_罪犯及嫌疑人名单\",\"state\":2,\"ruleId\":183478831689773,\"desc\":\"无 罪犯及嫌疑人名单\"},{\"ruleCode\":\"LMM_CH00_0035\",\"ruleType\":0,\"ruleName\":\"驴妈妈_百融个人信息关联校验\",\"state\":2,\"ruleId\":183478831689788,\"desc\":\"身份证关联的手机号个数<3\"},{\"ruleCode\":\"LMM_CH00_0010\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_执行公开信息\",\"state\":2,\"ruleId\":183478831689772,\"desc\":\"无 执行公开信息\"},{\"ruleCode\":\"LMM_CH00_0014\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_民商事裁判文书\",\"state\":2,\"ruleId\":183478831687702,\"desc\":\"民商事裁判文书 在2次以内\"},{\"ruleCode\":\"LMM_CH00_0038\",\"ruleType\":0,\"ruleName\":\"驴妈妈_在网状态非异常\",\"state\":2,\"ruleId\":183478831689792,\"desc\":\"在网状态非异常\"},{\"ruleCode\":\"LMM_CH00_0034\",\"ruleType\":0,\"ruleName\":\"同盾第二联系人手机号校验\",\"state\":2,\"ruleId\":183478831689787,\"desc\":\"非：第二联系人手机号命中虚假号码或通信小号库\"},{\"ruleCode\":\"LMM_CH00_0020\",\"ruleType\":0,\"ruleName\":\"驴妈妈_汇法网_欠款欠费名单\",\"state\":2,\"ruleId\":183478831689762,\"desc\":\"无 欠款欠费名单\"},{\"ruleCode\":\"LMM_CH00_0031\",\"ruleType\":0,\"ruleName\":\"同盾身份证命中法院失信名单校验\",\"state\":2,\"ruleId\":183478831689779,\"desc\":\"非：身份证命中法院失信名单\"}],\"refusedRules\":[],\"calcRules\":[],\"focusRules\":[]},{\"status\":\"0x0000\",\"msg\":\"通过\",\"nodeId\":183478829539331,\"nodeName\":\"信用历史\",\"execOrder\":5,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"LMM_CH00_0040\",\"ruleType\":0,\"ruleName\":\"同盾信用分筛选\",\"state\":2,\"ruleId\":183478831689794,\"desc\":\"同盾信用评分>480\"},{\"ruleCode\":\"LMM_CH00_0041\",\"ruleType\":0,\"ruleName\":\"芝麻信用分筛选\",\"state\":2,\"ruleId\":183478831689793,\"desc\":\"芝麻信用评分>650\"}],\"refusedRules\":[],\"calcRules\":[],\"focusRules\":[]},{\"status\":\"0x0000\",\"msg\":\"通过\",\"nodeId\":183478829539332,\"nodeName\":\"经济能力\",\"execOrder\":6,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"LMM_CH00_0042\",\"ruleType\":0,\"ruleName\":\"同盾经济能力评分筛选\",\"state\":2,\"ruleId\":183478831689795,\"desc\":\"非：经济能力评分 无记录\"}],\"refusedRules\":[],\"calcRules\":[],\"focusRules\":[]},{\"status\":\"0x0000\",\"msg\":\"通过\",\"nodeId\":183478829539333,\"nodeName\":\"加减分\",\"execOrder\":7,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"LMM_CS00_0005\",\"ruleType\":1,\"ruleName\":\"白骑士风险等级为多头风险\",\"state\":2,\"ruleId\":183478831687683,\"desc\":\"白骑士风险等级 为 多头风险\"},{\"ruleCode\":\"LMM_CS00_0007\",\"ruleType\":1,\"ruleName\":\"在网时长小于12个月\",\"state\":2,\"ruleId\":183478831687686,\"desc\":\"在网时长小于12个月\"},{\"ruleCode\":\"LMM_CS00_0012\",\"ruleType\":1,\"ruleName\":\"是否有金额超限类交易\",\"state\":2,\"ruleId\":183478831687697,\"desc\":\"LMM_CS00_0012\"},{\"ruleCode\":\"LMM_CS00_0013\",\"ruleType\":1,\"ruleName\":\"是否有金额不足交易\",\"state\":2,\"ruleId\":183478831687693,\"desc\":\"是否有金额不足交易\"},{\"ruleCode\":\"LMM_CS00_0002\",\"ruleType\":1,\"ruleName\":\"芝麻反欺诈分数为_40_80\",\"state\":2,\"ruleId\":183478831687700,\"desc\":\"芝麻反欺诈分数为 [40,80]\"},{\"ruleCode\":\"LMM_CS00_0001\",\"ruleType\":1,\"ruleName\":\"天御反欺诈评分_50_70\",\"state\":2,\"ruleId\":183478831687696,\"desc\":\"天御反欺诈评分[50,70]\"},{\"ruleCode\":\"LMM_CS00_0006\",\"ruleType\":1,\"ruleName\":\"年龄小于35岁and学历为空\",\"state\":2,\"ruleId\":183478831687691,\"desc\":\"年龄小于35岁and学历为空\"},{\"ruleCode\":\"LMM_CS00_0011\",\"ruleType\":1,\"ruleName\":\"是否有超过笔数限制交易\",\"state\":2,\"ruleId\":183478831687692,\"desc\":\"是否有超过笔数限制交易\"},{\"ruleCode\":\"LMM_CS00_0008\",\"ruleType\":1,\"ruleName\":\"三码验证结果为不一致\",\"state\":2,\"ruleId\":183478831687690,\"desc\":\"三码验证结果为不一致\"},{\"ruleCode\":\"LMM_CS00_0003\",\"ruleType\":1,\"ruleName\":\"同盾反欺诈分数为_40_80\",\"state\":2,\"ruleId\":183478831687682,\"desc\":\"同盾反欺诈分数为 [40,80]\"},{\"ruleCode\":\"LMM_CS00_0009\",\"ruleType\":1,\"ruleName\":\"是否存在盗卡风险\",\"state\":2,\"ruleId\":183478831687689,\"desc\":\"是否存在盗卡风险\"},{\"ruleCode\":\"LMM_CS00_0004\",\"ruleType\":1,\"ruleName\":\"白骑士风险等级为中风险\",\"state\":2,\"ruleId\":183478831687698,\"desc\":\"白骑士风险等级为中风险\"},{\"ruleCode\":\"LMM_CS00_0010\",\"ruleType\":1,\"ruleName\":\"是否发生吞卡\",\"state\":2,\"ruleId\":183478831687694,\"desc\":\"是否发生吞卡\"}],\"refusedRules\":[],\"calcRules\":[],\"focusRules\":[]}]},{\"nodeType\":4,\"nodeTypeName\":\"评分卡\",\"resultJson\":[{\"status\":\"0x0000\",\"msg\":\"执行成功\",\"nodeId\":183478829539329,\"nodeName\":\"资质评分\",\"execOrder\":8,\"cardId\":183478832939008,\"cardName\":\"驴妈妈贷前申请评分卡\",\"desc\":\"驴妈妈贷前申请评分卡\",\"outFields\":[{\"fieldId\":183227606759424,\"fieldName\":\"评分卡分数\",\"fieldCode\":\"score_pfk\",\"value\":71}]}]},{\"nodeType\":9,\"nodeTypeName\":\"决策选项\",\"resultJson\":[{\"nodeId\":183478829539337,\"nodeName\":\"授信金额\",\"status\":\"0x0000\",\"msg\":\"执行成功\",\"execOrder\":9,\"最终授信额度\":\"14490.0\",\"reason\":\"min( #{ND_2__score_pfk} *(20000-1000)/100+1000+ #{sz_isKeySubject_amt} + #{sz_ifbankvip_amt} , #{age_cap_amt} )\"},{\"nodeId\":183478829539338,\"nodeName\":\"评分盖帽\",\"status\":\"0x0000\",\"msg\":\"执行成功\",\"execOrder\":10,\"评分_盖帽金额\":\"20000\",\"reason\":\"#{ND_2__score_pfk}<=100&&#{ND_2__score_pfk}>=61\"},{\"nodeId\":183478829539339,\"nodeName\":\"决策选项_3\",\"status\":\"0x0000\",\"msg\":\"执行成功\",\"execOrder\":11,\"输出授信额度\":\"14000.0\",\"reason\":\"round( (min( #{ND_17__decision_score_cap_amt} , #{ND_16__decision_final_credit_line} ))/1000 )* 1000\"}]}]}";
//			respStr = "{\"status\":\"0x0000\",\"msg\":\"执行成功\",\"result\":\"拒绝\",\"score\":\"0\",\"data\":[{\"nodeType\":2,\"nodeTypeName\":\"政策规则\",\"resultJson\":[{\"status\":\"0x0000\",\"msg\":\"通过\",\"nodeId\":183478829539330,\"nodeName\":\"反欺诈规则\",\"execOrder\":1,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"LMM_AH00_0054\",\"ruleType\":0,\"ruleName\":\"联系地址过去90天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689747,\"desc\":\"联系地址过去90天内累计申请手机号码个数<=20\"},{\"ruleCode\":\"LMM_AH00_0013\",\"ruleType\":0,\"ruleName\":\"身份证过去60天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689769,\"desc\":\"身份证过去60天内累计申请次数<=15次\"},{\"ruleCode\":\"LMM_AH00_0022\",\"ruleType\":0,\"ruleName\":\"设备ID过去30天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689741,\"desc\":\"设备ID过去30天内累计申请的手机号码数<=3\"},{\"ruleCode\":\"LMM_AH00_0042\",\"ruleType\":0,\"ruleName\":\"GPS过去30天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689752,\"desc\":\"GPS过去30天内累计申请手机号码个数<=3\"},{\"ruleCode\":\"LMM_AH00_0046\",\"ruleType\":0,\"ruleName\":\"联系地址过去7天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689767,\"desc\":\"联系地址过去7天内累计申请身份证号个数<=5\"},{\"ruleCode\":\"LMM_AH00_0016\",\"ruleType\":0,\"ruleName\":\"设备ID过去7天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689764,\"desc\":\"设备ID过去7天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0002\",\"ruleType\":0,\"ruleName\":\"手机号码历史累计申请成功次数校验\",\"state\":2,\"ruleId\":183478831689731,\"desc\":\"手机号码历史累计申请成功次数=1次\"},{\"ruleCode\":\"LMM_AH00_0031\",\"ruleType\":0,\"ruleName\":\"IP过去7天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689770,\"desc\":\"IP过去7天内累计申请的手机号码数<=3\"},{\"ruleCode\":\"LMM_AH00_0008\",\"ruleType\":0,\"ruleName\":\"手机号码过去60天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689768,\"desc\":\"手机号码过去60天内累计申请次数<=15次\"},{\"ruleCode\":\"LMM_AH00_0028\",\"ruleType\":0,\"ruleName\":\"IP过去60天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689749,\"desc\":\"IP过去60天内累计申请的身份证号数<=5\"},{\"ruleCode\":\"LMM_AH00_0024\",\"ruleType\":0,\"ruleName\":\"设备ID过去90天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689729,\"desc\":\"设备ID过去90天内累计申请的手机号码数<=10\"},{\"ruleCode\":\"LMM_AH00_0032\",\"ruleType\":0,\"ruleName\":\"IP过去30天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687708,\"desc\":\"IP过去30天内累计申请的手机号码数<=3\"},{\"ruleCode\":\"LMM_AH00_0050\",\"ruleType\":0,\"ruleName\":\"联系地址过去1天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689754,\"desc\":\"联系地址过去1天内累计申请手机号码个数<=3\"},{\"ruleCode\":\"LMM_AH00_0015\",\"ruleType\":0,\"ruleName\":\"设备ID过去1天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689735,\"desc\":\"设备ID过去1天内累计申请的身份证号数<=2\"},{\"ruleCode\":\"LMM_AH00_0005\",\"ruleType\":0,\"ruleName\":\"手机号码过去1天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831687712,\"desc\":\"手机号码过去1天内累计申请次数<=3次\"},{\"ruleCode\":\"LMM_AH00_0025\",\"ruleType\":0,\"ruleName\":\"IP过去1天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689737,\"desc\":\"IP过去1天内累计申请的身份证号数<=2\"},{\"ruleCode\":\"LMM_AH00_0029\",\"ruleType\":0,\"ruleName\":\"IP过去90天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689766,\"desc\":\"IP过去90天内累计申请的身份证号数<=10\"},{\"ruleCode\":\"LMM_AH00_0038\",\"ruleType\":0,\"ruleName\":\"GPS过去60天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689753,\"desc\":\"GPS过去60天内累计申请的身份证号数<=5\"},{\"ruleCode\":\"LMM_AH00_0027\",\"ruleType\":0,\"ruleName\":\"IP过去30天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689730,\"desc\":\"IP过去30天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0026\",\"ruleType\":0,\"ruleName\":\"IP过去7天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689758,\"desc\":\"IP过去7天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0053\",\"ruleType\":0,\"ruleName\":\"联系地址过去60天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689745,\"desc\":\"联系地址过去60天内累计申请手机号码个数<=10\"},{\"ruleCode\":\"LMM_AH00_0039\",\"ruleType\":0,\"ruleName\":\"GPS过去90天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689739,\"desc\":\"GPS过去90天内累计申请的身份证号数<=10\"},{\"ruleCode\":\"LMM_AH00_0017\",\"ruleType\":0,\"ruleName\":\"设备ID过去30天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689757,\"desc\":\"设备ID过去30天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0014\",\"ruleType\":0,\"ruleName\":\"身份证过去90天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689728,\"desc\":\"身份证过去90天内累计申请次数<=30次\"},{\"ruleCode\":\"LMM_AH00_0004\",\"ruleType\":0,\"ruleName\":\"账户历史累计申请成功次数校验\",\"state\":2,\"ruleId\":183478831687704,\"desc\":\"账户历史累计申请成功次数=1次\"},{\"ruleCode\":\"LMM_AH00_0020\",\"ruleType\":0,\"ruleName\":\"设备ID过去1天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687714,\"desc\":\"设备ID过去1天内累计申请的手机号码数<=2\"},{\"ruleCode\":\"LMM_AH00_0018\",\"ruleType\":0,\"ruleName\":\"设备ID过去60天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689756,\"desc\":\"设备ID过去60天内累计申请的身份证号数<=5\"},{\"ruleCode\":\"LMM_AH00_0003\",\"ruleType\":0,\"ruleName\":\"银行卡历史累计申请成功次数校验\",\"state\":2,\"ruleId\":183478831689750,\"desc\":\"银行卡历史累计申请成功次数=1次\"},{\"ruleCode\":\"LMM_AH00_0033\",\"ruleType\":0,\"ruleName\":\"IP过去60天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687711,\"desc\":\"IP过去60天内累计申请的手机号码数<=5\"},{\"ruleCode\":\"LMM_AH00_0041\",\"ruleType\":0,\"ruleName\":\"GPS过去7天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689732,\"desc\":\"GPS过去7天内累计申请手机号码个数<=3\"},{\"ruleCode\":\"LMM_AH00_0036\",\"ruleType\":0,\"ruleName\":\"GPS过去7天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831687715,\"desc\":\"GPS过去7天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0021\",\"ruleType\":0,\"ruleName\":\"设备ID过去7天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687717,\"desc\":\"设备ID过去7天内累计申请的手机号码数<=3\"},{\"ruleCode\":\"LMM_AH00_0009\",\"ruleType\":0,\"ruleName\":\"手机号码过去90天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689761,\"desc\":\"手机号码过去90天内累计申请次数<=30次\"},{\"ruleCode\":\"LMM_AH00_0030\",\"ruleType\":0,\"ruleName\":\"IP过去1天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831687705,\"desc\":\"IP过去1天内累计申请的手机号码数<=2\"},{\"ruleCode\":\"LMM_AH00_0044\",\"ruleType\":0,\"ruleName\":\"GPS过去90天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689751,\"desc\":\"GPS过去90天内累计申请手机号码个数<=10\"},{\"ruleCode\":\"LMM_AH00_0048\",\"ruleType\":0,\"ruleName\":\"联系地址过去60天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689759,\"desc\":\"联系地址过去60天内累计申请身份证号个数<=10\"},{\"ruleCode\":\"LMM_AH00_0019\",\"ruleType\":0,\"ruleName\":\"设备ID过去90天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831687720,\"desc\":\"设备ID过去90天内累计申请的身份证号数<=10\"},{\"ruleCode\":\"LMM_AH00_0012\",\"ruleType\":0,\"ruleName\":\"身份证过去30天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831687706,\"desc\":\"身份证过去30天内累计申请次数<=10次\"},{\"ruleCode\":\"LMM_AH00_0037\",\"ruleType\":0,\"ruleName\":\"GPS过去30天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831689744,\"desc\":\"GPS过去30天内累计申请的身份证号数<=3\"},{\"ruleCode\":\"LMM_AH00_0047\",\"ruleType\":0,\"ruleName\":\"联系地址过去30天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689760,\"desc\":\"联系地址过去30天内累计申请身份证号个数<=7\"},{\"ruleCode\":\"LMM_AH00_0052\",\"ruleType\":0,\"ruleName\":\"联系地址过去30天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689743,\"desc\":\"联系地址过去30天内累计申请手机号码个数<=7\"},{\"ruleCode\":\"LMM_AH00_0011\",\"ruleType\":0,\"ruleName\":\"身份证过去7天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689733,\"desc\":\"身份证过去7天内累计申请次数<=5次\"},{\"ruleCode\":\"LMM_AH00_0049\",\"ruleType\":0,\"ruleName\":\"联系地址过去90天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689746,\"desc\":\"联系地址过去90天内累计申请身份证号个数<=20\"},{\"ruleCode\":\"LMM_AH00_0040\",\"ruleType\":0,\"ruleName\":\"GPS过去1天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831687707,\"desc\":\"GPS过去1天内累计申请手机号码个数<=2\"},{\"ruleCode\":\"LMM_AH00_0034\",\"ruleType\":0,\"ruleName\":\"IP过去90天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689740,\"desc\":\"IP过去90天内累计申请的手机号码数<=10\"},{\"ruleCode\":\"LMM_AH00_0051\",\"ruleType\":0,\"ruleName\":\"联系地址过去7天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831689742,\"desc\":\"联系地址过去7天内累计申请手机号码个数<=5\"},{\"ruleCode\":\"LMM_AH00_0001\",\"ruleType\":0,\"ruleName\":\"身份证历史累计申请成功次数校验\",\"state\":2,\"ruleId\":183478831689738,\"desc\":\"身份证历史累计申请成功次数=1次\"},{\"ruleCode\":\"LMM_AH00_0023\",\"ruleType\":0,\"ruleName\":\"设备ID过去60天内累计申请的手机号码数校验\",\"state\":2,\"ruleId\":183478831689763,\"desc\":\"设备ID过去60天内累计申请的手机号码数<=5\"},{\"ruleCode\":\"LMM_AH00_0045\",\"ruleType\":0,\"ruleName\":\"联系地址过去1天内累计申请身份证号个数校验\",\"state\":2,\"ruleId\":183478831689765,\"desc\":\"联系地址过去1天内累计申请身份证号个数<=3\"},{\"ruleCode\":\"LMM_AH00_0043\",\"ruleType\":0,\"ruleName\":\"GPS过去60天内累计申请手机号码个数校验\",\"state\":2,\"ruleId\":183478831687719,\"desc\":\"GPS过去60天内累计申请手机号码个数<=5\"},{\"ruleCode\":\"LMM_AH00_0006\",\"ruleType\":0,\"ruleName\":\"手机号码过去7天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831687713,\"desc\":\"手机号码过去7天内累计申请次数<=5次\"},{\"ruleCode\":\"LMM_AH00_0010\",\"ruleType\":0,\"ruleName\":\"身份证过去1天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831687709,\"desc\":\"身份证过去1天内累计申请次数<=3次\"},{\"ruleCode\":\"LMM_AH00_0035\",\"ruleType\":0,\"ruleName\":\"GPS过去1天内累计申请的身份证号数校验\",\"state\":2,\"ruleId\":183478831687718,\"desc\":\"GPS过去1天内累计申请的身份证号数<=2\"},{\"ruleCode\":\"LMM_AH00_0007\",\"ruleType\":0,\"ruleName\":\"手机号码过去30天内累计申请次数校验\",\"state\":2,\"ruleId\":183478831689755,\"desc\":\"手机号码过去30天内累计申请次数<=10次\"}],\"refusedRules\":[],\"calcRules\":[],\"focusRules\":[]},{\"status\":\"0x0000\",\"msg\":\"拒绝\",\"nodeId\":183478829539334,\"nodeName\":\"基本信息\",\"execOrder\":2,\"refusedScore\":\"\",\"calcScore\":\"\",\"missedRules\":[{\"ruleCode\":\"LMM_CH00_0003\",\"ruleType\":0,\"ruleName\":\"高位地市校验\",\"state\":2,\"ruleId\":183478831687699,\"desc\":\"身份证归属地不为 福建莆田，福建宁德,，福建龙岩\"},{\"ruleCode\":\"LMM_CH00_0002\",\"ruleType\":0,\"ruleName\":\"年龄校验\",\"state\":2,\"ruleId\":183478831687681,\"desc\":\"年龄为20到55岁\"},{\"ruleCode\":\"LMM_CH00_0001\",\"ruleType\":0,\"ruleName\":\"身份证位数校验\",\"state\":2,\"ruleId\":183478831687695,\"desc\":\"身份证为18位\"},{\"ruleCode\":\"LMM_CH00_0005\",\"ruleType\":0,\"ruleName\":\"申请时间校验\",\"state\":2,\"ruleId\":183478831687688,\"desc\":\"开放时间为6点到24点\"}],\"refusedRules\":[{\"ruleId\":183478831687687,\"ruleCode\":\"LMM_CH00_0004\",\"ruleName\":\"开放地市校验\",\"ruleAudit\":\"拒绝\",\"state\":1,\"desc\":\"手机号归属地为北京上海浙江江苏广东地区\",\"output\":[]}],\"calcRules\":[{\"ruleId\":183089805197315,\"ruleCode\":\"XBD_CS00_0006\",\"ruleName\":\"芝麻申请欺诈评分限制\",\"score\":80,\"state\":1,\"desc\":\"申请欺诈评分不能低于40分\",\"output\":[{\"code\":\"score\",\"name\":\"信用得分\",\"value\":80}]}],\"focusRules\":[]}]}]}";
			if(respStr!=null){
				ZBankResult zBankResult = new ZBankResult();
				try{
					zBankResult = ZBankUtil.processJSONResult(respStr);
					resultParam.put("json", respStr);
					resultParam.put("obj", zBankResult);
				}catch(Exception e){
					logger.error("塔返回串转对象失败");
				}
			}
		}

		return resultParam;
	}

//	public static void main(String[] args) {
//		Map<String,String> reqMap = new HashMap<String,String>();
//		//reqMap.put("engineCode", "ZBD_Acard");
//		reqMap.put("engineCode", "jiarealo");
//		reqMap.put("pid","1111");
//		reqMap.put("uid","1111");
//		//String url = "http://101.132.45.246:8080/api/decision";
//		String url = "http://172.21.203.23:8080/api/decision";
//		ZBankResult zBankResult = reqDecision(reqMap,url);
//	}

}
*/
