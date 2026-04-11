package com.efa.wizzmoni.masters.repository;

import com.efa.wizzmoni.masters.PostMasterDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class PostMasterRepository {

    private static final Logger log = LoggerFactory.getLogger(PostMasterRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── Select all post master records ────────────────────────────
    //  fn_efa_post_master_select(p_company_code)
    //  Returns: post_type, c_accountcode, remarks, company_code
    //  We LEFT JOIN fn_efa_accmaster_dd to get the account name for display.

    public List<PostMasterDto> findAll(String companyCode, double finyr) {
        String sql =
                "SELECT p.post_type, p.c_accountcode, p.remarks, p.company_code, " +
                        "       TRIM(a.c_name) AS c_name " +
                        "FROM   efa.fn_efa_post_master_select(?) p " +
                        "LEFT JOIN efa.fn_efa_accmaster_dd(?, ?) a " +
                        "       ON TRIM(a.c_accountcode) = TRIM(p.c_accountcode) " +
                        "ORDER BY p.post_type";

        log.info("[PostRepo] findAll: company='{}' finyr={}", companyCode, finyr);

        return jdbcTemplate.query(sql,
                (rs, rn) -> {
                    PostMasterDto dto = new PostMasterDto();
                    dto.setCompanyCode(rs.getString("company_code"));
                    dto.setPostType(trim(rs.getString("post_type")));
                    dto.setAccountCode(trim(rs.getString("c_accountcode")));
                    dto.setAccountName(trim(rs.getString("c_name")));
                    dto.setRemarks(trim(rs.getString("remarks")));
                    return dto;
                },
                companyCode,
                companyCode,
                new java.math.BigDecimal(finyr)
        );
    }

    // ── Account dropdown ──────────────────────────────────────────
    //  fn_efa_accmaster_dd(p_company_code, p_finyear)
    //  Returns: c_accountcode, c_name, c_accounttype, c_parent, company_code
    //  Only accounts (c_acgroup = 'A')

    public List<Map<String, Object>> getAccountDropdown(String companyCode, double finyr) {
        String sql =
                "SELECT TRIM(c_accountcode) AS c_accountcode, " +
                        "       TRIM(c_name)        AS c_name " +
                        "FROM   efa.fn_efa_accmaster_dd(?, ?) " +
                        "ORDER BY c_name";

        log.info("[PostRepo] getAccountDropdown: company='{}' finyr={}", companyCode, finyr);

        return jdbcTemplate.queryForList(sql,
                companyCode,
                new java.math.BigDecimal(finyr));
    }

    // ── Insert ────────────────────────────────────────────────────
    //  fn_efa_post_master_insert(company, post_type, accountcode, remarks)
    //  Returns integer 0 on success, raises EXCEPTION on failure.

    public void insert(PostMasterDto dto) {
        log.info("[PostRepo] fn_efa_post_master_insert: company='{}' postType='{}' account='{}'",
                dto.getCompanyCode(), dto.getPostType(), dto.getAccountCode());

        Integer result = jdbcTemplate.queryForObject(
                "SELECT efa.fn_efa_post_master_insert(?,?,?,?)",
                Integer.class,
                dto.getCompanyCode(),
                dto.getPostType().trim(),
                dto.getAccountCode().trim(),
                dto.getRemarks() != null ? dto.getRemarks().trim() : ""
        );

        if (result == null || result != 0) {
            throw new RuntimeException(
                    "fn_efa_post_master_insert returned unexpected result: " + result);
        }
        log.info("[PostRepo] fn_efa_post_master_insert OK: postType='{}'", dto.getPostType());
    }

    private String trim(String s) { return s != null ? s.trim() : ""; }
}