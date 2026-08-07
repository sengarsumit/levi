package com.koda.levi.config;

import com.koda.levi.entity.OrgUserStatus;
import com.koda.levi.entity.Users;
import com.koda.levi.repository.OrganizationUserRepository;
import com.koda.levi.repository.ProjectRepository;
import com.koda.levi.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The SockJS/STOMP HTTP handshake already requires a valid JWT (Spring Security's
 * "anyRequest().authenticated()"), but nothing previously authenticated individual STOMP
 * frames on the long-lived session afterward, and nothing scoped SUBSCRIBE destinations to
 * the subscriber's organization. Any connected client could subscribe to any org's
 * "/topic/projects/{id}/issues" and see that org's live issue updates. This authenticates
 * the STOMP session at CONNECT (the frontend already sends a Bearer token there) and
 * authorizes each project-issues subscription against real org membership at SUBSCRIBE.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private static final Pattern PROJECT_TOPIC = Pattern.compile("^/topic/projects/(\\d+)/issues$");

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ProjectRepository projectRepository;
    private final OrganizationUserRepository organizationUserRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) && !authorizeSubscription(accessor)) {
            logger.warn("Rejected WebSocket subscription to {} - caller is not an approved member of the owning organization",
                    accessor.getDestination());
            return null;
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (jwtService.isValid(token, userDetails)) {
                accessor.setUser(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
            }
        } catch (Exception e) {
            logger.warn("WebSocket CONNECT authentication failed: {}", e.getMessage());
        }
    }

    private boolean authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return true;
        }

        Matcher matcher = PROJECT_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            // Not a project-issues topic this interceptor knows how to scope.
            return true;
        }

        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth)
                || !(auth.getPrincipal() instanceof Users principal)) {
            return false;
        }

        Long projectId = Long.valueOf(matcher.group(1));
        return projectRepository.findById(projectId)
                .map(project -> organizationUserRepository
                        .findByUserIdAndOrganizationId(principal.getId(), project.getOrganization().getId())
                        .map(orgUser -> orgUser.getStatus() == OrgUserStatus.APPROVED)
                        .orElse(false))
                .orElse(false);
    }
}
