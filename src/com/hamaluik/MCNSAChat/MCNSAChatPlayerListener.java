package com.hamaluik.MCNSAChat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class MCNSAChatPlayerListener extends PlayerListener {
	private MCNSAChat plugin;
	
	// grab the main plug in so we can use it later
	public MCNSAChatPlayerListener(MCNSAChat instance) {
		plugin = instance;
	}
	
	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		// make sure the player is already recorded
		
		// sort out which channel to put them into
		String channelName = new String("");
		if(!plugin.getPlayer(player).channel.equals("")) {
			// stick them in their proper channel
			channelName = plugin.getPlayer(player).channel;
		}
		else {
			channelName = plugin.defaultChannel;
		}
		
		plugin.log.info("[MCNSAChat] " + player.getName() + " will be joining channel: '"+channelName+"'");
		
		// add them back to their channel
		// if their channel was destroyed when they left
		// it will be recreated (but with default colour and no password currently)
		plugin.getChannel(channelName).addPlayer(plugin, player);

		// "resume" their timeout timer when they log back on
		if(plugin.getPlayer(player).onTimeout) {
			// catch any negative time errors
			if(plugin.getPlayer(player).timeoutLeft - ((System.currentTimeMillis() / 1000) - plugin.getPlayer(player).timeoutBegin) < 0) {
				// ok, they should not be in timeout!!
				plugin.getPlayer(player).onTimeout = false;
				
				// we're done here!
				return;
			}
			
			// recreate the timer and schedule it with the saved time left (see onPlayerQuit)
			plugin.getPlayerTimer(player).schedule(new MCNSAChatUnTimeoutTask(plugin, player.getName()), plugin.getPlayer(player).timeoutLeft * 1000);
			// and reset the begin time
			plugin.getPlayer(player).timeoutBegin = System.currentTimeMillis() / 1000;
			// set the time left
			plugin.getPlayer(player).timeoutLength = plugin.getPlayer(player).timeoutLeft;
			
			// and notify
			plugin.sendMessage(player, "&cYou're still in timeout for " + (((float)((plugin.getPlayer(player).timeoutLength - ((System.currentTimeMillis() / 1000) - plugin.getPlayer(player).timeoutBegin)))) / 60) + " more minutes and cannot talk!");
			plugin.log.info("[MCNSAChat] " + player.getName() + " joined, and is still on timeout for " + ((float)plugin.getPlayer(player).timeoutLeft) + " more seconds");
		}
	}
	
	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		
		// remove them from the channel!
		String channelName = new String(plugin.getPlayer(player).channel);
		plugin.getChannel(channelName).removePlayer(plugin, player, false);
		// check to see if the channel needs to be destroyed..
		plugin.checkChannel(channelName);
		// add their channel name back in
		plugin.getPlayer(player).channel = channelName;
		
		// "pause" their timer if they log off
		if(plugin.getPlayer(player).onTimeout) {
			// turn the timer off
			plugin.getPlayerTimer(player).cancel();
			plugin.playerTimers.remove(player.getName());
			plugin.getPlayer(player).timeoutLeft = plugin.getPlayer(player).timeoutLength - ((System.currentTimeMillis() / 1000) - plugin.getPlayer(player).timeoutBegin);
			plugin.log.info("[MCNSAChat] " + player.getName() + " logged off with " + plugin.getPlayer(player).timeoutLeft + " seconds of timeout left");
		}
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		if(event.isCancelled()) {
			return;
		}
		
		// grab the player
		Player player = event.getPlayer();
		
		// make sure they're not on timeout
		if(plugin.getPlayer(player).onTimeout) {
			// they're on timeout
			
			// make sure they're not on negtive timeout
			if((plugin.getPlayer(player).timeoutLength - ((System.currentTimeMillis() / 1000) - plugin.getPlayer(player).timeoutBegin)) < 0) {
				// uhh, something went wrong and they're on a negative timeout!
				// shut that shit down
				plugin.getPlayer(player).onTimeout = false;
			}
			else {		
				// tell them how much time they left here
				plugin.sendMessage(player, "&cYou're still in timeout for " + String.format("%.2f", (((float)((plugin.getPlayer(player).timeoutLength - ((System.currentTimeMillis() / 1000) - plugin.getPlayer(player).timeoutBegin)))) / 60)) + " more minutes and cannot talk!");
				event.setCancelled(true);
				return;
			}
		}
		
		// process the chat!
		plugin.getChannel(plugin.getPlayer(player).channel).chat(plugin, player, event.getFormat());
		
		// cancel the chat event,
		// we'll handle it from here.
		event.setCancelled(true);
	}
	
	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if(event.isCancelled()) {
			return;
		}
		
		// make sure it's a /me command
		if(event.getMessage().startsWith("/me ")) {
			// grab the player
			Player player = event.getPlayer();
			
			// make sure they're not on timeout
			if(plugin.getPlayer(player).onTimeout) {
				// they're on timeout
				// tell them how much time they left here
				plugin.sendMessage(player, "&cYou're still in timeout for " + String.format("%.2f", (((float)((plugin.getPlayer(player).timeoutLength - ((System.currentTimeMillis() / 1000) - plugin.getPlayer(player).timeoutBegin)))) / 60)) + " more minutes and cannot talk!");
				event.setCancelled(true);
				return;
			}
			
			String message = new String(event.getMessage().substring(3));
			
			// process the chat!
			plugin.getChannel(plugin.getPlayer(player).channel).emote(plugin, player, message);
			
			// cancel the chat event,
			// we'll handle it from here.
			event.setCancelled(true);
		}
		if(event.getMessage().startsWith("/msg ") || event.getMessage().startsWith("/r ") || event.getMessage().startsWith("/tell ")) {
			// grab the player
			Player player = event.getPlayer();
			
			// make sure they're not on timeout
			if(plugin.getPlayer(player).onTimeout) {
				// they're on timeout
				// tell them how much time they left here
				plugin.sendMessage(player, "&cYou're still in timeout for " + String.format("%.2f", (((float)((plugin.getPlayer(player).timeoutLength - ((System.currentTimeMillis() / 1000) - plugin.getPlayer(player).timeoutBegin)))) / 60)) + " more minutes and cannot talk!");
				event.setCancelled(true);
				return;
			}
		}
		
		// ok, not a /me or a msg
		// but we can check for aliases!
		// note that we have to check for them here instead of registering
		// commands, since all commands normally have to be defined in plugin.yml
		// however, since we're loading these dynamically, we have to get tricksy
		for(String key: plugin.aliases.keySet()) {
			// make sure it's the exact command we're looking for
			if(event.getMessage().startsWith("/" + key + " ") || event.getMessage().equalsIgnoreCase("/"+key)) {
				// make sure there's appropriate length to the message
				if(event.getMessage().length() > (2 + key.length())) {
					String[] args = new String[1];
					args[0] = new String(event.getMessage().substring(2 + key.length()));
					// just pass it off to the regular command executor
					plugin.commandExecutor.onCommand((CommandSender)event.getPlayer(), null, key, args);
				}
				else {
					// they didn't supply any arguments!
					plugin.commandExecutor.onCommand((CommandSender)event.getPlayer(), null, key, new String[0]);
				}
				
				// cancel the event, we already caught it
				event.setCancelled(true);
			}
		}
	}
}
