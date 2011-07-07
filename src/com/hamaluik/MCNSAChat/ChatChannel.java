package com.hamaluik.MCNSAChat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.entity.Player;

public class ChatChannel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5510945016634966837L;

	/**
	* @serial
	*/
	String channelName = new String("untitled");
	
	/**
	* @serial
	*/
	boolean persistant = false;
	
	/**
	* @serial
	*/
	boolean local = false;
	
	/**
	* @serial
	*/
	boolean broadcast = false;
	
	/**
	* @serial
	*/
	boolean hideJoinLeave = false;
	
	/**
	* @serial
	*/
	int colour = 7;

	/**
	* @serial
	*/
	String password = new String("");

	/**
	* @serial
	*/
	String alias = new String("");
	
	/**
	* @serial
	*/
	HashSet<String> players = new HashSet<String>();
	
	/**
	* @serial
	*/
	HashSet<String> defaultListeners = new HashSet<String>();
	
	/**
	* @serial
	*/
	HashSet<String> listeningChannels = new HashSet<String>();
	
	/**
	* @serial
	*/
	ArrayList<String> permissions = new ArrayList<String>();

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		//always perform the default de-serialization first
		aInputStream.defaultReadObject();
	}
	
	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		//perform the default serialization for all non-transient, non-static fields
		aOutputStream.defaultWriteObject();
	}
	
	public HashSet<String> getListeners(MCNSAChat plugin, Player player) {
		// sort out who can hear the message and who cannot
		HashSet<String> nonLocalListeners = new HashSet<String>();
		HashSet<String> listeners = new HashSet<String>();
		
		// sort things out..
		if(broadcast) {
			// send it to EVERYONE!
			Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
			for(int i = 0; i < onlinePlayers.length; i++) {
				listeners.add(onlinePlayers[i].getName());
			}
		}
		else {
			// add all the people in this channel
			nonLocalListeners.addAll(players);
			
			// add all the people in the other channel that listen to this
			Iterator<String> itrA = listeningChannels.iterator();
			while(itrA.hasNext()) {
				nonLocalListeners.addAll(plugin.getChannel(itrA.next()).players);
			}
			
			itrA = nonLocalListeners.iterator();
			while(itrA.hasNext()) {
				// filter for local
				String nonLocalListener = itrA.next();
				if(local || plugin.getChannel(plugin.getPlayer(nonLocalListener).channel).local) {
					// if either of the players are in a local channel
					// make sure only local people are heard
					if(plugin.playerWithinRadius(player.getName(), nonLocalListener, plugin.localChatRadius)) {
						// make sure they're not on timeout
						if(!plugin.getPlayer(nonLocalListener).onTimeout) {
							if(!listeners.contains(nonLocalListener)) {
								listeners.add(nonLocalListener);
							}
						}
					}
				}
				else {
					// normal - not local, add all who are not on timeout
					if(!plugin.getPlayer(nonLocalListener).onTimeout) {
						if(!listeners.contains(nonLocalListener)) {
							listeners.add(nonLocalListener);
						}
					}
				}
			}
		}
		
		// add the sender!
		// (only if they aren't already listening)
		if(!listeners.contains(player.getName())) {
			listeners.add(player.getName());
		}
		
		// add the "default" listeners
		Iterator<String> itrA = defaultListeners.iterator();
		while(itrA.hasNext()) {
			String defaultListener = itrA.next();
			// check for groups
			if(defaultListener.startsWith("g:")) {
				// ok, a group. add all online players in the group
				// first, figure out the group name
				String groupName = defaultListener.substring(2);
				
				// loop through all online players and find those with the proper group
				Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
				for(int n = 0; n < onlinePlayers.length; n++) {
					if(plugin.inGroup(onlinePlayers[n], groupName)) {
						// ok, they're in the group
						// add them in..
						// but only if they're not already in it!
						if(!listeners.contains(onlinePlayers[n].getName())) {
							listeners.add(onlinePlayers[n].getName());
						}
					}
				}
				
			}
			// ok, not a group
			// do things normally
			else {
				if(!listeners.contains(defaultListener)) {
					// only add them if they're online
					if(plugin.getServer().getPlayer(defaultListener) != null) {
						if(!listeners.contains(defaultListener)) {
							listeners.add(defaultListener);
						}
					}
				}
			}
		}
		
		// FUCK IT ALL
		// quadruple check to make sure they have perms to be here
		HashSet<String> finalListeners = new HashSet<String>();
		if(permissions.size() > 0 && !broadcast) {
			itrA = listeners.iterator();
			while(itrA.hasNext()) {
				String listener = itrA.next();
				boolean hasPerms = true;
				Iterator<String> itrB = permissions.iterator();
				while(hasPerms && itrB.hasNext()) {
					if(!plugin.hasPermission(plugin.getServer().getPlayer(listener), itrB.next())) {
						hasPerms = false;
					}
				}
				
				if(hasPerms) {
					finalListeners.add(listener);
				}
			}
		}
		else {
			finalListeners.addAll(listeners);
		}
		
		return finalListeners;
	}
	
	public void chat(MCNSAChat plugin, Player player, String message) {
		// get all the listeners..
		HashSet<String> listeners = getListeners(plugin, player);
		
		// inform them if no one is listening (except mods etc)
		// less than 2 because there will always be at least ONE person
		// listening (themselves)
		if(listeners.size() < 2) {
			plugin.sendMessage(player, "&bNo one is around to hear you!");
		}
		
		// ok, loop through everyone in the server
		// add add anyone who is listening in
		for(String key: plugin.players.keySet()) {
			// first up, those with "see all" toggled on
			if(plugin.getPlayer(key).seeall) {
				if(!listeners.contains(key)) {
					// only add them to the list if they're not already on it
					listeners.add(key);
				}
			}

			// make sure they're not already on the list
			if(!listeners.contains(key)) {
				if(plugin.getPlayer(key).listeningChannels.contains(channelName.toLowerCase())) {
					// ok, they're listening to this channel
					listeners.add(key);
				}
			}
		}
		
		Iterator<String> itr = listeners.iterator();
		while(itr.hasNext()) {
			String listener = itr.next();
			// make sure they don't have this dude muted
			if(plugin.getPlayer(listener).mutedPlayers.contains(player.getName())) {
				// ok, they're muted. skip along.
				continue;
			}
			
			// check if they have permission to add colour to their chat
			// using classic server protocol colour codes
			// (&c for light red, etc)
			if(plugin.hasPermission(player, "mcnsachat.chatcolour") || plugin.hasPermission(player, "mcnsachat.chatcolour")) {
				plugin.sendMessage(listener, "<&"+ Integer.toHexString(colour) + this.channelName + "&f> " + message.replace("^", "&"));
			}
			else {
				plugin.sendMessageNoColour(listener, "<\u00A7"+ Integer.toHexString(colour) + this.channelName + "\u00A7f> " + message);
			}
		}
		
		plugin.log.info("<"+this.channelName+"> " + message);
	}
	
	public void emote(MCNSAChat plugin, Player player, String message) {
		// get all the listeners..
		HashSet<String> listeners = getListeners(plugin, player);
		
		// inform them if no one is listening (except mods etc)
		// less than 2 because there will always be at least ONE person
		// listening (themselves)
		if(listeners.size() < 2) {
			plugin.sendMessage(player, "&bNo one is around to hear you!");
		}
		
		// ok, loop through everyone in the server
		// add add anyone who is listening in
		for(String key: plugin.players.keySet()) {
			// first up, those with "see all" toggled on
			if(plugin.getPlayer(key).seeall) {
				if(!listeners.contains(key)) {
					// only add them to the list if they're not already on it
					listeners.add(key);
				}
			}

			// make sure they're not already on the list
			if(!listeners.contains(key)) {
				if(plugin.getPlayer(key).listeningChannels.contains(channelName.toLowerCase())) {
					// ok, they're listening to this channel
					listeners.add(key);
				}
			}
		}
		
		Iterator<String> itr = listeners.iterator();
		while(itr.hasNext()) {
			String listener = itr.next();
			// make sure they don't have this dude muted
			if(plugin.getPlayer(listener).mutedPlayers.contains(player.getName())) {
				// ok, they're muted. skip along.
				continue;
			}
			
			plugin.sendMessageNoColour(listener, "<\u00A7"+ Integer.toHexString(colour) + this.channelName + "\u00A7f> * " + player.getName() + message);
		}
		
		plugin.log.info("<"+this.channelName+"> * " + player.getName() + message);
	}
	
	public void addPlayer(MCNSAChat plugin, Player player) {
		if(players.contains(player.getName())) {
			return;
		}

		boolean showIt = true;
		if(plugin.hideJoinLeave) {
			showIt = false;
		}
		if(hideJoinLeave) {
			showIt = false;
		}
		if(plugin.getPlayer(player).poofed) {
			showIt = false;
		}
		
		// announce to the channel the new join
		if(showIt) {
			Iterator<String> itr = players.iterator();
			while(itr.hasNext()) {
				String pl = itr.next();
				// hide players if in local chat who are not near you
				if(plugin.getChannel(plugin.getPlayer(player).channel).local && !plugin.playerWithinRadius(player.getName(), pl, plugin.localChatRadius)) {
					// skip people not nearby if in local
					continue;
				}
				
				// make sure we only broadcast to online players
				if(plugin.getServer().getPlayer(pl) != null) {
					plugin.sendMessage(pl, "&b" + player.getName() + " has joined the channel!");
				}
			}
		}
		
		// add the player to the list
		players.add(player.getName());
		
		// set that player's channel
		plugin.players.get(player.getName()).channel = this.channelName.toLowerCase();
		
		// now inform the player they joined the channel
		plugin.sendMessage(player.getName(), "&bWelcome to the &" + Integer.toHexString(this.colour) + this.channelName + " &bchannel!");
		plugin.sendMessage(player.getName(), "&bPeople who can hear you now:");
		// format the players string
		String playersString = new String("");
		// get all the listeners..
		HashSet<String> listeners = getListeners(plugin, player);
		boolean first = true;
		Iterator<String> itr = listeners.iterator();
		while(itr.hasNext()) {
			String listener = itr.next();
			if(plugin.getServer().getPlayer(listener) != null) {
				// player is online!
				// make sure they're not poofed!
				if(!plugin.getPlayer(listener).poofed) {
					playersString = playersString + ((first) ? "&7" : "&f, &7") + listener;
					if(first) {
						first = false;
					}
				}
			}
		}
		plugin.sendMessage(player.getName(), playersString);
		
		// log...
		plugin.log.info("[MCNSAChat] "+player.getName()+" joined channel "+this.channelName);
	}
	
	public void removePlayer(MCNSAChat plugin, Player player, boolean announce) {
		// remove the player
		// make sure to remove that fucker!
		while(players.contains(player.getName())) {
			players.remove(player.getName());
		}
		
		// announce to the channel the leave
		if(!plugin.hideJoinLeave && !plugin.getPlayer(player).poofed && announce) {
			Iterator<String> itr = players.iterator();
			while(itr.hasNext()) {
				String pl = itr.next();
				// make sure we only broadcast to online players
				if(plugin.getServer().getPlayer(pl) != null) {
						plugin.sendMessage(pl, "\u00A7b" + player.getName() + " has left the channel!");
				}
			}
		}
		
		// remove that player's channel
		plugin.players.get(player.getName()).channel = "";
		
		// log...
		plugin.log.info("[MCNSAChat] "+player.getName()+" left the "+this.channelName+" channel");
	}
}
