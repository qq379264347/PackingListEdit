package com.zcb.packinglistedit;

public class Part {
    /**
     * 保修单编号
     */
    private String bxid;
    /**
     * 装箱单箱号
     */
    private String boxNumber;
    /**
     * 配件代码
     */
    private String code;
    /**
     * 配件名称
     */
    private String name;

    /**
     * 配件数量
     */
    private String num;

    public Part(String bxid, String boxNumber, String code, String name, String num) {
        this.bxid = bxid;
        this.boxNumber = boxNumber;
        this.code = code;
        this.name = name;
        this.num = num;
    }

    // Getters and Setters
    public String getBxid() { return bxid; }
    public String getBoxNumber() { return boxNumber; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getNum() { return num; }
    public void setBoxNumber(String boxNumber) { this.boxNumber = boxNumber; }
}