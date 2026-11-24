
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuItemDefinition;

class UiLegacyGameplayTooltipResolverTest {

    private static MenuItemDefinition action(
            String command,
            String parameter) {

        return new MenuItemDefinition(
                "test.action",
                "action",
                null,
                null,
                command,
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                parameter,
                null);
    }

    @Test
    void showsDescriptionAndConstructionMaterials() {

        var resolver = new UiLegacyGameplayTooltipResolver(
                (command, parameter) -> new UiLegacyGameplayTooltipResolver.Details(
                        List.of(
                                "A simple wooden wall."),
                        List.of("Wood", "Stone or Brick"),
                        List.of("Scaffolding"),
                        List.of(),
                        List.of(),
                        List.of()));

        String tooltip = resolver.apply(
                action("QUEUEANDPLACEAREA", "qlogwall"),
                "Log Wall");

        assertTrue(tooltip.startsWith("Log Wall"));

        assertTrue(tooltip.contains(
                "A simple wooden wall."));

        assertTrue(tooltip.contains(
                "Materials:\n- Wood"));

        assertTrue(tooltip.contains(
                "Required items:\n- Scaffolding"));
    }

    @Test
    void distinguishesCreatureAndHabitatRequirements() {

        var resolver = new UiLegacyGameplayTooltipResolver(
                (command, parameter) -> new UiLegacyGameplayTooltipResolver.Details(
                        List.of("Animal building"),
                        List.of(),
                        List.of("Food"),
                        List.of("Pig"),
                        List.of("Animal enclosure"),
                        List.of("Grass")));

        String tooltip = resolver.apply(
                action("BUILD", "pigfarm"),
                "Pig Farm");

        assertTrue(tooltip.contains(
                "Required items:\n- Food"));

        assertTrue(tooltip.contains(
                "Required creatures:\n- Pig"));

        assertTrue(tooltip.contains(
                "Required zones:\n- Animal enclosure"));

        assertTrue(tooltip.contains(
                "Habitat:\n- Grass"));
    }

    @Test
    void missingDefinitionFallsBackToTitle() {

        var resolver = new UiLegacyGameplayTooltipResolver(
                (command, parameter) -> null);

        String tooltip = resolver.apply(
                action("BUILD", "missing"),
                "Unknown Building");

        assertEquals("Unknown Building", tooltip);
    }

    @Test
    void nonActionItemsDoNotQueryGameplayDefinitions() {

        var resolver = new UiLegacyGameplayTooltipResolver(
                (command, parameter) -> {
                    fail("Lookup should not run");
                    return null;
                });

        MenuItemDefinition submenu = new MenuItemDefinition(
                "test.submenu",
                "submenu",
                null,
                "test.child",
                null,
                false,
                true,
                null,
                null,
                null,
                null,
                true);

        assertEquals(
                "Walls",
                resolver.apply(submenu, "Walls"));
    }

    @Test
    void workLocationsAndRepeatedStagesStayDistinct() {

        var step1 = new UiLegacyQueueStepFormatter.Step(
                4,
                xaos.actions.QueueItem.TYPE_WAIT,
                "38",
                "Wait 38 base turns");

        var step2 = new UiLegacyQueueStepFormatter.Step(
                8,
                xaos.actions.QueueItem.TYPE_WAIT,
                "20",
                "Wait 20 base turns");

        var phases = List.of(
                new UiLegacyQueuePhaseGrouper.Phase(
                        1,
                        UiLegacyQueuePhaseGrouper.PhaseKind.SETUP,
                        "Reserve Mason Bench",
                        List.of()),

                new UiLegacyQueuePhaseGrouper.Phase(
                        2,
                        UiLegacyQueuePhaseGrouper.PhaseKind.WORK,
                        "Work with Raw Stone at Mason Bench",
                        List.of(step1)),

                new UiLegacyQueuePhaseGrouper.Phase(
                        3,
                        UiLegacyQueuePhaseGrouper.PhaseKind.WORK,
                        "Work with Green Colour or Green Gel"
                                + " at Mason Bench",
                        List.of(step2)));

        var resolver = new UiLegacyGameplayTooltipResolver(
                (command, parameter) -> new UiLegacyGameplayTooltipResolver.Details(
                        List.of(),
                        List.of(),
                        List.of(
                                "Raw Stone",
                                "Green Colour or Green Gel"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("Mason Bench"),
                        phases));

        String tooltip = resolver.apply(
                action("QUEUEANDPLACEAREA",
                        "qengravedgreenwall"),
                "Engraved Green Wall");

        // Summary lists the location once.
        assertTrue(tooltip.contains(
                "Work locations:\n- Mason Bench"));

        // The two separate work stages remain.
        assertTrue(tooltip.contains(
                "2. Work with Raw Stone at Mason Bench"
                        + " (Wait 38 base turns)"));

        assertTrue(tooltip.contains(
                "3. Work with Green Colour or Green Gel"
                        + " at Mason Bench"
                        + " (Wait 20 base turns)"));

        assertTrue(tooltip.contains(
                "Item pickups:\n- Raw Stone"
                        + "\n- Green Colour or Green Gel"));
    }

}
