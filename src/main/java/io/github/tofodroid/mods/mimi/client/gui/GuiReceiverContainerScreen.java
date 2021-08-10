package io.github.tofodroid.mods.mimi.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;

import io.github.tofodroid.mods.mimi.common.container.ContainerReceiver;
import io.github.tofodroid.mods.mimi.common.item.ItemMidiSwitchboard;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.text.ITextComponent;

public class GuiReceiverContainerScreen extends ASwitchboardGui<ContainerReceiver> {
    // Button Boxes
    private static final Vector2f SOURCE_SELF_BUTTON_COORDS = new Vector2f(40,114);
    private static final Vector2f SOURCE_PUBLIC_BUTTON_COORDS = new Vector2f(59,114);
    private static final Vector2f SOURCE_CLEAR_BUTTON_COORDS = new Vector2f(78,114);
    private static final Vector2f NOTE_LETTER_BUTTON_COORDS = new Vector2f(14,151);
    private static final Vector2f NOTE_OCTAVE_BUTTON_COORDS = new Vector2f(33,151);
    private static final Vector2f NOTE_INVERT_BUTTON_COORDS = new Vector2f(97,151);
    private static final Vector2f ALL_MIDI_BUTTON_COORDS = new Vector2f(131,118);
    private static final Vector2f GEN_MIDI_BUTTON_COORDS = new Vector2f(150,118);
    private static final Vector2f CLEAR_MIDI_BUTTON_COORDS = new Vector2f(131,144);
    
    public GuiReceiverContainerScreen(ContainerReceiver container, PlayerInventory inv, ITextComponent textComponent) {
        super(container, inv, 311, 180, 311, "textures/gui/container_receiver.png", textComponent);
    }

    @Override
    public boolean mouseReleased(double dmouseX, double dmouseY, int button) {
        int imouseX = (int)Math.round(dmouseX);
        int imouseY = (int)Math.round(dmouseY);
        
		if(selectedSwitchboardStack != null) {
			if(clickedBox(imouseX, imouseY, SOURCE_SELF_BUTTON_COORDS)) {
				this.setSelfSource();
			} else if(clickedBox(imouseX, imouseY, SOURCE_PUBLIC_BUTTON_COORDS)) {
				this.setPublicSource();
			} else if(clickedBox(imouseX, imouseY, SOURCE_CLEAR_BUTTON_COORDS)) {
				this.clearSource();
			} else if(clickedBox(imouseX, imouseY, NOTE_LETTER_BUTTON_COORDS)) {
				this.shiftFilterNoteLetter();
			} else if(clickedBox(imouseX, imouseY, NOTE_OCTAVE_BUTTON_COORDS)) {
				this.shiftFilterNoteOctave();
			} else if(clickedBox(imouseX, imouseY, NOTE_INVERT_BUTTON_COORDS)) {
				this.toggleInvertFilterNote();
			} else if(clickedBox(imouseX, imouseY, CLEAR_MIDI_BUTTON_COORDS)) {
				this.clearChannels();
			} else if(clickedBox(imouseX, imouseY, ALL_MIDI_BUTTON_COORDS)) {
				this.enableAllChannels();
			} else {
				// Individual Midi Channel Buttons
				for(int i = 0; i < 16; i++) {
					Vector2f buttonCoords = new Vector2f(
						GEN_MIDI_BUTTON_COORDS.x + (i % 8) * 19,
						GEN_MIDI_BUTTON_COORDS.y + (i / 8) * 27
					);

					if(clickedBox(imouseX, imouseY, buttonCoords)) {
						this.toggleChannel(i);
						return super.mouseClicked(dmouseX, dmouseY, button);
					}
				}
			}
		}

        return super.mouseReleased(dmouseX, dmouseY, button);
    }

    @Override
    protected MatrixStack renderGraphics(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // Set Texture
        Minecraft.getInstance().getTextureManager().bindTexture(guiTexture);

        // GUI Background
        blit(matrixStack, this.guiLeft, this.guiTop, this.getBlitOffset(), 0, 0, this.xSize, this.ySize, TEXTURE_SIZE, TEXTURE_SIZE);

		if(this.selectedSwitchboardStack != null) {
			// Channel Output Status Lights
			SortedArraySet<Byte> acceptedChannels = ItemMidiSwitchboard.getEnabledChannelsSet(this.selectedSwitchboardStack);
			if(acceptedChannels != null && !acceptedChannels.isEmpty()) {
				for(Byte channelId : acceptedChannels) {
                    blit(matrixStack, this.guiLeft + 156 + 19 * (channelId % 8), this.guiTop + 136 + (channelId / 8) * 26, this.getBlitOffset(), 0, 181, 3, 3, TEXTURE_SIZE, TEXTURE_SIZE);
                }
			}
			
        	// Invert Status Light
			if(ItemMidiSwitchboard.getInvertNoteOct(selectedSwitchboardStack)) {
				blit(matrixStack, this.guiLeft + 116, this.guiTop + 157, this.getBlitOffset(), 0, 181, 3, 3, TEXTURE_SIZE, TEXTURE_SIZE);
			}
		}

        return matrixStack;
    }

    @Override
    protected MatrixStack renderText(MatrixStack matrixStack, int mouseX, int mouseY) {
		if(this.selectedSwitchboardStack != null) {
			// MIDI Source Name
			font.drawString(matrixStack, this.selectedSourceName.length() <= 22 ? this.selectedSourceName : this.selectedSourceName.substring(0,21) + "...", 16, 102, 0xFF00E600);
		
			// Filter Note
			font.drawString(matrixStack, this.filterNoteString, 54, 155, invalidFilterNote() ? 0xFFE60000 : 0xFF00E600);
		}
       
        return matrixStack;
    }
}