package de.keksuccino.fancymenu.customization.backend.action.actions.layout;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.customization.MenuCustomization;
import de.keksuccino.fancymenu.customization.backend.LayoutHandler;
import de.keksuccino.konkrete.localization.Locals;

import java.util.List;

public class DisableLayoutButtonAction extends ButtonActionContainer {

    public DisableLayoutButtonAction() {
        super("fancymenu_buttonaction_disable_layout");
    }

    @Override
    public String getAction() {
        return "disable_layout";
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(String value) {

        if (value != null) {

            List<LayoutHandler.LayoutProperties> enabled = LayoutHandler.getAsLayoutProperties(LayoutHandler.getEnabledLayouts());

            for (LayoutHandler.LayoutProperties l : enabled) {
                if (l.path != null) {
                    String name = Files.getNameWithoutExtension(l.path);
                    if (name.equals(value)) {
                        LayoutHandler.disableLayout(l);
                        return;
                    }
                }
            }

        }

    }

    @Override
    public String getActionDescription() {
        return Locals.localize("fancymenu.helper.buttonaction.disable_layout.desc");
    }

    @Override
    public String getValueDescription() {
        return Locals.localize("fancymenu.helper.buttonaction.disable_layout.value.desc");
    }

    @Override
    public String getValueExample() {
        return "my_cool_main_menu_layout";
    }

}
