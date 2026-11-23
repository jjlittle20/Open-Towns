
package xaos.ui.integration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import xaos.actions.ActionManager;
import xaos.actions.ActionManagerItem;
import xaos.actions.QueueItem;

import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;

import xaos.tiles.entities.buildings.BuildingManager;
import xaos.tiles.entities.buildings.BuildingManagerItem;
import xaos.tiles.entities.items.ItemManager;
import xaos.tiles.entities.items.ItemManagerItem;
import xaos.tiles.entities.living.LivingEntityManager;
import xaos.tiles.entities.living.LivingEntityManagerItem;
import xaos.tiles.terrain.TerrainManager;

import xaos.utils.Messages;
import xaos.utils.Utils;
import xaos.utils.UtilsIniHeaders;
import xaos.zones.ZoneManager;
import xaos.zones.ZoneManagerItem;

/**
 * Resolves descriptive gameplay information for
 * the new XML-defined construction menus.
 *
 * This is an internal compatibility adapter.
 *
 * It does not inspect player inventory, validate
 * placement, or decide whether actions are enabled.
 */
public final class UiLegacyGameplayTooltipResolver
        implements BiFunction<MenuItemDefinition, String, String> {

    /**
     * Structured tooltip information.
     *
     * Keeping these categories separate allows
     * future UI themes to render them differently.
     */
    public record Details(
            List<String> descriptions,
            List<String> materials,
            List<String> requiredItems,
            List<String> requiredCreatures,
            List<String> zones,
            List<String> habitats) {

        public Details {
            descriptions = clean(descriptions);
            materials = clean(materials);
            requiredItems = clean(requiredItems);
            requiredCreatures = clean(requiredCreatures);
            zones = clean(zones);
            habitats = clean(habitats);
        }
    }

    private final BiFunction<String, String, Details> lookup;

    /**
     * Production constructor using the original
     * Towns definition managers.
     */
    public UiLegacyGameplayTooltipResolver() {
        this(UiLegacyGameplayTooltipResolver::fromLegacy);
    }

    /**
     * Injected lookup for tests and future adapters.
     */
    public UiLegacyGameplayTooltipResolver(
            BiFunction<String, String, Details> lookup) {

        this.lookup = Objects.requireNonNull(
                lookup, "lookup");
    }

    @Override
    public String apply(
            MenuItemDefinition item,
            String title) {

        Objects.requireNonNull(item, "item");

        String heading = hasText(title)
                ? title
                : item.getId();

        if (!"action".equals(item.getType())
                || !hasText(item.getParameter())) {
            return heading;
        }

        Details details = lookup.apply(
                item.getCommand(),
                item.getParameter());

        if (details == null) {
            return heading;
        }

        List<String> lines = new ArrayList<>();
        lines.add(heading);

        // Descriptions are ordinary body text.
        if (!details.descriptions().isEmpty()) {
            lines.add("");
            lines.addAll(details.descriptions());
        }

        append(lines, "Materials", details.materials());
        append(lines, "Required items", details.requiredItems());
        append(lines, "Required creatures",
                details.requiredCreatures());
        append(lines, "Required zones", details.zones());
        append(lines, "Habitat", details.habitats());

        return String.join("\n", lines);
    }

    private static Details fromLegacy(
            String command,
            String parameter) {

        if (!hasText(command) || !hasText(parameter)) {
            return null;
        }

        if (CommandPanel.COMMAND_BUILD
                .equalsIgnoreCase(command)) {

            return buildingDetails(parameter);
        }

        if (CommandPanel.COMMAND_QUEUE_AND_PLACE
                .equalsIgnoreCase(command)
                || CommandPanel.COMMAND_QUEUE_AND_PLACE_AREA
                        .equalsIgnoreCase(command)) {

            return actionDetails(parameter);
        }

        return null;
    }

    private static Details actionDetails(String id) {

        ActionManagerItem action = ActionManager.getItem(id);

        if (action == null) {
            return null;
        }

        List<String> descriptions = new ArrayList<>();
        List<String> materials = new ArrayList<>();
        List<String> requiredItems = new ArrayList<>();
        List<String> requiredCreatures = new ArrayList<>();
        List<String> zones = new ArrayList<>();
        List<String> habitats = new ArrayList<>();

        List<QueueItem> queue = action.getQueue();

        if (queue != null) {

            for (QueueItem step : queue) {

                switch (step.getType()) {

                    case QueueItem.TYPE_MOVE -> {
                        addAlternatives(
                                materials,
                                step.getValue(),
                                UiLegacyGameplayTooltipResolver::itemName);
                    }

                    case QueueItem.TYPE_PICK -> {
                        addAlternatives(
                                requiredItems,
                                step.getValue(),
                                UiLegacyGameplayTooltipResolver::itemName);
                    }

                    case QueueItem.TYPE_PICK_FRIENDLY -> {
                        addAlternatives(
                                requiredCreatures,
                                step.getValue(),
                                UiLegacyGameplayTooltipResolver::creatureName);
                    }

                    case QueueItem.TYPE_CREATE_ITEM -> {

                        ItemManagerItem created = ItemManager.getItem(
                                step.getValue());

                        if (created != null) {

                            addAll(
                                    descriptions,
                                    created.getDescriptions());

                            if (created.isMilitaryItem()) {
                                addText(
                                        descriptions,
                                        created.getMilitaryString());
                            }

                            if (created.canBeEaten()) {
                                addText(
                                        descriptions,
                                        created.getEatString());
                            }

                            if (created.isBlockFluids()) {
                                addText(
                                        descriptions,
                                        Messages.getString(
                                                "SmartMenu.8"));
                            }

                            if (created.getZones() != null) {

                                for (String zoneId : created.getZones()) {

                                    ZoneManagerItem zone = ZoneManager.getItem(
                                            zoneId);

                                    if (zone != null) {
                                        addText(
                                                zones,
                                                zone.getName());
                                    }
                                }
                            }

                            if (created.getHabitat() != null) {

                                for (Integer terrainId : created.getHabitat()) {

                                    if (terrainId == null) {
                                        continue;
                                    }

                                    var terrain = TerrainManager.getItemByID(
                                            terrainId);

                                    if (terrain != null) {
                                        addText(
                                                habitats,
                                                terrain.getName());
                                    }
                                }
                            }
                        }
                    }

                    default -> {
                        // Other queue steps do not
                        // contribute to legacy tooltips.
                    }
                }
            }
        }

        return new Details(
                descriptions,
                materials,
                requiredItems,
                requiredCreatures,
                zones,
                habitats);
    }

    private static Details buildingDetails(String id) {

        BuildingManagerItem building = BuildingManager.getItem(id);

        if (building == null) {
            return null;
        }

        List<String> requiredItems = new ArrayList<>();
        List<String> requiredCreatures = new ArrayList<>();

        if (building.getPrerequisites() != null) {

            for (int[] alternatives : building.getPrerequisites()) {

                addIndexedAlternatives(
                        requiredItems,
                        alternatives,
                        UiLegacyGameplayTooltipResolver::itemName);
            }
        }

        if (building.getPrerequisitesFriendly() != null) {

            for (int[] alternatives : building.getPrerequisitesFriendly()) {

                addIndexedAlternatives(
                        requiredCreatures,
                        alternatives,
                        UiLegacyGameplayTooltipResolver::creatureName);
            }
        }

        return new Details(
                building.getDescriptions(),
                List.of(),
                requiredItems,
                requiredCreatures,
                List.of(),
                List.of());
    }

    private static String itemName(String id) {

        ItemManagerItem item = ItemManager.getItem(id);

        return item != null && hasText(item.getName())
                ? item.getName()
                : id;
    }

    private static String creatureName(String id) {

        LivingEntityManagerItem creature = LivingEntityManager.getItem(id);

        return creature != null
                && hasText(creature.getName())
                        ? creature.getName()
                        : id;
    }

    /**
     * A comma-separated queue value represents
     * alternatives, not separate required costs.
     */
    private static void addAlternatives(
            List<String> destination,
            String value,
            Function<String, String> names) {

        List<String> ids = Utils.getArray(value);

        if (ids == null || ids.isEmpty()) {
            return;
        }

        Set<String> resolved = new LinkedHashSet<>();

        for (String id : ids) {
            addText(resolved, names.apply(id.trim()));
        }

        if (!resolved.isEmpty()) {
            destination.add(String.join(
                    Messages.getString("SmartMenu.3"),
                    resolved));
        }
    }

    private static void addIndexedAlternatives(
            List<String> destination,
            int[] values,
            Function<String, String> names) {

        if (values == null || values.length == 0) {
            return;
        }

        Set<String> resolved = new LinkedHashSet<>();

        for (int value : values) {

            String id = UtilsIniHeaders.getStringIniHeader(
                    value);

            addText(resolved, names.apply(id));
        }

        if (!resolved.isEmpty()) {
            destination.add(String.join(
                    Messages.getString("SmartMenu.3"),
                    resolved));
        }
    }

    private static void append(
            List<String> lines,
            String heading,
            List<String> entries) {

        if (entries.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add(heading + ":");

        for (String entry : entries) {
            lines.add("- " + entry);
        }
    }

    private static List<String> clean(List<String> values) {

        if (values == null) {
            return List.of();
        }

        List<String> result = new ArrayList<>();

        for (String value : values) {
            addText(result, value);
        }

        return List.copyOf(result);
    }

    private static void addAll(
            List<String> destination,
            List<String> values) {

        if (values == null) {
            return;
        }

        for (String value : values) {
            addText(destination, value);
        }
    }

    private static void addText(
            java.util.Collection<String> destination,
            String value) {

        if (hasText(value)) {
            destination.add(value.trim());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
