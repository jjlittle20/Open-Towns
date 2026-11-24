
package xaos.internal.api.ui;

import java.util.List;
import java.util.Objects;

import xaos.api.ui.UiActionRegistration;
import xaos.api.ui.UiApi;
import xaos.api.ui.UiMenuItemRegistration;
import xaos.api.ui.UiMessageType;
import xaos.api.ui.UiWindowHandle;

/**
 * Mod-scoped view of the public UI API.
 *
 * Prevents accidental registrations using
 * another mod's namespace.
 *
 * This is API ownership validation, not
 * a security sandbox for untrusted Java code.
 */
public final class ModOwnedUiApi implements UiApi {

    private final String modId;
    private final UiApi delegate;

    public ModOwnedUiApi(
            String modId,
            UiApi delegate) {

        if (modId == null
                || modId.isBlank()
                || modId.contains(":")) {

            throw new IllegalArgumentException(
                    "Invalid mod ID: " + modId);
        }

        this.modId = modId;
        this.delegate = Objects.requireNonNull(
                delegate, "delegate");
    }

    @Override
    public void showMessage(
            UiMessageType type,
            String message) {

        delegate.showMessage(type, message);
    }

    @Override
    public void showInfoDialog(
            String title,
            List<String> lines) {

        delegate.showInfoDialog(title, lines);
    }

    @Override
    public UiActionRegistration registerGameAction(
            String id,
            String label,
            Runnable action) {

        requireOwnedId(id, "Action");

        return delegate.registerGameAction(
                id, label, action);
    }

    @Override
    public UiWindowHandle createWindow(
            String id,
            String title) {

        requireOwnedId(id, "Window");

        return delegate.createWindow(id, title);
    }

    @Override
    public UiMenuItemRegistration addMenuAction(
            String menuId,
            String itemId,
            String title,
            String icon,
            String actionId) {

        // The target menu may belong to the game
        // or another mod. The contributed item
        // and callback must belong to this mod.
        requireOwnedId(itemId, "Menu item");
        requireOwnedId(actionId, "Action");

        return delegate.addMenuAction(
                menuId,
                itemId,
                title,
                icon,
                actionId);
    }

    private void requireOwnedId(
            String id,
            String description) {

        String prefix = modId + ":";

        if (id == null
                || !id.startsWith(prefix)
                || id.length() == prefix.length()
                || id.chars().anyMatch(
                        Character::isWhitespace)) {

            throw new IllegalArgumentException(
                    description + " ID must belong "
                            + "to mod '" + modId
                            + "' (expected "
                            + prefix + "...)"
                            + ", got: " + id);
        }
    }
}
