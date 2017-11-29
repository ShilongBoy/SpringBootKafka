package model;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class AcceptSysHeadData {

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

	@XmlElementWrapper(name = "struct")
	@XmlElement(name = "data")
	public List<AcceptSysHeadStructData> getSysHeadStructData() {
		return sysHeadStructData;
	}

	public void setSysHeadStructData(List<AcceptSysHeadStructData> sysHeadStructData) {
		this.sysHeadStructData = sysHeadStructData;
	}

	private List<AcceptSysHeadStructData> sysHeadStructData;
	
}
