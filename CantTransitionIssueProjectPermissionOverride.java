package com.atlassian.jira.dev.reference.plugin.security;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.security.plugin.ProjectPermissionOverride;
import com.atlassian.jira.user.ApplicationUser;

import javax.annotation.Nullable;

import static com.atlassian.jira.permission.ProjectPermissions.TRANSITION_ISSUES;

public class CantTransitionIssueProjectPermissionOverride implements ProjectPermissionOverride {
    @Override
    public Decision hasPermission(final ProjectPermissionKey projectPermissionKey, final Project project, @Nullable final ApplicationUser applicationUser) {
        if (applicationUser == null || projectPermissionKey == null) {
            return Decision.ABSTAIN;
        } else if (applicationUser.getName().equals("brad_the_odlaw") && projectPermissionKey.equals(TRANSITION_ISSUES)) {
            return Decision.DENY;
        } else {
            return Decision.ABSTAIN;
        }
    }

    @Override
    public Reason getReason(final ProjectPermissionKey projectPermissionKey, final Project project, final ApplicationUser applicationUser) {
        return hasPermission(projectPermissionKey, project, applicationUser) == Decision.ABSTAIN ?
                new Reason("reference-plugin", "brad.does.have.permissions") :
                new Reason("reference-plugin", "brad.doesnt.have.permissions");
    }
}
