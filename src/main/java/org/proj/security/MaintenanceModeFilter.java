package org.proj.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.proj.entity.AdminSystemSettingsEntity;
import org.proj.repository.AdminSystemSettingsRepo;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final AdminSystemSettingsRepo settingsRepository;
    private final ObjectMapper objectMapper;

    public MaintenanceModeFilter(
            AdminSystemSettingsRepo settingsRepository,
            ObjectMapper objectMapper) {

        this.settingsRepository = settingsRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAdminEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAuthenticationEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!isMaintenanceEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAdminUser()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\": \"Service Unavailable\", \"message\": \"The system is currently undergoing maintenance.\"}");
    }

    private boolean isAdminEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/admin");
    }

    private boolean isAuthenticationEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/users/login");
    }

    private boolean isMaintenanceEnabled() {
        Optional<AdminSystemSettingsEntity> settingOpt = settingsRepository.findBySettingKey("GENERAL_SETTINGS");
        if (settingOpt.isPresent()) {
            try {
                JsonNode json = objectMapper.readTree(settingOpt.get().getSettingValue());
                if (json.has("maintenanceMode")) {
                    return json.get("maintenanceMode").asBoolean();
                }
            } catch (Exception e) {
                
            }
        }
        return false;
    }

    private boolean isAdminUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
}
