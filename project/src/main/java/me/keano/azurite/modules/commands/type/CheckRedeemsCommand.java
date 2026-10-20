package me.keano.azurite.modules.commands.type;

import me.keano.azurite.modules.commands.CommandManager;
import me.keano.azurite.modules.framework.commands.Command;
import me.keano.azurite.modules.redeems.Redeem;
import me.keano.azurite.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class CheckRedeemsCommand extends Command {

    public CheckRedeemsCommand(CommandManager manager) {
        super(manager, "checkredeems");
        this.setPermissible("azurite.checkredeems");
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList(
                "redeemscount",
                "redeemcount",
                "countredeems"
        );
    }

    @Override
    public List<String> usage() {
        return null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<Redeem> list = getInstance().getRedeemManager().getRedeemsList();
        if (list.isEmpty()) {
            sendMessage(sender, "&cNo redeems configured.");
            return;
        }
        sendMessage(sender, CC.t("&7&m-------------------------"));
        for (Redeem redeem : list) {
            sendMessage(sender, CC.t("&e" + redeem.getId() + " &7- &f" + CC.t(redeem.getName()) + " &7- &aUses: &f" + redeem.getUses()));
        }
        sendMessage(sender, CC.t("&7&m-------------------------"));
    }
}
