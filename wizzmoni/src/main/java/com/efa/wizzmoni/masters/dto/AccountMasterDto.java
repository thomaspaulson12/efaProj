package com.efa.wizzmoni.masters;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountMasterDto {

    private String companyCode;
    private String accountCode;
    private String accountName;
    private String acGroup;        // 'G' = Group, 'A' = Account
    private String parent;
    private int    level;
    private String accountType;    // '#' = system-created (cannot delete), 'U' = user-created
    private double leftKey;
    private double rightKey;
    private String balanceCheck;   // 'B' = Both, 'D' = Debit, 'C' = Credit
    private String dateAdd;
    private double finYear;
    private String category;
    private String dollarAc;       // 'Y' / 'N'
    private List<AccountMasterDto> children;

    public AccountMasterDto() {
        this.children = new ArrayList<>();
    }

    // ── Getters ──────────────────────────────────────────────────────

    public String getCompanyCode()  { return companyCode; }
    public String getAccountCode()  { return accountCode; }
    public String getAccountName()  { return accountName; }
    public String getAcGroup()      { return acGroup; }
    public String getParent()       { return parent; }
    public int    getLevel()        { return level; }
    public String getAccountType()  { return accountType; }
    public double getLeftKey()      { return leftKey; }
    public double getRightKey()     { return rightKey; }
    public String getBalanceCheck() { return balanceCheck; }
    public String getDateAdd()      { return dateAdd; }
    public double getFinYear()      { return finYear; }
    public String getCategory()     { return category; }
    public String getDollarAc()     { return dollarAc; }
    public List<AccountMasterDto> getChildren() { return children; }

    // ── Setters ──────────────────────────────────────────────────────

    public void setCompanyCode(String companyCode)   { this.companyCode = companyCode; }
    public void setAccountCode(String accountCode)   { this.accountCode = accountCode; }
    public void setAccountName(String accountName)   { this.accountName = accountName; }
    public void setAcGroup(String acGroup)           { this.acGroup = acGroup; }
    public void setParent(String parent)             { this.parent = parent; }
    public void setLevel(int level)                  { this.level = level; }
    public void setAccountType(String accountType)   { this.accountType = accountType; }
    public void setLeftKey(double leftKey)           { this.leftKey = leftKey; }
    public void setRightKey(double rightKey)         { this.rightKey = rightKey; }
    public void setBalanceCheck(String balanceCheck) { this.balanceCheck = balanceCheck; }
    public void setDateAdd(String dateAdd)           { this.dateAdd = dateAdd; }
    public void setFinYear(double finYear)           { this.finYear = finYear; }
    public void setCategory(String category)         { this.category = category; }
    public void setDollarAc(String dollarAc)         { this.dollarAc = dollarAc; }
    public void setChildren(List<AccountMasterDto> children) { this.children = children; }
}