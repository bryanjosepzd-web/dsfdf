package me.keano.azurite.modules.redeems;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public class RedeemSkullUtils {

    private static Field profileField;

    static {
        try {
            String cbPackage = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
            Class<?> skullMetaClass = Class.forName(cbPackage + ".inventory.CraftMetaSkull");
            profileField = skullMetaClass.getDeclaredField("profile");
            profileField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ItemStack applyTexture(ItemStack item, String base64Texture) {
        if (item == null || item.getType() != Material.SKULL_ITEM) return item;
        if (!(item.getItemMeta() instanceof SkullMeta)) return item;
        if (base64Texture == null || base64Texture.isEmpty()) return item;

        try {
            // Preserve existing meta data (name, lore, enchantments)
            ItemMeta existingMeta = item.getItemMeta();
            String existingName = existingMeta.hasDisplayName() ? existingMeta.getDisplayName() : null;
            List<String> existingLore = existingMeta.hasLore() ? existingMeta.getLore() : null;

            SkullMeta meta = (SkullMeta) item.getItemMeta();

            Object profile = Class.forName("com.mojang.authlib.GameProfile")
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), "redeem");

            Object property = Class.forName("com.mojang.authlib.properties.Property")
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", base64Texture);

            Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
            properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);

            profileField.set(meta, profile);

            // Restore name and lore after texture application
            if (existingName != null) {
                meta.setDisplayName(existingName);
            }
            if (existingLore != null) {
                meta.setLore(existingLore);
            }

            item.setItemMeta(meta);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return item;
    }

    public static ItemStack createSkull(String base64Texture) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        return applyTexture(item, base64Texture);
    }
}
