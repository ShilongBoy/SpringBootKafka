package model;

import javax.xml.bind.annotation.XmlAttribute;

public class ReturnAppHeadData {

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

}
