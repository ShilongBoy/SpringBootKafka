package model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "service")
public class ReturnXMLService {

	@XmlElementWrapper(name = "sys-header")
	@XmlElement(name = "data")
	public List<ReturnSysHeadData> getRespSysHeadData() {
		return respSysHeadData;
	}

	public void setRespSysHeadData(List<ReturnSysHeadData> respSysHeadData) {
		this.respSysHeadData = respSysHeadData;
	}

	private List<ReturnSysHeadData> respSysHeadData;

	@XmlElement(name = "app-header")
	public ReturnAppHeadData getRespAppHeadData() {
		return respAppHeadData;
	}

	public void setRespAppHeadData(ReturnAppHeadData respAppHeadData) {
		this.respAppHeadData = respAppHeadData;
	}

	private ReturnAppHeadData respAppHeadData;

	@XmlElement(name = "local-header")
	public ReturnLocalHeadData getRespLocalHeadData() {
		return respLocalHeadData;
	}

	public void setRespLocalHeadData(ReturnLocalHeadData respLocalHeadData) {
		this.respLocalHeadData = respLocalHeadData;
	}

	private ReturnLocalHeadData respLocalHeadData;

	@XmlElementWrapper(name = "body")
	@XmlElement(name = "data")
	public List<BodyData> getBodyData() {
		return bodyData;
	}

	public void setBodyData(List<BodyData> bodyData) {
		this.bodyData = bodyData;
	}

	private List<BodyData> bodyData;

}
