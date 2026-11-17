
package xaos.ui.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiSizePolicy;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiPoint;

class UiLayoutTest {

    private UiElement element(int width, int height) {
        UiElement element = new UiElement();
        element.setPreferredSize(new UiSize(width, height));
        return element;
    }

    @Test
    void absoluteLayoutPositionsChildrenIndependently() {
        UiContainer canvas = new UiContainer();
        canvas.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);
        canvas.setPadding(UiInsets.all(5));

        UiElement first = element(60, 25);
        first.setPosition(new UiPoint(10, 20));

        UiElement second = element(40, 20);
        second.setPosition(new UiPoint(100, 40));

        canvas.add(first);
        canvas.add(second);

        canvas.layout(new UiRect(50, 100, 300, 200));

        assertEquals(
                new UiRect(65, 125, 60, 25),
                first.bounds());

        assertEquals(
                new UiRect(155, 145, 40, 20),
                second.bounds());
    }

    @Test
    void absoluteLayoutMeasuresContentExtents() {
        UiContainer canvas = new UiContainer();
        canvas.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);
        canvas.setPadding(UiInsets.all(5));

        UiElement first = element(60, 25);
        first.setPosition(new UiPoint(10, 20));

        UiElement second = element(40, 20);
        second.setPosition(new UiPoint(100, 40));

        canvas.add(first);
        canvas.add(second);

        assertEquals(
                new UiSize(150, 70),
                canvas.measure(new UiSize(500, 500)));
    }

    @Test
    void absoluteLayoutSupportsFillFromChildPosition() {
        UiContainer canvas = new UiContainer();
        canvas.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);
        canvas.setPadding(UiInsets.all(10));

        UiElement child = element(40, 20);
        child.setPosition(new UiPoint(20, 30));
        child.setHorizontalSizePolicy(UiSizePolicy.FILL);
        child.setVerticalSizePolicy(UiSizePolicy.FILL);

        canvas.add(child);

        canvas.layout(new UiRect(0, 0, 200, 120));

        assertEquals(
                new UiRect(30, 40, 160, 70),
                child.bounds());
    }

    @Test
    void movingChildInvalidatesRootLayout() {
        UiContainer canvas = new UiContainer();
        canvas.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);

        UiElement child = element(80, 30);
        child.setPosition(new UiPoint(20, 30));

        canvas.add(child);

        UiRoot root = new UiRoot(canvas);
        root.updateLayout(new UiSize(400, 300));

        assertFalse(canvas.isLayoutDirty());

        child.setPosition(new UiPoint(100, 120));

        assertTrue(child.isLayoutDirty());
        assertTrue(canvas.isLayoutDirty());

        assertTrue(root.updateLayout(new UiSize(400, 300)));

        assertEquals(
                new UiRect(100, 120, 80, 30),
                child.bounds());

        assertFalse(canvas.isLayoutDirty());
    }

    @Test
    void bringToFrontChangesStackingOrder() {
        UiContainer canvas = new UiContainer();
        canvas.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);

        UiElement first = element(100, 50);
        UiElement second = element(100, 50);
        UiElement third = element(100, 50);

        canvas.add(first);
        canvas.add(second);
        canvas.add(third);

        UiRoot root = new UiRoot(canvas);
        root.updateLayout(new UiSize(400, 300));

        assertTrue(canvas.bringToFront(first));

        assertEquals(
                java.util.List.of(second, third, first),
                canvas.children());

        assertSame(canvas, first.parent());
        assertTrue(canvas.isLayoutDirty());

        assertTrue(root.updateLayout(new UiSize(400, 300)));

        // Already at the front, so no change.
        assertFalse(canvas.bringToFront(first));
        assertFalse(canvas.isLayoutDirty());

        // An element outside this container cannot be reordered.
        assertFalse(canvas.bringToFront(new UiElement()));
    }

    @Test
    void layoutClearsDirtyFlags() {
        UiContainer container = new UiContainer();
        UiElement child = element(50, 20);

        container.add(child);

        assertTrue(container.isLayoutDirty());
        assertTrue(child.isLayoutDirty());

        container.layout(new UiRect(0, 0, 200, 100));

        assertFalse(container.isLayoutDirty());
        assertFalse(child.isLayoutDirty());
    }

    @Test
    void childChangesInvalidateAllAncestors() {
        UiContainer root = new UiContainer();
        UiContainer inner = new UiContainer();
        UiElement child = element(50, 20);

        root.add(inner);
        inner.add(child);

        UiRect bounds = new UiRect(0, 0, 200, 100);
        root.layout(bounds);

        assertFalse(root.isLayoutDirty());
        assertFalse(inner.isLayoutDirty());
        assertFalse(child.isLayoutDirty());

        child.setPreferredSize(new UiSize(80, 30));

        assertTrue(child.isLayoutDirty());
        assertTrue(inner.isLayoutDirty());
        assertTrue(root.isLayoutDirty());

        root.layout(bounds);

        assertFalse(root.isLayoutDirty());
        assertFalse(inner.isLayoutDirty());
        assertFalse(child.isLayoutDirty());

        assertEquals(new UiRect(0, 0, 80, 30), child.bounds());
    }

    @Test
    void visibilityAndSizePolicyChangesInvalidateLayout() {
        UiContainer container = new UiContainer();
        UiElement child = element(50, 20);

        container.add(child);

        UiRect bounds = new UiRect(0, 0, 200, 100);
        container.layout(bounds);

        child.setVisible(false);

        assertTrue(container.isLayoutDirty());

        container.layout(bounds);

        assertFalse(container.isLayoutDirty());

        child.setVisible(true);
        container.layout(bounds);

        child.setHorizontalSizePolicy(UiSizePolicy.FILL);

        assertTrue(child.isLayoutDirty());
        assertTrue(container.isLayoutDirty());

        container.layout(bounds);

        child.setVerticalSizePolicy(UiSizePolicy.FILL);

        assertTrue(container.isLayoutDirty());
    }

    @Test
    void containerChangesInvalidateLayout() {
        UiContainer container = new UiContainer();
        UiRect bounds = new UiRect(0, 0, 200, 100);

        container.layout(bounds);

        container.setPadding(UiInsets.all(5));
        assertTrue(container.isLayoutDirty());

        container.layout(bounds);

        container.setSpacing(10);
        assertTrue(container.isLayoutDirty());

        container.layout(bounds);

        container.setLayoutStrategy(UiLinearLayout.HORIZONTAL);
        assertTrue(container.isLayoutDirty());

        container.layout(bounds);

        UiElement child = element(40, 20);

        container.add(child);
        assertTrue(container.isLayoutDirty());

        container.layout(bounds);

        container.remove(child);
        assertTrue(container.isLayoutDirty());

        container.layout(bounds);

        container.add(child);
        container.layout(bounds);

        container.clear();
        assertTrue(container.isLayoutDirty());
    }

    @Test
    void unchangedPropertiesDoNotInvalidateLayout() {
        UiContainer container = new UiContainer();
        UiElement child = element(50, 20);

        container.add(child);
        container.layout(new UiRect(0, 0, 200, 100));

        // Assign existing values again.
        child.setPreferredSize(new UiSize(50, 20));
        child.setMinimumSize(UiSize.ZERO);
        child.setVisible(true);

        child.setHorizontalSizePolicy(
                UiSizePolicy.PREFERRED);

        child.setVerticalSizePolicy(
                UiSizePolicy.PREFERRED);

        container.setPadding(UiInsets.NONE);
        container.setSpacing(0);

        container.setLayoutStrategy(
                UiLinearLayout.VERTICAL);

        assertFalse(child.isLayoutDirty());
        assertFalse(container.isLayoutDirty());
    }

    @Test
    void verticalFillRespondsToResizing() {
        UiContainer container = new UiContainer();
        container.setPadding(UiInsets.all(5));
        container.setSpacing(10);

        UiElement header = element(100, 20);
        UiElement content = element(80, 30);

        content.setHorizontalSizePolicy(UiSizePolicy.FILL);
        content.setVerticalSizePolicy(UiSizePolicy.FILL);

        container.add(header);
        container.add(content);

        container.layout(new UiRect(0, 0, 200, 150));

        assertEquals(
                new UiRect(5, 5, 100, 20),
                header.bounds());

        assertEquals(
                new UiRect(5, 35, 190, 110),
                content.bounds());

        // Resize the container.
        container.layout(new UiRect(0, 0, 200, 200));

        assertEquals(
                new UiRect(5, 35, 190, 160),
                content.bounds());
    }

    @Test
    void horizontalFillDistributesRemainingSpace() {
        UiContainer container = new UiContainer();
        container.setLayoutStrategy(UiLinearLayout.HORIZONTAL);
        container.setSpacing(5);

        UiElement fixed = element(40, 20);
        UiElement firstFill = element(30, 20);
        UiElement secondFill = element(30, 20);

        firstFill.setHorizontalSizePolicy(UiSizePolicy.FILL);
        secondFill.setHorizontalSizePolicy(UiSizePolicy.FILL);

        container.add(fixed);
        container.add(firstFill);
        container.add(secondFill);

        container.layout(new UiRect(0, 0, 201, 50));

        assertEquals(
                new UiRect(0, 0, 40, 20),
                fixed.bounds());

        assertEquals(
                new UiRect(45, 0, 76, 20),
                firstFill.bounds());

        assertEquals(
                new UiRect(126, 0, 75, 20),
                secondFill.bounds());
    }

    @Test
    void crossAxisFillExpandsToContainerHeight() {
        UiContainer container = new UiContainer();
        container.setLayoutStrategy(UiLinearLayout.HORIZONTAL);

        UiElement stretched = element(20, 10);
        UiElement normal = element(20, 15);

        stretched.setVerticalSizePolicy(UiSizePolicy.FILL);

        container.add(stretched);
        container.add(normal);

        container.layout(new UiRect(0, 0, 100, 70));

        assertEquals(
                new UiRect(0, 0, 20, 70),
                stretched.bounds());

        assertEquals(
                new UiRect(20, 0, 20, 15),
                normal.bounds());
    }

    @Test
    void nestedContainersCanFillAvailableSpace() {
        UiContainer outer = new UiContainer();
        outer.setPadding(UiInsets.all(5));

        UiElement header = element(120, 20);

        UiContainer inner = new UiContainer();
        inner.setLayoutStrategy(UiLinearLayout.HORIZONTAL);
        inner.setHorizontalSizePolicy(UiSizePolicy.FILL);
        inner.setVerticalSizePolicy(UiSizePolicy.FILL);

        UiElement left = element(50, 10);
        UiElement right = element(30, 10);

        right.setHorizontalSizePolicy(UiSizePolicy.FILL);
        right.setVerticalSizePolicy(UiSizePolicy.FILL);

        inner.add(left);
        inner.add(right);

        outer.add(header);
        outer.add(inner);

        outer.layout(new UiRect(0, 0, 200, 120));

        assertEquals(
                new UiRect(5, 25, 190, 90),
                inner.bounds());

        assertEquals(
                new UiRect(55, 25, 140, 90),
                right.bounds());
    }

    @Test
    void containerMeasuresChildren() {
        UiContainer container = new UiContainer();

        container.setPadding(UiInsets.all(8));
        container.setSpacing(4);

        container.add(element(120, 30));
        container.add(element(120, 30));

        UiSize measured = container.measure(
                new UiSize(500, 500));

        assertEquals(new UiSize(136, 80), measured);
    }

    @Test
    void childrenArePositionedVertically() {
        UiContainer container = new UiContainer();

        container.setPadding(UiInsets.all(5));
        container.setSpacing(10);

        UiElement first = element(100, 30);
        UiElement second = element(100, 30);

        container.add(first);
        container.add(second);

        container.layout(new UiRect(50, 100, 200, 150));

        assertEquals(
                new UiRect(55, 105, 100, 30),
                first.bounds());

        assertEquals(
                new UiRect(55, 145, 100, 30),
                second.bounds());
    }

    @Test
    void hiddenElementsDoNotTakeSpace() {
        UiContainer container = new UiContainer();

        UiElement first = element(100, 30);
        UiElement second = element(100, 30);

        second.setVisible(false);

        container.add(first);
        container.add(second);

        assertEquals(
                new UiSize(100, 30),
                container.measure(new UiSize(500, 500)));
    }

    @Test
    void nestedContainersWork() {
        UiContainer outer = new UiContainer();
        outer.setPadding(UiInsets.all(5));

        UiContainer inner = new UiContainer();
        inner.setPadding(UiInsets.all(3));

        UiElement child = element(40, 20);

        inner.add(child);
        outer.add(inner);

        outer.layout(new UiRect(100, 200, 150, 100));

        assertEquals(
                new UiRect(105, 205, 46, 26),
                inner.bounds());

        assertEquals(
                new UiRect(108, 208, 40, 20),
                child.bounds());
    }

    @Test
    void circularParentingIsRejected() {
        UiContainer parent = new UiContainer();
        UiContainer child = new UiContainer();

        parent.add(child);

        assertThrows(
                IllegalArgumentException.class,
                () -> child.add(parent));

        assertThrows(
                IllegalArgumentException.class,
                () -> parent.add(parent));
    }

    @Test
    void removingChildrenClearsParent() {
        UiContainer container = new UiContainer();
        UiElement child = element(100, 30);

        container.add(child);

        assertSame(container, child.parent());
        assertTrue(container.remove(child));
        assertNull(child.parent());
    }

    @Test
    void horizontalLayoutMeasuresAndPositionsChildren() {
        UiContainer container = new UiContainer();

        container.setLayoutStrategy(UiLinearLayout.HORIZONTAL);
        container.setPadding(UiInsets.all(5));
        container.setSpacing(10);

        UiElement first = element(80, 20);
        UiElement second = element(40, 30);

        container.add(first);
        container.add(second);

        assertEquals(
                new UiSize(140, 40),
                container.measure(new UiSize(500, 500)));

        container.layout(new UiRect(10, 20, 200, 80));

        assertEquals(
                new UiRect(15, 25, 80, 20),
                first.bounds());

        assertEquals(
                new UiRect(105, 25, 40, 30),
                second.bounds());
    }

    @Test
    void switchingLayoutStrategiesChangesMeasurement() {
        UiContainer container = new UiContainer();

        container.add(element(80, 20));
        container.add(element(40, 30));

        assertEquals(
                new UiSize(80, 50),
                container.measure(new UiSize(500, 500)));

        container.setLayoutStrategy(UiLinearLayout.HORIZONTAL);

        assertEquals(
                new UiSize(120, 30),
                container.measure(new UiSize(500, 500)));
    }

    @Test
    void hiddenChildrenDoNotCreateHorizontalSpacing() {
        UiContainer container = new UiContainer();

        container.setLayoutStrategy(UiLinearLayout.HORIZONTAL);
        container.setSpacing(7);

        UiElement first = element(20, 10);
        UiElement hidden = element(100, 50);
        UiElement last = element(30, 15);

        hidden.setVisible(false);

        container.add(first);
        container.add(hidden);
        container.add(last);

        assertEquals(
                new UiSize(57, 15),
                container.measure(new UiSize(500, 500)));
    }

}
