package model;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class SysHeadStructDataArrayData {

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

	@XmlElement(name = "field")
	public List<SysHeadStructDataArrayDataField> getSysHeadStructDataArrayDataField() {
		return sysHeadStructDataArrayDataField;
	}

	public void setSysHeadStructDataArrayDataField(
			List<SysHeadStructDataArrayDataField> sysHeadStructDataArrayDataField) {
		this.sysHeadStructDataArrayDataField = sysHeadStructDataArrayDataField;
	}

	private List<SysHeadStructDataArrayDataField> sysHeadStructDataArrayDataField;
}
