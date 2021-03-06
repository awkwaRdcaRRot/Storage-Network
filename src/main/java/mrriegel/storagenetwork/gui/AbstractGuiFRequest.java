package mrriegel.storagenetwork.gui;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mezz.jei.Internal;
import mrriegel.storagenetwork.StorageNetwork;
import mrriegel.storagenetwork.config.ConfigHandler;
import mrriegel.storagenetwork.gui.fremote.ContainerFRemote;
import mrriegel.storagenetwork.gui.frequest.ContainerFRequest;
import mrriegel.storagenetwork.helper.Util;
import mrriegel.storagenetwork.network.FRequestMessage;
import mrriegel.storagenetwork.network.PacketHandler;
import mrriegel.storagenetwork.network.SortMessage;
import mrriegel.storagenetwork.tile.TileFRequest;
import mrriegel.storagenetwork.tile.TileRequest.Sort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.mojang.realmsclient.gui.ChatFormatting;

public abstract class AbstractGuiFRequest extends MyGuiContainer {
	protected ResourceLocation texture = new ResourceLocation(StorageNetwork.MODID + ":textures/gui/frequest.png");
	protected int page = 1, maxPage = 1;
	public List<FluidStack> fluids;
	protected Fluid over;
	protected GuiTextField searchBar;
	protected Button direction, sort, left, right;
	protected List<FluidSlot> slots;

	public AbstractGuiFRequest(Container inventorySlotsIn) {
		super(inventorySlotsIn);
		this.xSize = 176;
		this.ySize = 256;
		this.fluids = Lists.newArrayList();
		PacketHandler.INSTANCE.sendToServer(new FRequestMessage(0, null));
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);
		searchBar = new GuiTextField(0, fontRendererObj, guiLeft + 81, guiTop + 96 + 64, 85, fontRendererObj.FONT_HEIGHT);
		searchBar.setMaxStringLength(30);
		searchBar.setEnableBackgroundDrawing(false);
		searchBar.setVisible(true);
		searchBar.setTextColor(16777215);
		direction = new Button(0, guiLeft + 7, guiTop + 93 + 64, "");
		buttonList.add(direction);
		sort = new Button(1, guiLeft + 21, guiTop + 93 + 64, "");
		buttonList.add(sort);
		left = new Button(2, guiLeft + 44, guiTop + 93 + 64, "<");
		buttonList.add(left);
		right = new Button(3, guiLeft + 58, guiTop + 93 + 64, ">");
		buttonList.add(right);
	}

	protected abstract boolean getDownwards();

	protected abstract void setDownwards(boolean d);

	protected abstract Sort getSort();

	protected abstract void setSort(Sort s);

	protected abstract BlockPos getPos();

	protected abstract BlockPos getMaster();

	protected abstract int getDim();

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(texture);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
		String search = searchBar.getText();
		List<FluidStack> tmp = search.equals("") ? Lists.newArrayList(fluids) : Lists.<FluidStack> newArrayList();
		if (!search.equals("")) {
			for (FluidStack s : fluids)
				if (search.startsWith("@")) {
					String name = Util.getModNameForFluid(s.getFluid());
					if (name.toLowerCase().contains(search.toLowerCase().substring(1)))
						tmp.add(s);
				} else {
					if (s.getLocalizedName().toLowerCase().contains(search.toLowerCase()))
						tmp.add(s);
				}
		}
		ItemStack a = null, b = null;
		if (inventorySlots instanceof ContainerFRemote) {
			a = ((ContainerFRemote) inventorySlots).inv.getStackInSlot(0);
			b = ((ContainerFRemote) inventorySlots).inv.getStackInSlot(1);
		} else if (inventorySlots instanceof ContainerFRequest) {
			TileFRequest tile = ((ContainerFRequest) inventorySlots).tile;
			a = tile.fill;
			b = tile.drain;
		}
		if (a == null)
			this.drawTexturedModalRect(i + 10, j + 139, 176, 22, 12, 14);
		if (b == null)
			this.drawTexturedModalRect(i + 46, j + 139, 176 + 12, 22, 12, 14);
		Collections.sort(tmp, new Comparator<FluidStack>() {
			int mul = getDownwards() ? -1 : 1;

			@Override
			public int compare(FluidStack o2, FluidStack o1) {
				switch (getSort()) {
				case AMOUNT:
					return Integer.compare(o1.amount, o2.amount) * mul;
				case NAME:
					return o2.getLocalizedName().compareToIgnoreCase(o1.getLocalizedName()) * mul;
				case MOD:
					return Util.getModNameForFluid(o2.getFluid()).compareToIgnoreCase(Util.getModNameForFluid(o1.getFluid())) * mul;
				}
				return 0;
			}
		});
		maxPage = tmp.size() / 48;
		if (tmp.size() % 48 != 0)
			maxPage++;
		if (maxPage < 1)
			maxPage = 1;
		if (page < 1)
			page = 1;
		if (page > maxPage)
			page = maxPage;
		if (page == 1) {
			left.visible = false;
			left.enabled = false;
		} else {
			left.visible = true;
			left.enabled = true;
		}
		if (page == maxPage) {
			right.visible = false;
			right.enabled = false;
		} else {
			right.visible = true;
			right.enabled = true;
		}
		searchBar.drawTextBox();
		slots = Lists.newArrayList();
		int index = (page - 1) * 48;
		for (int jj = 0; jj < 7; jj++) {
			for (int ii = 0; ii < 8; ii++) {
				int in = index;
				if (in >= tmp.size())
					break;
				slots.add(new FluidSlot(tmp.get(in).getFluid(), guiLeft + 10 + ii * 20, guiTop + 10 + jj * 20, tmp.get(in).amount, guiLeft, guiTop, true, true, ConfigHandler.smallFont, true));
				index++;
			}
		}
		for (FluidSlot s : slots) {
			s.drawSlot(mouseX, mouseY);
		}
		for (FluidSlot s : slots) {
			if (s.isMouseOverSlot(mouseX, mouseY)) {
				over = s.fluid;
				break;
			} else
				over = null;
		}

	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		for (FluidSlot s : slots) {
			s.drawTooltip(mouseX, mouseY);
		}
		if (isPointInRegion(81, 96 + 64, 85, fontRendererObj.FONT_HEIGHT, mouseX, mouseY)) {
			List<String> lis = Lists.newArrayList("Right click to clear the search bar.");
			if (!isShiftKeyDown())
				lis.add(ChatFormatting.ITALIC + "Hold shift for more information.");
			else {
				lis.add("Prefix @: Search for mod.");
			}
			drawHoveringText(lis, mouseX - guiLeft, mouseY - guiTop);
		}
		if (sort.isMouseOver())
			drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.req.tooltip_" + getSort().toString())), mouseX - guiLeft, mouseY - guiTop);
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (button.id == 2 && page > 1)
			page--;
		if (button.id == 3 && page < maxPage)
			page++;
		if (button.id == 0)
			setDownwards(!getDownwards());
		else if (button.id == 1)
			setSort(getSort().next());
		PacketHandler.INSTANCE.sendToServer(new SortMessage(getPos(), getDownwards(), getSort()));
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		searchBar.setFocused(false);
		if (isPointInRegion(81, 96 + 64, 85, fontRendererObj.FONT_HEIGHT, mouseX, mouseY)) {
			if (mouseButton == 1)
				searchBar.setText("");
			searchBar.setFocused(true);
		} else if (over != null && (mouseButton == 0 || mouseButton == 1) && mc.thePlayer.inventory.getItemStack() == null) {
			PacketHandler.INSTANCE.sendToServer(new FRequestMessage(mouseButton, over));
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (!this.checkHotbarKeys(keyCode)) {
			Keyboard.enableRepeatEvents(true);
			if (over != null && ConfigHandler.jeiLoaded && (keyCode == Keyboard.KEY_R || keyCode == Keyboard.KEY_U)) {
				if (keyCode == Keyboard.KEY_R)
					Internal.getRuntime().getRecipesGui().showRecipes(over);
				else
					Internal.getRuntime().getRecipesGui().showUses(over);
			} else if (this.searchBar.textboxKeyTyped(typedChar, keyCode)) {
				PacketHandler.INSTANCE.sendToServer(new FRequestMessage(0, null));
			} else {
				super.keyTyped(typedChar, keyCode);
			}
		}
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int i = Mouse.getX() * this.width / this.mc.displayWidth;
		int j = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
		if (i > (guiLeft + 7) && i < (guiLeft + xSize - 7) && j > (guiTop + 7) && j < (guiTop + 90)) {
			int mouse = Mouse.getEventDWheel();
			if (mouse == 0)
				return;
			if (mouse > 0 && page > 1)
				page--;
			if (mouse < 0 && page < maxPage)
				page++;
		}
	}

	class Button extends GuiButton {

		public Button(int p_i1021_1_, int p_i1021_2_, int p_i1021_3_, String p_i1021_6_) {
			super(p_i1021_1_, p_i1021_2_, p_i1021_3_, 14, 14, p_i1021_6_);
		}

		@Override
		public void drawButton(Minecraft p_146112_1_, int p_146112_2_, int p_146112_3_) {
			if (this.visible) {
				FontRenderer fontrenderer = p_146112_1_.fontRendererObj;
				p_146112_1_.getTextureManager().bindTexture(texture);
				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
				this.hovered = p_146112_2_ >= this.xPosition && p_146112_3_ >= this.yPosition && p_146112_2_ < this.xPosition + this.width && p_146112_3_ < this.yPosition + this.height;
				int k = this.getHoverState(this.hovered);
				GlStateManager.enableBlend();
				GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
				GlStateManager.blendFunc(770, 771);
				this.drawTexturedModalRect(this.xPosition, this.yPosition, 162 + 14 * k, 0, 14, 14);
				if (id == 0) {
					this.drawTexturedModalRect(this.xPosition + 4, this.yPosition + 3, 176 + (getDownwards() ? 6 : 0), 14, 6, 8);
				}
				if (id == 1) {
					this.drawTexturedModalRect(this.xPosition + 4, this.yPosition + 3, 188 + (getSort() == Sort.AMOUNT ? 6 : getSort() == Sort.MOD ? 12 : 0), 14, 6, 8);

				}
				this.mouseDragged(p_146112_1_, p_146112_2_, p_146112_3_);
				int l = 14737632;

				if (packedFGColour != 0) {
					l = packedFGColour;
				} else if (!this.enabled) {
					l = 10526880;
				} else if (this.hovered) {
					l = 16777120;
				}

				this.drawCenteredString(fontrenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, l);
			}
		}
	}

}
