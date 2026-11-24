
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UiLegacyRightMenuCompatibilityTest {

    @TempDir
    Path tempDir;

    @Test
    void baseGameMenuDoesNotRequireFallback() {

        Path base = tempDir.resolve(
                "data/menu_right.xml");

        assertFalse(
                UiLegacyRightMenuCompatibility.hasOverrides(
                        base.toFile(),
                        List.of(base.toString())));
    }

    @Test
    void campaignOverrideRequiresLegacyPanel() {

        Path base = tempDir.resolve(
                "data/menu_right.xml");

        Path campaign = tempDir.resolve(
                "campaigns/tutorial/menu_right.xml");

        assertTrue(
                UiLegacyRightMenuCompatibility.hasOverrides(
                        base.toFile(),
                        List.of(
                                base.toString(),
                                campaign.toString())));
    }

    @Test
    void missionOverrideRequiresLegacyPanel() {

        Path base = tempDir.resolve(
                "data/menu_right.xml");

        Path mission = tempDir.resolve(
                "campaigns/tutorial/mission1/menu_right.xml");

        assertTrue(
                UiLegacyRightMenuCompatibility.hasOverrides(
                        base.toFile(),
                        List.of(
                                base.toString(),
                                mission.toString())));
    }

    @Test
    void legacyModOverrideRequiresLegacyPanel() {

        Path base = tempDir.resolve(
                "data/menu_right.xml");

        Path mod = tempDir.resolve(
                "mods/example/data/menu_right.xml");

        assertTrue(
                UiLegacyRightMenuCompatibility.hasOverrides(
                        base.toFile(),
                        List.of(
                                base.toString(),
                                mod.toString())));
    }

    @Test
    void normalisedBasePathIsNotAnOverride() {

        Path base = tempDir.resolve(
                "data/menu_right.xml");

        Path equivalent = tempDir.resolve(
                "data/subfolder/../menu_right.xml");

        assertFalse(
                UiLegacyRightMenuCompatibility.hasOverrides(
                        base.toFile(),
                        List.of(equivalent.toString())));
    }

    @Test
    void emptyOverrideListDoesNotTriggerFallback() {

        Path base = tempDir.resolve(
                "data/menu_right.xml");

        assertFalse(
                UiLegacyRightMenuCompatibility.hasOverrides(
                        base.toFile(),
                        List.of()));
    }
}
