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

package dev.lambdaurora.aurorasdeco.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.lambdaurora.aurorasdeco.block.entity.SignPostBlockEntity;
import dev.lambdaurora.aurorasdeco.client.renderer.SignPostBlockEntityRenderer;
import dev.lambdaurora.aurorasdeco.registry.AurorasDecoPackets;
import dev.lambdaurora.aurorasdeco.util.ColorUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Optional;

/**
 * Represents the sign post editor screen.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@Environment(EnvType.CLIENT)
public class SignPostEditScreen extends Screen {
	private static final OrderedText END_CURSOR = OrderedText.styledForwardsVisitedString("_", Style.EMPTY);
	private final SignPostBlockEntity signPost;
	private int ticksSinceOpened;
	private int currentRow;
	private SelectionManager selectionManager;
	private final String[] text;

	public SignPostEditScreen(SignPostBlockEntity signPost) {
		super(Text.translatable("sign.edit"));
		var upSign = signPost.getUp();
		var downSign = signPost.getDown();
		this.text = new String[]{
				upSign == null ? "" : upSign.getText().getString(),
				downSign == null ? "" : downSign.getText().getString()
		};
		this.signPost = signPost;
		this.currentRow = this.validateRow(0);
	}

	@Override
	protected void init() {
		this.addDrawableChild(
				ButtonWidget.builder(ScreenTexts.DONE, button -> this.finishEditing())
						.position(this.width / 2 - 100, this.height / 4 + 120)
						.size(200, 20)
						.build()
		);
		this.selectionManager = new SelectionManager(() -> this.text[this.currentRow], text -> this.text[this.currentRow] = text,
				SelectionManager.makeClipboardGetter(this.client), SelectionManager.makeClipboardSetter(this.client),
				text -> this.client.textRenderer.getWidth(text) <= 90);
	}

	@Override
	public void removed() {
		if (this.signPost.getType().supports(this.signPost.getCachedState())) {
			byte flags = 0;
			if (this.signPost.getUp() != null) {
				flags |= 1;
			}
			if (this.signPost.getDown() != null) {
				flags |= 2;
			}

			var upText = (flags & 1) == 1 ? Optional.of(this.text[0]) : Optional.<String>empty();
			var downText = (flags & 2) == 2 ? Optional.of(this.text[1]) : Optional.<String>empty();

			ClientPlayNetworking.send(new AurorasDecoPackets.SignPostSetTextPayload(this.signPost.getPos(), flags, upText, downText));
		}
	}

	@Override
	public void tick() {
		++this.ticksSinceOpened;
		if (!this.signPost.getType().supports(this.signPost.getCachedState())) {
			this.finishEditing();
		}
	}

	private void finishEditing() {
		this.signPost.markDirty();
		this.client.setScreen(null);
	}

	@Override
	public void close() {
		this.finishEditing();
	}

	private int validateRow(int row) {
		row = MathHelper.clamp(row, 0, 1);

		if (row == 0 && this.signPost.getUp() == null)
			row = 1;

		if (row == 1 && this.signPost.getDown() == null)
			row = 0;

		return row;
	}

	/* Input */

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_UP) {
			this.currentRow = this.validateRow(this.currentRow - 1);
			this.selectionManager.putCursorAtEnd();
			return true;
		} else if (keyCode != GLFW.GLFW_KEY_DOWN && keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER) {
			return this.selectionManager.handleSpecialKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
		} else {
			this.currentRow = this.validateRow(this.currentRow + 1);
			this.selectionManager.putCursorAtEnd();
			return true;
		}
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		this.selectionManager.insert(chr);
		return true;
	}

	/* Rendering */

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
		MatrixStack matrices = graphics.getMatrices();
		var upData = this.signPost.getUp();
		var downData = this.signPost.getDown();

		DiffuseLighting.disableGuiDepthLighting();
		this.renderBackground(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 40, ColorUtil.TEXT_COLOR);
		matrices.push();
		matrices.translate(this.width / 2.f, 0.0, 50.0);
		float f = 93.75f;
		matrices.scale(f, -f, f);
		matrices.translate(0.0, -1.3125, 0.0);

		matrices.scale(1, -1, -0.6666667f);
		var immediate = this.client.getBufferBuilders().getEntityVertexConsumers();
		if (upData != null)
			this.renderSign(upData, 0, -6 / 16.f, graphics, immediate,
					LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

		if (downData != null)
			this.renderSign(downData, 1, 3 / 16.f, graphics, immediate,
					LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

		matrices.pop();
		DiffuseLighting.enableGuiDepthLighting();
		super.render(graphics, mouseX, mouseY, delta);
	}

	private void renderSign(SignPostBlockEntity.Sign sign, int row, float yOffset,
			DrawContext graphics, VertexConsumerProvider.Immediate vertexConsumers,
			int light, int overlay) {
		MatrixStack matrices = graphics.getMatrices();
		matrices.push();

		matrices.translate(0, yOffset, 0);

		matrices.push();
		matrices.translate(0, 0, 2 / 16.0);
		if (!sign.isLeft()) {
			matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180));
		}

		matrices.translate(-2 / 16.0, -.5 / 16.0, 0);
		this.client.getItemRenderer().renderItem(null, new ItemStack(sign.getSign()), ModelTransformationMode.FIXED,
				false, matrices, vertexConsumers,
				this.signPost.getWorld(), light, overlay, 0);
		matrices.pop();

		int color = SignPostBlockEntityRenderer.getColor(sign);
		boolean glowing = sign.isGlowing();
		{
			int backgroundColor = color;
			if (glowing) {
				color = sign.getColor().getSignColor();
			}

			float h = 0.010416667f;
			matrices.translate(0.03125 * (sign.isLeft() ? -1 : 1), -0.061f, 0.046666667f);
			matrices.scale(h, h, 1);

			var rowText = this.text[row];
			if (this.textRenderer.isRightToLeft()) {
				rowText = this.textRenderer.mirror(rowText);
			}

			var text = OrderedText.styledForwardsVisitedString(rowText, Style.EMPTY);
			float x = -this.textRenderer.getWidth(text) / 2.f;
			if (glowing) {
				this.textRenderer.drawWithOutline(text, x, 0, color, backgroundColor, matrices.peek().getPositionMatrix(), vertexConsumers, light);
			} else {
				this.textRenderer.draw(text, x, 0, color, false, matrices.peek().getPositionMatrix(), vertexConsumers,
						TextRenderer.TextLayerType.NORMAL, 0, light);
			}

			vertexConsumers.draw();

			if (this.currentRow == row) {
				boolean blink = this.ticksSinceOpened / 6 % 2 == 0;
				int selectionStart = this.selectionManager.getSelectionStart();
				int selectionEnd = this.selectionManager.getSelectionEnd();

				if (selectionStart >= 0 && blink) {
					int o = this.client.textRenderer.getWidth(rowText.substring(0, Math.min(selectionStart, rowText.length())));
					int cursorX = o + (int) x;
					if (selectionStart >= rowText.length()) {
						if (glowing) {
							this.textRenderer.drawWithOutline(END_CURSOR, cursorX, 0, color, backgroundColor, matrices.peek().getPositionMatrix(), vertexConsumers, light);
						} else {
							this.textRenderer.draw(END_CURSOR, cursorX, 0, color, false, matrices.peek().getPositionMatrix(), vertexConsumers,
									TextRenderer.TextLayerType.NORMAL, 0, light);
						}
						vertexConsumers.draw();
					} else {
						if (glowing) {
							graphics.fill(cursorX - 2, -3, cursorX + 1, 10, 0xff000000 | backgroundColor);
						}
						graphics.fill(cursorX - 1, -2, cursorX, 9, 0xff000000 | color);
					}
				}

				if (selectionStart != selectionEnd) {
					var model = matrices.peek().getPositionMatrix();

					int start = Math.min(selectionStart, selectionEnd);
					int end = Math.max(selectionStart, selectionEnd);
					int v = this.client.textRenderer.getWidth(rowText.substring(0, start)) + (int) x;
					int w = this.client.textRenderer.getWidth(rowText.substring(0, end)) + (int) x;
					int startX = Math.min(v, w) - 1;
					int endX = Math.max(v, w);
					Tessellator tessellator = Tessellator.getInstance();
					RenderSystem.setShader(GameRenderer::getPositionColorProgram);
					RenderSystem.enableColorLogicOp();
					RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
					BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
					buffer.vertex(model, startX, 9.f, 0.f).color(0, 0, 255, 255);
					buffer.vertex(model, endX, 9.f, 0.f).color(0, 0, 255, 255);
					buffer.vertex(model, endX, -2.f, 0.f).color(0, 0, 255, 255);
					buffer.vertex(model, startX, -2.f, 0.f).color(0, 0, 255, 255);
					BufferRenderer.draw(buffer.end());
					RenderSystem.disableColorLogicOp();
				}
			}
		}
		matrices.pop();
	}
}
