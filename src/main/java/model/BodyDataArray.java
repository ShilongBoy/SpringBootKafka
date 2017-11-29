package model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class BodyDataArray {

	@XmlElementWrapper(name = "struct")
	@XmlElement(name = "data")
	public List<BodyDataArrayData> getBodyDataArrayData() {
		return bodyDataArrayData;
	}

	public void setBodyDataArrayData(List<BodyDataArrayData> bodyDataArrayData) {
		this.bodyDataArrayData = bodyDataArrayData;
	}

	private List<BodyDataArrayData> bodyDataArrayData;

}
