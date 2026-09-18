package de.deringo.forgemind.core.permission;

public interface PermissionPolicy {

    PermissionDecision evaluate(PermissionRequest request);
}