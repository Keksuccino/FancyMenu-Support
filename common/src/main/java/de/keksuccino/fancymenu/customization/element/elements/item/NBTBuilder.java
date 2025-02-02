package de.keksuccino.fancymenu.customization.element.elements.item;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.stream.Stream;

public class NBTBuilder {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public static DataComponentPatch buildNbtFromString(@NotNull ItemStack target, @NotNull String nbtJson) {

        try {

            Stream<HolderLookup.RegistryLookup<?>> lookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).listRegistries();
            ItemParser parser = new ItemParser(HolderLookup.Provider.create(lookup));

            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(target.getItem());
            String dummyCommand = itemKey + nbtJson + " 1";

            ItemParser.ItemResult result = parser.parse(new StringReader(dummyCommand));

            return result.components();

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to parse ItemStack NBT data!", ex);
        }

        return null;

    }

}
