package model;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class BodyData {

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

	@XmlElement(name = "field")
	public BodyDataField getField() {
		return field;
	}

	public void setField(BodyDataField field) {
		this.field = field;
	}

	private BodyDataField field;

	@XmlElement(name = "reason")
	public String[] getRefuseReason() {
		return refuseReason;
	}

	public void setRefuseReason(String[] refuseReason) {
		this.refuseReason = refuseReason;
	}

	private String[] refuseReason;

	@XmlElement(name = "array")
	public List<BodyDataArray> getBodyDataArray() {
		return bodyDataArray;
	}

	public void setBodyDataArray(List<BodyDataArray> bodyDataArray) {
		this.bodyDataArray = bodyDataArray;
	}

	private List<BodyDataArray> bodyDataArray;
}
