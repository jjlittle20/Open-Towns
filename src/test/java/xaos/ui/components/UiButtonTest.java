
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;
import xaos.ui.rendering.UiSprite;

class UiButtonTest {

    private static final UiTextMetrics TEST_FONT = text -> new UiSize(text.length() * 7, 16);

    @Test
    void buttonMeasuresTextAndPadding() {
        UiButton button = new UiButton(
                "Inventory", TEST_FONT);

        // 9 characters * 7 = 63
        // Horizontal padding = 24
        // Vertical padding = 12
        assertEquals(
                new UiSize(87, 28),
                button.measure(new UiSize(500, 500)));
    }

    @Test
    void buttonRespectsMinimumSize() {
        UiButton button = new UiButton(
                "OK", TEST_FONT);

        assertEquals(
                new UiSize(64, 28),
                button.measure(new UiSize(500, 500)));
    }

    @Test
    void changingTextInvalidatesLayout() {
        UiContainer container = new UiContainer();
        UiButton button = new UiButton("OK", TEST_FONT);

        container.add(button);

        UiRoot root = new UiRoot(container);
        root.updateLayout(new UiSize(400, 200));

        assertFalse(container.isLayoutDirty());

        button.setText("Open Inventory");

        assertTrue(button.isLayoutDirty());
        assertTrue(container.isLayoutDirty());

        assertTrue(root.updateLayout(new UiSize(400, 200)));

        assertEquals(
                new UiRect(0, 0, 122, 28),
                button.bounds());
    }

    @Test
    void changingPaddingInvalidatesLayout() {
        UiButton button = new UiButton("OK", TEST_FONT);

        button.layout(new UiRect(0, 0, 64, 28));

        button.setPadding(UiInsets.all(20));

        assertTrue(button.isLayoutDirty());

        assertEquals(
                new UiSize(64, 56),
                button.measure(new UiSize(500, 500)));
    }

    @Test
    void disabledButtonsCannotActivate() {
        UiButton button = new UiButton("Click", TEST_FONT);

        AtomicInteger clicks = new AtomicInteger();

        button.setOnClick(clicks::incrementAndGet);

        button.activate();
        assertEquals(1, clicks.get());

        button.setEnabled(false);
        button.activate();

        assertEquals(1, clicks.get());

        button.setEnabled(true);
        button.activate();

        assertEquals(2, clicks.get());
    }

    @Test
    void disablingButtonClearsInteractionState() {
        UiButton button = new UiButton("Click", TEST_FONT);

        button.setHovered(true);
        button.setPressed(true);

        assertTrue(button.hovered());
        assertTrue(button.pressed());

        button.setEnabled(false);

        assertFalse(button.hovered());
        assertFalse(button.pressed());

        button.setHovered(true);
        button.setPressed(true);

        assertFalse(button.hovered());
        assertFalse(button.pressed());
    }

    @Test
    void interactionStateDoesNotInvalidateLayout() {
        UiButton button = new UiButton("Click", TEST_FONT);

        button.layout(new UiRect(0, 0, 100, 30));

        button.setHovered(true);
        button.setPressed(true);

        assertFalse(button.isLayoutDirty());
    }

    @Test
    void iconOnlyButtonUsesSpriteDimensions() {
        UiSprite icon = new UiSprite(
                "classic-ui",
                new UiRect(0, 0, 32, 32));

        UiButton button = new UiButton(icon);

        assertEquals(
                new UiSize(40, 40),
                button.measure(new UiSize(500, 500)));

        assertEquals("", button.text());
        assertSame(icon, button.icon());
        assertFalse(button.backgroundVisible());
    }

    @Test
    void iconOnlyButtonCanUseExactSpriteSize() {
        UiSprite icon = new UiSprite(
                "classic-ui",
                new UiRect(0, 0, 32, 32));

        UiButton button = new UiButton(icon);
        button.setPadding(UiInsets.NONE);

        assertEquals(
                new UiSize(32, 32),
                button.measure(new UiSize(500, 500)));
    }

    @Test
    void mixedButtonMeasuresTextAndIcon() {
        UiSprite icon = new UiSprite(
                "classic-ui",
                new UiRect(0, 0, 16, 16));

        UiButton button = new UiButton(
                "Build", icon, TEST_FONT);

        // Text 35 + icon 16 + spacing 6 + padding 24
        assertEquals(
                new UiSize(81, 28),
                button.measure(new UiSize(500, 500)));

        assertTrue(button.backgroundVisible());
    }

    @Test
    void changingIconInvalidatesLayout() {
        UiSprite firstIcon = new UiSprite(
                "classic-ui",
                new UiRect(0, 0, 16, 16));

        UiSprite largerIcon = new UiSprite(
                "classic-ui",
                new UiRect(0, 0, 48, 48));

        UiContainer container = new UiContainer();
        UiButton button = new UiButton(firstIcon);

        container.add(button);

        UiRoot root = new UiRoot(container);
        root.updateLayout(new UiSize(400, 200));

        assertFalse(container.isLayoutDirty());

        button.setIcon(largerIcon);

        assertTrue(button.isLayoutDirty());
        assertTrue(container.isLayoutDirty());

        assertTrue(root.updateLayout(new UiSize(400, 200)));

        assertEquals(
                new UiRect(0, 0, 56, 56),
                button.bounds());
    }

    @Test
    void selectedAndAppearanceChangesDoNotInvalidateLayout() {
        UiButton button = new UiButton("Build", TEST_FONT);

        button.layout(new UiRect(0, 0, 100, 30));

        button.setSelected(true);
        button.setHovered(true);
        button.setPressed(true);
        button.setBackgroundVisible(false);

        assertTrue(button.selected());
        assertTrue(button.hovered());
        assertTrue(button.pressed());
        assertFalse(button.backgroundVisible());

        assertFalse(button.isLayoutDirty());
    }

    @Test
    void iconOnlyButtonRequiresMetricsForText() {
        UiSprite icon = new UiSprite(
                "classic-ui",
                new UiRect(0, 0, 32, 32));

        UiButton button = new UiButton(icon);

        assertThrows(
                IllegalStateException.class,
                () -> button.setText("Build"));
    }

}
