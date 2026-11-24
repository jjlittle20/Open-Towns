
package xaos.ui.components;

import java.util.Objects;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiFocusable;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiSprite;
import xaos.ui.rendering.UiTextMetrics;

/**
 * Reusable button supporting text, sprites or both.
 *
 * Rendering and input handling are managed separately.
 */
public class UiButton extends UiElement implements UiFocusable {

    private String text;
    private UiSprite icon;
    private UiSize iconSize;
    private final UiTextMetrics textMetrics;

    private UiInsets padding;
    private int contentSpacing = 6;

    private boolean enabled = true;
    private boolean hovered;
    private boolean pressed;
    private boolean selected;
    private boolean backgroundVisible;
    private String tooltip;
    private UiTextMetrics tooltipMetrics;
    private UiButtonAppearance appearance = UiButtonAppearance.STANDARD;
    private Runnable onClick = () -> {
    };

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------

    public UiButton(String text, UiTextMetrics metrics) {
        this(text, null,
                Objects.requireNonNull(metrics, "metrics"));
    }

    public UiButton(UiSprite icon) {
        this("", Objects.requireNonNull(icon, "icon"), null);
    }

    public UiButton(
            String text,
            UiSprite icon,
            UiTextMetrics metrics) {

        this.text = Objects.requireNonNull(text, "text");
        this.icon = icon;
        this.textMetrics = metrics;
        this.tooltipMetrics = metrics;

        if (!text.isEmpty() && metrics == null) {
            throw new IllegalArgumentException(
                    "Text buttons require font metrics");
        }

        iconSize = icon == null
                ? UiSize.ZERO
                : icon.source().size();

        boolean iconOnly = icon != null && text.isEmpty();

        padding = iconOnly
                ? UiInsets.all(4)
                : new UiInsets(6, 12, 6, 12);

        backgroundVisible = !iconOnly;

        if (!iconOnly) {
            setMinimumSize(new UiSize(64, 28));
        }
    }

    // ------------------------------------------------------------
    // Text and icon
    // ------------------------------------------------------------

    public String text() {
        return text;
    }

    /**
     * Returns the actual size of the button's text.
     *
     * An empty string requires no text space.
     */
    public UiSize measuredTextSize() {
        if (text.isEmpty()) {
            return UiSize.ZERO;
        }

        return Objects.requireNonNull(
                textMetrics.measure(text),
                "textMetrics result");
    }

    public void setText(String text) {
        String newText = Objects.requireNonNull(text, "text");

        if (!newText.isEmpty() && textMetrics == null) {
            throw new IllegalStateException(
                    "This button has no text metrics");
        }

        if (!this.text.equals(newText)) {
            this.text = newText;
            invalidateLayout();
        }
    }

    public UiSprite icon() {
        return icon;
    }

    public void setIcon(UiSprite icon) {
        if (!Objects.equals(this.icon, icon)) {
            this.icon = icon;
            this.iconSize = icon == null
                    ? UiSize.ZERO
                    : icon.source().size();

            invalidateLayout();
        }
    }

    public UiSize iconSize() {
        return iconSize;
    }

    public void setIconSize(UiSize iconSize) {
        UiSize newSize = Objects.requireNonNull(
                iconSize, "iconSize");

        if (icon == null) {
            throw new IllegalStateException(
                    "Cannot size a button without an icon");
        }

        if (!this.iconSize.equals(newSize)) {
            this.iconSize = newSize;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Layout properties
    // ------------------------------------------------------------

    public UiInsets padding() {
        return padding;
    }

    public void setPadding(UiInsets padding) {
        UiInsets newPadding = Objects.requireNonNull(
                padding, "padding");

        if (!this.padding.equals(newPadding)) {
            this.padding = newPadding;
            invalidateLayout();
        }
    }

    public int contentSpacing() {
        return contentSpacing;
    }

    public void setContentSpacing(int spacing) {
        if (spacing < 0) {
            throw new IllegalArgumentException(
                    "Content spacing cannot be negative");
        }

        if (contentSpacing != spacing) {
            contentSpacing = spacing;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Keyboard focus
    // ------------------------------------------------------------

    @Override
    public boolean canReceiveFocus() {
        return enabled;
    }

    // ------------------------------------------------------------
    // Visual state
    // ------------------------------------------------------------

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (!enabled) {
            hovered = false;
            pressed = false;
        }
    }

    public boolean hovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = enabled && hovered;
    }

    public boolean pressed() {
        return pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = enabled && pressed;
    }

    public boolean selected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean backgroundVisible() {
        return backgroundVisible;
    }

    public void setBackgroundVisible(boolean visible) {
        backgroundVisible = visible;
    }

    /**
     * Semantic appearance requested by this button.
     *
     * The theme controls the actual artwork.
     */
    public UiButtonAppearance appearance() {
        return appearance;
    }

    public void setAppearance(
            UiButtonAppearance appearance) {

        UiButtonAppearance next = Objects.requireNonNull(
                appearance, "appearance");

        if (this.appearance != next) {
            this.appearance = next;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------

    public void setOnClick(Runnable onClick) {
        this.onClick = Objects.requireNonNull(
                onClick, "onClick");
    }

    public void activate() {
        if (enabled && visible()) {
            onClick.run();
        }
    }

    // ------------------------------------------------------------
    // Measurement
    // ------------------------------------------------------------

    @Override
    public UiSize measure(UiSize available) {
        Objects.requireNonNull(available, "available");

        if (!visible()) {
            return UiSize.ZERO;
        }

        UiSize textSize = text.isEmpty()
                ? UiSize.ZERO
                : Objects.requireNonNull(
                        textMetrics.measure(text),
                        "textMetrics result");

        int iconWidth = icon == null ? 0 : iconSize.width();
        int iconHeight = icon == null ? 0 : iconSize.height();

        boolean hasText = !text.isEmpty();
        boolean hasIcon = icon != null;

        int spacing = hasText && hasIcon
                ? contentSpacing
                : 0;

        int contentWidth = Math.addExact(
                Math.addExact(textSize.width(), iconWidth),
                spacing);

        int contentHeight = Math.max(
                textSize.height(), iconHeight);

        int desiredWidth = Math.max(
                preferredSize().width(),
                Math.addExact(
                        contentWidth, padding.horizontal()));

        int desiredHeight = Math.max(
                preferredSize().height(),
                Math.addExact(
                        contentHeight, padding.vertical()));

        return new UiSize(
                Math.max(
                        minimumSize().width(),
                        Math.min(desiredWidth, available.width())),
                Math.max(
                        minimumSize().height(),
                        Math.min(desiredHeight, available.height())));
    }

    /**
     * Optional tooltip displayed when the pointer
     * rests over this button.
     *
     * Newlines separate tooltip lines.
     */
    public String tooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    /**
     * Allows icon-only buttons to supply their
     * own font metrics for tooltip rendering.
     */
    public void setTooltip(
            String tooltip,
            UiTextMetrics metrics) {

        this.tooltip = tooltip;
        this.tooltipMetrics = Objects.requireNonNull(metrics, "metrics");
    }

    public UiTextMetrics tooltipMetrics() {
        return tooltipMetrics;
    }

}
