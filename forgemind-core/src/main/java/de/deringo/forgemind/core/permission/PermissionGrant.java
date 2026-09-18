package de.deringo.forgemind.core.permission;

public record PermissionGrant(
        boolean allowed,
        PermissionScope scope
) {

    public static PermissionGrant deny() {
        return new PermissionGrant(
                false,
                PermissionScope.ONCE
        );
    }

    public static PermissionGrant allow(
            PermissionScope scope
    ) {
        return new PermissionGrant(
                true,
                scope
        );
    }
}