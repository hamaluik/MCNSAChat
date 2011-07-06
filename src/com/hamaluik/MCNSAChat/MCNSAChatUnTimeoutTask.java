package com.hamaluik.MCNSAChat;

import java.util.TimerTask;

import org.bukkit.entity.Player;

// timer..
// ignore this for now
public class MCNSAChatUnTimeoutTask extends TimerTask {
	private MCNSAChat plugin;
	private String player;
	
	MCNSAChatUnTimeoutTask(MCNSAChat instance, String _player) {
		plugin = instance;
		player = _player;
	}
	
	public void run() {
		plugin.sendMessage(player, "&bYou've been in timeout for " + ((float)plugin.getPlayer(player).timeoutLength / 60) + " minutes!");
		plugin.sendMessage(player, "&bYou're no longer on timeout!");
		
		// if we're announcing to the server, do so!
		if(plugin.announceTimeouts) {
			Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
			for(int i = 0; i < onlinePlayers.length; i++) {
				plugin.sendMessage(onlinePlayers[i], "&7" + player + " &bis no longer on a timeout!");
			}
		}
		
		plugin.getPlayer(player).onTimeout = false;
		plugin.getPlayer(player).timeoutBegin = -1;
		plugin.getPlayer(player).timeoutLength = -1;
		
		plugin.log.info("[MCNSAChat] " + player + " is no longer in timeout!");
	}
}