package pojo;

public class AppCardInfo {
    /**
     *  cardNo 卡号
     */
    private String cardNo;

    private String checkOption;

    private String track2;

    private String ccy;

    private String channel;

    private String cupDate;

    private String cupAreaCode;

    private String merchNo;

    private Double tranAmt;

    public String getRetCode() {
        return retCode;
    }

    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    private String addtnlDataPrivate;

    private String reserved;

    private String integrateOption;

    private String retMsg;

    private String retCode;

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getCheckOption() {
        return checkOption;
    }

    public void setCheckOption(String checkOption) {
        this.checkOption = checkOption;
    }

    public String getTrack2() {
        return track2;
    }

    public void setTrack2(String track2) {
        this.track2 = track2;
    }

    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCupDate() {
        return cupDate;
    }

    public void setCupDate(String cupDate) {
        this.cupDate = cupDate;
    }

    public String getCupAreaCode() {
        return cupAreaCode;
    }

    public void setCupAreaCode(String cupAreaCode) {
        this.cupAreaCode = cupAreaCode;
    }

    public String getMerchNo() {
        return merchNo;
    }

    public void setMerchNo(String merchNo) {
        this.merchNo = merchNo;
    }

    public String getAddtnlDataPrivate() {
        return addtnlDataPrivate;
    }

    public void setAddtnlDataPrivate(String addtnlDataPrivate) {
        this.addtnlDataPrivate = addtnlDataPrivate;
    }

    public String getReserved() {
        return reserved;
    }

    public void setReserved(String reserved) {
        this.reserved = reserved;
    }

    public String getIntegrateOption() {
        return integrateOption;
    }

    public void setIntegrateOption(String integrateOption) {
        this.integrateOption = integrateOption;
    }

    public String getRetMsg() {
        return retMsg;
    }

    public void setRetMsg(String retMsg) {
        this.retMsg = retMsg;
    }

    public Double getTranAmt() {
        return tranAmt;
    }

    public void setTranAmt(Double tranAmt) {
        this.tranAmt = tranAmt;
    }

    @Override
    public String toString() {
        return "AppCardInfo{" +
                "cardNo='" + cardNo + '\'' +
                ", checkOption='" + checkOption + '\'' +
                ", track2='" + track2 + '\'' +
                ", ccy='" + ccy + '\'' +
                ", channel='" + channel + '\'' +
                ", cupDate='" + cupDate + '\'' +
                ", cupAreaCode='" + cupAreaCode + '\'' +
                ", merchNo='" + merchNo + '\'' +
                ", tranAmt='" + tranAmt + '\'' +
                ", addtnlDataPrivate='" + addtnlDataPrivate + '\'' +
                ", reserved='" + reserved + '\'' +
                ", integrateOption='" + integrateOption + '\'' +
                ", retMsg='" + retMsg + '\'' +
                ", retCode='" + retCode + '\'' +
                '}';
    }
}
