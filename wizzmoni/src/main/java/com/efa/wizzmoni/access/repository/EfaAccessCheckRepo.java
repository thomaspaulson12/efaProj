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
                u.setFinYr(rs.getInt("finyr"));
                u.setFinStart(rs.getTimestamp("fin_start"));
                u.setFinEnd(rs.getTimestamp("fin_end"));
                u.setControlAc(rs.getString("ControlAc"));
                u.setMaxBp(rs.getBigDecimal("MaxBp"));
                u.setMaxCp(rs.getBigDecimal("MaxCp"));
                u.setFinYearClose(rs.getBoolean("finyearclose"));
                u.setUserName(rs.getString("username"));
                u.setVersion(rs.getInt("version"));
                u.setCashParent(rs.getString("cash_parent"));
                u.setBankParent(rs.getString("bank_parent"));
                u.setAoBranch(rs.getString("aobranch"));
                u.setSysDate(rs.getTimestamp("sysdate"));
                u.setExportFlag(rs.getString("export_flag"));
                u.setStatusFlag(rs.getString("status_flag"));
                return u;
            }, branch, user);

            log.debug("==== User details fetched successfully for user={}", user);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            log.warn("==== No user found for branch={} user={}", branch, user);
        } catch (Exception ex) {
            log.error("==== Error fetching user details for branch={} user={}, reason={}", branch, user, ex.getMessage(), ex);
            throw new RuntimeException("No Data Found");
        }

        log.debug("<<<< Exiting getUserDetails");
        return userData;
    }
}