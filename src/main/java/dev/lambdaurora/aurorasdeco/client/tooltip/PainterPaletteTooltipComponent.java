/*
 * Copyright (c) 2021 LambdAurora <email@lambdaurora.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.aurorasdeco.client.tooltip;

import dev.lambdaurora.aurorasdeco.blackboard.BlackboardColor;
import dev.lambdaurora.aurorasdeco.item.PainterPaletteItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Represents the painter's palette tooltip component.
 *
 * @author LambdAurora
 * @version 1.0.0-beta.13
 * @since 1.0.0-beta.6
 */
@Environment(EnvType.CLIENT)
public class PainterPaletteTooltipComponent implements TooltipComponent {
	// Matches vanilla's own BundleTooltipComponent.SlotSprite.SLOT, which is no longer exposed publicly.
	private static final Identifier SLOT_SPRITE = Identifier.ofVanilla("container/bundle/slot");

	private final PainterPaletteItem.PainterPaletteInventory inventory;
	private final Text selectedToolText;

	public PainterPaletteTooltipComponent(PainterPaletteItem.PainterPaletteInventory inventory) {
		this.inventory = inventory;
		var enabledFlags = MinecraftClient.getInstance().world.getEnabledFeatures();
		this.selectedToolText = PainterPaletteItem.getSelectedToolMessage(inventory, enabledFlags).formatted(Formatting.GRAY);
	}

	@Override
	public int getHeight() {
		int height = 12;
		ItemStack primaryColorStack = this.inventory.getSelectedColor();

		if (primaryColorStack.isEmpty()) return height;

		return height + 24;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		int width = textRenderer.getWidth(this.selectedToolText);

		ItemStack primaryColorStack = this.inventory.getSelectedColor();

		if (primaryColorStack.isEmpty()) return width;

		return Math.max(width, 18 * 5 + 2);
	}

	@Override
	public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix4f, VertexConsumerProvider.Immediate immediate) {
		textRenderer.draw(
				this.selectedToolText, x, y, 0xffffffff, true, matrix4f, immediate, TextRenderer.TextLayerType.NORMAL,
				0, LightmapTextureManager.MAX_LIGHT_COORDINATE
		);
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext graphics) {
		ItemStack primaryColorStack = this.inventory.getSelectedColor();

		if (primaryColorStack.isEmpty()) return;

		MatrixStack matrices = graphics.getMatrices();
		ItemStack previousColorStack = this.inventory.getPreviousColorStack();
		ItemStack nextColorStack = this.inventory.getNextColorStack();

		matrices.push();
		y += 12;

		matrices.translate(x, y, 0);
		this.drawSlot(graphics, textRenderer, previousColorStack, inventory.getSlotOf(previousColorStack), true, false);

		matrices.translate(18, 0, 0);
		this.drawSlot(graphics, textRenderer, primaryColorStack, inventory.getSelectedColorSlot(), false, false);
		HandledScreen.drawSlotHighlight(graphics, 2, 2, 0);

		matrices.translate(18, 0, 0);
		this.drawSlot(graphics, textRenderer, nextColorStack, inventory.getSlotOf(nextColorStack), false, true);

		matrices.pop();
	}

	private void drawSlot(
			DrawContext graphics, TextRenderer textRenderer, ItemStack stack,
			int index, boolean start, boolean end
	) {
		graphics.drawGuiTexture(SLOT_SPRITE, 0, 0, 18, 20);

		if (!stack.isEmpty()) {
			graphics.drawItem(stack, 2, 2, index);
			this.drawColorOverlay(graphics, stack);
		}
	}

	private void drawColorOverlay(DrawContext graphics, ItemStack stack) {
		var color = BlackboardColor.fromItem(stack.getItem());
		if (color != null) {
			graphics.getMatrices().push();
			graphics.getMatrices().translate(14, 14, 210);
			graphics.fill(0, 0, 4, 4, color.getColor());
			graphics.getMatrices().pop();
		}
	}
}
