package io.github.tofodroid.mods.mimi.client.gui;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.github.tofodroid.mods.mimi.common.instruments.InstrumentDataUtil;
import io.github.tofodroid.mods.mimi.common.item.ItemInstrument;
import io.github.tofodroid.mods.mimi.common.network.InstrumentDataUpdatePacket;
import io.github.tofodroid.mods.mimi.common.network.MidiNoteOffPacket;
import io.github.tofodroid.mods.mimi.common.network.MidiNoteOnPacket;
import io.github.tofodroid.mods.mimi.common.network.NetworkManager;
import io.github.tofodroid.mods.mimi.common.tile.TileInstrument;
import io.github.tofodroid.mods.mimi.util.PlayerNameUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import org.lwjgl.glfw.GLFW;

public class GuiInstrument<T> extends Screen {
    // Texture
    private static final ResourceLocation guiTexture = new ResourceLocation(MIMIMod.MODID, "textures/gui/gui_instrument.png");
    private static final Integer GUI_WIDTH = 368;
    private static final Integer GUI_HEIGHT = 246;
    private static final Integer NOTE_OFFSET_X = 11;
    private static final Integer NOTE_OFFSET_Y = 109;
    private static final Integer TEXTURE_SIZE = 402;
    private static final Integer NOTE_WIDTH = 14;
    private static final Integer BUTTON_SIZE = 15;

    // GUI
    private static final Vector2f MAESTRO_MIDI_BUTTON_COORDS = new Vector2f(217,39);
    private static final Vector2f MAESTRO_SELF_BUTTON_COORDS = new Vector2f(236,39);
    private static final Vector2f MAESTRO_CLEAR_BUTTON_COORDS = new Vector2f(255,39);
    private static final Vector2f TOGGLE_MIDI_BUTTON_COORDS = new Vector2f(332,39);
    private static final Vector2f CLEAR_MIDI_BUTTON_COORDS = new Vector2f(15,79);
    private static final Vector2f ALL_MIDI_BUTTON_COORDS = new Vector2f(338,79);
    private static final Vector2f GEN_MIDI_BUTTON_COORDS = new Vector2f(34,79);
    private static final Vector2f GEN_SHIFT_BUTTON_COORDS = new Vector2f(327,124);
    
    private Integer startX;
    private Integer startY;

    // MIDI
    private static final Integer KEYBOARD_START_NOTE = 21;

    // Mouse
    private Byte mouseNote = null;

    //Keyboard
    private static final Integer VISIBLE_NOTES = 44;
    private Integer visibleNoteShift = KEYBOARD_START_NOTE;
    private String minNoteString = "C3";
    private ConcurrentHashMap<Byte, Instant> heldNotes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Byte, Instant> releasedNotes = new ConcurrentHashMap<>();
    private final Integer ACCENT_LEFT_MIN_SCAN = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_S);
    private final Integer ACCENT_LEFT_MAX_SCAN = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_APOSTROPHE);
    private final Integer ACCENT_RIGHT_MIN_SCAN = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_1);
    private final Integer ACCENT_RIGHT_MAX_SCAN = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_EQUAL);
    private final Integer NOTE_LEFT_MIN_SCAN = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_Z);
    private final Integer NOTE_LEFT_MAX_SCAN = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_SLASH);
    private final Integer NOTE_RIGHT_MIN_SCAN = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_Q);
    private final Integer NOTE_RIGHT_MAX_SCAN = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_RIGHT_BRACKET);

    // Data
    private final Byte instrumentId;
    private final T instrumentData;
    private final InstrumentDataUtil<T> instrumentUtil;
    private final PlayerEntity player;
    private final World world;
    private String selectedMaestroName = "None";

    public GuiInstrument(PlayerEntity player, World worldIn, Byte instrumentId, T instrumentData, InstrumentDataUtil<T> instrumentUtil) {
        super(new TranslationTextComponent("item.MIMIMod.gui_instrument"));
        this.instrumentId = instrumentId;
        this.instrumentData = instrumentData;
        this.instrumentUtil = instrumentUtil;
        this.player = player;
        this.world = worldIn;
        this.refreshMaestroName();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init() {
        startX = (this.width - GUI_WIDTH) / 2;
        startY = Math.round((this.height - GUI_HEIGHT) / 1.25f);
        this.heldNotes.clear();
        this.releasedNotes.clear();
    }
    
    @Override
    public void closeScreen() {
        super.closeScreen();
        this.allNotesOff();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        matrixStack = renderGraphics(matrixStack);
        matrixStack = renderText(matrixStack);
    }

    @Override
    public boolean mouseClicked(double dmouseX, double dmouseY, int mouseButton) {
        int imouseX = (int)Math.round(dmouseX);
        int imouseY = (int)Math.round(dmouseY);
        int firstNoteX = startX + NOTE_OFFSET_X;
        int firstNoteY = startY + NOTE_OFFSET_Y;
        int relativeMouseX = imouseX - firstNoteX;
        int relativeMouseY = imouseY - firstNoteY;

        // Check if click position is within keyboard
        if(relativeMouseX >= 0 && relativeMouseY >= 0 && imouseX < (startX + GUI_WIDTH - 51) && imouseY < (startY + GUI_HEIGHT - 11)) {
            Byte midiNote = null;
            
            if(relativeMouseY <= 84) {
                midiNote = keyNumToMidiNote(2*((relativeMouseX + NOTE_WIDTH/2) / NOTE_WIDTH));
            }

            if(midiNote == null) {
                midiNote = keyNumToMidiNote(2*(relativeMouseX / NOTE_WIDTH)+1);
            }
            
            if(midiNote != null) {
                this.mouseNote = midiNote;
                this.onGuiNotePress(midiNote, Byte.MAX_VALUE);
            }
        }

        // Shift Buttons
        for(int i = 0; i < 4; i++) {
            Vector2f buttonCoords = new Vector2f(
                GEN_SHIFT_BUTTON_COORDS.x,
                GEN_SHIFT_BUTTON_COORDS.y + i * 19
            );

            if(clickedBox(imouseX, imouseY, buttonCoords)) {
                switch(i) {
                    case 0:
                        this.shiftVisibleNotes(true, 1);
                        break;
                    case 1:
                        this.shiftVisibleNotes(false, 1);
                        break;
                    case 2:
                        this.shiftVisibleNotes(true, 7);
                        break;
                    case 3:
                        this.shiftVisibleNotes(false, 7);
                        break;
                }
            }
        }

        // Midi Buttons
        if(instrumentData != null) {
            if(clickedBox(imouseX, imouseY, MAESTRO_MIDI_BUTTON_COORDS)) {
                // Link MIDI Device Button
                instrumentUtil.linkToMaestro(instrumentData, InstrumentDataUtil.MIDI_MAESTRO_ID);
                this.syncInstrumentToServer();
                this.refreshMaestroName();
                this.allNotesOff();
            } else if(clickedBox(imouseX, imouseY, MAESTRO_SELF_BUTTON_COORDS)) {
                // Link Self Button
                instrumentUtil.linkToMaestro(instrumentData, player.getUniqueID());
                this.syncInstrumentToServer();
                this.refreshMaestroName();
                this.allNotesOff();
            } else if(clickedBox(imouseX, imouseY, MAESTRO_CLEAR_BUTTON_COORDS)) {
                // Link Clear Button
                instrumentUtil.linkToMaestro(instrumentData, null);
                this.syncInstrumentToServer();
                this.refreshMaestroName();
                this.allNotesOff();
            } else if(clickedBox(imouseX, imouseY, TOGGLE_MIDI_BUTTON_COORDS)) {
                // Toggle MIDI Enabled
                instrumentUtil.toggleMidiEnabled(instrumentData);
                this.syncInstrumentToServer();
                if(!instrumentUtil.isMidiEnabled(instrumentData)) {
                    this.allNotesOff();
                }
            } else if(clickedBox(imouseX, imouseY, CLEAR_MIDI_BUTTON_COORDS)) {
                // Clear Midi Channels Button
                instrumentUtil.clearAcceptedChannels(instrumentData);
                this.syncInstrumentToServer();
                this.allNotesOff();
            } else if(clickedBox(imouseX, imouseY, ALL_MIDI_BUTTON_COORDS)) {
                // Select All Midi Channels Button
                instrumentUtil.setAcceptAllChannels(instrumentData);
                this.syncInstrumentToServer();
                this.allNotesOff();
            } else {
                // Individual Midi Channel Buttons
                for(int i = 0; i < 16; i++) {
                    Vector2f buttonCoords = new Vector2f(
                        GEN_MIDI_BUTTON_COORDS.x + i * 19,
                        GEN_MIDI_BUTTON_COORDS.y
                    );
    
                    if(clickedBox(imouseX, imouseY, buttonCoords)) {
                        instrumentUtil.toggleChannel(instrumentData, new Integer(i).byteValue());
                        this.syncInstrumentToServer();
                        this.allNotesOff();
                        return super.mouseClicked(dmouseX, dmouseY, mouseButton);
                    }
                }
            }
        }
        
        return super.mouseClicked(dmouseX, dmouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(this.mouseNote != null) {
            this.onGuiNoteRelease(mouseNote);
            this.mouseNote = null;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        
        if(keyCode == GLFW.GLFW_KEY_LEFT) {
            shiftVisibleNotes(false, 1);
        } else if(keyCode == GLFW.GLFW_KEY_RIGHT) {
            shiftVisibleNotes(true, 1);
        } else if(keyCode == GLFW.GLFW_KEY_DOWN) {
            shiftVisibleNotes(false, 7);
        } else if(keyCode == GLFW.GLFW_KEY_UP) {
            shiftVisibleNotes(true, 7);
        } else {
            Byte midiNoteNum = getMidiNoteFromScanCode(scanCode);

            if(midiNoteNum != null && !this.heldNotes.containsKey(midiNoteNum)) {
                this.onGuiNotePress(midiNoteNum, Byte.MAX_VALUE);
            }
        }
        
        return true;
    }
    
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        super.keyReleased(keyCode, scanCode, modifiers);

        Byte midiNoteNum = getMidiNoteFromScanCode(scanCode);

        if(midiNoteNum != null) {
            this.onGuiNoteRelease(midiNoteNum);
        }

        return true;
    }

    public Byte getInstrumentId() {
        return this.instrumentUtil.getInstrumentIdFromData(this.instrumentData);
    }

    // Midi Functions
    private Byte keyNumToMidiNote(Integer keyNum) {
        if(keyNum == null) {
            return null;
        }

        Byte result = null;
        Integer octaveNote = (keyNum + 2 * visibleNoteShift) % 14;
        Integer octaveNum = (keyNum + 2 * visibleNoteShift) / 14;

        if(octaveNote != 0 && octaveNote != 6) {
            octaveNote -= octaveNote > 6 ? 2 : 1;
            result = new Integer(octaveNote + 12 * octaveNum).byteValue();
        }

        return result;
    }
    
    private Integer midiNoteToKeyNum(Byte midiNote) {
        if(midiNote == null) {
            return null;
        }

        Integer octaveNote = midiNote % 12;
        Integer octaveNum = midiNote / 12;
        octaveNote += octaveNote > 4 ? 2 : 1;
        Integer result = octaveNote + 14 * (octaveNum) - 2 * visibleNoteShift;

        if(result >= 0 && result <= VISIBLE_NOTES) {
            return result;
        }
        return null;
    }

    private void shiftVisibleNotes(Boolean up, Integer amount) {
        if(up) {
            visibleNoteShift += amount;
        } else {
            visibleNoteShift -= amount;
        }

        // Clamp between 0 and 66
        visibleNoteShift = visibleNoteShift < 0 ? 0 : visibleNoteShift > 30 ? 30 : visibleNoteShift;
        
        // Set Min Note String
        Integer octaveNoteNum = visibleNoteShift % 7;
        Integer minOctave = (visibleNoteShift+2) / 7;
        this.minNoteString = noteLetterFromNum(octaveNoteNum) + minOctave.toString();
    }

    public void onMidiNoteOn(Byte channel, Byte midiNote, Byte velocity) {
        if(instrumentUtil.isMidiEnabled(instrumentData) && instrumentUtil.doesAcceptChannel(instrumentData, channel)) {
            this.onGuiNotePress(midiNote, velocity);
        }
    }
    
    public void onMidiNoteOff(Byte channel, Byte midiNote) {
        if(instrumentUtil.isMidiEnabled(instrumentData) && instrumentUtil.doesAcceptChannel(instrumentData, channel)) {
            this.onGuiNoteRelease(midiNote);
        }
    }

    public void onMidiAllNotesOff(Byte channel) {
        if(instrumentUtil.isMidiEnabled(instrumentData) && instrumentUtil.doesAcceptChannel(instrumentData, channel)) {
            this.allNotesOff();
        }
    }
    
    private void allNotesOff() {
        // Release all notes
        List<Byte> notesToRemove = new ArrayList<>(this.heldNotes.keySet());
        for(Byte note : notesToRemove) {
            this.releaseNote(note);
        }

        // Send all notes off packet
        MidiNoteOffPacket packet = new MidiNoteOffPacket(MidiNoteOffPacket.ALL_NOTES_OFF, instrumentId, player.getUniqueID());
        NetworkManager.NET_CHANNEL.sendToServer(packet);
    }

    private void onGuiNotePress(Byte midiNote, Byte velocity) {
        // hold note
        this.holdNote(midiNote, velocity);

        // send packet
        MidiNoteOnPacket packet = new MidiNoteOnPacket(midiNote, velocity, instrumentId, player.getUniqueID(), player.getPosition());
        NetworkManager.NET_CHANNEL.sendToServer(packet);
    }
    
    private void onGuiNoteRelease(Byte midiNote) {
        // release note
        this.releaseNote(midiNote);

        // send packet
        MidiNoteOffPacket packet = new MidiNoteOffPacket(midiNote, instrumentId, player.getUniqueID());
        NetworkManager.NET_CHANNEL.sendToServer(packet);
    }


    private void holdNote(Byte midiNote, Byte velocity) {
        this.releasedNotes.remove(midiNote);
        this.heldNotes.put(midiNote, Instant.now());
    }

    private void releaseNote(Byte midiNote) {
        if(this.heldNotes.remove(midiNote) != null) {
            this.releasedNotes.put(midiNote, Instant.now());
        }
    }

    // Keyboard Input Functions
    private Byte getMidiNoteFromScanCode(Integer scanCode) {
        Integer keyNum = null;

        if (scanCode >= ACCENT_LEFT_MIN_SCAN && scanCode <= ACCENT_LEFT_MAX_SCAN) {
            //Accent note - 1st row
            keyNum = scanCode - ACCENT_LEFT_MIN_SCAN + 1;
            keyNum *= 2;
        } else if(scanCode >= NOTE_LEFT_MIN_SCAN && scanCode <= NOTE_LEFT_MAX_SCAN) {
            //Primary note 1st row
            keyNum = scanCode - NOTE_LEFT_MIN_SCAN + 1;
            keyNum += (keyNum-1);
        } else if(scanCode >= ACCENT_RIGHT_MIN_SCAN && scanCode <= ACCENT_RIGHT_MAX_SCAN) {
            //Accent note - 2nd row
            keyNum = scanCode - ACCENT_RIGHT_MIN_SCAN + (ACCENT_LEFT_MAX_SCAN - ACCENT_LEFT_MIN_SCAN) + 1;
            keyNum *= 2;
        } else if(scanCode >= NOTE_RIGHT_MIN_SCAN && scanCode <= NOTE_RIGHT_MAX_SCAN) {
            //Primary note 2nd row
            keyNum = scanCode - NOTE_RIGHT_MIN_SCAN + (NOTE_LEFT_MAX_SCAN - NOTE_LEFT_MIN_SCAN) + 2;
            keyNum += (keyNum-1);
        }

        if(keyNum != null) {
            Byte result = keyNumToMidiNote(keyNum);
            return result;
        }

        return null;
    }

    // Mouse Input Functions
    private Boolean clickedBox(Integer mouseX, Integer mouseY, Vector2f buttonPos) {
        Integer buttonMinX = startX + new Float(buttonPos.x).intValue();
        Integer buttonMaxX = buttonMinX + BUTTON_SIZE;
        Integer buttonMinY = startY + new Float(buttonPos.y).intValue();
        Integer buttonMaxY = buttonMinY + BUTTON_SIZE;

        Boolean result = mouseX >= buttonMinX && mouseX <= buttonMaxX && mouseY >= buttonMinY && mouseY <= buttonMaxY;

        if(result) {
            Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        return result;
    }

    // Render Functions
    private MatrixStack renderGraphics(MatrixStack matrixStack) {
        setAlpha(1.0f);

        // Set Texture
        Minecraft.getInstance().getTextureManager().bindTexture(guiTexture);

        // Visible Notes
        Integer keyboardTextureShift = (visibleNoteShift % (NOTE_WIDTH/2)) * NOTE_WIDTH;
        blit(matrixStack, startX + NOTE_OFFSET_X - 1, startY + NOTE_OFFSET_Y - 1, this.getBlitOffset(), keyboardTextureShift, 274, 308, 128, TEXTURE_SIZE, TEXTURE_SIZE);

        // Note Edges
        if(visibleNoteShift == 0) {
            blit(matrixStack, startX + NOTE_OFFSET_X , startY + NOTE_OFFSET_Y, this.getBlitOffset(), 392, 249, 6, 86, TEXTURE_SIZE, TEXTURE_SIZE);
        }
        
        // Active Notes
        matrixStack = renderAndCleanNoteSet(matrixStack, this.heldNotes, 5000, true, entry -> {this.onGuiNoteRelease(entry.getKey());});
        matrixStack = renderAndCleanNoteSet(matrixStack, this.releasedNotes, 1000, false, entry -> {this.releasedNotes.remove(entry.getKey());});

        // Reset alpha for next layers
        setAlpha(1.0f);

        // GUI Background and Note Keys
        blit(matrixStack, startX, startY, this.getBlitOffset(), 0, 0, GUI_WIDTH, GUI_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // Note Key Covers
        blit(matrixStack, startX + NOTE_OFFSET_X - 1, startY + NOTE_OFFSET_Y + 55, this.getBlitOffset(), keyboardTextureShift, 247, 308, 26, TEXTURE_SIZE, TEXTURE_SIZE);
        
        // MIDI Enabled Status Light
        if(this.instrumentUtil.isMidiEnabled(instrumentData)) {
            blit(matrixStack, startX + 351, startY + 45, this.getBlitOffset(), 369, 42, 3, 3, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        // Channel Output Status Lights
        SortedArraySet<Byte> acceptedChannels = this.instrumentUtil.getAcceptedChannelsSet(this.instrumentData);

        if(acceptedChannels != null && !acceptedChannels.isEmpty()) {
            for(Byte channelId : acceptedChannels) {
                blit(matrixStack, startX + 40 + 19 * channelId, startY + 97, this.getBlitOffset(), 369, 42, 3, 3, TEXTURE_SIZE, TEXTURE_SIZE);
            }
        }
        
        return matrixStack;
    }

    private MatrixStack renderAndCleanNoteSet(MatrixStack matrixStack, ConcurrentHashMap<Byte,Instant> noteMap, Integer sustainMillis, Boolean held, Consumer<Entry<Byte,Instant>> removeHandler) {
        List<Entry<Byte,Instant>> notesToRemove = new ArrayList<>();
        if(!noteMap.isEmpty()) {
            for(Entry<Byte,Instant> entry : noteMap.entrySet()) {
                if(Math.abs(ChronoUnit.MILLIS.between(Instant.now(), entry.getValue())) > sustainMillis) {
                    notesToRemove.add(entry);
                }
                matrixStack = this.renderNote(matrixStack, entry.getKey(), held, entry.getValue());
            }

            notesToRemove.forEach(entry -> removeHandler.accept(entry));
        }

        return matrixStack;
    }

    private MatrixStack renderNote(MatrixStack matrixStack, Byte note, Boolean held, Instant releaseTime) {
        Float alpha = 1.0f;
        Integer keyNum = midiNoteToKeyNum(note);

        // If we can't find a visible key for the note then skip rendering
        if(keyNum == null) {
            return matrixStack;
        }

        if(!held) {
            alpha -= Math.min(Math.abs(ChronoUnit.MILLIS.between(Instant.now(), releaseTime))/1000f, 1.0f);
        }

        setAlpha(alpha);

        blit(
            matrixStack, 
            startX + NOTE_OFFSET_X + (keyNum - 1) * NOTE_WIDTH/2, 
            startY + NOTE_OFFSET_Y + 43 + (keyNum % 2) * 42, 
            this.getBlitOffset(), 
            382 - (keyNum % 2) * 13, 
            0, 12, 41, 
            TEXTURE_SIZE, TEXTURE_SIZE
        );
        
        return matrixStack;
    }

    private MatrixStack renderText(MatrixStack matrixStack) {
        // Min Note Text
        font.drawString(matrixStack, this.minNoteString, startX + 335, startY + 224, 0xFF00E600);

        // Maestro Name
        font.drawString(matrixStack, this.selectedMaestroName.length() <= 22 ? this.selectedMaestroName : this.selectedMaestroName.substring(0,21) + "...", startX + 81, startY + 43, 0xFF00E600);
        
        return matrixStack;
    }

    private String noteLetterFromNum(Integer octaveNoteNum) {
        switch(octaveNoteNum) {
            case 0:
                return "C";
            case 1:
                return "D";
            case 2:
                return "E";
            case 3:
                return "F";
            case 4:
                return "G";
            case 5:
                return "A";
            case 6:
                return "B";
        }

        return "";
    }

    private void refreshMaestroName() {
        UUID linkedMaestro = instrumentUtil.getLinkedMaestro(instrumentData);
        if(linkedMaestro != null) {
            if(linkedMaestro.equals(player.getUniqueID())) {
                this.selectedMaestroName = player.getName().getString();
            } else if(linkedMaestro.equals(InstrumentDataUtil.MIDI_MAESTRO_ID)) {
                this.selectedMaestroName = "MIDI Input Device";
            } else {
                this.selectedMaestroName = PlayerNameUtils.getPlayerNameFromUUID(linkedMaestro, world);
            }
        } else {
            this.selectedMaestroName = "None";
        }
    }

    // Data Utils
    public InstrumentDataUtil<T> getInstrumentDataUtil() {
        return this.instrumentUtil;
    }

    public T getInsturmentData() {
        return this.instrumentData;
    }
    
    public void syncInstrumentToServer() {
        InstrumentDataUpdatePacket packet = null;

        if(instrumentData instanceof ItemStack) {
            packet = ItemInstrument.getSyncPacket((ItemStack)instrumentData);
        } else if(instrumentData instanceof TileInstrument) {
            packet = TileInstrument.getSyncPacket((TileInstrument)instrumentData);
        }
        
        if(packet != null) {
            NetworkManager.NET_CHANNEL.sendToServer(packet);
        }
    }

    @SuppressWarnings("deprecation")
    private void setAlpha(float alpha) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, alpha);
    }
}