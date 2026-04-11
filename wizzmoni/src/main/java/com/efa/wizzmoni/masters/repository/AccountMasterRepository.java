package com.efa.wizzmoni.masters.repository;

import com.efa.wizzmoni.masters.AccountMasterDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

@Repository
public class AccountMasterRepository {

    private static final Logger log = LoggerFactory.getLogger(AccountMasterRepository.class);

    @Autowired
    @Qualifier("efaJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd-MM-yyyy");

    // ── RowMapper: direct table columns ──────────────────────────

    private final RowMapper<AccountMasterDto> tableRowMapper = (rs, rowNum) -> {
        AccountMasterDto dto = new AccountMasterDto();
        dto.setAccountCode(trim(rs, "c_accountcode"));
        dto.setAccountName(trim(rs, "c_name"));
        dto.setAcGroup(trim(rs, "c_acgroup"));
        dto.setParent(trim(rs, "c_parent"));
        dto.setLevel(rs.getInt("i_level"));
        dto.setAccountType(trim(rs, "c_accounttype"));
        dto.setLeftKey(rs.getDouble("n_leftkey"));
        dto.setRightKey(rs.getDouble("n_rightkey"));
        dto.setBalanceCheck(trim(rs, "c_balancecheck"));
        dto.setFinYear(rs.getDouble("finyr"));
        dto.setCategory(trim(rs, "c_cat"));
        dto.setDollarAc(trim(rs, "dollar_ac"));
        java.sql.Timestamp ts = rs.getTimestamp("date_add");
        dto.setDateAdd(ts != null ? SDF.format(ts) : "");
        return dto;
    };

    private String trim(ResultSet rs, String col) throws SQLException {
        String val = rs.getString(col);
        return val != null ? val.trim() : "";
    }

    // ── Tree ──────────────────────────────────────────────────────

    public List<AccountMasterDto> findAllForTree(String companyCode, double finyr) {
        String sql = "SELECT c_accountcode, c_name, c_acgroup, c_parent, " +
                "       i_level, c_accounttype, n_leftkey, n_rightkey, " +
                "       c_balancecheck, date_add, finyr, c_cat, dollar_ac " +
                "FROM efa.account_master " +
                "WHERE company_code = ? AND finyr = ? " +
                "ORDER BY n_leftkey";
        log.info("[Repo] findAllForTree: company='{}' finyr={}", companyCode, finyr);
        List<AccountMasterDto> list = jdbcTemplate.query(sql, tableRowMapper, companyCode, finyr);
        log.info("[Repo] findAllForTree returned {} rows", list.size());
        return list;
    }

    // ── Detail: direct table query ────────────────────────────────

    public AccountMasterDto findByCode(String companyCode, double finyr, String accountCode) {
        String sql = "SELECT c_accountcode, c_name, c_acgroup, c_parent, " +
                "       i_level, c_accounttype, n_leftkey, n_rightkey, " +
                "       c_balancecheck, date_add, finyr, c_cat, dollar_ac " +
                "FROM efa.account_master " +
                "WHERE company_code = ? AND finyr = ? AND TRIM(c_accountcode) = TRIM(?)";
        log.info("[Repo] findByCode: company='{}' finyr={} code='{}'", companyCode, finyr, accountCode);
        List<AccountMasterDto> results = jdbcTemplate.query(
                sql, tableRowMapper, companyCode, finyr, accountCode.trim());
        log.info("[Repo] findByCode returned {} rows", results.size());
        return results.isEmpty() ? null : results.get(0);
    }

    // ── Update Account Name via stored function ───────────────────
    //
    //  Calls: efa.fn_efa_accountmaster_update(p_companycode, p_c_accountcode, p_c_name)
    //  Returns 0 on success, raises EXCEPTION on failure (caught by Spring as DataAccessException)
    //  Only allowed for level 3 and level 4 accounts (enforced in service layer)

    public void updateAccountName(String companyCode, String accountCode, String newName) {
        log.info("[Repo] updateAccountName: company='{}' code='{}' newName='{}'",
                companyCode, accountCode, newName);

        // SELECT fn() returns integer — queryForObject handles it
        Integer result = jdbcTemplate.queryForObject(
                "SELECT efa.fn_efa_accountmaster_update(?, ?, ?)",
                Integer.class,
                companyCode,
                accountCode.trim(),
                newName.trim()
        );

        log.info("[Repo] updateAccountName result={}", result);
        // result == 0 means success; non-zero or exception means failure (handled by fn)
    }

    // ── Insert ───────────────────────────────────────────────────
    //
    //  Mirrors the PowerBuilder save logic exactly — two DB calls:
    //
    //  Step 1 — fn_efa_accountmaster_insert(...)
    //           Inserts into account_master with placeholder leftkey/rightkey=0.
    //           Also inserts account_balance rows for all branches (if acGroup=A, cat=A).
    //           Checks maintenance mode and that the financial year is open.
    //
    //  Step 2 — fn_efa_populatekeys_account(...)
    //           Assigns the correct n_leftkey / n_rightkey for the new node
    //           by inspecting the live tree. May trigger a full tree rebuild
    //           via fn_efa_populatekeys_tree() if key space is exhausted.
    //
    //  Java does NO manual key shifting — the DB handles everything.

    public void insertAccount(AccountMasterDto dto) {

        String companyCode  = dto.getCompanyCode();
        String accountCode  = dto.getAccountCode().trim();
        String parent       = dto.getParent().trim();
        String acGroup      = dto.getAcGroup()      != null ? dto.getAcGroup()      : "A";
        String accountType  = dto.getAccountType()  != null ? dto.getAccountType()  : "A";
        String balanceCheck = dto.getBalanceCheck() != null ? dto.getBalanceCheck() : "B";
        String category     = dto.getCategory()     != null ? dto.getCategory()     : "A";
        String dollarAc     = dto.getDollarAc()     != null ? dto.getDollarAc()     : "N";

        // ── Step 1: Insert via DB function (leftkey/rightkey = 0 placeholders) ──
        log.info("[Repo] Step1 fn_efa_accountmaster_insert: company='{}' code='{}' parent='{}' level={}",
                companyCode, accountCode, parent, dto.getLevel());

        Integer insertResult = jdbcTemplate.queryForObject(
                "SELECT efa.fn_efa_accountmaster_insert(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                Integer.class,
                companyCode,                                // p_c_companycode  varchar
                accountCode,                               // p_c_accountcode  varchar
                dto.getAccountName(),                      // p_c_name         varchar
                acGroup,                                   // p_c_acgroup      char   (A=Account / G=Group)
                parent,                                    // p_c_parent       varchar
                dto.getLevel(),                            // p_i_level        integer
                accountType,                               // p_c_accounttype  char
                java.math.BigDecimal.ZERO,                 // p_n_leftkey      numeric  → 0 placeholder
                java.math.BigDecimal.ZERO,                 // p_n_rightkey     numeric  → 0 placeholder
                new java.math.BigDecimal(dto.getFinYear()),// p_finyr          numeric
                balanceCheck,                              // p_balchk         char
                category,                                  // p_cat            char   (A=All / S=Some)
                dollarAc                                   // p_dollar_ac      char
        );
        log.info("[Repo] fn_efa_accountmaster_insert returned: {}", insertResult);

        // ── Step 2: Assign correct nested-set keys via DB function ──
        log.info("[Repo] Step2 fn_efa_populatekeys_account: code='{}' parent='{}' acGroup='{}'",
                accountCode, parent, acGroup);

        // fn_efa_populatekeys_account returns integer (0=success)
        Integer keysResult = jdbcTemplate.queryForObject(
                "SELECT efa.fn_efa_populatekeys_account(?,?,?,?)",
                Integer.class,
                companyCode,
                accountCode.trim(),
                parent.trim(),
                acGroup.trim()
        );
        if (keysResult == null || keysResult != 0) {
            throw new RuntimeException(
                    "fn_efa_populatekeys_account returned unexpected result: " + keysResult);
        }
        log.info("[Repo] fn_efa_populatekeys_account completed OK for code='{}'", accountCode);
    }

    // ── Delete via DB function ────────────────────────────────────
    //
    //  Mirrors PB delete script exactly:
    //
    //  PB passes:
    //    @c_companycode  = login company code
    //    @c_accountcode  = account being deleted
    //    @ac_finyr       = the account's own stored finyr  (ll_acfinyr)
    //    @log_finyr      = active login financial year      (ll_logfinyr = DEFAULT_FINYR = 35)
    //    @c_acgroup      = A or G
    //    @cat            = c_cat (A=All branches / S=Some)
    //    @dollar_ac      = Y or N
    //
    //  The DB function handles:
    //    - maintenance mode check
    //    - deleting from account_master (and subtree)
    //    - deleting from account_balance for all branches / financial years
    //    - nested-set key cleanup

    public void deleteAccount(String companyCode,
                              double acFinyr,    // account's own finyr (from DB record)
                              double logFinyr,   // active login finyr  (DEFAULT_FINYR)
                              String accountCode,
                              String acGroup,
                              String category,
                              String dollarAc) {

        log.info("[Repo] fn_efa_accountmaster_delete: company='{}' code='{}' acFinyr={} logFinyr={} acGroup='{}' cat='{}' dollar='{}'",
                companyCode, accountCode, acFinyr, logFinyr, acGroup, category, dollarAc);

        // fn_efa_accountmaster_delete returns integer (0=success)
        Integer deleteResult = jdbcTemplate.queryForObject(
                "SELECT efa.fn_efa_accountmaster_delete(?,?,?,?,?,?,?)",
                Integer.class,
                companyCode,
                accountCode.trim(),
                new java.math.BigDecimal(acFinyr),
                new java.math.BigDecimal(logFinyr),
                acGroup.trim(),
                category.trim(),
                dollarAc.trim()
        );
        if (deleteResult == null || deleteResult != 0) {
            throw new RuntimeException(
                    "fn_efa_accountmaster_delete returned unexpected result: " + deleteResult);
        }
        log.info("[Repo] fn_efa_accountmaster_delete completed OK for code='{}'", accountCode);
    }
    // ── Apply to Branches ─────────────────────────────────────────
    //
    //  getBranchesForAccount:
    //    Returns all branches for the company with applicable='Y'/'N'
    //    by LEFT JOINing branch_accounts for this specific account+finyr.
    //    Mirrors PB dw_2.retrieve(company, account, finyr, parent).
    //
    //  applyBranchToAccount:
    //    Calls fn_efa_branchac_insert for one branch.
    //    Only called for newly-checked rows (mirrors PB loop condition:
    //    Newmodified! AND applicable='Y').

    public java.util.List<java.util.Map<String, Object>> getBranchesForAccount(
            String companyCode, String accountCode, double finyr) {

        // fn_efa_ddw_branch returns: brcode (char), name (char)
        // LEFT JOIN branch_accounts to determine applicable = Y/N
        String sql =
                "SELECT b.brcode        AS c_branchcode, " +
                        "       TRIM(b.name)    AS c_branchname, " +
                        "       CASE WHEN ba.c_accountcode IS NOT NULL THEN 'Y' ELSE 'N' END AS applicable " +
                        "FROM   efa.fn_efa_ddw_branch(?) b " +
                        "LEFT JOIN efa.branch_accounts ba " +
                        "       ON ba.company_code  = ? " +
                        "      AND TRIM(ba.c_branchcode)  = TRIM(b.brcode) " +
                        "      AND TRIM(ba.c_accountcode) = TRIM(?) " +
                        "      AND ba.finyr         = ? ";

        log.info("[Repo] getBranchesForAccount: company='{}' account='{}' finyr={}",
                companyCode, accountCode, finyr);

        return jdbcTemplate.queryForList(sql,
                companyCode,                          // fn_efa_ddw_branch param
                companyCode,                          // LEFT JOIN company_code
                accountCode.trim(),                   // LEFT JOIN c_accountcode
                new java.math.BigDecimal(finyr));     // LEFT JOIN finyr
    }

    public void applyBranchToAccount(String companyCode, String branchCode,
                                     String accountCode, double logFinyr, String parent) {
        log.info("[Repo] fn_efa_branchac_insert: company='{}' branch='{}' account='{}' finyr={}",
                companyCode, branchCode, accountCode, logFinyr);

        // fn signature:
        //   fn_efa_branchac_insert(p_c_companycode, p_branch, p_account, p_log_finyr, p_parent)
        //   RETURNS void — raises EXCEPTION on failure
        // fn_efa_branchac_insert returns integer (0=success)
        Integer branchResult = jdbcTemplate.queryForObject(
                "SELECT efa.fn_efa_branchac_insert(?,?,?,?,?)",
                Integer.class,
                companyCode,
                branchCode.trim(),
                accountCode.trim(),
                new java.math.BigDecimal(logFinyr),
                parent.trim()
        );
        if (branchResult == null || branchResult != 0) {
            throw new RuntimeException(
                    "fn_efa_branchac_insert returned unexpected result: " + branchResult);
        }
        log.info("[Repo] fn_efa_branchac_insert OK: branch='{}' account='{}'",
                branchCode, accountCode);
    }

    // ── Opening Balance ───────────────────────────────────────────
    //
    //  retrieve: fn_efa_opbal_retrieve(company, account, finyr)
    //    → rows: out_branch, out_opening, out_d_date, out_dr_open,
    //            out_cr_open, out_closing, out_finyr
    //
    //  update:   fn_efa_opbal_update(company, account, branch,
    //                                opbal, orgbal, finyr, d_date)
    //    → void, raises EXCEPTION on failure
    //    Called per row only when opbal != orgbal (mirrors PB DataModified check)

    public java.util.List<java.util.Map<String, Object>> getOpeningBalance(
            String companyCode, String accountCode, double finyr) {

        // JOIN fn_efa_opbal_retrieve with fn_efa_ddw_branch to get branch name
        // fn_efa_ddw_branch returns: brcode (char), name (char)
        String sql =
                "SELECT ob.out_branch, " +
                        "       TRIM(b.name) AS out_branch_name, " +
                        "       ob.out_opening, ob.out_d_date, " +
                        "       ob.out_dr_open, ob.out_cr_open, ob.out_closing, ob.out_finyr " +
                        "FROM   efa.fn_efa_opbal_retrieve(?, ?, ?) ob " +
                        "LEFT JOIN efa.fn_efa_ddw_branch(?) b " +
                        "       ON TRIM(b.brcode) = TRIM(ob.out_branch) " +
                        "ORDER BY TRIM(b.name)";

        log.info("[Repo] fn_efa_opbal_retrieve: company='{}' account='{}' finyr={}",
                companyCode, accountCode, finyr);

        return jdbcTemplate.queryForList(sql,
                companyCode,                          // fn_efa_opbal_retrieve param
                accountCode.trim(),
                new java.math.BigDecimal(finyr),
                companyCode);                         // fn_efa_ddw_branch param
    }

    public void updateOpeningBalance(String companyCode, String accountCode,
                                     String branchCode, java.math.BigDecimal opbal,
                                     java.math.BigDecimal orgbal, double finyr,
                                     java.sql.Timestamp dDate) {
        log.info("[Repo] fn_efa_opbal_update: company='{}' account='{}' branch='{}' opbal={} orgbal={}",
                companyCode, accountCode, branchCode, opbal, orgbal);

        // fn signature:
        //   fn_efa_opbal_update(p_c_companycode, p_c_accountcode, p_c_branchcode,
        //                       p_opbal, p_orgbal, p_finyr, p_date)
        //   RETURNS void
        // fn_efa_opbal_update returns integer (0 = success, raises EXCEPTION on failure)
        Integer result = jdbcTemplate.queryForObject(
                "SELECT efa.fn_efa_opbal_update(?,?,?,?,?,?,?)",
                Integer.class,
                companyCode,
                accountCode.trim(),
                branchCode.trim(),
                opbal,
                orgbal,
                new java.math.BigDecimal(finyr),
                dDate
        );
        if (result == null || result != 0) {
            throw new RuntimeException(
                    "fn_efa_opbal_update returned unexpected result: " + result +
                            " for branch='" + branchCode + "'");
        }

        log.info("[Repo] fn_efa_opbal_update OK: branch='{}'", branchCode);
    }

}