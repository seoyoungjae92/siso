package com.siso.backend.admin;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class AdminLoginEventListener {

    private final AdminLoginAttemptGuard guard;

    public AdminLoginEventListener(AdminLoginAttemptGuard guard) {
        this.guard = guard;
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String ip = remoteAddress(event);
        if (ip != null) {
            guard.recordFailure(ip);
        }
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String ip = remoteAddress(event);
        if (ip != null) {
            guard.recordSuccess(ip);
        }
    }

    private String remoteAddress(AbstractAuthenticationEvent event) {
        Object details = event.getAuthentication().getDetails();
        if (details instanceof WebAuthenticationDetails webDetails) {
            return webDetails.getRemoteAddress();
        }
        return null;
    }
}
