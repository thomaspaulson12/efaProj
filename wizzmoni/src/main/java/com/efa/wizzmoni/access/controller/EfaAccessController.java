package com.efa.wizzmoni.access.controller;

import com.efa.wizzmoni.access.data.EfaUserData;
import com.efa.wizzmoni.access.service.EfaAccessCheckService;
import com.efa.wizzmoni.access.service.EfaMenuRightsService;
import com.efa.wizzmoni.common.constants.CommonVariables;
import com.efa.wizzmoni.exception.EfaBusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Controller
@RequestMapping("/efa")
public class EfaAccessController {

    private static final Logger log = LoggerFactory.getLogger(EfaAccessController.class);

    private final EfaAccessCheckService accessCheckService;
    private final EfaMenuRightsService menuRightService;
    private final CommonVariables commonVariables;

    public EfaAccessController(EfaAccessCheckService accessCheckService,
                               EfaMenuRightsService menuRightService,
                               CommonVariables commonVariables) {

        this.accessCheckService = accessCheckService;
        this.menuRightService = menuRightService;
        this.commonVariables = commonVariables;
    }

    @PostMapping("/home")
    public String loginCheck(@RequestParam String branch,
                             @RequestParam String user,
                             @RequestParam String app,
                             @RequestParam String token,
                             HttpServletRequest request) {

        log.info(">>>> Entering /home loginCheck for branch={}, user={}, app={}", branch, user, app);

        EfaUserData data = accessCheckService.validateLogin(branch, user);

        if (data == null) {
            log.warn("==== Invalid login attempt for branch={}, user={}", branch, user);
            throw new EfaBusinessException("Invalid Login");
        }
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        return "redirect:/efa/dashboard";
    }

//        try {
//            rights = menuRightService.fetchMenuRights(branch, user, app);
//        } catch (Exception ex) {
//
//            log.error("==== Menu rights failed: {}", ex.getMessage());
//
//            String msg = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
//            String target = URLEncoder.encode(commonVariables.getPathHost(), StandardCharsets.UTF_8);
//
//            return "redirect:/efa/session-expired?msg=" + msg + "&target=" + target;
//        }

//        var authorities = rights.stream()
//                .map(SimpleGrantedAuthority::new)
//                .toList();
//
//        UsernamePasswordAuthenticationToken auth =
//                new UsernamePasswordAuthenticationToken(data, null, authorities);
//
//        SecurityContextHolder.getContext().setAuthentication(auth);
//
//        request.getSession(true).setAttribute(
//                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
//                SecurityContextHolder.getContext()
//        );
//
//        request.getSession(true).setAttribute("USER_DATA", data);
//
//        log.info("<<<< Authentication stored in SecurityContext");
//
//        return "redirect:/efa/dashboard";
//    }

    @GetMapping("/session-expired")
    public String expired(@RequestParam(required = false) String msg,
                          @RequestParam(required = false) String target,
                          Model model) {

        model.addAttribute("message", msg != null ? msg : "Session Expired");
        model.addAttribute("targetUrl", target != null ? target : "/");

        return "session-expired";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {

        EfaUserData userData = (EfaUserData) session.getAttribute("USER_DATA");
        return "index";
}
}
