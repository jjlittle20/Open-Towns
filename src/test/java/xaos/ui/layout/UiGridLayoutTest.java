
package xaos.ui.layout;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import xaos.ui.components.UiButton;
import xaos.ui.core.UiContainer;
import xaos.ui.rendering.UiTextMetrics;

class UiGridLayoutTest {

        private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 8, 16);

        private static UiButton iconButton() {

                UiButton button = new UiButton("", FONT);

                button.setMinimumSize(
                                new UiSize(64, 64));

                button.setPreferredSize(
                                new UiSize(64, 64));

                button.setPadding(UiInsets.NONE);

                return button;
        }

        @Test
        void laysOutFiveButtonsInTwoColumns() {

                UiContainer grid = new UiContainer();

                grid.setLayoutStrategy(
                                new UiGridLayout(2));

                grid.setSpacing(16);

                List<UiButton> buttons = new ArrayList<>();

                for (int i = 0; i < 5; i++) {

                        UiButton button = iconButton();

                        buttons.add(button);
                        grid.add(button);
                }

                assertEquals(
                                new UiSize(144, 224),
                                grid.measure(new UiSize(200, 500)));

                grid.layout(new UiRect(
                                10, 20, 200, 300));

                assertEquals(
                                new UiRect(10, 20, 64, 64),
                                buttons.get(0).bounds());

                assertEquals(
                                new UiRect(90, 20, 64, 64),
                                buttons.get(1).bounds());

                assertEquals(
                                new UiRect(10, 100, 64, 64),
                                buttons.get(2).bounds());

                assertEquals(
                                new UiRect(90, 100, 64, 64),
                                buttons.get(3).bounds());

                assertEquals(
                                new UiRect(10, 180, 64, 64),
                                buttons.get(4).bounds());
        }

        @Test
        void hiddenButtonsDoNotOccupyCells() {

                UiContainer grid = new UiContainer();

                grid.setLayoutStrategy(
                                new UiGridLayout(2));

                grid.setSpacing(16);

                UiButton first = iconButton();
                UiButton hidden = iconButton();
                UiButton third = iconButton();

                hidden.setVisible(false);

                grid.add(first);
                grid.add(hidden);
                grid.add(third);

                grid.layout(new UiRect(
                                0, 0, 200, 100));

                assertEquals(
                                new UiRect(0, 0, 64, 64),
                                first.bounds());

                assertEquals(
                                new UiRect(80, 0, 64, 64),
                                third.bounds());

                assertEquals(
                                0,
                                hidden.bounds().width());
        }

        @Test
        void rejectsInvalidColumnCounts() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> new UiGridLayout(0));
        }

        @Test
        void centresTwoColumnGridHorizontally() {

                UiContainer grid = new UiContainer();

                grid.setLayoutStrategy(
                                new UiGridLayout(2, true));

                grid.setSpacing(16);

                UiButton first = iconButton();
                UiButton second = iconButton();
                UiButton third = iconButton();

                grid.add(first);
                grid.add(second);
                grid.add(third);

                grid.layout(new UiRect(
                                10, 20, 200, 300));

                // Grid width = 64 + 16 + 64 = 144.
                // Remaining width = 200 - 144 = 56.
                // Horizontal offset = 28.
                assertEquals(
                                new UiRect(38, 20, 64, 64),
                                first.bounds());

                assertEquals(
                                new UiRect(118, 20, 64, 64),
                                second.bounds());

                // The final item remains in column one.
                assertEquals(
                                new UiRect(38, 100, 64, 64),
                                third.bounds());
        }

}
