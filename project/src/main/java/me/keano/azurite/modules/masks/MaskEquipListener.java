package me.keano.azurite.modules.masks;

import me.keano.azurite.modules.framework.Module;
import me.keano.azurite.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.EnderPearl;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class MaskEquipListener extends Module<MaskManager> {

    private static String CB_PACKAGE;
    private static String NMS_PACKAGE;

    private static Class<?> craftItemStackClass;
    private static Class<?> nmsItemStackClass;
    private static Class<?> packetClass;
    private static Class<?> craftPlayerClass;
    private static Class<?> entityPlayerClass;
    private static Class<?> playerConnectionClass;

    private static Method asNMSCopy;
    private static Method sendPacket;
    private static Method getHandle;

    private static Constructor<?> packetConstructor;

    static {
        try {
            CB_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();
            NMS_PACKAGE = CB_PACKAGE.replace("org.bukkit.craftbukkit", "net.minecraft.server");

            craftItemStackClass = Class.forName(CB_PACKAGE + ".inventory.CraftItemStack");
            nmsItemStackClass = Class.forName(NMS_PACKAGE + ".ItemStack");
            packetClass = Class.forName(NMS_PACKAGE + ".PacketPlayOutEntityEquipment");
            craftPlayerClass = Class.forName(CB_PACKAGE + ".entity.CraftPlayer");
            entityPlayerClass = Class.forName(NMS_PACKAGE + ".EntityPlayer");
            playerConnectionClass = Class.forName(NMS_PACKAGE + ".PlayerConnection");

            asNMSCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            sendPacket = playerConnectionClass.getMethod("sendPacket", Class.forName(NMS_PACKAGE + ".Packet"));
            getHandle = craftPlayerClass.getMethod("getHandle");

            packetConstructor = packetClass.getConstructor(int.class, int.class, nmsItemStackClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Map<UUID, String> activeVisualMasks = new HashMap<>();

    public MaskEquipListener(MaskManager manager) {
        super(manager);
    }

    // Change 3: When a player presses a number key (1–9) to swap their current
    // held item with a hotbar slot, the InventoryClickEvent fires with
    // ClickType.NUMBER_KEY and SlotType.CONTAINER (the destination slot is in
    // the player inventory, not the armor slot). The event handler at MONITOR
    // priority (onInventoryClick) only schedules a delayed checkAndUpdateVisual,
    // which runs AFTER the swap has already happened. By that point the client
    // has already moved the fake mask head item to the hotbar slot instead of
    // the real fused helmet, causing the mask to appear unequipped.
    //
    // Fix: At LOWEST priority (before other handlers and before MONITOR), detect
    // a NUMBER_KEY click. If the player has an active mask visual, immediately
    // send the real helmet visual back to the player's client so the swap
    // operates on the real helmet item, not the fake mask head. Then schedule
    // a re-check on the next tick to restore the mask visual if the helmet is
    // still equipped after the swap.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onHotbarSwap(InventoryClickEvent e) {
        if (e.getClick() != ClickType.NUMBER_KEY) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        String activeMask = activeVisualMasks.get(player.getUniqueId());
        if (activeMask == null) return;

        // Send the real helmet to the player's own client so the number-key
        // swap operates on the real item, not the fake mask head.
        sendRealHelmetVisual(player);
        activeVisualMasks.remove(player.getUniqueId());

        // After the swap resolves, re-check and restore the mask visual if the
        // fused helmet is still in the helmet slot.
        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            forceRefreshVisual(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            checkAndUpdateVisual(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        // Bug 4 fix: clear the cached visual on reconnect so checkAndUpdateVisual
        // does not skip re-sending the fake helmet. The cache may still hold a
        // stale entry from the previous session, which causes the early-return
        // in checkAndUpdateVisual (same maskId) to skip sending the equipment
        // packet to the newly connected client.
        activeVisualMasks.remove(player.getUniqueId());

        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            forceRefreshVisual(player);
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        activeVisualMasks.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            checkAndUpdateVisual(player);
        }, 2L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDamage(PlayerItemDamageEvent e) {
        // Bug 2: Cancel durability reduction for fused mask helmets to prevent flicker
        if (getManager().isMaskItem(e.getItem())) {
            e.setCancelled(true);
            return;
        }

        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            checkAndUpdateVisual(player);
        }, 1L);
    }

    // Bug 1 fix: On Spigot 1.8, when a player right-clicks an EnderPearl
    // while on cooldown, the throw is cancelled by EnderpearlTimer but the
    // client still resets its own helmet display, removing the mask visual.
    // ProjectileLaunchEvent only fires when the pearl actually launches, so
    // it does not cover the cooldown-cancelled case. We listen to
    // PlayerInteractEvent at MONITOR priority (after EnderpearlTimer cancels)
    // and force-refresh the mask visual so the wearer continues to see it.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPearlInteract(PlayerInteractEvent e) {
        if (!e.getAction().name().contains("RIGHT")) return;
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.ENDER_PEARL) return;

        Player player = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            forceRefreshVisual(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        // Bug 2 fix: The 1.8 client resets the wearer's own helmet view after
        // throwing an Ender Pearl. The old code relied on the cached
        // activeVisualMasks entry, which could be null or stale. Instead we
        // re-resolve the mask from the actual helmet NBT and force-send the
        // fake visual back to the wearer after one tick.
        if (!(e.getEntity() instanceof EnderPearl)) return;
        if (!(e.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) e.getEntity().getShooter();

        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            forceRefreshVisual(player);
        }, 1L);
    }

    // Bug 3 fix: Repeated fishing-rod use causes the 1.8 client to reset the
    // wearer's own helmet display, similar to the Ender Pearl issue. We refresh
    // the fake mask visual after the rod state change so the wearer sees the
    // mask again. We ignore the initial FISHING state (cast) and only act on
    // reel-in / failed bite / in-ground states where the client reset occurs.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.FISHING) return;

        Player player = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            forceRefreshVisual(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        activeVisualMasks.remove(player.getUniqueId());

        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            checkAndUpdateVisual(player);
        }, 2L);
    }

    // Bug 3 fix: When a player right-clicks a helmet from the hotbar to equip
    // it directly, InventoryClickEvent never fires so checkAndUpdateVisual is
    // never called and the mask head visual is never applied. We listen to
    // PlayerInteractEvent for right-click with a helmet and schedule a visual
    // check one tick later (after the server has moved the item to the helmet
    // slot). This covers right-click equipping from hotbar or main hand.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHelmetInteract(PlayerInteractEvent e) {
        if (!e.getAction().name().contains("RIGHT")) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        if (!MaskManager.isHelmet(item.getType())) return;
        if (!getManager().isMaskItem(item)) return;

        Player player = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
            forceRefreshVisual(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        activeVisualMasks.remove(e.getEntity().getUniqueId());
    }

    public void checkAndUpdateVisual(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();

        if (helmet == null || helmet.getType() == Material.AIR) {
            String activeMask = activeVisualMasks.get(player.getUniqueId());
            if (activeMask != null) {
                activeVisualMasks.remove(player.getUniqueId());
                getManager().clearEquippedMask(player.getUniqueId());
            }
            return;
        }

        String maskId = getManager().getMaskIdFromItem(helmet);
        if (maskId == null) {
            String activeMask = activeVisualMasks.get(player.getUniqueId());
            if (activeMask != null) {
                sendRealHelmetVisual(player);
                activeVisualMasks.remove(player.getUniqueId());
                getManager().clearEquippedMask(player.getUniqueId());
            }
            return;
        }

        // Only re-send if the mask changed or no active visual
        String activeMask = activeVisualMasks.get(player.getUniqueId());
        if (activeMask != null && activeMask.equalsIgnoreCase(maskId)) {
            // Mask is still the same and active - don't re-send to avoid flicker
            return;
        }

        sendMaskVisual(player, maskId);
    }

    // Bug 2/3/4 fix: Shared helper that re-resolves the current mask from the
    // helmet's NBT and force-sends the fake visual, bypassing the cache-based
    // early-return in checkAndUpdateVisual. Used by the Ender Pearl, fishing
    // rod, and reconnect handlers where the client has already reset its own
    // equipment view and needs the fake helmet re-sent unconditionally.
    private void forceRefreshVisual(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();

        if (helmet == null || helmet.getType() == Material.AIR) {
            activeVisualMasks.remove(player.getUniqueId());
            getManager().clearEquippedMask(player.getUniqueId());
            return;
        }

        String maskId = getManager().getMaskIdFromItem(helmet);
        if (maskId == null) {
            sendRealHelmetVisual(player);
            activeVisualMasks.remove(player.getUniqueId());
            getManager().clearEquippedMask(player.getUniqueId());
            return;
        }

        sendMaskVisual(player, maskId);
    }

    public void sendMaskVisual(Player player, String maskId) {
        MaskManager.Mask mask = getManager().getMask(maskId);
        if (mask == null) return;

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() == Material.AIR) return;

        ItemStack fakeHelmet = buildVisualItem(mask, helmet);

        sendEquipmentPacket(player, 4, fakeHelmet);
        activeVisualMasks.put(player.getUniqueId(), maskId);
        getManager().setEquippedMask(player.getUniqueId(), maskId);
    }

    private ItemStack buildVisualItem(MaskManager.Mask mask, ItemStack helmet) {
        ItemStack visual = SkullUtils.createSkull(mask.getTexture());

        ItemMeta visualMeta = visual.getItemMeta();
        ItemMeta helmetMeta = helmet.getItemMeta();

        if (visualMeta != null && helmetMeta != null) {
            // Bug 1 fix: always preserve the helmet's original display name
            if (helmetMeta.hasDisplayName()) {
                visualMeta.setDisplayName(helmetMeta.getDisplayName());
            }

            // Bug 1 fix: strip color codes and " Mask" suffix to produce the clean
            // mask name (e.g. "Thor") for the lore line, matching the fusion lore.
            String maskName = mask.getDisplayName();
            maskName = ChatColor.stripColor(CC.t(maskName));
            maskName = maskName.replaceAll("(?i)\\s+Mask$", "").trim();
            String maskLoreLine = CC.t("&8" + maskName);
            List<String> lore = new ArrayList<>();
            if (helmetMeta.hasLore()) {
                lore.addAll(helmetMeta.getLore());
            }
            if (!lore.contains(maskLoreLine)) {
                lore.add(maskLoreLine);
            }
            visualMeta.setLore(lore);

            visual.setItemMeta(visualMeta);

            if (helmet.getEnchantments() != null && !helmet.getEnchantments().isEmpty()) {
                visual.addUnsafeEnchantments(helmet.getEnchantments());
            }
        }

        return visual;
    }

    public void sendRealHelmetVisual(Player player) {
        ItemStack realHelmet = player.getInventory().getHelmet();
        if (realHelmet == null || realHelmet.getType() == Material.AIR) {
            sendEquipmentPacket(player, 4, null);
        } else {
            sendEquipmentPacket(player, 4, realHelmet);
        }
    }

    private void sendEquipmentPacket(Player player, int slot, ItemStack item) {
        try {
            Object entityPlayer = getHandle.invoke(player);
            int entityId = player.getEntityId();

            Object nmsItem = null;
            if (item != null) {
                nmsItem = asNMSCopy.invoke(null, item);
            }

            Object packet = packetConstructor.newInstance(entityId, slot, nmsItem);

            Object connection = entityPlayerClass.getField("playerConnection").get(entityPlayer);
            sendPacket.invoke(connection, packet);

            for (Player nearby : Bukkit.getOnlinePlayers()) {
                if (nearby.equals(player)) continue;
                if (!nearby.getWorld().equals(player.getWorld())) continue;
                if (!nearby.canSee(player)) continue;

                Object nearbyHandle = getHandle.invoke(nearby);
                Object nearbyConn = entityPlayerClass.getField("playerConnection").get(nearbyHandle);
                sendPacket.invoke(nearbyConn, packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
