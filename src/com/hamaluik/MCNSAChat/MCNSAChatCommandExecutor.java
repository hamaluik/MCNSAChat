package com.hamaluik.MCNSAChat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MCNSAChatCommandExecutor implements CommandExecutor {
	private final MCNSAChat plugin;
	
	public MCNSAChatCommandExecutor(MCNSAChat instance) {
		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		String playerName = new String("*Console*");
		
		// we only care if a player is sending this command
		if((sender instanceof Player)) {
			// aaannnddd grab the player.
			player = (Player)sender;
			playerName = player.getName();
			// make sure the player exists in the database..
			plugin.getPlayer(player);
		}
		else {
			// for now
			sender.sendMessage("You can't do that, bob! (yet)");
			return true;
		}
		
		// sort out which command was called
		if(label.equalsIgnoreCase("ch")) {
			if(args.length > 0) {
				// check out subcommands
				if(args[0].startsWith(" ") || args[0].equals("")) {
					plugin.sendMessage(playerName, "&cYou have an extra space in there!");
					return true;
				}
				else if(args[0].equalsIgnoreCase("list")) {
					if(args.length > 1) {
						plugin.sendMessage(playerName, "&cToo many arguments!");
						return true;
					}
					
					plugin.sendMessage(playerName, "&9Chat channels:");
					String channelsListString = new String("");
					boolean first = true;
					for(String key: plugin.channels.keySet()) {					
						// clear any stupid channels
						if(plugin.getChannel(key).channelName.equals("") || (plugin.getChannel(key).players.size() < 1 && !plugin.getChannel(key).persistant)) {
							plugin.removeChannel(key);
							continue;
						}
						
						// filter for permissions
						// if the player doesn't have permission to see everything
						boolean hasPerms = true;
						if(!plugin.hasPermission(player, "mcnsachat.listall")) {
							if(plugin.getChannel(key).permissions.size() > 0) {
								// ok, the channel DOES have permisssions, let's see if we have them
								hasPerms = false;
								// now search through all the permissions
								for(int i = 0; !hasPerms && i < plugin.getChannel(key).permissions.size(); i++) {
									if(plugin.hasPermission(player, plugin.getChannel(key).permissions.get(i))) {
										hasPerms = true;
									}
								}
							}
						}
						
						if(!hasPerms || !plugin.getChannel(key).password.equals("")) {
							// ok, they did NOT have permission to see (or it was password blocked)
							// (or it was empty, ie people logged out of it)
							// Skip along!
							continue;
						}
						
						channelsListString = channelsListString + ((first) ? ("&" + Integer.toHexString(plugin.getChannel(key).colour)) : ("&f, &" + Integer.toHexString(plugin.getChannel(key).colour))) + plugin.channels.get(key).channelName + ((!plugin.getChannel(key).password.equals("")) ? ("&f[*]") : (""));
						// if it was our first time, it no longer can be..
						if(first) {
							first = false;
						}
					}
					plugin.sendMessage(playerName, channelsListString);
					return true;
				}
				else if(args[0].equalsIgnoreCase("who")) {
					if(args.length > 1) {
						plugin.sendMessage(playerName, "&cToo many arguments!");
						return true;
					}
					
					// get a list of everyone in the channel and say it!
					plugin.sendMessage(playerName, "&bPeople who can hear you now:");
					boolean first = true;
					String namesList = new String("");
					// get all the listeners..
					HashSet<String> listeners = plugin.getChannel(plugin.getPlayer(player).channel).getListeners(plugin, player);
					Iterator<String> itr = listeners.iterator();
					while(itr.hasNext()) {
						String listener = itr.next();
						// make sure they're not poofed
						if(!plugin.getPlayer(listener).poofed || plugin.hasPermission(player, "mcnsachat.poof")) {
							namesList = namesList + ((first)?("&7"):("&f, &7")) + listener + ((plugin.getPlayer(listener).poofed) ? ("&b*") : (""));
							if(first) {
								first = false;
							}
						}
					}
					plugin.sendMessage(player, namesList);
					
					// and we're done.
					return true;
				}
				else if(args[0].equalsIgnoreCase("mute")) {
					// make sure we have the proper args
					if(args.length != 2) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					// check for list command
					if(args[1].equalsIgnoreCase("list")) {
						plugin.sendMessage(player.getName(), "&bYou have muted the following people:");
						// format the players string
						String playersString = new String("");
						Iterator<String> itr = plugin.getPlayer(player).mutedPlayers.iterator();
						boolean first = true;
						while(itr.hasNext()) {
							String mutedPlayer = itr.next();
							if(plugin.getServer().getPlayer(mutedPlayer) != null) {
								// player is online!
								playersString = playersString + ((first) ? "&7" : "&f, &7") + mutedPlayer;
							}
							else {
								// player is offline!
								playersString = playersString + ((first) ? "&8" : "&f, &8") + mutedPlayer;
							}
							if(first) {
								first = false;
							}
						}
						plugin.sendMessage(player.getName(), playersString);
						return true;
					}
					
					// make sure they don't already have the player muted
					if(plugin.getPlayer(player).mutedPlayers.contains(args[1])) {
						plugin.sendMessage(playerName, "&cYou've already muted that person!");
						return true;
					}

					// get the name
					Player mutee = plugin.getServer().getPlayer(args[1]);
					if(mutee == null) {
						// that player did not exist :/
						plugin.sendMessage(playerName, "&cThat player does not exist!");
						return true;
					}
					
					// make sure the player CAN be muted
					if(plugin.hasPermission(plugin.getServer().getPlayer(mutee.getName()), "mcnsachat.cannotmute")) {
						plugin.sendMessage(playerName, "&cYou cannot mute that person!");
						return true;
					}
					
					// add them to the mute list!
					plugin.getPlayer(player).mutedPlayers.add(mutee.getName());
					plugin.sendMessage(playerName, "&b" + mutee.getName() + " has been muted!");
					plugin.sendMessage(mutee.getName(), "&bYou have been muted by " +playerName+"!");
					
					// we're done here
					return true;
				}
				else if(args[0].equalsIgnoreCase("unmute")) {
					// make sure we have the proper args
					if(args.length != 2) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}

					// get the name
					Player unmutee = plugin.getServer().getPlayer(args[1]);
					if(unmutee == null) {
						// that player did not exist :/
						plugin.sendMessage(playerName, "&cThat player does not exist!");
						return true;
					}
					
					// make sure they don't already have the player muted
					if(!plugin.getPlayer(player).mutedPlayers.contains(unmutee.getName())) {
						plugin.sendMessage(playerName, "&cYou don't have that person muted!");
						return true;
					}
					
					// remove them from the mute list
					plugin.getPlayer(player).mutedPlayers.remove(unmutee.getName());
					plugin.sendMessage(playerName, "&b" + unmutee.getName() + " has been unmuted!");
					plugin.sendMessage(unmutee.getName(), "&bYou have been unmuted by " +playerName+"!");
					
					// we're done here
					return true;
				}
				else if(args[0].equalsIgnoreCase("move")) {
					if(!plugin.hasPermission(player, "mcnsachat.move")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					if(args.length != 4 || !args[2].equalsIgnoreCase("to")) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					// grab the player
					Player targetPlayer = plugin.getServer().getPlayer(args[1]);
					String targetChannel = new String(args[3]);
					if(targetPlayer == null) {
						// that player did not exist :/
						plugin.sendMessage(playerName, "&cThat player does not exist!");
						return true;
					}
					
					// move their channel
					// first alert them
					plugin.sendMessage(playerName, "&b" + targetPlayer.getName() + " has been moved to "+targetChannel+"!");
					plugin.sendMessage(targetPlayer.getName(), "&bYou have been moved to the "+plugin.getChannel(targetChannel).channelName+" channel by " +playerName+"!");
					
					// remove them from the old channel..
					String oldChannel = new String(plugin.getPlayer(targetPlayer).channel);
					plugin.getChannel(oldChannel).removePlayer(plugin, targetPlayer, true);
					// clear empty channels
					plugin.checkChannel(oldChannel);
					int colour = -1;
					// see if it is a new channel (if it is, set it's colour)
					if(!plugin.channels.containsKey(targetChannel.toLowerCase())) {
						colour = plugin.defaultColour;
					}
					// ok, now add them..
					plugin.getChannel(targetChannel).addPlayer(plugin, targetPlayer);
					// set the colour if we need to..
					if(colour > -1) {
						plugin.getChannel(targetChannel).colour = colour;
					}
					
					// we're done here
					return true;
				}
				else if(args[0].equalsIgnoreCase("movelock")) {
					if(!plugin.hasPermission(player, "mcnsachat.move") || !plugin.hasPermission(player, "mcnsachat.lock")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					if(args.length != 4 || !args[2].equalsIgnoreCase("to")) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					// grab the player
					Player targetPlayer = plugin.getServer().getPlayer(args[1]);
					String targetChannel = new String(args[3]);
					if(targetPlayer == null) {
						// that player did not exist :/
						plugin.sendMessage(playerName, "&cThat player does not exist!");
						return true;
					}
					
					// move their channel
					// first alert them
					plugin.sendMessage(playerName, "&b" + targetPlayer.getName() + " has been moved to "+targetChannel+"!");
					plugin.sendMessage(targetPlayer.getName(), "&bYou have been moved and locked to the "+plugin.getChannel(targetChannel).channelName+" channel by " +playerName+"!");
					
					// remove them from the old channel..
					String oldChannel = new String(plugin.getPlayer(targetPlayer).channel);
					plugin.getChannel(oldChannel).removePlayer(plugin, targetPlayer, true);
					// clear empty channels
					plugin.checkChannel(oldChannel);
					int colour = -1;
					// see if it is a new channel (if it is, set it's colour)
					if(!plugin.channels.containsKey(targetChannel.toLowerCase())) {
						colour = plugin.defaultColour;
					}
					// ok, now add them..
					plugin.getChannel(targetChannel).addPlayer(plugin, targetPlayer);
					// set the colour if we need to..
					if(colour > -1) {
						plugin.getChannel(targetChannel).colour = colour;
					}
					
					// and finally, lock them
					plugin.getPlayer(targetPlayer).locked = true;
					
					// we're done here
					return true;
				}
				else if(args[0].equalsIgnoreCase("lock")) {
					if(!plugin.hasPermission(player, "mcnsachat.lock")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					// check for the proper thingy
					if(args.length != 2) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					// grab the player
					Player targetPlayer = plugin.getServer().getPlayer(args[1]);
					if(targetPlayer == null) {
						// that player did not exist :/
						plugin.sendMessage(playerName, "&cThat player does not exist!");
						return true;
					}
					
					// go ahead and lock them
					plugin.getPlayer(targetPlayer).locked = true;
					plugin.sendMessage(playerName, "&b" + targetPlayer.getName() + " has been locked in their channel!");
					plugin.sendMessage(targetPlayer.getName(), "&bYou have been locked in your channel by " +playerName+"!");
					
					// we're done here
					return true;
				}
				else if(args[0].equalsIgnoreCase("unlock")) {
					if(!plugin.hasPermission(player, "mcnsachat.lock")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					// check for the proper thingy
					if(args.length != 2) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					// grab the player
					Player targetPlayer = plugin.getServer().getPlayer(args[1]);
					if(targetPlayer == null) {
						// that player did not exist :/
						plugin.sendMessage(playerName, "&cThat player does not exist!");
						return true;
					}
					
					// go ahead and lock them
					plugin.getPlayer(targetPlayer).locked = false;
					plugin.sendMessage(playerName, "&b" + targetPlayer.getName() + " has been unlocked from their channel!");
					plugin.sendMessage(targetPlayer.getName(), "&bYou have been unlocked from your channel by " +playerName+"!");
					
					// we're done here
					return true;
				}
				else if(args[0].equalsIgnoreCase("search")) {
					if(!plugin.hasPermission(player, "mcnsachat.search")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					// check for the proper thingy
					if(args.length != 2) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					// find the player name
					String searcheeName = args[1];
					// try to auto-complete the name
					if(plugin.getServer().getPlayer(searcheeName) != null) {
						// ok, we found a player. set that name!
						searcheeName = plugin.getServer().getPlayer(searcheeName).getName();
					}
					
					// ok, see if they exist in our records
					if(!plugin.players.containsKey(searcheeName)) {
						// they do not exist!
						plugin.sendMessage(playerName, "&cThat player does not exist in our records!");
						return true;
					}
					
					// make sure they are not poofed
					if(plugin.getPlayer(searcheeName).poofed) {
						// they are poofed
						plugin.sendMessage(playerName, "&cThat player is being sneaky!");
						return true;
					}
					
					// they exist! inform..
					plugin.sendMessage(playerName, "&b" + searcheeName + " is in the \""+plugin.getChannel(plugin.getPlayer(searcheeName).channel).channelName+"\" channel!");
					
					return true;
				}
				else if(args[0].equalsIgnoreCase("+poof")) {
					if(!plugin.hasPermission(player, "mcnsachat.poof")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}

					if(args.length > 1) {
						plugin.sendMessage(playerName, "&cToo many arguments!");
						return true;
					}
					
					// they have perms, set it up!
					plugin.getPlayer(player).poofed = true;
					plugin.sendMessage(playerName, "&bYou are now hidden in the chat channels (until you talk, of course)!");
					return true;
				}
				else if(args[0].equalsIgnoreCase("-poof")) {
					if(!plugin.hasPermission(player, "mcnsachat.poof")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}

					if(args.length > 1) {
						plugin.sendMessage(playerName, "&cToo many arguments!");
						return true;
					}
					
					plugin.getPlayer(player).poofed = false;
					plugin.sendMessage(playerName, "&bYou are no longer hidden in the chat channels!");
					return true;
				}
				else if(args[0].equalsIgnoreCase("+listen")) {
					if(!plugin.hasPermission(player, "mcnsachat.listen")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					// check for the proper thingy
					if(args.length != 2) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					// make sure the channel they're looking for exists
					if(!plugin.channels.containsKey(args[1].toLowerCase())) {
						plugin.sendMessage(playerName, "&cThat channel does not exist!");
						return true;
					}
					
					// make sure they're not already listening in to the channel
					if(plugin.getPlayer(player).listeningChannels.contains(args[1].toLowerCase())) {
						plugin.sendMessage(playerName, "&cYou're already listening to that channel!");
						return true;
					}
					
					// ok, add it to the listening list
					plugin.getPlayer(player).listeningChannels.add(args[1].toLowerCase());
					plugin.sendMessage(playerName, "&bYou are now listening in on the "+plugin.getChannel(args[1]).channelName+" channel!");
					
					return true;
				}
				else if(args[0].equalsIgnoreCase("-listen")) {
					if(!plugin.hasPermission(player, "mcnsachat.listen")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					// check for the proper thingy
					if(args.length != 2) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					// make sure they're listening in to the channel
					if(!plugin.getPlayer(player).listeningChannels.contains(args[1].toLowerCase())) {
						plugin.sendMessage(playerName, "&cYou're not listening to that channel!");
						return true;
					}
					
					// ok, remove it
					plugin.getPlayer(player).listeningChannels.remove(args[1].toLowerCase());
					plugin.sendMessage(playerName, "&bYou are no longer listening in on the "+plugin.getChannel(args[1]).channelName+" channel!");
					
					return true;
				}
				else if(args[0].equalsIgnoreCase("+seeall")) {
					if(!plugin.hasPermission(player, "mcnsachat.seeall")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}

					if(args.length > 1) {
						plugin.sendMessage(playerName, "&cToo many arguments!");
						return true;
					}
					
					// they have perms, set it up!
					plugin.getPlayer(player).seeall = true;
					plugin.sendMessage(playerName, "&bYou can now see all chat channels!");
					return true;
				}
				else if(args[0].equalsIgnoreCase("-seeall")) {
					if(!plugin.hasPermission(player, "mcnsachat.seeall")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}

					if(args.length > 1) {
						plugin.sendMessage(playerName, "&cToo many arguments!");
						return true;
					}
					
					// they have perms, set it up!
					plugin.getPlayer(player).seeall = false;
					plugin.sendMessage(playerName, "&bYou can no longer see all chat channels!");
					return true;
				}
				else if(args[0].equalsIgnoreCase("colour") || args[0].equalsIgnoreCase("color")) {
					if(!(plugin.hasPermission(player, "mcnsachat.channelcolour") || plugin.hasPermission(player, "mcnsachat.channelcolor"))) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					// check for the proper thingy
					if(args.length != 2) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					String colourName = args[1].toLowerCase();
					if(!plugin.colours.containsKey(colourName)) {
						// uh-oh! they did not supply a proper colour
						// let them know!
						plugin.sendMessage(playerName, "&cThat colour doesn't exist!");
						plugin.sendMessage(playerName, "&bAllowable colours:");
						String allowableColours = new String("");
						boolean first = true;
						for(String key: plugin.colours.keySet()) {
							allowableColours = allowableColours + ((first) ? ("&"+Integer.toHexString(plugin.colours.get(key))) : ("&f, &"+Integer.toHexString(plugin.colours.get(key)))) + key;
							if(first) {
								first = false;
							}
						}
						plugin.sendMessage(playerName, allowableColours);
						return true;
					}
					
					// they have a valid colour, set it!
					plugin.getChannel(plugin.getPlayer(player).channel).colour = plugin.colours.get(colourName);
					// cheers
					plugin.sendMessage(playerName, "&bThis channel's colour has been &"+Integer.toHexString(plugin.colours.get(colourName))+"changed!");
					return true;
				}
				else if(args[0].equalsIgnoreCase("invite")) {
					if(args.length != 2) {
						plugin.sendMessage(playerName, "&cInvalid command usage!");
						return true;
					}
					
					// grab the player
					Player targetPlayer = plugin.getServer().getPlayer(args[1]);
					if(targetPlayer == null) {
						// that player did not exist :/
						plugin.sendMessage(playerName, "&cThat player does not exist!");
						return true;
					}
					
					// ok, go ahead and invite them
					plugin.sendMessage(playerName, "&b" + targetPlayer.getName() + " has been invited to your channel!");
					plugin.sendMessage(targetPlayer.getName(), "&bYou have been invited to the "+plugin.getChannel(plugin.getPlayer(player).channel).channelName+" channel by "+playerName+"!");
					plugin.sendMessage(targetPlayer.getName(), "&bType \"/ch "+plugin.getChannel(plugin.getPlayer(player).channel).channelName+"\" to join it!");
					
					// we're done here..
					return true;
				}
				else if(args[0].equalsIgnoreCase("password")) {
					if(!plugin.hasPermission(player, "mcnsachat.password")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					// check for the proper thingy
					if(args.length == 2) {
						plugin.getChannel(plugin.getPlayer(player).channel).password = args[1];
						plugin.sendMessage(playerName, "&bYou have set the "+plugin.getChannel(plugin.getPlayer(player).channel).channelName+" channel's password to \""+args[1]+"\"!");
						return true;
					}
					else if(args.length == 1) {
						// reset the password..
						plugin.getChannel(plugin.getPlayer(player).channel).password = "";
						plugin.sendMessage(playerName, "&bYou have cleared the "+plugin.getChannel(plugin.getPlayer(player).channel).channelName+" channel's password!");
					}
					else {
						plugin.sendMessage(playerName, "&cToo many arguments!");
					}

					// we're done here..
					return true;
				}
				else if(args[0].equalsIgnoreCase("timeout")) {
					if(!plugin.hasPermission(player, "mcnsachat.timeout")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					if(args.length != 2 && args.length != 3) {
						plugin.sendMessage(player, "&cIncorrect usage!");
						return true;
					}
					
					// see if we're listing instead of timeouting
					if(args[1].equalsIgnoreCase("list")) {
						plugin.sendMessage(player, "&3Players on timeouts:");
						String message = new String("");
						for(String key: plugin.players.keySet()) {
							if(plugin.getPlayer(key).onTimeout) {
								message += "&f" + key + "&b (" + String.format("%.2f", (((float)((plugin.getPlayer(key).timeoutLength - ((System.currentTimeMillis() / 1000) - plugin.getPlayer(key).timeoutBegin)))) / 60)) + "m), ";
							}
						}
						plugin.sendMessage(player, message);
						return true;
					}
					
					if(args.length != 3) {
						plugin.sendMessage(player, "&cIncorrect usage!");
						return true;
					}
					
					// now find the player..
					String targetPlayer = new String(args[1]);
					if(plugin.getServer().getPlayer(targetPlayer) == null) {
						plugin.sendMessage(player, "&cThat player doesn't exist!");
						return true;
					}
					targetPlayer = plugin.getServer().getPlayer(targetPlayer).getName();
					
					// make sure that person isn't already on a timeout
					if(plugin.getPlayer(targetPlayer).onTimeout) {
						plugin.sendMessage(player, "&cThat player is already on a timeout!");
						return true;
					}
					
					int timeoutTime = 0;
					try {
						timeoutTime = Integer.parseInt(args[2]);
					}
					catch(Exception e) {
						plugin.sendMessage(player, "&cThat is not a valid timeframe!");
						return true;
					}
					
					if((60000 * (long)timeoutTime) < 1) {
						plugin.sendMessage(player, "&cThat is not a valid timeframe!");
						return true;
					}
					
					// ok, so we have the person and time
					// create the timer!
					plugin.getPlayer(targetPlayer).timeoutBegin = System.currentTimeMillis() / 1000;
					if(plugin.playerTimers.containsKey(targetPlayer)) {
						plugin.playerTimers.remove(targetPlayer);
					}
					plugin.getPlayerTimer(targetPlayer).schedule(new MCNSAChatUnTimeoutTask(plugin, targetPlayer), (60000 * (long)timeoutTime));
					plugin.getPlayer(targetPlayer).onTimeout = true;
					plugin.getPlayer(targetPlayer).timeoutLength = 60 * timeoutTime;
					
					// notify!
					//plugin.sendMessage(player, "&7" + targetPlayer + "&b has been put in timeout for " + timeoutTime + " minutes!");
					plugin.sendMessage(targetPlayer, "&7" + playerName + "&b has put you in timeout for " + timeoutTime + " minutes!");
					plugin.log.info("[MCNSAChat] " + playerName + " has put " + targetPlayer + " in timeout for " + timeoutTime + " minutes");
					
					// if we're announcing to the server, do so!
					if(plugin.announceTimeouts) {
						Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
						for(int i = 0; i < onlinePlayers.length; i++) {
							plugin.sendMessage(onlinePlayers[i], "&7" + playerName + " &bhas sent &7" + targetPlayer + " &bto timeout for " + timeoutTime + " minutes!");
						}
					}
								
					// we're done
					return true;
				}
				else if(args[0].equalsIgnoreCase("untimeout")) {
					if(!plugin.hasPermission(player, "mcnsachat.timeout")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					if(args.length != 2) {
						plugin.sendMessage(player, "&cIncorrect usage!");
						return true;
					}
					
					// now find the player..
					String targetPlayer = new String(args[1]);
					if(plugin.getServer().getPlayer(targetPlayer) == null) {
						plugin.sendMessage(player, "&cThat player doesn't exist!");
						return true;
					}
					targetPlayer = plugin.getServer().getPlayer(targetPlayer).getName();
					
					// make sure that person is already on a timeout
					if(!plugin.getPlayer(targetPlayer).onTimeout) {
						plugin.sendMessage(player, "&cThat player isn't on a timeout!");
						return true;
					}
					
					// ok, untimeout them!
					// cancel the timer..
					plugin.getPlayerTimer(targetPlayer).cancel();
					plugin.playerTimers.remove(targetPlayer);
					plugin.getPlayer(player).onTimeout = false;
					plugin.getPlayer(player).timeoutBegin = -1;
					plugin.getPlayer(targetPlayer).timeoutLength = -1;

					// notify
					//plugin.sendMessage(player, "&f" + targetPlayer + "&b has been pulled from their timeout!");
					plugin.sendMessage(targetPlayer, "&7" + playerName + "&b has pulled you from your timeout! You can talk again!");
					plugin.log.info("[MCNSAChat] " + playerName + " has pulled " + targetPlayer + " from their timeout");
					if(plugin.announceTimeouts) {
						Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
						for(int i = 0; i < onlinePlayers.length; i++) {
							plugin.sendMessage(onlinePlayers[i], "&7" + targetPlayer + "&b has been pulled from their timeout!");
						}
					}
					
					return true;
				}
				// the following is not working properly atm
				else if(args[0].equalsIgnoreCase("reset")) {
					if(!plugin.hasPermission(player, "mcnsachat.reset")) {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
						return true;
					}
					
					if(args.length != 2 || !args[1].equalsIgnoreCase("-a")) {
						// send a confirmation message
						plugin.sendMessage(player, "&5Are you sure you want to reset the chat channels?");
						plugin.sendMessage(player, "&5This will delete all channels and player information");
						plugin.sendMessage(player, "&5such as mutes, locks, etc, and send everyone to the");
						plugin.sendMessage(player, "&5the default channel. To confirm, send \"/ch reset -a\"");
						return true;
					}
					
					plugin.sendMessage(player, "&5Resetting chat channels..");
					plugin.log.info("[MCNSAChat] " + playerName + " is resetting the chat system...");
					
					// reset all data
					plugin.aliases.clear();
					plugin.channels.clear();
					plugin.players.clear();
					plugin.colours.clear();
					plugin.playerTimers.clear();
					// close it down..
					plugin.onDisable();
					
					plugin.log.info("[MCNSAChat] chat data cleared!");
					
					// and reload..
					plugin.onEnable();
					
					// and we're done!
					plugin.sendMessage(player, "&5MCNSA Chat has been reset!");
					return true;
				}
				else if(args[0].equalsIgnoreCase("toggleconnect")) {
					if(plugin.hasPermission(player, "mcnsachat.toggleconnect")) {
						plugin.hideConnectDisconnect = !plugin.hideConnectDisconnect;
						if(!plugin.hideConnectDisconnect) {
							plugin.sendMessage(player, "&aConnect / disconnect messages enabled!");
						}
						else {
							plugin.sendMessage(player, "&cConnect / disconnect messages disabled!");
						}
					}
					else {
						plugin.sendMessage(playerName, "&cYou don't have permission to do that!");
					}
					
					return true;
				}
				else if(args[0].equalsIgnoreCase("help")) {
					if(args.length > 1) {
						if(args[1].equalsIgnoreCase("aliases")) {
							showHelp(player, -1);
						}
						else {
							try {
								showHelp(player, Integer.parseInt(args[1]) - 1);	
							}
							catch(Exception e) {
								showHelp(player, 0);
							}
						}
					}
					else {
						showHelp(player, 0);
					}
					
					return true;
				}
				
				// ok, so we've checked out all the possible commands
				// guess they want to join a channel!
				
				if(args.length > 2) {
					plugin.sendMessage(playerName, "&cToo many arguments!");
					return true;
				}
				
				
				// first make sure they're not already IN the channel
				if(args[0].equalsIgnoreCase(plugin.getPlayer(player).channel)) {
					plugin.sendMessage(playerName, "&cYou're already in that channel!");
					return true;
				}
				
				boolean newChannel = true;
				if(plugin.channels.containsKey(args[0].toLowerCase())) {
					newChannel = false;
				}
				
				// make sure they have proper permissions
				if(plugin.getChannel(args[0]).permissions.size() > 0) {
					// ok, the channel has permissions,
					// check them out!
					// loop through all the permissions
					boolean hasPerms = false;
					for(int i = 0; !hasPerms && i < plugin.getChannel(args[0]).permissions.size(); i++) {
						// if they have a permission that is listed, check it
						if(plugin.hasPermission(player, plugin.getChannel(args[0]).permissions.get(i))) {
							hasPerms = true;
						}
					}
					
					// if we do NOT have perms, don't let us in!
					if(!hasPerms) {
						plugin.sendMessage(playerName, "&cYou don't have permission for that channel!");
						return true;
					}
				}
				
				// ok, passed permission check
				// now for passwords
				
				// check to see if we're CREATING a channel with a password
				// (password will = "" if no password is requested)
				String password = new String("");
				if(!newChannel) {
					// channel already exists, check for passwords...
					if(!plugin.getChannel(args[0]).password.equals("")) {
						// there is a password, check it
						if(args.length < 2) {
							// they didn't supply a password :/
							plugin.sendMessage(playerName, "&cThat channel is password protected!");
							return true;
						}
						
						// check the passwords
						if(!args[1].equals(plugin.getChannel(args[0]).password)) {
							// password DOES NOT match
							plugin.sendMessage(playerName, "&cThat is not the password for the channel!");
							return true;
						}
					}
				}
				// now check to see if they supplied a password
				else if(args.length == 2) {
					// ok, they want a password-protected channel!
					// set it!
					password = args[1];
				}
				
				// ok, they passed the password check
				// check whether they are locked or not
				if(plugin.getPlayer(player).locked) {
					// locked - cannot change channel
					plugin.sendMessage(playerName, "&cYou have been locked in this channel and cannot leave!");
					return true;
				}
				
				// ok, FINE, let them in
				// better let them into the channel!
				// but first, remove them from the old channel..
				String oldChannel = new String(plugin.getPlayer(player).channel);
				
				plugin.getChannel(oldChannel).removePlayer(plugin, player, true);
				// clear empty channels
				plugin.checkChannel(oldChannel);
				int colour = -1;
				// see if it is a new channel (if it is, set it's colour)
				if(newChannel) {
					colour = plugin.defaultColour;
				}
				// ok, now add them..
				
				plugin.getChannel(args[0]).addPlayer(plugin, player);
				// set the colour if we need to..
				if(colour > -1) {
					plugin.getChannel(args[0]).colour = colour;
				}
				// if they were creating and added a password, set that password here:
				if(!password.equals("")) {
					plugin.getChannel(args[0]).password = password;
					plugin.sendMessage(playerName, "&bYou created this channel with the following password:");
					plugin.sendMessage(playerName, "&b\"&f" + password + "&b\"");
				}
				
				// and we're done
				return true;
			}
		}
		// ok, /ch is taken care of
		// now check for aliases
		else if(plugin.aliases.containsKey(label.toLowerCase())) {
			// ok, the command is an alias
			// get the channel they want to deal with
			String aliasChannel = new String(plugin.aliases.get(label.toLowerCase()));
			
			// make sure they have proper permissions
			if(plugin.getChannel(aliasChannel).permissions.size() > 0) {
				// ok, the channel has permissions,
				// check them out!
				// loop through all the permissions
				boolean hasPerms = false;
				for(int i = 0; !hasPerms && i < plugin.getChannel(aliasChannel).permissions.size(); i++) {
					// if they have a permission that is listed, check it
					if(plugin.hasPermission(player, plugin.getChannel(aliasChannel).permissions.get(i))) {
						hasPerms = true;
					}
				}
				
				// if we do NOT have perms, don't let us in!
				if(!hasPerms) {
					plugin.sendMessage(playerName, "&cYou don't have permission for that channel!");
					return true;
				}
			}
			
			// see if they're just broadcasting to that channel
			// or if they want to switch to it
			// if they want to switch, there will be no arguments
			// if they just want to chat, there WILL be arguments
			if(args.length == 0) {
				// they wish to switch
				// note: they already have permission to..
				// also, there should not be a password on channels with aliases
				
				// check whether they are locked or not
				if(plugin.getPlayer(player).locked) {
					// locked - cannot change channel
					plugin.sendMessage(playerName, "&cYou have been locked in this channel and cannot leave!");
					return true;
				}
				
				// ok, FINE, let them in
				// better let them into the channel!
				// but first, remove them from the old channel..
				String oldChannel = new String(plugin.getPlayer(player).channel);				
				plugin.getChannel(oldChannel).removePlayer(plugin, player, true);
				// clear empty channels
				plugin.checkChannel(oldChannel);
				// add them..
				plugin.getChannel(aliasChannel).addPlayer(plugin, player);
				// done!
				return true;
			}
			else {
				// they wish to chat				
				// get their message
				String message = new String(args[0]);
				
				if(message.startsWith("/")) {
					message = "." + message;
				}
				
				// and send the chat..
				// change their channel first
				String playerChannel = new String(plugin.getPlayer(player).channel);
				plugin.getPlayer(player).channel = aliasChannel;
				// now make them chat
				player.chat(message);
				// and revert their channel!
				plugin.getPlayer(player).channel = playerChannel;
				
				// and we're done
				return true;
			}
		}
		
		
		// show help by default
		showHelp(player, 0);
		return true;
	}
	
	// show formatted help
	// help depends on the permissions of player
	private void showHelp(Player player, Integer page) {
		if(page >= 0) {
			// generate a list of all help items that this player has access to
			ArrayList<HelpItem> helpList = new ArrayList<HelpItem>();
			
			// start with default commands
			helpList.add(new HelpItem("&3/ch &bhelp &f[page #]", "&7shows this help (for a given page)"));
			helpList.add(new HelpItem("&3/ch &bhelp &faliases", "&7shows help for channel aliases"));
			helpList.add(new HelpItem("&3/ch &f<channel> [password]", "&7joins/creates &f<channel> &7with optional password"));
			helpList.add(new HelpItem("&3/ch &blist", "&7lists all current channels on the server"));
			helpList.add(new HelpItem("&3/ch &bwho", "&7lists all online players in your channel"));
			helpList.add(new HelpItem("&3/ch &bmute &f<player>", "&7makes it so that you cannot hear &f<player> &7anymore"));
			helpList.add(new HelpItem("&3/ch &bunmute &f<player>", "&7unmutes &f<player>"));
			helpList.add(new HelpItem("&3/ch &bmute list", "&7lists everyone that you have muted"));
			helpList.add(new HelpItem("&3/ch &binvite &f<player>", "&7invites &f<player> &7to join your channel"));
			
			// now add permission-based commands
			if(plugin.hasPermission(player, "mcnsachat.move")) {
				helpList.add(new HelpItem("&3/ch &bmove &f<player> &bto &f<channel>", "&7moves &f<player> &7to &f<channel>"));
			}
			if(plugin.hasPermission(player, "mcnsachat.lock")) {
				helpList.add(new HelpItem("&3/ch &block &f<player>", "&7prevents &f<player> &7from changing channels"));
			}
			if(plugin.hasPermission(player, "mcnsachat.lock") && plugin.hasPermission(player, "mcnsachat.move")) {
				helpList.add(new HelpItem("&3/ch &bmovelock &f<player> &bto &f<channel>", "&7moves AND locks &f<player>"));
			}
			if(plugin.hasPermission(player, "mcnsachat.unlock")) {
				helpList.add(new HelpItem("&3/ch &bunlock &f<player>", "&7allows &f<player> &7to change channels"));
			}
			if(plugin.hasPermission(player, "mcnsachat.search")) {
				helpList.add(new HelpItem("&3/ch &bsearch &f<player>", "&7informs you which channel &f<player> &7is in"));
			}
			if(plugin.hasPermission(player, "mcnsachat.poof")) {
				helpList.add(new HelpItem("&3/ch &7[&f+&7|&f-&7]&bpoof", "&7channel enter/leave stealth mode, + = on, - = off"));
			}
			if(plugin.hasPermission(player, "mcnsachat.listen")) {
				helpList.add(new HelpItem("&3/ch &7[&f+&7|&f-&7]&blisten <channel>", "&7listens in on a specific channel, + = on, - = off"));
			}
			if(plugin.hasPermission(player, "mcnsachat.seeall")) {
				helpList.add(new HelpItem("&3/ch &7[&f+&7|&f-&7]&bseeall", "&7see chat from ALL channels, + = on, - = off"));
			}
			if(plugin.hasPermission(player, "mcnsachat.colour") || plugin.hasPermission(player, "mcnsachat.color")) {
				helpList.add(new HelpItem("&3/ch &bcolour &f<colour>", "&7changes the colour of your current channel"));
			}
			if(plugin.hasPermission(player, "mcnsachat.password")) {
				helpList.add(new HelpItem("&3/ch &bpassword &f<password>", "&7changes the password of your current channel"));
			}
			if(plugin.hasPermission(player, "mcnsachat.reset")) {
				helpList.add(new HelpItem("&3/ch &breset", "&7Resets the entire chat channel system"));
			}
			if(plugin.hasPermission(player, "mcnsachat.timeout")) {
				helpList.add(new HelpItem("&3/ch &btimeout &f<player> &f<minutes>", "&7Puts &f<player> &7in timeout (cannot talk) for &f<minutes> &7minutes"));
			}
			if(plugin.hasPermission(player, "mcnsachat.timeout")) {
				helpList.add(new HelpItem("&3/ch &btimeout &flist", "&7Lists all players who are current in timeouts"));
			}
			if(plugin.hasPermission(player, "mcnsachat.timeout")) {
				helpList.add(new HelpItem("&3/ch &buntimeout &f<player>", "&7Removes &f<player> &7from timeout early"));
			}
			if(plugin.hasPermission(player, "mcnsachat.toggleconnect")) {
				helpList.add(new HelpItem("&3/ch &btoggleconnect", "&7Toggles player connect/disconnect notifications"));
			}
			
			// get the total number of pages this person can see
			Integer numPages = helpList.size() / 5;
			if(helpList.size() % 5 != 0) {
				numPages++;
			}
			
			// make sure we have a valid page
			if(page > numPages || page < 0) {
				page = 0;
			}
			
			// get the start and end indices
			Integer start = page*5;
			Integer end = start+5;
			if(end > helpList.size()) {
				end = helpList.size();
			}
			
			plugin.sendMessage(player, "&7--- &3Chat Help &7- &bPage &f"+Integer.toString(page+1)+"&7/&f"+numPages+" &7---");
			for(int i = start; i < end; i++) {
				plugin.sendMessage(player, helpList.get(i).command);
				plugin.sendMessage(player, "     " + helpList.get(i).description);
			}
		}
		else {
			// they want help about aliases
			plugin.sendMessage(player, "&7--- &3Chat Aliases Help &7---");
			plugin.sendMessage(player, "&bAliases are ways of quickly joining or talking");
			plugin.sendMessage(player, "&bto other channels. To use an alias to join a channel,");
			plugin.sendMessage(player, "&btype \"&f/<alias>&f&b\" where &f<alias>&b is one of");
			plugin.sendMessage(player, "&bthe aliases listed below. Similarly, to just send a");
			plugin.sendMessage(player, "&bquick message to a channel type \"&f/<alias> <message>&b\"");
			plugin.sendMessage(player, "&3Aliases that you can use:");
			for(String alias: plugin.aliases.keySet()) {
				if(plugin.getChannel(plugin.aliases.get(alias)).permissions.size() > 0) {
					// it has perms, see if they pass
					boolean hasPerms = false;
					for(int i = 0; !hasPerms && i < plugin.getChannel(plugin.aliases.get(alias)).permissions.size(); i++) {
						if(plugin.hasPermission(player, plugin.getChannel(plugin.aliases.get(alias)).permissions.get(i))) {
							hasPerms = true;
						}
					}
					
					if(!hasPerms) {
						// they don't have permission to see this channel
						// skip along
						continue;
					}
				}

				plugin.sendMessage(player, "&3/" + alias + "   &f(&b" + plugin.getChannel(plugin.aliases.get(alias)).channelName + "&f)");
			}
		}
	}
}
