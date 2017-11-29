package model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class ReturnSysHeadStructData {

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

	@XmlElement(name = "field")
	public SysHeadStructDataField getField() {
		return field;
	}

	public void setField(SysHeadStructDataField field) {
		this.field = field;
	}

	private SysHeadStructDataField field;

	@XmlElement(name = "array")
	public SysHeadStructDataArray getSysHeadStructDataArray() {
		return sysHeadStructDataArray;
	}

	public void setSysHeadStructDataArray(SysHeadStructDataArray sysHeadStructDataArray) {
		this.sysHeadStructDataArray = sysHeadStructDataArray;
	}

	private SysHeadStructDataArray sysHeadStructDataArray;

}
