package me.keano.azurite.modules.redeems;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Redeem {

    private final String id;
    private String name;
    private String material;
    private int data;
    private String texture;
    private boolean textureEnabled;
    private int slot;
    private List<String> commands;
    private List<String> lore;
    private int uses;
    private String claimMessage;
    private String alreadyClaimedMessage;

    public Redeem(String id) {
        this.id = id;
        this.name = "&aNew Redeem";
        this.material = "SKULL_ITEM";
        this.data = 3;
        this.texture = "";
        this.textureEnabled = false;
        this.slot = -1;
        this.commands = new ArrayList<>();
        this.lore = new ArrayList<>();
        this.uses = 0;
        this.claimMessage = "&aYou have redeemed &e%redeemname%&a!";
        this.alreadyClaimedMessage = "&cYou have already redeemed this!";
    }
}
