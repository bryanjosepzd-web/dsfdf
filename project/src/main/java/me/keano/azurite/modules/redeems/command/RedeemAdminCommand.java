package me.keano.azurite.modules.redeems.command;

import me.keano.azurite.modules.commands.CommandManager;
import me.keano.azurite.modules.framework.Config;
import me.keano.azurite.modules.framework.commands.Command;
import me.keano.azurite.modules.redeems.Redeem;
import me.keano.azurite.modules.redeems.RedeemManager;
import me.keano.azurite.modules.redeems.menu.RedeemEditorMenu;
import me.keano.azurite.modules.redeems.menu.RedeemIndividualMenu;
import me.keano.azurite.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RedeemAdminCommand extends Command {

    public RedeemAdminCommand(CommandManager manager) {
        super(manager, "redeemadmin");
        this.setPermissible("azurite.redeemadmin");
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("radmin");
    }

    @Override
    public List<String> usage() {
        return Arrays.asList(
                CC.t("&7&m-------------------------"),
                CC.t("&cRedeem Admin Commands:"),
                CC.t("&e/redeemadmin &7- Open the redeem editor menu"),
                CC.t("&e/redeemadmin create <id> &7- Create a new redeem"),
                CC.t("&e/redeemadmin delete <id> &7- Delete a redeem"),
                CC.t("&e/redeemadmin edit <id> &7- Edit a redeem"),
                CC.t("&e/redeemadmin list &7- List all redeems"),
                CC.t("&e/redeemadmin reset <player> [id] &7- Reset player's redeemed history"),
                CC.t("&e/redeemadmin reload &7- Reload redeems"),
                CC.t("&7&m-------------------------")
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendUsage(sender);
                return;
            }
            Player player = (Player) sender;
            new RedeemEditorMenu(getInstance().getMenuManager(), player, getRedeemManager()).open();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(sender, args);
                return;
            case "delete":
                handleDelete(sender, args);
                return;
            case "edit":
                handleEdit(sender, args);
                return;
            case "list":
                handleList(sender);
                return;
            case "reset":
                handleReset(sender, args);
                return;
            case "reload":
                getRedeemManager().reload();
                sendMessage(sender, "&aRedeems reloaded.");
                return;
        }

        sendUsage(sender);
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "&cUsage: /redeemadmin create <id>");
            return;
        }
        String id = args[1].toLowerCase();
        if (getRedeemManager().getRedeem(id) != null) {
            sendMessage(sender, "&cThat redeem already exists!");
            return;
        }
        getRedeemManager().createRedeem(id);
        sendMessage(sender, "&aCreated redeem with id: &e" + id);
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "&cUsage: /redeemadmin delete <id>");
            return;
        }
        String id = args[1].toLowerCase();
        if (getRedeemManager().getRedeem(id) == null) {
            sendMessage(sender, "&cThat redeem does not exist!");
            return;
        }
        getRedeemManager().deleteRedeem(id);
        sendMessage(sender, "&aDeleted redeem: &e" + id);
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, Config.PLAYER_ONLY);
            return;
        }
        if (args.length < 2) {
            sendMessage(sender, "&cUsage: /redeemadmin edit <id>");
            return;
        }
        String id = args[1].toLowerCase();
        Redeem redeem = getRedeemManager().getRedeem(id);
        if (redeem == null) {
            sendMessage(sender, "&cThat redeem does not exist!");
            return;
        }
        Player player = (Player) sender;
        new RedeemIndividualMenu(getInstance().getMenuManager(), player, getRedeemManager(), redeem).open();
    }

    private void handleList(CommandSender sender) {
        List<Redeem> list = getRedeemManager().getRedeemsList();
        if (list.isEmpty()) {
            sendMessage(sender, "&cNo redeems configured.");
            return;
        }
        StringBuilder sb = new StringBuilder("&aRedeems: ");
        for (int i = 0; i < list.size(); i++) {
            sb.append("&e").append(list.get(i).getId());
            if (i < list.size() - 1) sb.append("&7, ");
        }
        sendMessage(sender, sb.toString());
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "&cUsage: /redeemadmin reset <player> [id]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sendMessage(sender, Config.PLAYER_NOT_FOUND.replace("%player%", args[1]));
            return;
        }
        if (args.length >= 3) {
            String id = args[2].toLowerCase();
            if (getRedeemManager().getRedeem(id) == null) {
                sendMessage(sender, "&cThat redeem does not exist!");
                return;
            }
            getRedeemManager().resetPlayerRedeem(target.getUniqueId(), id);
            sendMessage(sender, "&aReset redeem &e" + id + " &afor &e" + target.getName());
        } else {
            getRedeemManager().resetPlayer(target.getUniqueId());
            sendMessage(sender, "&aReset all redeems for &e" + target.getName());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> sub = Arrays.asList("create", "delete", "edit", "list", "reset", "reload");
            return sub.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("edit"))) {
            return getRedeemManager().getRedeemsList().stream()
                    .map(Redeem::getId)
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
            return getRedeemManager().getRedeemsList().stream()
                    .map(Redeem::getId)
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private RedeemManager getRedeemManager() {
        return getInstance().getRedeemManager();
    }
}
