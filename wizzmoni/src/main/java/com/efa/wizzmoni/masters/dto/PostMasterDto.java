package com.efa.wizzmoni.masters;

public class PostMasterDto {

    private String companyCode;
    private String postType;
    private String accountCode;
    private String accountName;    // for display only (from fn_efa_accmaster_dd join)
    private String remarks;

    public PostMasterDto() {}

    public String getCompanyCode()  { return companyCode; }
    public String getPostType()     { return postType; }
    public String getAccountCode()  { return accountCode; }
    public String getAccountName()  { return accountName; }
    public String getRemarks()      { return remarks; }

    public void setCompanyCode(String v)  { this.companyCode = v; }
    public void setPostType(String v)     { this.postType = v; }
    public void setAccountCode(String v)  { this.accountCode = v; }
    public void setAccountName(String v)  { this.accountName = v; }
    public void setRemarks(String v)      { this.remarks = v; }
}