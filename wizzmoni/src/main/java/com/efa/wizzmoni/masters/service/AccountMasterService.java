package com.efa.wizzmoni.masters.service;

import com.efa.wizzmoni.masters.AccountMasterDto;
import com.efa.wizzmoni.masters.repository.AccountMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccountMasterService {

    private static final Logger log = LoggerFactory.getLogger(AccountMasterService.class);

    @Autowired
    private AccountMasterRepository repository;

    private static final String DEFAULT_COMPANY = "XM";
    private static final double DEFAULT_FINYR   = 35;

    // ── Debug ─────────────────────────────────────────────────────

    public List<AccountMasterDto> debugGetFlatList(String companyCode, double finyr) {
        return repository.findAllForTree(companyCode, finyr);
    }

    // ── Tree ──────────────────────────────────────────────────────

    public AccountMasterDto getAccountTree(String companyCode, double finyr) {
        List<AccountMasterDto> flatList = repository.findAllForTree(companyCode, finyr);
        log.info("[Tree] Rows from DB: {}", flatList.size());
        if (flatList.isEmpty()) return null;

        Map<String, AccountMasterDto> map = new LinkedHashMap<>();
        for (AccountMasterDto dto : flatList) {
            dto.setCompanyCode(companyCode);
            String key = dto.getAccountCode();
            if (key != null && !key.isBlank()) map.put(key, dto);
        }

        AccountMasterDto root = null;
        for (AccountMasterDto dto : flatList) {
            if (dto.getLevel() == 1) { root = dto; continue; }
            String p = dto.getParent();
            if (p != null && !p.isBlank()) {
                AccountMasterDto parent = map.get(p.trim());
                if (parent != null) parent.getChildren().add(dto);
                else log.warn("[Tree] Parent '{}' not found for '{}'", p, dto.getAccountCode());
            }
        }
        if (root == null) {
            for (AccountMasterDto dto : flatList) {
                String p = dto.getParent() != null ? dto.getParent().trim() : "";
                if (p.isEmpty() || !map.containsKey(p)) { root = dto; break; }
            }
        }
        if (root != null)
            log.info("[Tree] Root='{}' children={}", root.getAccountCode(), root.getChildren().size());
        else
            log.error("[Tree] Root could not be determined!");
        return root;
    }

    // ── Detail ────────────────────────────────────────────────────

    public AccountMasterDto getAccountDetail(String companyCode, double finyr, String accountCode) {
        log.info("[Detail] company='{}' finyr={} code='{}'", companyCode, finyr, accountCode);
        AccountMasterDto dto = repository.findByCode(companyCode, finyr, accountCode);
        if (dto != null) dto.setCompanyCode(companyCode);
        return dto;
    }

    // ── Update Account Name ───────────────────────────────────────
    //
    //  Rules (enforced here, not in DB fn):
    //   - Only level 3 and level 4 accounts may be renamed
    //   - Name must not be blank
    //   - Calls efa.fn_efa_accountmaster_update(companyCode, accountCode, newName)

    public void updateAccountName(String companyCode, double finyr,
                                  String accountCode, String newName) {
        if (newName == null || newName.isBlank())
            throw new IllegalArgumentException("Account name cannot be empty.");

        // Verify account exists and check its level
        AccountMasterDto existing = repository.findByCode(companyCode, finyr, accountCode);
        if (existing == null)
            throw new RuntimeException("Account not found: " + accountCode);

        int level = existing.getLevel();
        if (level < 3)
            throw new IllegalArgumentException(
                    "Only Level 3 and Level 4 accounts can be renamed. " +
                            "This account is Level " + level + ".");

        log.info("[Update] Renaming '{}' (level={}) → '{}'", accountCode, level, newName);
        repository.updateAccountName(companyCode, accountCode, newName);
        log.info("[Update] Done.");
    }

    // ── Save (Insert) ─────────────────────────────────────────────
    //
    //  Mirrors PB validation before calling DB functions:
    //
    //  1. Account code must be exactly 6 characters
    //     → PB: if len(master_c_accountcode) < 6 → reject
    //
    //  2. Level 3 must be Group (G), not Account (A)
    //     → PB: if acGroup='A' AND (parentLevel+1)=3 → "Only group accounts allowed at this level"
    //     (Level 4 can be Account — the commented-out PB check confirms this was intentionally relaxed)
    //
    //  3. finyr uses the active login financial year (DEFAULT_FINYR = 35)
    //     → PB: @finyr = ll_logfinyr  (login structure finyr, not the account's stored finyr)

    public void saveAccount(AccountMasterDto dto) {
        if (dto.getCompanyCode() == null || dto.getCompanyCode().isBlank())
            dto.setCompanyCode(DEFAULT_COMPANY);
        if (dto.getFinYear() == 0) dto.setFinYear(DEFAULT_FINYR);

        // ── Validation 1: Account code must be exactly 6 characters ──
        String code = dto.getAccountCode() != null ? dto.getAccountCode().trim() : "";
        if (code.length() < 6)
            throw new IllegalArgumentException("A/c code should be of 6 characters.");
        dto.setAccountCode(code);

        // ── Validation 2: Level 3 must always be a Group (G) ─────────
        int newLevel = dto.getLevel();
        String acGroup = dto.getAcGroup() != null ? dto.getAcGroup().trim() : "A";
        if ("A".equals(acGroup) && newLevel == 3)
            throw new IllegalArgumentException(
                    "Only Group accounts are allowed at Level 3. " +
                            "Please select a Group type.");

        log.info("[Save] code='{}' parent='{}' level={} acGroup='{}'",
                dto.getAccountCode(), dto.getParent(), newLevel, acGroup);
        repository.insertAccount(dto);
    }

    // ── Delete ────────────────────────────────────────────────────
    //
    //  PB passes both the account's own finyr (ac_finyr) and the
    //  login/active finyr (log_finyr = DEFAULT_FINYR).
    //  We first load the account record to get ac_finyr, acGroup, cat, dollarAc
    //  — all required by fn_efa_accountmaster_delete.

    public void deleteAccount(String companyCode, String accountCode) {
        log.info("[Delete] company='{}' code='{}'", companyCode, accountCode);

        // Load account record to get the fields PB reads from the datawindow
        AccountMasterDto existing = repository.findByCode(companyCode, DEFAULT_FINYR, accountCode);
        if (existing == null)
            throw new RuntimeException("Account not found: " + accountCode);

        double acFinyr  = existing.getFinYear();                                        // ll_acfinyr
        double logFinyr = DEFAULT_FINYR;                                                // ll_logfinyr
        String acGroup  = existing.getAcGroup()   != null ? existing.getAcGroup()   : "A";
        String category = existing.getCategory()  != null ? existing.getCategory()  : "A";
        String dollarAc = existing.getDollarAc()  != null ? existing.getDollarAc()  : "N";

        repository.deleteAccount(companyCode, acFinyr, logFinyr,
                accountCode, acGroup, category, dollarAc);
        log.info("[Delete] Done.");
    }

    // ── Defaults ──────────────────────────────────────────────────

    public String getDefaultCompany() { return DEFAULT_COMPANY; }
    public double getDefaultFinyr()   { return DEFAULT_FINYR; }
    // ── Apply to Branches ─────────────────────────────────────────
    //
    //  PB logic:
    //  - Only opens popup if c_acgroup='A' AND c_cat='S'
    //  - If c_cat='A' → "A/c is Applicable for all Branches and Cannot be Changed"
    //  - These checks are enforced in the controller/frontend; service handles data.

    public java.util.List<java.util.Map<String, Object>> getBranchesForAccount(
            String companyCode, String accountCode) {
        // Uses DEFAULT_FINYR as the active finyr (= login finyr in PB)
        return repository.getBranchesForAccount(companyCode, accountCode, DEFAULT_FINYR);
    }

    public void applyBranches(String companyCode, String accountCode,
                              java.util.List<String> branches) {
        if (branches == null || branches.isEmpty())
            throw new IllegalArgumentException("No branches selected to apply.");

        // Load the account to get the parent — PB reads it from str_openbal.par
        AccountMasterDto account = repository.findByCode(companyCode, DEFAULT_FINYR, accountCode);
        if (account == null)
            throw new RuntimeException("Account not found: " + accountCode);

        // Validate: must be Account (A) type with Some Branches (S) category
        if (!"A".equals(account.getAcGroup()))
            throw new IllegalArgumentException("Apply to Branches is only available for Account type (not Group).");
        if (!"S".equals(account.getCategory()))
            throw new IllegalArgumentException("This account is applicable to All Branches and cannot be changed.");

        String parent = account.getParent() != null ? account.getParent().trim() : "";

        // Call fn_efa_branchac_insert for each newly selected branch
        // Mirrors PB loop: only Newmodified! rows with applicable='Y'
        for (String branch : branches) {
            log.info("[ApplyBranch] company='{}' account='{}' branch='{}'",
                    companyCode, accountCode, branch);
            repository.applyBranchToAccount(
                    companyCode, branch.trim(), accountCode, DEFAULT_FINYR, parent);
        }
        log.info("[ApplyBranch] Done — {} branch(es) applied.", branches.size());
    }

    // ── Opening Balance ───────────────────────────────────────────
    //
    //  PB display logic (ue_retrieve):
    //    opening > 0 → dr_open = opening,  cr_open = 0
    //    opening < 0 → cr_open = opening * -1, dr_open = 0
    //    opening = 0 → both = 0
    //
    //  We apply this split here so the frontend gets dr_open / cr_open
    //  directly — no logic needed in JS.

    public java.util.List<java.util.Map<String, Object>> getOpeningBalance(
            String companyCode, String accountCode) {

        var rows = repository.getOpeningBalance(companyCode, accountCode, DEFAULT_FINYR);

        // Apply PB display split: opening → dr_open / cr_open
        for (var row : rows) {
            Object openingObj = row.get("out_opening");
            java.math.BigDecimal opening = openingObj != null
                    ? new java.math.BigDecimal(openingObj.toString())
                    : java.math.BigDecimal.ZERO;

            java.math.BigDecimal drOpen = java.math.BigDecimal.ZERO;
            java.math.BigDecimal crOpen = java.math.BigDecimal.ZERO;

            if (opening.compareTo(java.math.BigDecimal.ZERO) > 0) {
                drOpen = opening;
            } else if (opening.compareTo(java.math.BigDecimal.ZERO) < 0) {
                crOpen = opening.negate();   // * -1 as PB does
            }

            row.put("dr_open", drOpen);
            row.put("cr_open", crOpen);
        }

        return rows;
    }

    // Save opening balance rows that changed (mirrors PB DataModified! check)
    // Each entry: { branch, drOpen, crOpen, orgBal, dDate }
    // opbal = drOpen - crOpen  (Debit positive, Credit negative)
    public void saveOpeningBalance(String companyCode, String accountCode,
                                   java.util.List<java.util.Map<String, Object>> rows) {

        if (rows == null || rows.isEmpty())
            throw new IllegalArgumentException("No rows to save.");

        for (var row : rows) {
            String branch = String.valueOf(row.get("branch")).trim();

            java.math.BigDecimal drOpen  = new java.math.BigDecimal(
                    row.getOrDefault("drOpen", "0").toString());
            java.math.BigDecimal crOpen  = new java.math.BigDecimal(
                    row.getOrDefault("crOpen", "0").toString());
            java.math.BigDecimal orgBal  = new java.math.BigDecimal(
                    row.getOrDefault("orgBal", "0").toString());

            // opbal: Debit = positive, Credit = negative (mirrors PB convention)
            java.math.BigDecimal opbal = drOpen.subtract(crOpen);

            // Only call DB fn if value actually changed — mirrors PB DataModified! check
            if (opbal.compareTo(orgBal) == 0) {
                log.info("[OpenBal] branch='{}' unchanged, skipping.", branch);
                continue;
            }

            // Parse d_date (ISO string from frontend)
            java.sql.Timestamp dDate;
            try {
                String dateStr = String.valueOf(row.get("dDate"));
                dDate = java.sql.Timestamp.valueOf(
                        java.time.LocalDateTime.parse(dateStr,
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception ex) {
                // fallback: try parsing as date only
                try {
                    String dateStr = String.valueOf(row.get("dDate")).substring(0, 19)
                            .replace("T", " ");
                    dDate = java.sql.Timestamp.valueOf(dateStr);
                } catch (Exception ex2) {
                    throw new RuntimeException(
                            "Invalid date format for branch '" + branch + "': " + row.get("dDate"));
                }
            }

            log.info("[OpenBal] saving branch='{}' opbal={} orgbal={}", branch, opbal, orgBal);
            repository.updateOpeningBalance(
                    companyCode, accountCode, branch, opbal, orgBal, DEFAULT_FINYR, dDate);
        }
    }

}