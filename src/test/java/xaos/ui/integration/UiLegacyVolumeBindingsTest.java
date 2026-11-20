
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuXmlLoader;

class UiLegacyVolumeBindingsTest {

    private static MenuDefinition loadAudio()
            throws Exception {

        URL resource = UiLegacyVolumeBindingsTest.class
                .getClassLoader()
                .getResource(
                        "data/menus/main/options/audio_menu.xml");

        assertNotNull(resource);

        return MenuXmlLoader.load(
                Path.of(resource.toURI()).toFile());
    }

    private static MenuItemDefinition item(
            MenuDefinition definition,
            String id) {

        return definition.getItems().stream()
                .filter(entry -> id.equals(entry.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void bothAudioSlidersResolveFromRealXml()
            throws Exception {

        MenuDefinition audio = loadAudio();

        AtomicInteger music = new AtomicInteger(8);
        AtomicInteger fx = new AtomicInteger(4);
        AtomicInteger saves = new AtomicInteger();

        UiLegacyVolumeBindings bindings = new UiLegacyVolumeBindings(
                music::get,
                music::set,
                fx::get,
                fx::set,
                saves::incrementAndGet);

        UiSliderBinding musicSlider = bindings.forItem(item(
                audio,
                "main.options.audio.music_volume"));

        UiSliderBinding fxSlider = bindings.forItem(item(
                audio,
                "main.options.audio.fx_volume"));

        assertEquals(8, musicSlider.currentValue());
        assertEquals(4, fxSlider.currentValue());

        assertEquals(1, musicSlider.minimum());
        assertEquals(10, musicSlider.maximum());
        assertEquals(1, musicSlider.step());

        musicSlider.setValue(6);

        assertEquals(6, music.get());
        assertEquals(4, fx.get());

        fxSlider.setValue(9);

        assertEquals(6, music.get());
        assertEquals(9, fx.get());

        // No settings file writes during changes.
        assertEquals(0, saves.get());

        musicSlider.commit();
        fxSlider.commit();

        assertEquals(2, saves.get());
    }

    @Test
    void sliderValuesClampToSupportedRange() {

        AtomicInteger music = new AtomicInteger(5);

        UiSliderBinding slider = new UiSliderBinding(
                1, 10, 1,
                music::get,
                music::set,
                () -> {
                });

        // Towns currently supports 1 to 10.
        slider.setValue(0);
        assertEquals(1, music.get());

        slider.setValue(50);
        assertEquals(10, music.get());

        slider.setValue(7);
        assertEquals(7, music.get());
    }

    @Test
    void unchangedValuesAreNotWrittenRepeatedly() {

        AtomicInteger value = new AtomicInteger(5);
        AtomicInteger changes = new AtomicInteger();

        UiSliderBinding slider = new UiSliderBinding(
                1, 10, 1,
                value::get,
                next -> {
                    changes.incrementAndGet();
                    value.set(next);
                },
                () -> {
                });

        slider.setValue(5);
        slider.setValue(5);

        assertEquals(0, changes.get());

        slider.setValue(6);
        slider.setValue(6);

        assertEquals(1, changes.get());
    }

    @Test
    void slidersReadChangesMadeOutsideTheUi() {

        AtomicInteger value = new AtomicInteger(3);

        UiSliderBinding slider = new UiSliderBinding(
                1, 10, 1,
                value::get,
                value::set,
                () -> {
                });

        assertEquals(3, slider.currentValue());

        // Simulate the legacy menu changing volume.
        value.set(8);

        assertEquals(8, slider.currentValue());
    }

    @Test
    void rejectsUnsupportedControlTypes()
            throws Exception {

        MenuDefinition audio = loadAudio();

        UiLegacyVolumeBindings bindings = new UiLegacyVolumeBindings(
                () -> 5,
                value -> {
                },
                () -> 5,
                value -> {
                },
                () -> {
                });

        MenuItemDefinition musicToggle = item(audio, "main.options.audio.music");

        assertThrows(
                IllegalArgumentException.class,
                () -> bindings.forItem(musicToggle));
    }
}
