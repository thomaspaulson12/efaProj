package com.efa.wizzmoni.access.service;

import com.efa.wizzmoni.access.data.EfaUserData;
import com.efa.wizzmoni.access.repository.EfaAccessCheckRepo;
import com.efa.wizzmoni.exception.EfaBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EfaAccessCheckService {

    private final EfaAccessCheckRepo repo;
    private static final Logger log = LoggerFactory.getLogger(EfaAccessCheckService.class);

    public EfaAccessCheckService(EfaAccessCheckRepo repo) {
        this.repo = repo;
    }

    public EfaUserData validateLogin(String branch, String user) {
        log.debug(">>>> Entering validateLogin for branch={} user={}", branch, user);

        try {
            EfaUserData data = repo.getUserDetails(branch, user);

            if (data == null) {
                log.warn("==== Invalid login attempt for branch={} user={}", branch, user);
                throw new EfaBusinessException("Invalid Login");
            }

            log.debug("==== Login successful for user={}", user);
            return data;

        } catch (EfaBusinessException ex) {
            // Already a business exception, log and rethrow
            log.warn("==== Business exception during login: {}", ex.getMessage());
            throw ex;

        } catch (Exception ex) {
            // Unexpected technical exception
            log.error("==== Unexpected error validating login for branch={} user={}, reason={}", branch, user, ex.getMessage(), ex);
            throw new EfaBusinessException("Error validating login, please contact admin");

        } finally {
            log.debug("<<<< Exiting validateLogin");
        }
    }
}