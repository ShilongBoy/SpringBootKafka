package model;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class BodyDataArrayData {

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

	@XmlElement(name = "field")
	public List<BodyDataArrayDataField> getBodyDataArrayDataField() {
		return bodyDataArrayDataField;
	}

	public void setBodyDataArrayDataField(List<BodyDataArrayDataField> bodyDataArrayDataField) {
		this.bodyDataArrayDataField = bodyDataArrayDataField;
	}

	private List<BodyDataArrayDataField> bodyDataArrayDataField;
}
