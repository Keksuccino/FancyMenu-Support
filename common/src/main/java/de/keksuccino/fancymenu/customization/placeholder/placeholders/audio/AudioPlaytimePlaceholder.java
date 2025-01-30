package de.keksuccino.fancymenu.customization.placeholder.placeholders.audio;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class AudioPlaytimePlaceholder extends Placeholder {

    public AudioPlaytimePlaceholder() {
        super("audio_playtime");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String elementId = dps.values.get("element_identifier");
        String showPercentageStr = dps.values.get("show_percentage");
        boolean showPercentage = StringUtils.equalsIgnoreCase(showPercentageStr, "true");

        if (elementId != null) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
            if (layer != null) {
                AbstractElement element = layer.getElementByInstanceIdentifier(elementId);
                if (element instanceof AudioElement audio) {
                    if (audio.currentAudio != null && audio.currentAudio.isReady() && (audio.currentAudio.isPlaying() || audio.currentAudio.isPaused())) {
                        float playTimeSeconds = audio.currentAudio.getPlayTime();
                        float durationSeconds = audio.currentAudio.getDuration();

                        if (showPercentage) {
                            // Return percentage without % symbol (0-100)
                            if (durationSeconds > 0) {
                                int percentage = (int)((playTimeSeconds / durationSeconds) * 100);
                                return String.valueOf(Math.min(100, Math.max(0, percentage)));
                            }
                            return "0";
                        } else {
                            // Return MM:SS format
                            int minutes = (int)(playTimeSeconds / 60);
                            int seconds = (int)(playTimeSeconds % 60);
                            return String.format("%02d:%02d", minutes, seconds);
                        }
                    }
                }
            }
        }
        return showPercentage ? "0" : "00:00";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("element_identifier");
        l.add("show_percentage"); // true/false - if true returns percentage (0-100) instead of time
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.audio_playtime");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.audio_playtime.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.audio");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("element_identifier", "put_identifier_of_audio_element_here");
        m.put("show_percentage", "false");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}