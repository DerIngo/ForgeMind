package de.deringo.forgemind.core.permission;

public final class DefaultPermissionPolicy
        implements PermissionPolicy {

    @Override
    public PermissionDecision evaluate(
            PermissionRequest request
    ) {
        return switch (request.capability()) {
            case READ, NETWORK -> PermissionDecision.ALLOW;

            case WRITE, EXECUTE, GIT -> PermissionDecision.ASK;
        };
    }
}