package com.sp.model;


import com.sp.Utils.Constants;

import java.math.BigInteger;

public class Line {

    private Integer lineNumber;
    private Integer offset;

    private Integer operandSizePrefix;
    private Integer addressSizePrefix;
    private Integer changeSegmentPrefix;

    //operation code 1 or 2 byte
    private Integer operationCode;

    //byte mod,reg,r/m
    private Integer mrm;
    //byte scale, index, base
    private Integer sib;

    //address 0,1,2 or 4 bytes
    private Integer address;
    //1,2 or 4
    private Integer addressSize;

    //operand 0,1,2 or 4 bytes
    private BigInteger operand;
    //1,2 or 4
    private Integer operandSize;

    //line content
    private String line;
    //line error
    private String error;


    //some type of line; like segment begin;
    //than find open and close segment
    private String type;

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getOperandSizePrefix() {
        return operandSizePrefix;
    }

    public void setOperandSizePrefix(Integer operandSizePrefix) {
        this.operandSizePrefix = operandSizePrefix;
    }

    public Integer getAddressSizePrefix() {
        return addressSizePrefix;
    }

    public void setAddressSizePrefix(Integer addressSizePrefix) {
        this.addressSizePrefix = addressSizePrefix;
    }

    public Integer getChangeSegmentPrefix() {
        return changeSegmentPrefix;
    }

    public void setChangeSegmentPrefix(Integer changeSegmentPrefix) {
        if (changeSegmentPrefix != null) {
            if (this.changeSegmentPrefix == null) {
                this.changeSegmentPrefix = changeSegmentPrefix;
            }
        }
    }

    public Integer getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(Integer operationCode) {
        this.operationCode = operationCode;
    }

    public Integer getMrm() {
        return mrm;
    }

    public void setMrm(Integer mrm) {
        this.mrm = mrm;
    }

    public Integer getSib() {
        return sib;
    }

    public void setSib(Integer sib) {
        this.sib = sib;
    }

    public Integer getAddress() {
        return address;
    }

    public void setAddress(Integer address) {
        this.address = address;
    }

    public Integer getAddressSize() {
        return addressSize;
    }

    public void setAddressSize(Integer addressSize) {
        this.addressSize = addressSize;
    }

    public BigInteger getOperand() {
        return operand;
    }

    public void setOperand(BigInteger operand) {
        this.operand = operand;
    }

    public Integer getOperandSize() {
        return operandSize;
    }

    public void setOperandSize(Integer operandSize) {
        this.operandSize = operandSize;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            if (!error()) {
                this.error = error;
            } else {
                this.error += "\n\t " + error;
            }
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer generateOffset() {
        if (line.contains(Constants.getKeyWords()[16])) {
            return 0;
        }
        return (operationCode() ? 1 : 0) + (mrm() ? 1 : 0) + (sib() ? 1 : 0) +
                (address() ? addressSize : 0) + (operand() ? operandSize : 0) +
                (changeSegmentPrefix() ? 1 : 0) + (addressSizePrefix() ? 1 : 0) + (operandSizePrefix() ? 1 : 0);
    }

    private boolean changeSegmentPrefix() {
        return changeSegmentPrefix != null;
    }

    private boolean addressSizePrefix() {
        return addressSizePrefix != null;
    }

    private boolean operandSizePrefix() {
        return operandSizePrefix != null;
    }

    private boolean offset() {
        return offset != null;
    }

    private boolean operationCode() {
        return operationCode != null;
    }

    private boolean mrm() {
        return mrm != null;
    }

    private boolean sib() {
        return sib != null;
    }

    private boolean address() {
        return address != null;
    }

    private boolean operand() {
        return operand != null;
    }

    private boolean error() {
        return error != null;
    }

    @Override
    public String toString() {
        //4 4 2 2 8 8 line  34
        return String.format("%1$4d" + (offset() ? " %2$04X" : "") + (changeSegmentPrefix() ? " %10$02X:" : "") +
                (addressSizePrefix() ? " %11$02X|" : "") + (operandSizePrefix() ? " %12$02X|" : "") + (operationCode() ? " %3$02X" : "") +
                (mrm() ? " %4$02X" : "") + (sib() ? "%5$02X" : "") + (address() ? " %6$0" + addressSize * 2 + "X" : "") +
                (operand() ? " %7$0" + operandSize * 2 + "X" : "") + " %8$S" + (error() ? "\n\t %9$S" : ""),
                lineNumber, offset, operationCode, mrm, sib, address, operand, line, error, changeSegmentPrefix,
                addressSizePrefix, operandSizePrefix);
    }
}