package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class StringCustomizationItem extends CustomizationItemBase {
	
	public float scale = 1.0F;
	public boolean shadow = false;
	public boolean centered = false;
	
	public StringCustomizationItem(PropertiesSection item) {
		super(item);

		if ((this.action != null) && this.action.equalsIgnoreCase("addtext")) {
			this.value = item.getEntryValue("value");
			if (this.value != null) {
				this.value = MenuCustomization.convertString(this.value);
			}
			
			String sh = item.getEntryValue("shadow");
			if ((sh != null)) {
				if (sh.equalsIgnoreCase("true")) {
					this.shadow = true;
				}
			}

			String ce = item.getEntryValue("centered");
			if ((ce != null)) {
				if (ce.equalsIgnoreCase("true")) {
					this.centered = true;
				}
			}
			
			String sc = item.getEntryValue("scale");
			if ((sc != null) && MathUtils.isFloat(sc)) {
				this.scale = Float.parseFloat(sc);
			}
		}
	}

	public void render(MatrixStack matrix, Screen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}
		
		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		TextRenderer font = MinecraftClient.getInstance().textRenderer;

		RenderSystem.enableBlend();
		matrix.push();
		matrix.scale(this.scale, this.scale, this.scale);
		if (this.shadow) {
			//drawStringWithShadow
			font.drawWithShadow(matrix, "§f" + this.value, x, y, 0);
		} else {
			//drawString
			font.draw(matrix, "§f" + this.value, x, y, 0);
		}
		matrix.pop();
		RenderSystem.disableBlend();
	}

	@Override
	public int getPosX(Screen menu) {
		int x = super.getPosX(menu);
		if (this.centered) {
			x -= (int) ((MinecraftClient.getInstance().textRenderer.getWidth(this.value) / 2) * this.scale);
		}
		x = (int)(x / this.scale);
		return x;
	}

	@Override
	public int getPosY(Screen menu) {
		return (int) (super.getPosY(menu) / this.scale);
	}
	
	@Override
	public StringCustomizationItem clone() {
		StringCustomizationItem item = new StringCustomizationItem(new PropertiesSection(""));
		item.centered = this.centered;
		item.height = this.height;
		item.orientation = this.orientation;
		item.posX = this.posX;
		item.posY = this.posY;
		item.scale = this.scale;
		item.shadow = this.shadow;
		item.value = this.value;
		item.width = this.width;
		item.action = this.action;
		return item;
	}

}
