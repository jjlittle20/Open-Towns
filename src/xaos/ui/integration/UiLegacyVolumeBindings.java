
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import xaos.main.Game;
import xaos.panels.menus.MenuItemDefinition;
import xaos.utils.Utils;
import xaos.utils.UtilsAL;

/**
 * Compatibility bindings for the original
 * Towns audio volume settings.
 *
 * Uses live audio updates while dragging,
 * with settings persistence on completion.
 */
public final class UiLegacyVolumeBindings {

    private final IntSupplier musicVolume;
    private final IntConsumer setMusicVolume;

    private final IntSupplier fxVolume;
    private final IntConsumer setFxVolume;

    private final Runnable saveSettings;

    /**
     * Injectable constructor for testing.
     */
    public UiLegacyVolumeBindings(
            IntSupplier musicVolume,
            IntConsumer setMusicVolume,
            IntSupplier fxVolume,
            IntConsumer setFxVolume,
            Runnable saveSettings) {

        this.musicVolume = Objects.requireNonNull(
                musicVolume, "musicVolume");

        this.setMusicVolume = Objects.requireNonNull(
                setMusicVolume, "setMusicVolume");

        this.fxVolume = Objects.requireNonNull(
                fxVolume, "fxVolume");

        this.setFxVolume = Objects.requireNonNull(
                setFxVolume, "setFxVolume");

        this.saveSettings = Objects.requireNonNull(
                saveSettings, "saveSettings");
    }

    /**
     * Connects the binding to the existing
     * game and audio systems.
     */
    public static UiLegacyVolumeBindings forGame() {

        return new UiLegacyVolumeBindings(

                Game::getVolumeMusic,

                value -> {
                    Game.setVolumeMusic(value);

                    // Apply the new volume immediately
                    // to active OpenAL sources.
                    UtilsAL.setMusicVolume(
                            Game.getVolumeMusic());
                },

                Game::getVolumeFX,

                value -> {
                    Game.setVolumeFX(value);

                    UtilsAL.setFXVolume(
                            Game.getVolumeFX());
                },

                Utils::saveOptions);
    }

    /**
     * Creates a slider binding for a supported
     * Audio XML entry.
     */
    public UiSliderBinding forItem(
            MenuItemDefinition item) {

        Objects.requireNonNull(item, "item");

        if (!"slider".equals(item.getType())) {
            throw new IllegalArgumentException(
                    "Not a slider item: "
                            + item.getId());
        }

        return switch (item.getCommand()) {

            case "COMMAND_MM_ADD_MUSIC_VOLUME" ->
                new UiSliderBinding(
                        1, 10, 1,
                        musicVolume,
                        setMusicVolume,
                        saveSettings,
                        value -> (value * 10) + "%");

            case "COMMAND_MM_ADD_FX_VOLUME" ->
                new UiSliderBinding(
                        1, 10, 1,
                        fxVolume,
                        setFxVolume,
                        saveSettings,
                        value -> (value * 10) + "%");

            default ->
                throw new IllegalArgumentException(
                        "Unsupported XML slider command '"
                                + item.getCommand()
                                + "' for item "
                                + item.getId());
        };

    }
}
