
package xaos.ui.rendering;

import xaos.ui.layout.UiSize;

/**
 * Measures text using the active font's dimensions.
 *
 * Independent of any specific font or rendering backend.
 */
@FunctionalInterface
public interface UiTextMetrics {

    /**
     * Returns the dimensions needed to display a string.
     */
    UiSize measure(String text);
}
