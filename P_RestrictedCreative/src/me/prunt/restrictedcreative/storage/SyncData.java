package me.prunt.restrictedcreative.storage;

import java.util.List;

import org.bukkit.Bukkit;

import me.prunt.restrictedcreative.Main;

public class SyncData implements Runnable {
    private Main main;
    private List<String> toAdd;
    private List<String> toRemove;

    public SyncData(Main main, List<String> fadd, List<String> fdel) {
	this.main = main;
	this.toAdd = fadd;
	this.toRemove = fdel;
    }

    @Override
    public void run() {
	int addedCount = toAdd.size();
	int removedCount = toRemove.size();

	// If no changes should be made
	if (addedCount + removedCount == 0)
	    return;

	long start = System.currentTimeMillis();
	String or = DataHandler.isUsingSQLite() ? "or" : "";

	main.sendMessage(Bukkit.getConsoleSender(), true, "database.save");

	main.getDB().setAutoCommit(false);

	if (addedCount > 0) {
	    for (String str : toAdd) {
		main.getDB().executeUpdate("INSERT " + or + "IGNORE INTO " + main.getDB().getTableName()
			+ " (block) VALUES ('" + str + "')");
	    }
	    main.sendMessage(Bukkit.getConsoleSender(),
		    main.getMessage(true, "database.added").replaceAll("%blocks%", String.valueOf(addedCount)));
	}
	if (removedCount > 0) {
	    for (String str : toRemove) {
		main.getDB()
			.executeUpdate("DELETE FROM " + main.getDB().getTableName() + " WHERE block = '" + str + "'");
	    }
	    main.sendMessage(Bukkit.getConsoleSender(),
		    main.getMessage(true, "database.removed").replaceAll("%blocks%", String.valueOf(removedCount)));
	}

	main.getDB().commit();
	main.getDB().setAutoCommit(true);

	Bukkit.getScheduler().runTask(main, new Runnable() {
	    @Override
	    public void run() {
		DataHandler.addToDatabase.removeAll(toAdd);
		DataHandler.removeFromDatabase.removeAll(toRemove);

		String took = String.valueOf(System.currentTimeMillis() - start);

		main.sendMessage(Bukkit.getConsoleSender(),
			main.getMessage(true, "database.done").replaceAll("%mills%", took));
	    }
	});
    }
}
