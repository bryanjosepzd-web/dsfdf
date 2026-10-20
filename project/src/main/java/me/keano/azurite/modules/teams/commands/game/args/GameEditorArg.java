package me.keano.azurite.modules.teams.commands.game.args;

import me.keano.azurite.modules.commands.CommandManager;
import me.keano.azurite.modules.framework.Config;
import me.keano.azurite.modules.framework.commands.Argument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

/**
 * /game editor - Opens the game editor GUI (same as f editor but different title/button).
 */
public class GameEditorArg extends Argument {

    public GameEditorArg(CommandManager manager) {
        super(
                manager,
                Collections.singletonList("editor")
        );
        this.setPermissible("azurite.systeam");
    }

    @Override
    public String usage() {
        return "/game editor";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, Config.PLAYER_ONLY);
            return;
        }

        Player player = (Player) sender;
        getInstance().getFactionEditorManager().openGameMenu(player);
    }
}
