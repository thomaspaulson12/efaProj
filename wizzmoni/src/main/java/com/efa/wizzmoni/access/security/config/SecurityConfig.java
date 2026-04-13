package com.efa.wizzmoni.access.security.config;

//import com.efa.wizzmoni.access.security.filter.ApiRateLimitFilter;
import com.efa.wizzmoni.access.security.filter.AuthLaunchFilter;
//import com.efa.wizzmoni.access.security.filter.SessionValidationFilter;
import com.efa.wizzmoni.common.constants.CommonVariables;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.config.Customizer;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CommonVariables commonVariables;
//    private final ApiRateLimitFilter apiRateLimitFilter;
//    private final SessionValidationFilter sessionValidationFilter;
    private final AuthLaunchFilter authLaunchFilter;

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    public SecurityConfig(CommonVariables commonVariables,
//                          ApiRateLimitFilter apiRateLimitFilter,
//                          SessionValidationFilter sessionValidationFilter,
                          AuthLaunchFilter authLaunchFilter) {

        this.commonVariables = commonVariables;
//        this.apiRateLimitFilter = apiRateLimitFilter;
//        this.sessionValidationFilter = sessionValidationFilter;
        this.authLaunchFilter = authLaunchFilter;

        log.info("====== SecurityConfig initialized. PathHost={}", commonVariables.getPathHost());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        String sessionExpiredRedirect =
                "/efa/session-expired?msg=Session%20Expired&target=" +
                        URLEncoder.encode(commonVariables.getPathHost(), StandardCharsets.UTF_8);

        http
                // ─── HEADERS ───────────────────────────────────────────
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .cacheControl(Customizer.withDefaults())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data:; " +
                                        "font-src 'self'; " +
                                        "connect-src 'self'; " +
                                        "frame-ancestors 'none'; " +
                                        "object-src 'none';"
                        ))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=()"
                        ))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Referrer-Policy",
                                "strict-origin-when-cross-origin"
                        ))
                )

                // ─── CSRF ─────────────────────────────────────────────
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/efa/home")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )

                // ─── FILTERS ──────────────────────────────────────────
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

                .addFilterBefore(authLaunchFilter, UsernamePasswordAuthenticationFilter.class)
//                .addFilterBefore(apiRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
//                .addFilterBefore(sessionValidationFilter, UsernamePasswordAuthenticationFilter.class)

                // ─── AUTHORIZATION ───────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/css/**", "/fonts/**", "/images/**",
                                "/js/**", "/webjars/**", "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/efa/home").permitAll()
                        .requestMatchers("/efa/session-expired").permitAll()
                        .requestMatchers("/error").permitAll()
                        //.requestMatchers("/efa/**").permitAll()
                        .requestMatchers("/efa/**").authenticated()
                )

                // ─── SESSION ─────────────────────────────────────────
                .sessionManagement(session -> session
                        .invalidSessionUrl(sessionExpiredRedirect)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(true)
                )

                // ─── DISABLE DEFAULT LOGIN ───────────────────────────
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())

                // ─── EXCEPTION HANDLING ─────────────────────────────
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String target = URLEncoder.encode(
                                    commonVariables.getPathHost(), StandardCharsets.UTF_8
                            );
                            response.sendRedirect(
                                    "/efa/session-expired?msg=Session%20Expired&target=" + target
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {

                            HttpSession session = request.getSession(false);

                            if (session != null) {
                                String reason = accessDeniedException.getMessage();

                                if (reason != null && reason.contains("CSRF")) {
                                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                    response.setContentType("application/json");
                                    response.getWriter().write(
                                            "{\"error\": \"Invalid request. Please refresh the page.\"}"
                                    );
                                    return;
                                }

                                session.setAttribute("ACCESS_DENIED_MESSAGE",
                                        "You do not have permission to access this menu.");
                            }

                            response.sendRedirect("/efa/dashboard");
                        })
                );

        return http.build();
    }

    // ─── CSRF COOKIE FILTER ───────────────────────────────
    static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            CsrfToken csrfToken =
                    (CsrfToken) request.getAttribute(CsrfToken.class.getName());

            if (csrfToken != null) {
                csrfToken.getToken();
            }

            filterChain.doFilter(request, response);
        }
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return username -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
                    "No default user"
            );
        };
    }
}