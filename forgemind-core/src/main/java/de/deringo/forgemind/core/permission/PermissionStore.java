package de.deringo.forgemind.core.permission;

public interface PermissionStore {

    boolean isAllowed(PermissionKey key);

    void allow(
            PermissionKey key,
            PermissionScope scope
    );
}