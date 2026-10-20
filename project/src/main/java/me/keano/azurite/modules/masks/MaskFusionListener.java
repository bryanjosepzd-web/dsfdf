package me.keano.azurite.modules.masks;

import me.keano.azurite.modules.framework.Module;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class MaskFusionListener extends Module<MaskManager> {

    public MaskFusionListener(MaskManager manager) {
        super(manager);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        ItemStack cursor = e.getCursor();
        ItemStack clicked = e.getCurrentItem();

        if (cursor == null || cursor.getType() == Material.AIR) return;
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (!getManager().isMaskItem(cursor)) return;
        if (!MaskManager.isHelmet(clicked.getType())) return;
        if (getManager().isMaskItem(clicked)) return;

        String maskId = getManager().getMaskIdFromItem(cursor);
        if (maskId == null) return;

        e.setCancelled(true);

        ItemStack fusedHelmet = fuseHelmetWithMask(clicked, maskId);

        e.setCurrentItem(fusedHelmet);

        if (cursor.getAmount() <= 1) {
            e.setCursor(new ItemStack(Material.AIR));
        } else {
            cursor.setAmount(cursor.getAmount() - 1);
            e.setCursor(cursor);
        }

        player.updateInventory();
        player.sendMessage(CC.t("&aYou fused your mask with your helmet!"));
    }

    private ItemStack fuseHelmetWithMask(ItemStack helmet, String maskId) {
        ItemStack fused = helmet.clone();

        ItemMeta meta = fused.getItemMeta();
        if (meta == null) return fused;

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        MaskManager.Mask mask = getManager().getMask(maskId);
        if (mask != null) {
            // Bug 1 fix: strip any existing color codes, remove the " Mask" suffix
            // case-insensitively, then prepend &8 (dark gray) so the lore line
            // shows the clean mask name (e.g. "Thor") instead of a raw placeholder.
            String maskName = mask.getDisplayName();
            maskName = ChatColor.stripColor(CC.t(maskName));
            maskName = maskName.replaceAll("(?i)\\s+Mask$", "").trim();
            String maskLoreLine = CC.t("&8" + maskName);
            if (!lore.contains(maskLoreLine)) {
                lore.add(maskLoreLine);
            }
        }

        // Make the fused helmet unbreakable on modern versions (1.9+) to prevent
        // durability flicker. On 1.8 the PlayerItemDamageEvent cancellation in
        // MaskEquipListener handles it. We use reflection because
        // ItemMeta.setUnbreakable() does not exist in the Spigot 1.8 API that
        // this project compiles against.
        if (Utils.isModernVer()) {
            try {
                Method setUnbreakable = meta.getClass().getMethod("setUnbreakable", boolean.class);
                setUnbreakable.invoke(meta, true);
            } catch (Exception ignored) {
            }
            try {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf("HIDE_UNBREAKABLE"));
            } catch (IllegalArgumentException ignored) {
            }
        }

        meta.setLore(lore);
        fused.setItemMeta(meta);

        fused = NBTUtils.setMaskId(fused, maskId);
        return fused;
    }
}
