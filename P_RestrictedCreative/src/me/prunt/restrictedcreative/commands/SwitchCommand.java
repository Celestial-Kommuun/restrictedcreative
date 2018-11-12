package me.prunt.restrictedcreative.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.prunt.restrictedcreative.Main;
import me.prunt.restrictedcreative.utils.Utils;

public class SwitchCommand implements CommandExecutor {
    private Main main;
    private GameMode gm;

    public SwitchCommand(Main plugin, GameMode gm) {
	this.main = plugin;
	this.gm = gm;
    }

    private Main getMain() {
	return this.main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	switch (args.length) {
	case 0:
	    if (!(sender instanceof Player))
		return false;

	    Player p = (Player) sender;

	    // Don't switch to creative mode when it's not allowed at player's height
	    if (this.gm == GameMode.CREATIVE && !getMain().getUtils().isHeightOk(p)) {
		getMain().getUtils().sendMessage(p, true, "disabled.region");
		return true;
	    }

	    p.setGameMode(this.gm);
	    main.getUtils().sendMessage(p, true, "gamemodes." + getName() + ".me");
	    return true;
	case 1:
	    if (sender.hasPermission("rc.commands." + getName() + ".others")) {
		Player p1 = Bukkit.getPlayer(args[0]);

		if (p1 == null || !p1.isOnline()) {
		    Utils.sendMessage(sender,
			    main.getUtils().getMessage(true, "not-found").replaceAll("%player%", args[0]));
		    return true;
		}

		p1.setGameMode(this.gm);
		main.getUtils().sendMessage(p1, true, "gamemodes." + getName() + ".me");
		Utils.sendMessage(sender, main.getUtils().getMessage(true, "gamemodes." + getName() + ".other")
			.replaceAll("%player%", p1.getName()));
		return true;
	    }

	    break;
	default:
	    return false;
	}

	main.getUtils().sendMessage(sender, false, "no-permission");
	return true;
    }

    private String getName() {
	return this.gm.toString().toLowerCase();
    }
}
