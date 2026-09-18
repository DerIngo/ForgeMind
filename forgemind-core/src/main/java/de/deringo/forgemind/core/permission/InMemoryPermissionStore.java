package de.deringo.forgemind.core.permission;

import java.util.HashSet;
import java.util.Set;

public final class InMemoryPermissionStore
        implements PermissionStore {

    private final Set<PermissionKey> sessionGrants =
            new HashSet<>();

    @Override
    public boolean isAllowed(PermissionKey key) {
        return sessionGrants.contains(key);
    }

    @Override
    public void allow(
            PermissionKey key,
            PermissionScope scope
    ) {
        switch (scope) {
            case ONCE -> {
                // Intentionally not persisted.
            }

            case SESSION ->
                    sessionGrants.add(key);

            case PROJECT, GLOBAL ->
                    throw new UnsupportedOperationException(
                            "Persistent permission scope is not implemented yet: "
                                    + scope
                    );
        }
    }
}