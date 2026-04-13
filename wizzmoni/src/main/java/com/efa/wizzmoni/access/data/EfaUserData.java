package com.efa.wizzmoni.access.data;

import java.math.BigDecimal;
import java.sql.Date;

public class EfaUserData {

    private String companyCode;
    private String companyName;
    private String branchCode;
    private String branchName;
    private String branchAdd1;
    private String branchAdd2;
    private String phone;
    private String fax;
    private String userCode;
    private BigDecimal finYr;           // PG: numeric → BigDecimal (was Integer)
    private Date finStart;              // PG: date    → java.sql.Date (was Timestamp)
    private Date finEnd;                // PG: date    → java.sql.Date (was Timestamp)
    private String controlAc;
    private BigDecimal maxBp;
    private BigDecimal maxCp;
    private Integer finYearClose;       // PG: integer → Integer (was Boolean)
    private String userName;
    private Integer version;
    private String cashParent;
    private String bankParent;
    private Character aoBranch;         // PG: character(1) → Character (was String)
    private java.sql.Timestamp sysDate;
    private String exportFlag;
    private String statusFlag;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getBranchAdd1() {
        return branchAdd1;
    }

    public void setBranchAdd1(String branchAdd1) {
        this.branchAdd1 = branchAdd1;
    }

    public String getBranchAdd2() {
        return branchAdd2;
    }

    public void setBranchAdd2(String branchAdd2) {
        this.branchAdd2 = branchAdd2;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public BigDecimal getFinYr() {
        return finYr;
    }

    public void setFinYr(BigDecimal finYr) {
        this.finYr = finYr;
    }

    public Date getFinStart() {
        return finStart;
    }

    public void setFinStart(Date finStart) {
        this.finStart = finStart;
    }

    public Date getFinEnd() {
        return finEnd;
    }

    public void setFinEnd(Date finEnd) {
        this.finEnd = finEnd;
    }

    public String getControlAc() {
        return controlAc;
    }

    public void setControlAc(String controlAc) {
        this.controlAc = controlAc;
    }

    public BigDecimal getMaxBp() {
        return maxBp;
    }

    public void setMaxBp(BigDecimal maxBp) {
        this.maxBp = maxBp;
    }

    public BigDecimal getMaxCp() {
        return maxCp;
    }

    public void setMaxCp(BigDecimal maxCp) {
        this.maxCp = maxCp;
    }

    public Integer getFinYearClose() {
        return finYearClose;
    }

    public void setFinYearClose(Integer finYearClose) {
        this.finYearClose = finYearClose;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getCashParent() {
        return cashParent;
    }

    public void setCashParent(String cashParent) {
        this.cashParent = cashParent;
    }

    public String getBankParent() {
        return bankParent;
    }

    public void setBankParent(String bankParent) {
        this.bankParent = bankParent;
    }

    public Character getAoBranch() {
        return aoBranch;
    }

    public void setAoBranch(Character aoBranch) {
        this.aoBranch = aoBranch;
    }

    public java.sql.Timestamp getSysDate() {
        return sysDate;
    }

    public void setSysDate(java.sql.Timestamp sysDate) {
        this.sysDate = sysDate;
    }

    public String getExportFlag() {
        return exportFlag;
    }

    public void setExportFlag(String exportFlag) {
        this.exportFlag = exportFlag;
    }

    public String getStatusFlag() {
        return statusFlag;
    }

    public void setStatusFlag(String statusFlag) {
        this.statusFlag = statusFlag;
    }
}