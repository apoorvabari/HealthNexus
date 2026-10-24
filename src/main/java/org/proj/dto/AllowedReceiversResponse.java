package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowedReceiversResponse {
    /** The normalised role name of the currently-authenticated user (e.g. "DOCTOR"). */
    private String currentRole;

    /** Ordered list of roles the current user is permitted to message. */
    private List<String> allowedRoles;

    /**
     * Contacts grouped by receiver role.
     * Key = role name (e.g. "PATIENT"), Value = list of contacts for that role.
     */
    private Map<String, List<ChatContactDto>> receivers;
}
