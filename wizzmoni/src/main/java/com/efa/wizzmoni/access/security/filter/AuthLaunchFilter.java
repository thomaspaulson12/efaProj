package com.efa.wizzmoni.access.security.filter;


import com.efa.wizzmoni.access.security.util.AuthEncryptionUtil;
import com.efa.wizzmoni.common.constants.CommonVariables;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AuthLaunchFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthLaunchFilter.class);

    private static final long MAX_AGE_MS = 5 * 60 * 1000;
    private static final String LAUNCH_URI = "/efa/home";

    private final String secret;
    private final AuthEncryptionUtil authEncryptionUtil;
    private final CommonVariables commonVariables;

    public AuthLaunchFilter(
            @Value("${efa.auth.secret}") String secret,
            AuthEncryptionUtil authEncryptionUtil,
            CommonVariables commonVariables) {

        this.secret = secret;
        this.authEncryptionUtil = authEncryptionUtil;
        this.commonVariables = commonVariables;

        log.info("====== AuthLaunchFilter initialized.");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        log.debug(">>>> AuthLaunchFilter: uri={}, method={}", uri, method);

        if (!uri.endsWith(LAUNCH_URI)) {
            chain.doFilter(request, response);
            return;
        }

        String token = request.getParameter("token");
        String branch = request.getParameter("branch");
        String userId = request.getParameter("user");
        String app = request.getParameter("app");
        String timestamp = request.getParameter("timestamp");

        if (token == null || token.isEmpty()) {
            rejectLaunch(response, "Missing token");
            return;
        }

        String decrypted;
        try {
            decrypted = authEncryptionUtil.decrypt(token, secret);
        } catch (Exception e) {
            rejectLaunch(response, "Invalid token");
            return;
        }

        String trustedBranch = extractParam(decrypted, "branch");
        String trustedUserId = extractParam(decrypted, "user");
        String trustedApp = extractParam(decrypted, "app");
        String trustedTimestamp = extractParam(decrypted, "timestamp");

        if (trustedUserId.isEmpty() || trustedBranch.isEmpty()
                || trustedApp.isEmpty() || trustedTimestamp.isEmpty()) {
            rejectLaunch(response, "Missing token");
            return;
        }

        try {
            long ts = Long.parseLong(trustedTimestamp);
            long age = System.currentTimeMillis() - ts;

            if (Math.abs(age) > MAX_AGE_MS) {
                rejectLaunch(response, "Token expired");
                return;
            }

        } catch (NumberFormatException e) {
            rejectLaunch(response, "Invalid timestamp");
            return;
        }

        if (!trustedUserId.equals(userId)
                || !trustedBranch.equals(branch)
                || !trustedApp.equals(app)) {

            rejectLaunch(response, "Parameter mismatch");
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractParam(String data, String key) {
        if (data == null || data.isEmpty()) return "";

        for (String pair : data.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return "";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        return uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/fonts/")
                || uri.startsWith("/images/")
                || uri.startsWith("/webjars/")
                || uri.equals("/favicon.ico")
                || uri.contains("/efa/session-expired");
    }

    private void rejectLaunch(HttpServletResponse response, String reason) throws IOException {

        String rawTarget = commonVariables.getPathHost();

        String encodedMsg = URLEncoder.encode(
                "Invalid request. Please Login Again",
                StandardCharsets.UTF_8
        );

        String encodedTarget = URLEncoder.encode(rawTarget, StandardCharsets.UTF_8);

        response.sendRedirect(
                "/efa/session-expired?msg=" + encodedMsg + "&target=" + encodedTarget
        );
    }
}