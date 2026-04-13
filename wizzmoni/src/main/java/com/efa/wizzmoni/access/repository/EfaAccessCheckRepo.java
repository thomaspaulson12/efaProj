package com.efa.wizzmoni.access.repository;

import com.efa.wizzmoni.access.data.EfaUserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EfaAccessCheckRepo {

    private static final Logger log = LoggerFactory.getLogger(EfaAccessCheckRepo.class);

    private final JdbcTemplate jdbcTemplate;

    public EfaAccessCheckRepo(@Qualifier("efaJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public EfaUserData getUserDetails(String branch, String user) {
        log.debug(">>>> Entering getUserDetails for branch={} user={}", branch, user);

        String sql = "SELECT * FROM efa.fn_efa_login_session_select(?, ?)";

        EfaUserData userData = null;
        try {
            userData = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                EfaUserData u = new EfaUserData();
                u.setCompanyCode(rs.getString("company_code"));
                u.setCompanyName(rs.getString("company_name"));
                u.setBranchCode(rs.getString("branch_code"));
                u.setBranchName(rs.getString("branch_name"));
                u.setBranchAdd1(rs.getString("branch_add1"));
                u.setBranchAdd2(rs.getString("branch_add2"));
                u.setPhone(rs.getString("phone"));
                u.setFax(rs.getString("fax"));
                u.setUserCode(rs.getString("user_code"));
                u.setFinYr(rs.getBigDecimal("finyr"));              // Fixed: was getInt()
                u.setFinStart(rs.getDate("fin_start"));             // Fixed: was getTimestamp()
                u.setFinEnd(rs.getDate("fin_end"));                 // Fixed: was getTimestamp()
                u.setControlAc(rs.getString("controlac"));
                u.setMaxBp(rs.getBigDecimal("maxbp"));
                u.setMaxCp(rs.getBigDecimal("maxcp"));
                u.setFinYearClose(rs.getInt("finyearclose"));       // Fixed: was getBoolean()
                u.setUserName(rs.getString("username"));
                u.setVersion(rs.getInt("version"));
                u.setCashParent(rs.getString("cash_parent"));
                u.setBankParent(rs.getString("bank_parent"));
                String aoBranch = rs.getString("aobranch");
                u.setAoBranch(aoBranch != null && !aoBranch.isEmpty()
                        ? aoBranch.charAt(0) : null);               // Fixed: was getString() directly
                u.setSysDate(rs.getTimestamp("sysdate"));
                u.setExportFlag(rs.getString("export_flag"));
                // Note: status_flag is NOT returned by fn_efa_login_session_select.
                // It must be set separately after login validation if needed.
                return u;
            }, branch, user);

            log.debug("==== User details fetched successfully for user={}", user);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            log.warn("==== No user found for branch={} user={}", branch, user);
        } catch (Exception ex) {
            log.error("==== Error fetching user details for branch={} user={}, reason={}",
                    branch, user, ex.getMessage(), ex);
            throw new RuntimeException("No Data Found");
        }

        log.debug("<<<< Exiting getUserDetails");
        return userData;
    }
}