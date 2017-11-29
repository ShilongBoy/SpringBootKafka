package model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "service")
public class AcceptXMLService {

	@XmlElementWrapper(name = "sys-header")
	@XmlElement(name = "data")
	public List<AcceptSysHeadData> getAcceptSysHeadData() {
		return acceptSysHeadData;
	}

	public void setAcceptSysHeadData(List<AcceptSysHeadData> acceptSysHeadData) {
		this.acceptSysHeadData = acceptSysHeadData;
	}
	private List<AcceptSysHeadData> acceptSysHeadData;

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
