package com.efa.wizzmoni.masters.controller;

import com.efa.wizzmoni.masters.PostMasterDto;
import com.efa.wizzmoni.masters.service.PostMasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/masters")
public class PostMasterController {

    private static final Logger log = LoggerFactory.getLogger(PostMasterController.class);

    @Autowired
    private PostMasterService postMasterService;

    // ── Page ──────────────────────────────────────────────────────

    @GetMapping("/post")
    public String postMasterPage(Model model) {
        model.addAttribute("companyCode", postMasterService.getDefaultCompany());
        model.addAttribute("finyr", (int) postMasterService.getDefaultFinyr());
        return "masters/post";
    }

    // ── REST: Get all records ─────────────────────────────────────

    @GetMapping("/api/post/list")
    @ResponseBody
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "XM") String companyCode) {
        try {
            List<PostMasterDto> list = postMasterService.getAll(companyCode);
            return ResponseEntity.ok(Map.of("rows", list));
        } catch (Exception e) {
            log.error("[PostController] getAll error", e);
            return ResponseEntity.status(500).body(Map.of("error", cleanError(e)));
        }
    }

    // ── REST: Account dropdown ────────────────────────────────────

    @GetMapping("/api/post/accounts")
    @ResponseBody
    public ResponseEntity<?> getAccounts(
            @RequestParam(defaultValue = "XM") String companyCode) {
        try {
            var accounts = postMasterService.getAccountDropdown(companyCode);
            return ResponseEntity.ok(Map.of("accounts", accounts));
        } catch (Exception e) {
            log.error("[PostController] getAccounts error", e);
            return ResponseEntity.status(500).body(Map.of("error", cleanError(e)));
        }
    }

    // ── REST: Insert multiple rows ────────────────────────────────
    //
    //  POST /masters/api/post/save
    //  Body: { "companyCode":"XM", "rows": [
    //    { "postType":"DR    ", "accountCode":"CASH01", "remarks":"Cash payments" },
    //    { "postType":"CR    ", "accountCode":"BANK01", "remarks":"Bank receipts" }
    //  ]}
    //
    //  Validates ALL rows first (mirrors PB loop with early return on error),
    //  then inserts each by calling fn_efa_post_master_insert per row.

    @PostMapping("/api/post/save")
    @ResponseBody
    public ResponseEntity<?> save(@RequestBody Map<String, Object> body) {
        try {
            String companyCode = (String) body.getOrDefault("companyCode", "XM");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows =
                    (List<Map<String, Object>>) body.get("rows");

            if (rows == null || rows.isEmpty())
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "No rows to save."));

            // ── Validate ALL rows first (mirrors PB for loop with early return) ──
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> r = rows.get(i);
                PostMasterDto dto = new PostMasterDto();
                dto.setCompanyCode(companyCode);
                dto.setPostType   ((String) r.get("postType"));
                dto.setAccountCode((String) r.get("accountCode"));
                dto.setRemarks    ((String) r.get("remarks"));
                try {
                    postMasterService.validate(dto);
                } catch (IllegalArgumentException e) {
                    // Return row index so frontend can highlight the offending row
                    return ResponseEntity.badRequest().body(Map.of(
                            "success",  false,
                            "error",    "Row " + (i + 1) + ": " + e.getMessage(),
                            "rowIndex", i
                    ));
                }
            }

            // ── Insert each row ───────────────────────────────────────────────────
            int saved = 0;
            for (Map<String, Object> r : rows) {
                PostMasterDto dto = new PostMasterDto();
                dto.setCompanyCode(companyCode);
                dto.setPostType   ((String) r.get("postType"));
                dto.setAccountCode((String) r.get("accountCode"));
                dto.setRemarks    ((String) r.get("remarks"));
                postMasterService.insert(dto);
                saved++;
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", saved + " row" + (saved > 1 ? "s" : "") + " saved successfully."
            ));
        } catch (Exception e) {
            log.error("[PostController] save error", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", cleanError(e)));
        }
    }

    // ── Clean DB error (same pattern as AccountMasterController) ──

    private String cleanError(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        String msg = cause.getMessage();
        if (msg == null) msg = e.getMessage();
        if (msg == null) return "An unexpected error occurred.";
        if (msg.startsWith("ERROR: ")) msg = msg.substring(7);
        int nl = msg.indexOf('\n');
        if (nl > 0) msg = msg.substring(0, nl).trim();
        return msg.trim();
    }
}