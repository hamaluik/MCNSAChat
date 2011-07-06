package com.hamaluik.MCNSAChat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class MCNSAChat extends JavaPlugin {
	Logger log = Logger.getLogger("Minecraft");
	public PermissionHandler permissionHandler;
	
	MCNSAChatPlayerListener playerListener = new MCNSAChatPlayerListener(this);
	MCNSAChatCommandExecutor commandExecutor = new MCNSAChatCommandExecutor(this);
	MCNSAChatWorldListener worldListener = new MCNSAChatWorldListener(this);
	
	// data..
	public ConcurrentHashMap<String, ChatPlayer> players = new ConcurrentHashMap<String, ChatPlayer>();
	public ConcurrentHashMap<String, ChatChannel> channels = new ConcurrentHashMap<String, ChatChannel>();
	public ConcurrentHashMap<String, String> aliases = new ConcurrentHashMap<String, String>();
	public ConcurrentHashMap<String, Integer> colours = new ConcurrentHashMap<String, Integer>();
	public ConcurrentHashMap<String, Timer> playerTimers = new ConcurrentHashMap<String, Timer>(); // store these here instead of in each player because they are not serializable
	public Integer localChatRadius = 200;
	public String defaultChannel = "G";
	public Integer defaultColour = 7;
	public boolean hideJoinLeave = false;
	public boolean announceTimeouts = true;
	
	// startup routine..
	@SuppressWarnings("unchecked")
	public void onEnable() {
		// setup the colour list
		colours.put("black", 0);
		colours.put("dblue", 1);
		colours.put("dgreen", 2);
		colours.put("dteal", 3);
		colours.put("dred", 4);
		colours.put("purple", 5);
		colours.put("gold", 6);
		colours.put("grey", 7);
		colours.put("dgrey", 8);
		colours.put("blue", 9);
		colours.put("green", 10);
		colours.put("teal", 11);
		colours.put("red", 12);
		colours.put("pink", 13);
		colours.put("yellow", 14);
		colours.put("white", 15);
		
		// setup permissions
		setupPermissions();
		
		// load the configuration
		checkConfiguration();
		// now load the saved hashmaps
		try {
			players = (ConcurrentHashMap<String, ChatPlayer>)load(this.getDataFolder() + "/players.hash");
			channels = (ConcurrentHashMap<String, ChatChannel>)load(this.getDataFolder() + "/channels.hash");
		} catch (Exception e) {
			log.info("[ERROR] [MCNSAChat] failed to load saved data: " + e.toString());
		}
		loadConfiguration();
		
		// import the plugin manager
		PluginManager pm = this.getServer().getPluginManager();
		
		// listen for events..
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener, Event.Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Event.Priority.Highest, this);
		pm.registerEvent(Event.Type.WORLD_SAVE, worldListener, Event.Priority.Normal, this);
		
		// register commands
		this.getCommand("ch").setExecutor(commandExecutor);
		
		// log startup information
		log.info("[MCNSAChat] plugin enabled");
	}

	// shutdown routine
	public void onDisable() {
		// clear people's timeout timers
		for(String player: players.keySet()) {
			this.getPlayer(player).onTimeout = false;
		}
		
		// now save the hashmaps
		try {
			save(players, this.getDataFolder() + "/players.hash");
			save(channels, this.getDataFolder() + "/channels.hash");
		} catch (Exception e) {
			log.info("[ERROR] [MCNSAChat] failed to save data!: " + e.toString());
		}
		// log shutdown information
		log.info("[MCNSAChat] plugin disabled");
	}
	
	public void save(Object obj, String path) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
		oos.writeObject(obj);
		oos.flush();
		oos.close();
	}
	
	public Object load(String path) throws Exception {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
		Object result = ois.readObject();
		ois.close();
		return result;
	}
	
	// just an interface function for checking permissions
	// if permissions are down, default to OP status.
	public boolean hasPermission(Player player, String permission) {
		if(permissionHandler == null) {
			return player.isOp();
		}
		else {
			return (permissionHandler.has(player, permission) || permissionHandler.has(player, "mcnsachat.everything"));
		}
	}
	
	// just an interface function for checking permissions groups
	// if permissions are down, default to OP status.
	public boolean inGroup(Player player, String group) {
		if(permissionHandler == null) {
			return player.isOp();
		}
		else {
			return (permissionHandler.inGroup(player.getWorld().getName(), player.getName(), group));
		}
	}
	
	// load the permissions plugin..
	private void setupPermissions() {
		Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");
		
		if(this.permissionHandler == null) {
			if(permissionsPlugin != null) {
				this.permissionHandler = ((Permissions)permissionsPlugin).getHandler();
				log.info("[MCNSAChat] permissions successfully loaded");
			} else {
				log.info("[MCNSAChat] permission system not detected, defaulting to OP");
			}
		}
	}
	
	private void checkConfiguration() {
		// first, check to see if the file exists
		File configFile = new File(getDataFolder() + "/config.yml");
		if(!configFile.exists()) {
			// file doesn't exist yet :/
			log.info("[MCNSAChat] config file not found, will attempt to create a default!");
			new File(getDataFolder().toString()).mkdir();
			try {
				// create the file
				configFile.createNewFile();
				// and attempt to write the defaults to it
				FileWriter out = new FileWriter(getDataFolder() + "/config.yml");
				out.write("---\r\n");
				out.write("# radius of [Local] chat channel (in blocks)\r\n");
				out.write("local-chat-radius: 200\r\n\r\n");
				out.write("# default chat channel new players will join\r\n");
				out.write("# (defined in \"default-channels\")\r\n");
				out.write("default-channel: Global\r\n\r\n");
				out.write("# list of default (always-on channels)\r\n");
				out.write("# and their properties\r\n");
				out.write("default-channels:\r\n");
				out.write("    - name: Global\r\n");
				out.write("    - name: Local\r\n");
				out.write("      local: yes\r\n");
				out.write("    - name: Admin\r\n");
				out.write("      permissions:\r\n");
				out.write("          - 'mcnsachat.admin'\r\n");
				// TODO: update default config
				out.close();
			} catch(IOException ex) {
				// something went wrong :/
				log.info("[MCNSAChat] error: config file does not exist and could not be created");
			}
		}
	}
	
	// load settings and strings from the config file
	private void loadConfiguration() {		
		Configuration config = getConfiguration();
		this.localChatRadius = config.getInt("local-chat-radius", 200);
		this.defaultChannel = config.getString("default-channel", "G").toLowerCase();
		if(this.colours.containsKey(colours.get(config.getString("default-colour")))) {
			this.defaultColour = this.colours.get(config.getString("default-colour"));
		}
		else {
			this.defaultColour = 7;
		}
		this.hideJoinLeave = config.getBoolean("hide-join-leave-messages", false);
		this.announceTimeouts = config.getBoolean("announce-timeouts", true);
		
		// load the default channels
		List<ConfigurationNode> defaultChannels = config.getNodeList("default-channels", null);
		Iterator<ConfigurationNode> it = defaultChannels.iterator();
		// loop through all the channels specified in the config
		while(it.hasNext()) {
			ConfigurationNode node = it.next();
			
			// load the info for the channels
			ChatChannel channel = new ChatChannel();
			channel.channelName = node.getString("name", "untitled");
			String channelKey = channel.channelName.toLowerCase();
			
			// if the channel does not exist, add it
			if(!channels.containsKey(channelKey)) {
				this.channels.put(channelKey, channel);
			}
			
			// and finally load the remaining details!
			this.channels.get(channelKey).channelName = channel.channelName;
			this.channels.get(channelKey).persistant = true; // (true for default channels)
			this.channels.get(channelKey).local = node.getBoolean("local", false);
			this.channels.get(channelKey).broadcast = node.getBoolean("broadcast", false);
			this.channels.get(channelKey).hideJoinLeave = node.getBoolean("hide-join-leave-messages", false);
			if(colours.containsKey(node.getString("colour", config.getString("default-colour")))) {
				this.channels.get(channelKey).colour = colours.get(node.getString("colour", config.getString("default-colour"))); 
			}
			else {
				this.channels.get(channelKey).colour = defaultColour;
			}
			this.channels.get(channelKey).alias = node.getString("alias", "").toLowerCase();
			
			// if the channel has an alias, add it to the list
			if(!this.channels.get(channelKey).alias.equals("")) {
				this.aliases.put(this.channels.get(channelKey).alias, channelKey);
			}
			
			// load channel permissions
			List<String> channelPermissions = node.getStringList("permissions", null);
			for(int i = 0; i < channelPermissions.size(); i++) {
				this.channels.get(channelKey).permissions.add(channelPermissions.get(i));
			}
			
			// load the channel default listeners
			List<String> channelListeners = node.getStringList("listeners", null);
			for(int i = 0; i < channelListeners.size(); i++) {
				this.channels.get(channelKey).defaultListeners.add(channelListeners.get(i));
			}
			
			// load the channel listening channels
			List<String> listeningChannels = node.getStringList("listening-channels", null);
			for(int i = 0; i < listeningChannels.size(); i++) {
				this.channels.get(channelKey).listeningChannels.add(listeningChannels.get(i));
			}
		}
	}
	
	public ChatPlayer getPlayer(String player) {
		if(!players.containsKey(player)) {
			// add the player to the list..
			players.put(player, new ChatPlayer());
			// set the default channel.
			getChannel(defaultChannel).addPlayer(this, getServer().getPlayer(player));
		}
		return players.get(player);
	}
	
	public ChatPlayer getPlayer(Player player) {
		return getPlayer(player.getName());
	}
	
	public Timer getPlayerTimer(String player) {
		if(!playerTimers.containsKey(player)) {
			// add the player to the list..
			playerTimers.put(player, new Timer());
		}
		return playerTimers.get(player);
	}
	
	public Timer getPlayerTimer(Player player) {
		return getPlayerTimer(player.getName());
	}
	
	// channel getting wrapper
	public ChatChannel getChannel(String name) {
		// if it already exists, don't worry about it
		if(channels.containsKey(name.toLowerCase())) {
			return channels.get(name.toLowerCase());
		}
		
		// ok, we need to create the channel!
		channels.put(name.toLowerCase(), new ChatChannel());
		// and change the channel stuff
		channels.get(name.toLowerCase()).channelName = name;
		channels.get(name.toLowerCase()).colour = defaultColour;

		return channels.get(name.toLowerCase());
	}
	
	// if the channel is empty and NOT persistant
	// destroy it!
	public void checkChannel(String name) {	
		// if it's persistant, don't bother
		if(getChannel(name).persistant) {
			return;
		}
		
		// ok, not persistant. let's see if there is
		// anyone still in it
		if(getChannel(name).players.size() < 1) {
			// channel is empty
			// DESTROY IT
			removeChannel(name);
		}
	}
	
	// to remove the channel
	public void removeChannel(String name) {		
		// make sure it exists first..
		if(!channels.containsKey(name.toLowerCase())) {
			return;
		}
		
		// loop through and move anyone who is in it to the default channel
		Iterator<String> itr = getChannel(name).players.iterator();
		while(itr.hasNext()) {
			getPlayer(itr.next()).channel = defaultChannel;
		}
		
		// loop through all the players to see who is listening in
		for(String key: players.keySet()) {
			if(players.get(key).listeningChannels.contains(name.toLowerCase())) {
				// they're listening in!
				// remove this channel from their list
				players.get(key).listeningChannels.remove(name.toLowerCase());
			}
		}
		
		// and go ahead and remove it
		channels.remove(name.toLowerCase());
	}
	
	public Player nameToPlayer(String name) {
		return this.getServer().getPlayer(name);
	}
	
	// allow for colour tags to be used in strings..
	public String processColours(String str) {
		return str.replaceAll("(&([a-f0-9]))", "\u00A7$2");
	}
	
	public void sendMessage(String name, String message) {
		if(this.nameToPlayer(name) != null) {
			this.nameToPlayer(name).sendMessage(this.processColours(message));
		}
	}
	
	public void sendMessage(Player player, String message) {
		player.sendMessage(this.processColours(message));
	}
	
	public void sendMessageNoColour(String name, String message) {
		if(this.nameToPlayer(name) != null) {
			this.nameToPlayer(name).sendMessage(message);
		}
	}
	
	public HashSet<String> playersWithinRadius(Player player, Integer radius) {
		List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
		HashSet<String> ret = new HashSet<String>();
		for(int i = 0; i < nearby.size(); i++) {
			if(nearby.get(i) instanceof Player) {
				ret.add(((Player)nearby.get(i)).getName());
			}
		}
		return ret;
	}
	
	public boolean playerWithinRadius(String player1, String player2, Integer radius) {
		List<Entity> nearby = getServer().getPlayer(player1).getNearbyEntities(radius, radius, radius);
		for(int i = 0; i < nearby.size(); i++) {
			if(nearby.get(i) instanceof Player) {
				if(player2.equals(((Player)nearby.get(i)).getName())) {
					return true;
				}
			}
		}
		return false;
	}
}
