package me.keano.azurite.modules.teams.editor;

import lombok.Getter;
import lombok.Setter;
import me.keano.azurite.HCF;
import me.keano.azurite.modules.framework.Manager;
import me.keano.azurite.modules.teams.Team;
import me.keano.azurite.modules.teams.TeamManager;
import me.keano.azurite.modules.teams.claims.Claim;
import me.keano.azurite.modules.teams.claims.ClaimManager;
import me.keano.azurite.modules.events.koth.Koth;
import me.keano.azurite.modules.teams.editor.menu.FactionEditorMenu;
import me.keano.azurite.modules.teams.enums.TeamType;
import me.keano.azurite.modules.teams.type.*;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import me.keano.azurite.utils.ItemUtils;
import me.keano.azurite.utils.Tasks;
import me.keano.azurite.utils.Utils;
import me.keano.azurite.utils.cuboid.Cuboid;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public class FactionEditorManager extends Manager implements Listener {

    private final Map<UUID, Consumer<String>> chatInputs = new HashMap<>();

    // Claim system (mirrors SysTeamClaimArg)
    private final Map<UUID, EditorClaim> claimMap = new HashMap<>();
    private final ItemStack claimWand;

    // Per-player scroll index and applied type for the metadata selector
    private final Map<UUID, Integer> scrollIndex = new HashMap<>();
    private final Map<UUID, TeamType> appliedType = new HashMap<>();

    // Track which faction each player is currently editing
    private final Map<UUID, Team> editingFaction = new HashMap<>();

    // Capture zone selection system (golden hoe)
    private final Map<UUID, EditorClaim> captureZoneMap = new HashMap<>();
    private final ItemStack captureZoneWand;

    // Koth loot command reward selection (per player, per koth)
    private final Map<UUID, Integer> selectedCommandIndex = new HashMap<>();

    // Claim types available in the metadata selector (index-based)
    @Getter
    private final List<TeamType> claimTypes = Arrays.asList(
            TeamType.ROAD,
            TeamType.SAFEZONE,
            TeamType.EVENT,
            TeamType.CONQUEST,
            TeamType.CITADEL,
            TeamType.NO_PEARLING
    );

    public FactionEditorManager(HCF instance) {
        super(instance);
        registerListener(this);
        registerListener(new NoPearlingListener(this));

        this.claimWand = new ItemBuilder(ItemUtils.getMat(
                getTeamConfig().getString("CLAIMING.CLAIM_WAND.TYPE")))
                .setName(getTeamConfig().getString("CLAIMING.CLAIM_WAND.NAME") + " &7(System Claim)")
                .setLore(getTeamConfig().getStringList("CLAIMING.CLAIM_WAND.LORE"))
                .toItemStack();

        this.captureZoneWand = new ItemBuilder(Material.GOLD_HOE)
                .setName("&6&lCapture Zone Wand")
                .setLore(Arrays.asList(
                        "&eRight-click &7to set corner 1",
                        "&eLeft-click &7to set corner 2",
                        "&aShift + Left-click &7to accept",
                        "&cShift + Right-click &7to cancel"
                ))
                .toItemStack();
    }

    // -- Chat Input --

    public void awaitChatInput(Player player, Consumer<String> callback) {
        chatInputs.put(player.getUniqueId(), callback);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Consumer<String> callback = chatInputs.remove(e.getPlayer().getUniqueId());
        if (callback == null) return;

        e.setCancelled(true);
        String message = e.getMessage();

        HCF instance = getInstance();
        instance.getServer().getScheduler().runTask(instance, () -> callback.accept(message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        chatInputs.remove(e.getPlayer().getUniqueId());
        claimMap.remove(e.getPlayer().getUniqueId());
        captureZoneMap.remove(e.getPlayer().getUniqueId());
        scrollIndex.remove(e.getPlayer().getUniqueId());
        appliedType.remove(e.getPlayer().getUniqueId());
        editingFaction.remove(e.getPlayer().getUniqueId());
        selectedCommandIndex.remove(e.getPlayer().getUniqueId());
    }

    // -- Lock all inventory interactions for plugin-owned menus --

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (getInstance().getMenuManager().getMenus().containsKey(e.getWhoClicked().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(InventoryMoveItemEvent e) {
        // No-op: our inventories are player-owned so this won't fire from our menus
    }

    // -- Faction Creation (replicates SysTeamCreateArg logic via API) --

    public Team createFaction(String name) {
        TeamManager tm = getInstance().getTeamManager();

        if (tm.getTeam(name) != null) {
            return null;
        }

        Team team = new SafezoneTeam(tm, name);
        team.setCustomColor(CC.t("&f"));
        team.save();
        return team;
    }

    // -- Set Color (replicates SysTeamSetColorArg logic via API) --

    public void setFactionColor(Team team, String colorCode) {
        team.setCustomColor(CC.t(colorCode));
        team.save();
    }

    // -- Give Claim Wand (replicates SysTeamClaimArg.execute logic) --

    public void giveClaimWand(Player player, Team team) {
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(CC.t("&cYour inventory is full!"));
            return;
        }

        claimMap.put(player.getUniqueId(), new EditorClaim(team));
        Utils.giveClaimingWand(this, player, claimWand);
    }

    // -- Unclaim at player location (replicates SysTeamUnclaimArg.execute logic) --

    public boolean unclaimAtPlayer(Player player, Team team) {
        ClaimManager cm = getInstance().getTeamManager().getClaimManager();
        Claim atPlayer = cm.getClaim(player.getLocation());

        if (atPlayer == null || atPlayer.getTeam() != team.getUniqueID()) {
            return false;
        }

        if (team.getHq() != null && atPlayer.contains(team.getHq())) {
            team.setHq(null);
        }

        cm.deleteClaim(atPlayer);
        team.getClaims().remove(atPlayer);
        team.save();
        return true;
    }

    // -- Claim Wand Interact Handler (replicates SysTeamClaimArg.onInteract) --

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        if (!claimMap.containsKey(player.getUniqueId())) return;
        if (e.getItem() == null) return;
        if (!e.getItem().isSimilar(claimWand)) return;

        EditorClaim claim = claimMap.get(player.getUniqueId());
        Team team = claim.getSystemTeam();
        Location location1 = claim.getLocation1();
        Location location2 = claim.getLocation2();

        e.setCancelled(true);

        if (e.getAction() == Action.RIGHT_CLICK_AIR) {
            player.sendMessage(CC.t("&eClaim selection cancelled."));
            clearSelection(player);
            setItemInHand(player, new ItemStack(Material.AIR));
            return;
        }

        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && player.isSneaking()) {
            if (location1 == null || location2 == null) {
                player.sendMessage(CC.t("&cYou need to select both corners first!"));
                return;
            }

            Claim toAdd = new Claim(team.getUniqueID(), location1, location2);
            getInstance().getTeamManager().getClaimManager().saveClaim(toAdd);
            team.getClaims().add(toAdd);
            team.save();

            if (getInstance().getWallManager().getWallType(toAdd, team, player) != null) {
                getInstance().getTeamManager().getClaimManager().teleportSafe(player);
            }

            for (Player inClaim : toAdd.getPlayers()) {
                if (getInstance().getWallManager().getWallType(toAdd, team, inClaim) != null) {
                    getInstance().getTeamManager().getClaimManager().teleportSafe(inClaim);
                }
            }

            player.sendMessage(CC.t("&aClaimed successfully for &f" + team.getName() + "&a!"));

            clearSelection(player);
            setItemInHand(player, new ItemStack(Material.AIR));

            // Reopen the faction edit menu after claim completion
            Tasks.execute(this, () -> reopenEditMenu(player, team));
            return;
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getClickedBlock() == null) return;
            Location blockClick = e.getClickedBlock().getLocation();
            getInstance().getWallManager().clearPillar(player, claim.getLocation1());
            Tasks.execute(this, () -> getInstance().getWallManager().sendPillar(player, blockClick));
            claim.setLocation1(blockClick);
            return;
        }

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (e.getClickedBlock() == null) return;
            Location blockClick = e.getClickedBlock().getLocation();
            getInstance().getWallManager().clearPillar(player, claim.getLocation2());
            Tasks.execute(this, () -> getInstance().getWallManager().sendPillar(player, blockClick));
            claim.setLocation2(blockClick);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        e.getDrops().remove(claimWand);
        e.getDrops().remove(captureZoneWand);
        clearSelection(player);
        clearCaptureZoneSelection(player);
    }

    @EventHandler
    public void onItem(PlayerItemDamageEvent e) {
        Player player = e.getPlayer();
        if (e.getItem().isSimilar(claimWand)) {
            clearSelection(player);
            setItemInHand(player, new ItemStack(Material.AIR));
        }
        if (e.getItem().isSimilar(captureZoneWand)) {
            clearCaptureZoneSelection(player);
            setItemInHand(player, new ItemStack(Material.AIR));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        Item item = e.getItemDrop();
        if (item.getItemStack().isSimilar(claimWand)) {
            item.remove();
            clearSelection(player);
        }
        if (item.getItemStack().isSimilar(captureZoneWand)) {
            item.remove();
            clearCaptureZoneSelection(player);
        }
    }

    private void clearSelection(Player player) {
        if (claimMap.containsKey(player.getUniqueId())) {
            EditorClaim claim = claimMap.get(player.getUniqueId());
            getInstance().getWallManager().clearPillar(player, claim.getLocation1());
            getInstance().getWallManager().clearPillar(player, claim.getLocation2());
            claimMap.remove(player.getUniqueId());
        }
    }

    // -- Capture Zone Wand (for koths) --

    public void giveCaptureZoneWand(Player player, Team team) {
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(CC.t("&cYour inventory is full!"));
            return;
        }

        captureZoneMap.put(player.getUniqueId(), new EditorClaim(team));
        Utils.giveClaimingWand(this, player, captureZoneWand);
    }

    @EventHandler
    public void onCaptureZoneInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        if (!captureZoneMap.containsKey(player.getUniqueId())) return;
        if (e.getItem() == null) return;
        if (!e.getItem().isSimilar(captureZoneWand)) return;

        EditorClaim claim = captureZoneMap.get(player.getUniqueId());
        Team team = claim.getSystemTeam();
        Location location1 = claim.getLocation1();
        Location location2 = claim.getLocation2();

        e.setCancelled(true);

        if (e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (player.isSneaking()) {
                player.sendMessage(CC.t("&eCapture zone selection cancelled."));
                clearCaptureZoneSelection(player);
                setItemInHand(player, new ItemStack(Material.AIR));
                Tasks.execute(this, () -> reopenEditMenu(player, team));
                return;
            }
            player.sendMessage(CC.t("&eSelection cancelled."));
            clearCaptureZoneSelection(player);
            setItemInHand(player, new ItemStack(Material.AIR));
            return;
        }

        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && player.isSneaking()) {
            if (location1 == null || location2 == null) {
                player.sendMessage(CC.t("&cYou need to select both corners first!"));
                return;
            }

            Cuboid capZone = new Cuboid(location1, location2);
            Koth koth = getInstance().getKothManager().getKoth(team.getName());
            if (koth != null) {
                koth.setCaptureZone(capZone);
                koth.save();
                player.sendMessage(CC.t("&aCapture zone set for &f" + team.getName() + "&a!"));
            } else {
                player.sendMessage(CC.t("&cNo koth found for this team!"));
            }

            clearCaptureZoneSelection(player);
            setItemInHand(player, new ItemStack(Material.AIR));
            Tasks.execute(this, () -> reopenEditMenu(player, team));
            return;
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getClickedBlock() == null) return;
            Location blockClick = e.getClickedBlock().getLocation();
            getInstance().getWallManager().clearPillar(player, claim.getLocation1());
            Tasks.execute(this, () -> getInstance().getWallManager().sendPillar(player, blockClick));
            claim.setLocation1(blockClick);
            return;
        }

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (e.getClickedBlock() == null) return;
            Location blockClick = e.getClickedBlock().getLocation();
            getInstance().getWallManager().clearPillar(player, claim.getLocation2());
            Tasks.execute(this, () -> getInstance().getWallManager().sendPillar(player, blockClick));
            claim.setLocation2(blockClick);
        }
    }

    private void clearCaptureZoneSelection(Player player) {
        if (captureZoneMap.containsKey(player.getUniqueId())) {
            EditorClaim claim = captureZoneMap.get(player.getUniqueId());
            getInstance().getWallManager().clearPillar(player, claim.getLocation1());
            getInstance().getWallManager().clearPillar(player, claim.getLocation2());
            captureZoneMap.remove(player.getUniqueId());
        }
    }

    // -- Koth Loot Command Reward helpers --

    public int getSelectedCommandIndex(Player player) {
        return selectedCommandIndex.getOrDefault(player.getUniqueId(), 0);
    }

    public void setSelectedCommandIndex(Player player, int index) {
        selectedCommandIndex.put(player.getUniqueId(), index);
    }

    public void removeSelectedCommandIndex(Player player) {
        selectedCommandIndex.remove(player.getUniqueId());
    }

    // -- Metadata / Type Selector helpers --

    public int getScrollIndex(Player player) {
        return scrollIndex.getOrDefault(player.getUniqueId(), 0);
    }

    public void setScrollIndex(Player player, int index) {
        scrollIndex.put(player.getUniqueId(), index);
    }

    public TeamType getAppliedType(Player player) {
        return appliedType.get(player.getUniqueId());
    }

    public void setAppliedType(Player player, TeamType type) {
        appliedType.put(player.getUniqueId(), type);
    }

    public void removeAppliedType(Player player) {
        appliedType.remove(player.getUniqueId());
    }

    // -- Editing faction tracking --

    public void setEditingFaction(Player player, Team team) {
        editingFaction.put(player.getUniqueId(), team);
    }

    public Team getEditingFaction(Player player) {
        return editingFaction.get(player.getUniqueId());
    }

    // -- Menu opening helpers --

    public void openMainMenu(Player player) {
        new FactionEditorMenu(getInstance().getMenuManager(), player).open();
    }

    public void openGameMenu(Player player) {
        new me.keano.azurite.modules.teams.editor.menu.GameEditorMenu(getInstance().getMenuManager(), player).open();
    }

    public void reopenEditMenu(Player player, Team team) {
        if (player.isOnline()) {
            new me.keano.azurite.modules.teams.editor.menu.FactionEditMenu(
                    getInstance().getMenuManager(), player, team).open();
        }
    }

    // -- Color code to wool data value mapping --

    public static int colorToWoolData(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) return 0;

        // Normalize: extract the &x code
        String code = colorCode;
        if (code.startsWith("\u00a7")) {
            code = "&" + code.charAt(1);
        }
        if (code.length() < 2) return 0;

        char c = Character.toLowerCase(code.charAt(code.startsWith("&") ? 1 : 0));

        switch (c) {
            case 'f': return 0;  // White
            case 'e': return 4;  // Yellow
            case '6': return 1;  // Orange
            case 'c': return 14; // Red
            case '4': return 14; // Dark Red
            case 'd': return 6;  // Pink/Magenta
            case '5': return 10; // Purple
            case '9': return 11; // Blue
            case '1': return 11; // Dark Blue
            case '3': return 9;  // Cyan
            case 'b': return 3;  // Light Blue
            case 'a': return 5;  // Lime Green
            case '2': return 13; // Green
            case '8': return 7;  // Gray
            case '7': return 8;  // Light Gray
            case '0': return 15; // Black
            default: return 0;   // Default white
        }
    }

    // -- Inner claim class --

    @Getter
    @Setter
    private static class EditorClaim {
        private Team systemTeam;
        private Location location1;
        private Location location2;

        public EditorClaim(Team team) {
            this.systemTeam = team;
            this.location1 = null;
            this.location2 = null;
        }
    }
}
