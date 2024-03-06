package top.srcres.mods.creativepagejump.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.srcres.mods.creativepagejump.Config;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow
    private CreativeTabsScreenPage currentPage;
    @Shadow
    private Slot destroyItemSlot;
    @Shadow
    private static CreativeModeTab selectedTab;
    @Shadow
    @Final
    private static Component TRASH_SLOT_TOOLTIP;
    @Shadow
    @Final
    private List<CreativeTabsScreenPage> pages;

    @Unique
    private EditBox cpj$pageNumEditBox;
    @Unique
    private MutableComponent cpj$pageNumSuffixComponent;
    @Unique
    private int cpj$totalLength = 0;

    public CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Shadow
    protected boolean checkTabHovering(GuiGraphics pGuiGraphics, CreativeModeTab pCreativeModeTab, int pMouseX, int pMouseY) {
        return false;
    }

    /**
     * @author src_resources
     * @reason Overwrites the original method in order to update the content within {@link #cpj$pageNumEditBox}
     *         whenever the current page changes.
     */
    @Overwrite(remap = false)
    public void setCurrentPage(CreativeTabsScreenPage currentPage) {
        int pageIndex = this.pages.indexOf(currentPage);
        if (pageIndex != -1) {
            this.cpj$pageNumEditBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
            // *Pay attention* to index converting as noted within #cpj$pageNumEditBoxUpdated.
            this.cpj$pageNumEditBox.value = Integer.toString(pageIndex + 1);
        }
        this.cpj$setCurrentPage(currentPage);
    }

    @Unique
    private void cpj$setCurrentPage(CreativeTabsScreenPage currentPage) {
        this.currentPage = currentPage;
    }

    @Inject(method = "init", at = @At("RETURN"))
    protected void cpj$init(CallbackInfo ci) {
        if (this.minecraft.gameMode.hasInfiniteItems()) {
            if (this.pages.size() > 1) {
                this.cpj$pageNumSuffixComponent = Component.literal(String.format(" / %d", this.pages.size()));
                this.cpj$totalLength = Config.pageNumEditBoxLength + this.font.width(this.cpj$pageNumSuffixComponent);
                this.cpj$pageNumEditBox = new EditBox(this.font, this.leftPos + this.imageWidth / 2 - this.cpj$totalLength / 2, this.topPos - 50,
                        Config.pageNumEditBoxLength, 20, this.cpj$pageNumEditBox, Component.literal(""));
                this.cpj$pageNumEditBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                int currentPageIndex = this.pages.indexOf(this.currentPage);
                this.cpj$pageNumEditBox.value = Integer.toString(currentPageIndex + 1);
                this.cpj$pageNumEditBox.setResponder(this::cpj$pageNumEditBoxUpdated);
                this.addWidget(this.cpj$pageNumEditBox);
            }
        }
    }

    /**
     * @author src_resources
     * @reason Overwrote the original method to implement the feature of showing the page number input box above
     *         the creative inventory for the user to write in.
     */
    @Overwrite
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        for (CreativeModeTab creativemodetab : this.currentPage.getVisibleTabs()) {
            if (this.checkTabHovering(pGuiGraphics, creativemodetab, pMouseX, pMouseY)) {
                break;
            }
        }

        if (this.destroyItemSlot != null && selectedTab.getType() == CreativeModeTab.Type.INVENTORY && this.isHovering(this.destroyItemSlot.x, this.destroyItemSlot.y, 16, 16, (double)pMouseX, (double)pMouseY)) {
            pGuiGraphics.renderTooltip(this.font, TRASH_SLOT_TOOLTIP, pMouseX, pMouseY);
        }

        if (this.pages.size() > 1) {
            this.cpj$pageNumEditBox.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
            pGuiGraphics.drawString(this.font, this.cpj$pageNumSuffixComponent.getVisualOrderText(),
                    this.leftPos + this.imageWidth / 2 - this.cpj$totalLength / 2 + Config.pageNumEditBoxLength,
                    this.topPos - 44, -1);
            pGuiGraphics.pose().popPose();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    protected void charTypedTabSearch(char pCodePoint, int pModifiers, CallbackInfoReturnable<Boolean> cir) {
        // The callback has to be invoked manually since the origin listener implementation has been overwritten
        // by CreativeModeInventoryScreen within method CreativeModeInventoryScreen#charTyped.
        if (this.cpj$pageNumEditBox != null && this.cpj$pageNumEditBox.charTyped(pCodePoint, pModifiers)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    protected void keyPressedTabSearch(int pKeyCode, int pScanCode, int pModifiers, CallbackInfoReturnable<Boolean> cir) {
        // Need to replace vanilla key pressing logic when editing tab search EditBox.
        if (this.cpj$pageNumEditBox != null && this.cpj$pageNumEditBox.isFocused()) {
            // Handle Esc quitting screen logic at first, otherwise the player
            // may feel weird for being unable to close the screen.
            if (pKeyCode == 256 && this.shouldCloseOnEsc()) {
                this.onClose();
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(this.cpj$pageNumEditBox.keyPressed(pKeyCode, pScanCode, pModifiers));
            }
            cir.cancel();
        }
    }

    @Unique
    private void cpj$pageNumEditBoxUpdated(String pNewText) {
        if (pNewText.isEmpty()) {
            this.cpj$pageNumEditBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
        } else {
            int newPage = this.cpj$validateAndParsePageNumber(pNewText);
            if (newPage == -1) {
                this.cpj$pageNumEditBox.setTextColor(0xff0000);
            } else {
                this.cpj$pageNumEditBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                // *Note* that the index from input begins at 1, while the index within Java List begins at 0.
                // Hence, minus 1 is *required*.
                this.cpj$setCurrentPage(this.pages.get(newPage - 1));
            }
        }
    }

    /** Returns -1 whenever numStr is invalid, either not an integer number or out of total page indexes. */
    @Unique
    private int cpj$validateAndParsePageNumber(String numStr) {
        int newPage;
        try {
            newPage = Integer.parseInt(numStr);
        } catch (NumberFormatException ex) {
            return -1;
        }
        if (newPage < 1 || newPage > this.pages.size()) {
            return -1;
        }
        return newPage;
    }
}
