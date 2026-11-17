
package xaos.ui.components;

import java.util.Objects;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiSizePolicy;

/**
 * A reusable window with a title bar and content area.
 *
 * This class defines the structure and layout of a window.
 * Rendering, dragging, closing and input handling will be
 * implemented separately.
 */
public class UiWindow extends UiContainer {

    private String title;

    private final UiElement titleBar;
    private final UiContainer content;

    // ------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------

    public UiWindow(String title) {
        this.title = Objects.requireNonNull(title, "title");

        setLayoutStrategy(UiLinearLayout.VERTICAL);

        setPadding(UiInsets.all(8));
        setSpacing(4);

        // Default minimum window dimensions.
        setMinimumSize(new UiSize(180, 80));

        // Title bar.
        titleBar = new UiElement();

        titleBar.setPreferredSize(new UiSize(0, 24));
        titleBar.setMinimumSize(new UiSize(0, 24));

        titleBar.setHorizontalSizePolicy(
                UiSizePolicy.FILL);

        // Content area.
        content = new UiContainer();

        content.setLayoutStrategy(UiLinearLayout.VERTICAL);

        content.setHorizontalSizePolicy(
                UiSizePolicy.FILL);

        content.setVerticalSizePolicy(
                UiSizePolicy.FILL);

        // Add the structural elements to the window.
        add(titleBar);
        add(content);
    }

    // ------------------------------------------------------------
    // Title
    // ------------------------------------------------------------

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        String newTitle = Objects.requireNonNull(
                title, "title");

        if (!this.title.equals(newTitle)) {
            this.title = newTitle;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Window components
    // ------------------------------------------------------------

    /**
     * Returns the title bar element.
     *
     * This will eventually serve as the region used
     * for dragging and rendering the window title.
     */
    public UiElement titleBar() {
        return titleBar;
    }

    /**
     * Returns the window's content container.
     *
     * UI controls should be added to this container,
     * not directly to the window.
     */
    public UiContainer content() {
        return content;
    }
}
