package model;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class ReturnSysHeadData {

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
	public List<ReturnSysHeadStructData> getSysHeadStructData() {
		return sysHeadStructData;
	}

	public void setSysHeadStructData(List<ReturnSysHeadStructData> sysHeadStructData) {
		this.sysHeadStructData = sysHeadStructData;
	}

	private List<ReturnSysHeadStructData> sysHeadStructData;
}
