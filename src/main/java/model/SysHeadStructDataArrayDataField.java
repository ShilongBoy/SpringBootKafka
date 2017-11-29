package model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class SysHeadStructDataArrayDataField {

	@XmlAttribute(name = "scale")
	public String getScale() {
		return scale;
	}
	public void setScale(String scale) {
		this.scale = scale;
	}
	private String scale;
	
	@XmlAttribute(name = "length")
	public String getLength() {
		return length;
	}
	public void setLength(String length) {
		this.length = length;
	}
	private String length;
	
	@XmlAttribute(name = "type")
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	private String type;
	
	@XmlValue
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	private String value;
}
