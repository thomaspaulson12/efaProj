package com.efa.wizzmoni.masters.controller;

import com.efa.wizzmoni.masters.AccountMasterDto;
import com.efa.wizzmoni.masters.service.AccountMasterService;
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
public class AccountMasterController {

    private static final Logger log = LoggerFactory.getLogger(AccountMasterController.class);

    @Autowired
    private AccountMasterService accountMasterService;

    // ── Page ──────────────────────────────────────────────────────

    @GetMapping("/account")
    public String accountMasterPage(Model model) {
        model.addAttribute("companyCode", accountMasterService.getDefaultCompany());
        model.addAttribute("finyr", (int) accountMasterService.getDefaultFinyr());
        return "masters/account";
    }


    // ── REST: Tree ────────────────────────────────────────────────

    @GetMapping("/api/account/tree")
    @ResponseBody
    public ResponseEntity<?> getTree(
            @RequestParam(defaultValue = "XM") String companyCode,
            @RequestParam(defaultValue = "16") double finyr) {
        try {
            AccountMasterDto tree = accountMasterService.getAccountTree(companyCode, finyr);
            if (tree == null)
                return ResponseEntity.ok(Map.of("error",
                        "No data found for company=" + companyCode + " finyr=" + finyr));
            return ResponseEntity.ok(tree);
        } catch (Exception e) {
            log.error("[Controller] getTree error", e);
            return ResponseEntity.status(500).body(Map.of("error", cleanDbError(e)));
        }
    }

    // ── REST: Detail ──────────────────────────────────────────────

    @GetMapping("/api/account/detail")
    @ResponseBody
    public ResponseEntity<?> getDetail(
            @RequestParam(defaultValue = "XM") String companyCode,
            @RequestParam(defaultValue = "16") double finyr,
            @RequestParam String accountCode) {
        try {
            AccountMasterDto dto = accountMasterService.getAccountDetail(companyCode, finyr, accountCode);
            if (dto == null)
                return ResponseEntity.status(404).body(Map.of("error", "Account not found: " + accountCode));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("[Controller] getDetail error for code='{}'", accountCode, e);
            return ResponseEntity.status(500).body(Map.of("error", cleanDbError(e)));
        }
    }

    // ── REST: Update Account Name ─────────────────────────────────
    //
    //  PUT /masters/api/account/update
    //  Body: { "companyCode":"XM", "finYear":16, "accountCode":"SOME-001", "accountName":"New Name" }
    //
    //  Calls efa.fn_efa_accountmaster_update(companyCode, accountCode, newName)
    //  Only level 3 and level 4 accounts allowed (enforced in service)

    @PutMapping("/api/account/update")
    @ResponseBody
    public ResponseEntity<?> updateAccount(@RequestBody AccountMasterDto dto) {
        try {
            log.info("[Controller] updateAccount: code='{}' newName='{}'",
                    dto.getAccountCode(), dto.getAccountName());

            accountMasterService.updateAccountName(
                    dto.getCompanyCode() != null ? dto.getCompanyCode() : "XM",
                    dto.getFinYear() > 0 ? dto.getFinYear() : 16,
                    dto.getAccountCode(),
                    dto.getAccountName()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account name updated successfully."
            ));
        } catch (IllegalArgumentException e) {
            // Business rule violation (blank name, wrong level)
            log.warn("[Controller] updateAccount rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", cleanDbError(e)));
        } catch (Exception e) {
            log.error("[Controller] updateAccount error", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", cleanDbError(e)));
        }
    }

    // ── REST: Save (Insert) ───────────────────────────────────────

    @PostMapping("/api/account/save")
    @ResponseBody
    public ResponseEntity<?> saveAccount(@RequestBody AccountMasterDto dto) {
        try {
            accountMasterService.saveAccount(dto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Account saved."));
        } catch (Exception e) {
            log.error("[Controller] saveAccount error", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", cleanDbError(e)));
        }
    }

    // ── REST: Delete ──────────────────────────────────────────────

    // ── REST: Delete ──────────────────────────────────────────────
    //
    //  No finyr param needed — service loads the account record
    //  and passes both ac_finyr and log_finyr to the DB function,
    //  mirroring the PB delete script exactly.

    @DeleteMapping("/api/account/delete")
    @ResponseBody
    public ResponseEntity<?> deleteAccount(
            @RequestParam(defaultValue = "XM") String companyCode,
            @RequestParam String accountCode) {
        try {
            accountMasterService.deleteAccount(companyCode, accountCode);
            return ResponseEntity.ok(Map.of("success", true, "message", "Account deleted."));
        } catch (IllegalArgumentException e) {
            log.warn("[Controller] deleteAccount rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", cleanDbError(e)));
        } catch (Exception e) {
            log.error("[Controller] deleteAccount error", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", cleanDbError(e)));
        }
    }

    // ── DEBUG ─────────────────────────────────────────────────────

    @GetMapping("/api/account/debug/ping")
    @ResponseBody
    public ResponseEntity<?> debugPing() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "company", accountMasterService.getDefaultCompany(),
                "finyr", accountMasterService.getDefaultFinyr()
        ));
    }

    @GetMapping("/api/account/debug/flat")
    @ResponseBody
    public ResponseEntity<?> debugFlat(
            @RequestParam(defaultValue = "XM") String companyCode,
            @RequestParam(defaultValue = "16") double finyr) {
        try {
            List<AccountMasterDto> list = accountMasterService.debugGetFlatList(companyCode, finyr);
            List<Map<String, Object>> rows = list.stream()
                    .map(d -> Map.<String, Object>of(
                            "code",   d.getAccountCode()  != null ? d.getAccountCode()  : "NULL",
                            "name",   d.getAccountName()  != null ? d.getAccountName()  : "NULL",
                            "level",  d.getLevel(),
                            "parent", d.getParent()       != null ? d.getParent()       : "NULL",
                            "type",   d.getAccountType()  != null ? d.getAccountType()  : "NULL"
                    ))
                    .toList();
            return ResponseEntity.ok(Map.of("totalRows", list.size(), "rows", rows));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", cleanDbError(e)));
        }
    }
    // ── REST: Get branches for account ────────────────────────────
    //
    //  GET /masters/api/account/branches?companyCode=XM&accountCode=XXX
    //  Returns all branches with applicable=Y/N for the given account.
    //  Frontend uses this to populate the Apply to Branches popup.

    @GetMapping("/api/account/branches")
    @ResponseBody
    public ResponseEntity<?> getBranches(
            @RequestParam(defaultValue = "XM") String companyCode,
            @RequestParam String accountCode) {
        try {
            var branches = accountMasterService.getBranchesForAccount(companyCode, accountCode);
            return ResponseEntity.ok(Map.of("branches", branches));
        } catch (Exception e) {
            log.error("[Controller] getBranches error", e);
            return ResponseEntity.status(500).body(Map.of("error", cleanDbError(e)));
        }
    }

    // ── REST: Apply branches to account ───────────────────────────
    //
    //  POST /masters/api/account/apply-branches
    //  Body: { "companyCode":"XM", "accountCode":"XXX", "branches":["B001","B002"] }
    //
    //  Calls fn_efa_branchac_insert for each branch in the list.
    //  Only newly-selected (unchecked → checked) branches should be in the list.
    //  Already-applied branches are disabled in the UI and never submitted.

    @PostMapping("/api/account/apply-branches")
    @ResponseBody
    public ResponseEntity<?> applyBranches(@RequestBody Map<String, Object> body) {
        try {
            String companyCode  = (String) body.getOrDefault("companyCode", "XM");
            String accountCode  = (String) body.get("accountCode");
            @SuppressWarnings("unchecked")
            List<String> branches = (List<String>) body.get("branches");

            if (accountCode == null || accountCode.isBlank())
                return ResponseEntity.badRequest().body(Map.of("success", false,
                        "error", "Account code is required."));

            accountMasterService.applyBranches(companyCode, accountCode, branches);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Branches applied successfully."
            ));
        } catch (IllegalArgumentException e) {
            log.warn("[Controller] applyBranches rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "error", cleanDbError(e)));
        } catch (Exception e) {
            log.error("[Controller] applyBranches error", e);
            return ResponseEntity.status(500).body(Map.of("success", false,
                    "error", cleanDbError(e)));
        }
    }

    // ── REST: Get Opening Balance ─────────────────────────────────
    //  GET /masters/api/account/opening-balance?companyCode=XM&accountCode=XXX
    //  Returns all branch rows with dr_open/cr_open already split.

    @GetMapping("/api/account/opening-balance")
    @ResponseBody
    public ResponseEntity<?> getOpeningBalance(
            @RequestParam(defaultValue = "XM") String companyCode,
            @RequestParam String accountCode) {
        try {
            var rows = accountMasterService.getOpeningBalance(companyCode, accountCode);
            return ResponseEntity.ok(Map.of("rows", rows));
        } catch (Exception e) {
            log.error("[Controller] getOpeningBalance error", e);
            return ResponseEntity.status(500).body(Map.of("error", cleanDbError(e)));
        }
    }

    // ── REST: Save Opening Balance ────────────────────────────────
    //  POST /masters/api/account/opening-balance
    //  Body: {
    //    "companyCode": "XM",
    //    "accountCode": "XXXX",
    //    "rows": [
    //      { "branch":"B001", "drOpen":"1000.00", "crOpen":"0.00",
    //        "orgBal":"0.00", "dDate":"2024-04-01 00:00:00" },
    //      ...
    //    ]
    //  }
    //  Calls fn_efa_opbal_update per changed row only.

    @PostMapping("/api/account/opening-balance")
    @ResponseBody
    public ResponseEntity<?> saveOpeningBalance(@RequestBody Map<String, Object> body) {
        try {
            String companyCode = (String) body.getOrDefault("companyCode", "XM");
            String accountCode = (String) body.get("accountCode");

            if (accountCode == null || accountCode.isBlank())
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "error", "Account code is required."));

            @SuppressWarnings("unchecked")
            var rows = (java.util.List<java.util.Map<String, Object>>) body.get("rows");

            accountMasterService.saveOpeningBalance(companyCode, accountCode, rows);

            return ResponseEntity.ok(Map.of("success", true,
                    "message", "Opening balance saved successfully."));
        } catch (IllegalArgumentException e) {
            log.warn("[Controller] saveOpeningBalance rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "error", cleanDbError(e)));
        } catch (Exception e) {
            log.error("[Controller] saveOpeningBalance error", e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "error", cleanDbError(e)));
        }
    }

    // ── Extract clean DB error message ───────────────────────────
    //
    //  PostgreSQL RAISE EXCEPTION messages arrive wrapped in Spring's
    //  DataAccessException chain. We walk the cause chain to find the
    //  innermost message, which is exactly what the DB fn() raised.
    //
    //  e.g. full message: "ERROR: Accounts Found Under the Group, Aborting[...]"
    //       clean output: "Accounts Found Under the Group, Aborting[...]"
    //
    //  This is applied to ALL error responses so only the DB message
    //  reaches the frontend — no SQL, no Java stack details.

    private String cleanDbError(Exception e) {
        // Walk to the deepest cause
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        if (msg == null) msg = e.getMessage();
        if (msg == null) return "An unexpected error occurred.";

        // Strip leading "ERROR: " prefix that PostgreSQL prepends
        if (msg.startsWith("ERROR: ")) msg = msg.substring(7);

        // Take only the first line (removes "Where: PL/pgSQL function..." lines)
        int nl = msg.indexOf('\n');
        if (nl > 0) msg = msg.substring(0, nl).trim();

        return msg.trim();
    }

}