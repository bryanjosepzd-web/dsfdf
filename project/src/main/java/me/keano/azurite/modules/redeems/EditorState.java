package me.keano.azurite.modules.redeems;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EditorState {

    public enum State {
        NONE,
        CREATING_REDEEM,
        ENTERING_TEXTURE,
        ENTERING_COMMAND,
        ENTERING_LORE,
        ENTERING_ITEM_NAME,
        ENTERING_MENU_ROWS,
        SELECTING_SLOT,
        EDITING_LORE_LINE,
        ENTERING_CLAIM_MESSAGE,
        ENTERING_ALREADY_CLAIMED_MESSAGE,
        ENTERING_MENU_NAME,
        SELECTING_ITEM
    }

    private final UUID uuid;
    private State state;
    private String editingRedeemId;
    private StringBuilder textureBuffer;
    private int selectedCommandIndex;
    private int selectedLoreIndex;
    private boolean editingLoreLine;

    public EditorState(UUID uuid) {
        this.uuid = uuid;
        this.state = State.NONE;
        this.textureBuffer = new StringBuilder();
        this.selectedCommandIndex = 0;
        this.selectedLoreIndex = 0;
        this.editingLoreLine = false;
    }

    public void reset() {
        this.state = State.NONE;
        this.editingRedeemId = null;
        this.textureBuffer = new StringBuilder();
        this.selectedCommandIndex = 0;
        this.selectedLoreIndex = 0;
        this.editingLoreLine = false;
    }
}
