package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.platform.services.IPlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "fabric";
    }

    @Override
    public String getPlatformDisplayName() {
        return "Fabric";
    }

    @Override
    public String getLoaderVersion() {
        return this.getModVersion("fabric");
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String getModVersion(String modId) {
        try {
            Optional<ModContainer> o = FabricLoader.getInstance().getModContainer(modId);
            if (o.isPresent()) {
                ModContainer c = o.get();
                return c.getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0";
    }

    @Override
    public List<String> getLoadedModIds() {
        List<String> l = new ArrayList<>();
        for (ModContainer info : FabricLoader.getInstance().getAllMods()) {
            l.add(info.getMetadata().getId());
        }
        return l;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isOnClient() {
        return (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT);
    }

    @Override
    public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
        return KeyBindingHelper.getBoundKeyOf(keyMapping);
    }

    @Override
    public @Nullable ResourceLocation getItemKey(@NotNull Item item) {
        try {
            return BuiltInRegistries.ITEM.getKey(item);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEffectKey(@NotNull MobEffect effect) {
        try {
            return BuiltInRegistries.MOB_EFFECT.getKey(effect);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ResourceLocation getEntityKey(@NotNull EntityType<?> type) {
        try {
            return BuiltInRegistries.ENTITY_TYPE.getKey(type);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
