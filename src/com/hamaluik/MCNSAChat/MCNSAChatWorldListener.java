package com.hamaluik.MCNSAChat;

import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldSaveEvent;

public class MCNSAChatWorldListener extends WorldListener {
	private MCNSAChat plugin;
	
	// grab the main plug in so we can use it later
	public MCNSAChatWorldListener(MCNSAChat instance) {
		plugin = instance;
	}
	
	@Override
	public void onWorldSave(WorldSaveEvent event) {
		// save our "database" whenever the world saves
		try {
			plugin.save(plugin.players, plugin.getDataFolder() + "/players.hash");
			plugin.save(plugin.channels, plugin.getDataFolder() + "/channels.hash");
			plugin.log.info("[MCNSAChat] channel and player data saved!");
		} catch (Exception e) {
			plugin.log.info("[ERROR] [MCNSAChat] failed to save data!: " + e.toString());
		}
	}
}
