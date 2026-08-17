package com.koda.levi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinOrganizationRequest {
    // Optional. When present, must belong to the org being joined; validated in
    // OrganizationService.joinOrganization(). A signal for the approving admin, never a
    // grant by itself — approveMember() always overwrites the final role.
    private Long preferredRoleId;
}
