package model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class SysHeadStructDataArray {

	@XmlElementWrapper(name = "struct")
	@XmlElement(name = "data")
	public List<SysHeadStructDataArrayData> getSysHeadStructDataArrayData() {
		return sysHeadStructDataArrayData;
	}

	public void setSysHeadStructDataArrayData(List<SysHeadStructDataArrayData> sysHeadStructDataArrayData) {
		this.sysHeadStructDataArrayData = sysHeadStructDataArrayData;
	}

	private List<SysHeadStructDataArrayData> sysHeadStructDataArrayData;
}
