package de.deringo.forgemind.core.permission;

public interface PermissionHandler {

    PermissionGrant requestPermission(
            PermissionRequest request
    );
}