package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * An action to use with buttons, tickers, etc.<br>
 * Needs to get registered to the {@link ActionRegistry} at mod init.
 */
public abstract class Action {

    private final String identifier;

    public Action(@NotNull String uniqueIdentifier) {
        this.identifier = Objects.requireNonNull(uniqueIdentifier);
    }

    /**
     * If this action has a value.
     */
    public abstract boolean hasValue();

    /**
     * @param value The value that was set to the action or NULL if this action has no value.
     */
    public abstract void execute(@Nullable String value);

    @NotNull
    public abstract Component getActionDisplayName();

    @NotNull
    public abstract Component[] getActionDescription();

    @Nullable
    public abstract Component getValueDisplayName();

    /**
     * An example of how the value of this action should look like.
     */
    @Nullable
    public abstract String getValueExample();

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @Nullable
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
