package complexicon.proxysigns;

//thank you iso2013, simpleauthority, phoenix616, Choco and electronicboy for helping me with my plugin <3

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.jline.internal.Log;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitScheduler;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import ch.jamiete.mcping.MinecraftPing;
import ch.jamiete.mcping.MinecraftPingOptions;
import ch.jamiete.mcping.MinecraftPingReply;

public class MainPlugin extends JavaPlugin implements Listener, PluginMessageListener{
	
	FileConfiguration config = getConfig();
	
	public int port = 0;
	public String ip = "unset";
	boolean exception;
	public static YamlConfiguration LANG;
	public static File LANG_FILE;
	
	public String version = "2.6-Stable";
	
	MinecraftPingReply mcping = null;
	
	BukkitScheduler sched = getServer().getScheduler();
	
	@Override
	public void onPluginMessageReceived(String arg0, Player arg1, byte[] arg2) {
		if(!arg0.equals("BungeeCord")){
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(arg2);
		String subchannel = in.readUTF();
		if(subchannel.equals("ServerIP")){
			in.readUTF();
	    	ip = in.readUTF();
			port = in.readShort();
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		
		loadLang();
		
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
		if(!config.isSet("index.count")){
			config.set("index.count", 0);
			saveConfig();
		}
		
		sched.scheduleAsyncRepeatingTask(this, new Runnable(){

			@Override
			public void run() {
				for(int x = 1; x < config.getInt("index.count") || x == config.getInt("index.count");x++){
					
					final int passX = x;
					
					try {
						mcping = new MinecraftPing().getPing(new MinecraftPingOptions().setHostname(config.getString("index."+x+".ip")).setPort( config.getInt("index."+x+".port")));
						sched.runTask(getPlugin(), new Runnable(){

							@Override
							public void run() {
								
								Location loc = new Location(Bukkit.getWorld(config.getString("index."+passX+".world")), config.getInt("index."+passX+".x"), config.getInt("index."+passX+".y"), config.getInt("index."+passX+".z"));
								if(loc.getBlock().getState()instanceof Sign){
									Sign s = (Sign) loc.getBlock().getState();
									if(s.getLine(0).contains(Lang.SIGN_CONNECT.toString())){
										s.setLine(0, Lang.SIGN_CONNECT.toString());
										s.setLine(1, mcping.getPlayers().getOnline()+"/"+mcping.getPlayers().getMax());
										s.setLine(2, s.getLine(2));
										s.setLine(3, "§2Online");
										s.update(true);
									}
								}
								
							}
							
						});
					}catch(IOException e) {
						sched.runTask(getPlugin(), new Runnable(){

							@Override
							public void run() {
								
								Location loc = new Location(Bukkit.getWorld(config.getString("index."+passX+".world")), config.getInt("index."+passX+".x"), config.getInt("index."+passX+".y"), config.getInt("index."+passX+".z"));
								if(loc.getBlock().getState()instanceof Sign){
									Sign s = (Sign) loc.getBlock().getState();
									if(s.getLine(0).contains(Lang.SIGN_CONNECT.toString())){
										s.setLine(0, Lang.SIGN_CONNECT.toString());
										s.setLine(1, "0/0");
										s.setLine(2, s.getLine(2));
										s.setLine(3, "§4OFFLINE");
										s.update(true);
									}
								}
								
							}
							
						});
					}
				}
			}
			
		}, 0L, 100L);
		
	}

		
	protected Plugin getPlugin() {
		return this;
	}

	@EventHandler
	public void onSignChange(SignChangeEvent e){
		if(e.getLine(0).equalsIgnoreCase("[connect]")){
			
			if(e.getLine(1).isEmpty()){
				e.getBlock().breakNaturally();
				e.getPlayer().sendMessage(Lang.SIGN_SERVERNAME_ERROR.toString());
				return;
			}
			
			if(e.getLine(2).isEmpty()){
				e.getBlock().breakNaturally();
				e.getPlayer().sendMessage(Lang.SIGN_SHOWNAME_ERROR.toString());
				return;
			}
			
			BukkitScheduler sched = getServer().getScheduler();
			
			ByteArrayOutputStream bytee = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytee);
			

			try {
				out.writeUTF("ServerIP");
				out.writeUTF(e.getLine(1));
			} catch (Exception exception) {
				System.err.println("ERROR:");
				exception.printStackTrace();
			}
			
			e.getPlayer().sendPluginMessage(MainPlugin.this.getPlugin(), "BungeeCord", bytee.toByteArray());
			
			String bungee = e.getLine(1);
			
			sched.runTaskLater(this, new Runnable(){

				@Override
				public void run() {
					
					if(ip == "unset" || port == 0){
						e.getBlock().breakNaturally();
						e.getPlayer().sendMessage(Lang.SERVER_ERROR.toString());
						return;
					}
					
					//index
					config.set("signs."+e.getLine(2)+".bungee", bungee);
					config.set("signs."+e.getLine(2)+".name", e.getLine(2));
					config.set("index.count", config.getInt("index.count")+1);
					
					//block props
					config.set("index."+config.getInt("index.count")+".bungee", bungee);
					config.set("index."+config.getInt("index.count")+".x", e.getBlock().getX());
					config.set("index."+config.getInt("index.count")+".y", e.getBlock().getY());
					config.set("index."+config.getInt("index.count")+".z", e.getBlock().getZ());
					config.set("index."+config.getInt("index.count")+".world", e.getPlayer().getWorld().getName());
					
					//bungee stuff
					config.set("index."+config.getInt("index.count")+".ip", ip);
					config.set("index."+config.getInt("index.count")+".port", port);
					
					saveConfig();
					
					ip = "unset";
					port = 0;
				}
				
			}, 20L);
			
			e.setLine(0, Lang.SIGN_CONNECT.toString());
			e.setLine(1, "§10/0");
			e.setLine(2, e.getLine(2));
			e.setLine(3, "§1Getting Status");
			
			e.getPlayer().sendMessage(Lang.SIGN_CREATE.toString().replace("%sign", e.getLine(2)));
		}
		
		if(e.getLine(0).equalsIgnoreCase("[prxysgns]")){
			e.setLine(0, "§4[ProxySigns]");
			e.setLine(1, ChatColor.DARK_PURPLE + version);
			e.setLine(2, "§2 Thx4Install");
			e.setLine(3, "§2By §bComplexicon");
		}
	}
	
	@EventHandler
	public void onSignInteract(PlayerInteractEvent e){
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
			if(e.getClickedBlock().getState() instanceof Sign){
				Player p = e.getPlayer();
				Sign s = (Sign) e.getClickedBlock().getState();
				if(s.getLine(0).contains(Lang.SIGN_CONNECT.toString())){
					if(s.getLine(2).equals(config.getString("signs."+s.getLine(2)+".name"))){
						p.sendMessage(Lang.SERVER_CONNECT.toString().replace("%server", s.getLine(2)));
					
						ByteArrayOutputStream b = new ByteArrayOutputStream();
						DataOutputStream out = new DataOutputStream(b);

						try {
							out.writeUTF("Connect");
							out.writeUTF(config.getString("signs."+ s.getLine(2) +".bungee"));
						} catch (IOException ex){
							System.err.println("ERROR:");
							ex.printStackTrace();
						}
					
						p.sendPluginMessage(this, "BungeeCord", b.toByteArray());
					}
				}
				
			}
		}
	}
	
	@EventHandler
	public void onSignBreak(BlockBreakEvent e){
		if(e.getBlock().getState() instanceof Sign){
			Player p = e.getPlayer();
			Sign s = (Sign) e.getBlock().getState();
			if(s.getLine(0).contains(Lang.SIGN_CONNECT.toString())){
				if(s.getLine(2).equals(config.getString("signs."+s.getLine(2)+".name"))){
					config.set("signs."+s.getLine(2), null);
					config.set("index."+config.getInt("index.count"), null );
					config.set("index.count", config.getInt("index.count") -1);
					this.saveConfig();
					p.sendMessage(Lang.SIGN_DESTROY.toString().replace("%sign", s.getLine(2)));
				}
			}
		}
	}
	
	public enum Lang {
		SIGN_CONNECT("sign-connect", "&6Connect"),
	    SIGN_DESTROY("sign-destroy", "&4You have destroyed '%sign'"),
		SIGN_CREATE("sign-create", "&2You have created the sign for '%sign' successfully!"),
		SIGN_SERVERNAME_ERROR("sign-servername-error", "&4You have to enter a valid Servername!"),
		SIGN_SHOWNAME_ERROR("sign-showname-error", "&4You have to enter a name for the Sign!"),
		SERVER_ERROR("server-error", "&4Server does not exist"),
		SERVER_CONNECT("server-connect", "&2You are getting connected to the Server '%server'!");
		
	 
	    private String path;
	    private String def;
	    private static YamlConfiguration LANG;
	    
	    Lang(String path, String start) {
	        this.path = path;
	        this.def = start;
	    }
	    
	    public static void setFile(YamlConfiguration config) {
	        LANG = config;
	    }
	 
	    @Override
	    public String toString() {
	        return ChatColor.translateAlternateColorCodes('&', LANG.getString(this.path, def));
	    }
	    
	    public String getDefault() {
	        return this.def;
	    }
	 
	    public String getPath() {
	        return this.path;
	    }
		
	}
	
	public YamlConfiguration getLang() {
	    return LANG;
	}
	
	public File getLangFile() {
	    return LANG_FILE;
	}
	
	@SuppressWarnings("deprecation")
	public void loadLang() {
	    File lang = new File(getDataFolder(), "lang.yml");
	    if (!lang.exists()) {
	        try {
	            getDataFolder().mkdir();
	            lang.createNewFile();
	            InputStream defConfigStream = this.getResource("lang.yml");
	            if (defConfigStream != null) {
	                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	                defConfig.save(lang);
	                Lang.setFile(defConfig);
	            }
	        } catch(IOException e) {
	            e.printStackTrace(); // So they notice
	            Log.error("Couldn't create language file.");
	            Log.error("This is a fatal error. Now disabling");
	            this.setEnabled(false); // Without it loaded, we can't send them messages
	        }
	    }
	    YamlConfiguration conf = YamlConfiguration.loadConfiguration(lang);
	    for(Lang item:Lang.values()) {
	        if (conf.getString(item.getPath()) == null) {
	            conf.set(item.getPath(), item.getDefault());
	        }
	    }
	    Lang.setFile(conf);
	    MainPlugin.LANG = conf;
	    MainPlugin.LANG_FILE = lang;
	    try {
	        conf.save(getLangFile());
	    } catch(IOException e) {
	        Log.warn("Failed to save lang.yml.");
	        Log.warn("Report this stack trace to Complexicon.");
	        e.printStackTrace();
	    }
	}
	
}