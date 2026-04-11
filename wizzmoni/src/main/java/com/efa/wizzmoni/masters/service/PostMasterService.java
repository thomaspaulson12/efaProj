package com.efa.wizzmoni.masters.service;

import com.efa.wizzmoni.masters.PostMasterDto;
import com.efa.wizzmoni.masters.repository.PostMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PostMasterService {

    private static final Logger log = LoggerFactory.getLogger(PostMasterService.class);

    @Autowired
    private PostMasterRepository repository;

    private static final String DEFAULT_COMPANY = "XM";
    private static final double DEFAULT_FINYR   = 35;

    // ── Select ────────────────────────────────────────────────────

    public List<PostMasterDto> getAll(String companyCode) {
        return repository.findAll(companyCode, DEFAULT_FINYR);
    }

    // ── Account dropdown ──────────────────────────────────────────

    public List<Map<String, Object>> getAccountDropdown(String companyCode) {
        return repository.getAccountDropdown(companyCode, DEFAULT_FINYR);
    }

    // ── Validate (called per-row before bulk insert) ─────────────
    //  Mirrors PB loop: checks each row individually with early return.

    public void validate(PostMasterDto dto) {
        if (dto.getPostType() == null || dto.getPostType().isBlank())
            throw new IllegalArgumentException("Post Type is required.");
        if (dto.getPostType().trim().length() != 6)
            throw new IllegalArgumentException(
                    "Post Type must be exactly 6 characters (got " +
                            dto.getPostType().trim().length() + ").");
        if (dto.getAccountCode() == null || dto.getAccountCode().isBlank())
            throw new IllegalArgumentException("Account Code is required.");
        if (dto.getRemarks() == null || dto.getRemarks().isBlank())
            throw new IllegalArgumentException("Remarks is required.");
    }

    // ── Insert ────────────────────────────────────────────────────

    public void insert(PostMasterDto dto) {
        if (dto.getCompanyCode() == null || dto.getCompanyCode().isBlank())
            dto.setCompanyCode(DEFAULT_COMPANY);
        validate(dto);
        log.info("[PostService] insert: postType='{}' account='{}'",
                dto.getPostType(), dto.getAccountCode());
        repository.insert(dto);
    }

    // ── Batch insert ──────────────────────────────────────────────
    //  Validates ALL rows first (mirrors PB: returns -1 on first error),
    //  then inserts each. Remarks is mandatory per PB validation.

    public void insertBatch(String companyCode, List<PostMasterDto> rows) {
        if (rows == null || rows.isEmpty())
            throw new IllegalArgumentException("No rows to save.");

        String company = (companyCode != null && !companyCode.isBlank())
                ? companyCode : DEFAULT_COMPANY;

        // Pass 1: validate all rows first
        for (int i = 0; i < rows.size(); i++) {
            PostMasterDto dto = rows.get(i);
            dto.setCompanyCode(company);
            int rowNum = i + 1;
            try {
                validate(dto);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Row " + rowNum + ": " + e.getMessage());
            }
        }

        // Pass 2: insert all rows
        for (PostMasterDto dto : rows) {
            log.info("[PostService] insertBatch: postType='{}' account='{}'",
                    dto.getPostType(), dto.getAccountCode());
            repository.insert(dto);
        }
    }


    public String getDefaultCompany() { return DEFAULT_COMPANY; }
    public double getDefaultFinyr()   { return DEFAULT_FINYR; }
}