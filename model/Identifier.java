package com.sp.model;


import java.math.BigInteger;

public class Identifier {

    //name of identifier
    private String name;

    //type of identifier like SEGMENT or PROC or BYTE
    private String type;

    //offset of line which contains
    private Integer offset;

    //for equ
    private BigInteger value;

    //only for procedures or segments
    private Integer length;

    //which segment contains this data
    private String attribute;

    //only for segment identifiers
    private Boolean closed;
    private String segmentRegister;

    public Identifier(String name, String type, Integer offset, String attribute) {
        this.name = name;
        this.type = type;
        this.offset = offset;
        this.attribute = attribute;
    }

    public Identifier(String name, String type, Integer offset, BigInteger value, String attribute) {
        this.name = name;
        this.type = type;
        this.offset = offset;
        this.value = value;
        this.attribute = attribute;
    }

    public Identifier(String name, String type, Integer offset, Integer length, Boolean closed) {
        this.name = name;
        this.type = type;
        this.offset = offset;
        this.length = length;
        this.closed = closed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public String getSegmentRegister() {
        return segmentRegister;
    }

    public void setSegmentRegister(String segmentRegister) {
        this.segmentRegister = segmentRegister;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Identifier that = (Identifier) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
