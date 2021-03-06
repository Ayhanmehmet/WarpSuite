package com.mrz.dyndns.server.warpsuite;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrz.dyndns.server.EnhancedCommandSystem.CommandSystem;
import com.mrz.dyndns.server.EnhancedCommandSystem.SimpleCommand;
import com.mrz.dyndns.server.warpsuite.commands.WarpSuiteCommand;
import com.mrz.dyndns.server.warpsuite.commands.admin.DeleteOtherPlayersWarp;
import com.mrz.dyndns.server.warpsuite.commands.admin.ListOtherPlayersWarps;
import com.mrz.dyndns.server.warpsuite.commands.admin.SetOtherPlayersWarp;
import com.mrz.dyndns.server.warpsuite.commands.admin.WarpPlayerToMyWarp;
import com.mrz.dyndns.server.warpsuite.commands.admin.WarpPlayerToTheirWarp;
import com.mrz.dyndns.server.warpsuite.commands.invites.AcceptInvite;
import com.mrz.dyndns.server.warpsuite.commands.invites.DenyInvite;
import com.mrz.dyndns.server.warpsuite.commands.invites.SendInvite;
import com.mrz.dyndns.server.warpsuite.commands.publicWarps.GoToPublicWarp;
import com.mrz.dyndns.server.warpsuite.commands.publicWarps.GoToPublicWarpIfApplicable;
import com.mrz.dyndns.server.warpsuite.commands.publicWarps.PublicRemoveWarp;
import com.mrz.dyndns.server.warpsuite.commands.publicWarps.PublicSetWarp;
import com.mrz.dyndns.server.warpsuite.commands.publicWarps.SendPlayerToPublicWarp;
import com.mrz.dyndns.server.warpsuite.commands.user.GoPlayersOwnWarp;
import com.mrz.dyndns.server.warpsuite.commands.user.ListPlayersOwnWarps;
import com.mrz.dyndns.server.warpsuite.commands.user.RemovePlayersOwnWarp;
import com.mrz.dyndns.server.warpsuite.commands.user.SetPlayersOwnWarp;
import com.mrz.dyndns.server.warpsuite.listeners.EntityDamageByEntityListener;
import com.mrz.dyndns.server.warpsuite.listeners.PlayerMoveListener;
import com.mrz.dyndns.server.warpsuite.managers.PendingWarpManager;
import com.mrz.dyndns.server.warpsuite.managers.PlayerManager;
import com.mrz.dyndns.server.warpsuite.managers.PublicWarpManager;
import com.mrz.dyndns.server.warpsuite.permissions.Permissions;
import com.mrz.dyndns.server.warpsuite.players.WarpSuitePlayer;
import com.mrz.dyndns.server.warpsuite.util.Coloring;
import com.mrz.dyndns.server.warpsuite.util.Config;
import com.mrz.dyndns.server.warpsuite.util.Util;

public class WarpSuite extends JavaPlugin
{
	private CommandSystem cs;
	private PlayerManager playerManager;
	private PublicWarpManager publicWarpManager;
	private boolean usingMultiverse;
	private PendingWarpManager pendingWarpManager;
	
	@Override
	public void onEnable()
	{
		Util.initialize(this);
//		Util.setDebugging(true);
		
		cs = new CommandSystem(this);
		
		getConfig().options().copyDefaults(true);
		saveConfig();
		Config.load(getConfig());
		
		boolean overrideWarp = Config.forceWarpCommandOverride;
		boolean overrideDelwarp = Config.forceDelwarpCommandOverride;
		boolean overrideSetwarp = Config.forceSetwarpCommandOverride;
		
		playerManager = new PlayerManager(this);
		publicWarpManager = new PublicWarpManager(this);
		pendingWarpManager = new PendingWarpManager();
		
		WarpSuiteCommand cmd = new SetPlayersOwnWarp(this);
		cs.registerCommand("warp|go set|add", cmd, overrideWarp);
		cs.registerCommand("setwarp", cmd, overrideSetwarp);
		
		cs.registerCommand("warp|go delete|del|remove|clear", new RemovePlayersOwnWarp(this), overrideWarp);
		cs.registerCommand("delwarp", cmd, overrideDelwarp);
		
		cs.registerCommand("warp list", new ListPlayersOwnWarps(this), overrideWarp);
		
		cs.registerCommand("warp|go", new GoToPublicWarpIfApplicable(this), overrideWarp);
		cs.registerCommand("warp|go my", new GoPlayersOwnWarp(this), overrideWarp);
		
		//admin commands
		cs.registerCommand("warp|go {*} sendto|to their|his|her", new WarpPlayerToTheirWarp(this), overrideWarp);
		cs.registerCommand("warp|go {*} to my", new WarpPlayerToMyWarp(this), overrideWarp);
		cs.registerCommand("warp|go {*} sendto|to public", new SendPlayerToPublicWarp(this), overrideWarp);
		cs.registerCommand("warp|go {*} set|add", new SetOtherPlayersWarp(this), overrideWarp);
		cs.registerCommand("warp|go {*} delete|del|remove|clear", new DeleteOtherPlayersWarp(this), overrideWarp);
		cs.registerCommand("warp|go {*} list", new ListOtherPlayersWarps(this), overrideWarp);
		
		//public warps
		cs.registerCommand("warp|go set|add public", new PublicSetWarp(this), overrideWarp);
		cs.registerCommand("warp|go public", new GoToPublicWarp(this), overrideWarp);
		cs.registerCommand("warp|go delete|del|remove|clear public", new PublicRemoveWarp(this), overrideWarp);
		
		//invites
		cs.registerCommand("warp|go invite {*} to", new SendInvite(this), overrideWarp);
		cs.registerCommand("warp|go accept", new AcceptInvite(this), overrideWarp);
		cs.registerCommand("warp|go deny", new DenyInvite(this), overrideWarp);
		
		final WarpSuite plugin = this;
		cs.registerCommand("warp|go reload", new SimpleCommand() {
			@Override
			public boolean Execute(String commandName, final CommandSender sender, List<String> args, List<String> variables)
			{
				if(Permissions.RELOAD.check(sender, true))
				{
					plugin.reloadConfig();
					Config.load(plugin.getConfig());
					for(WarpSuitePlayer player : plugin.getPlayerManager().getPlayers())
					{
						player.getConfig().reloadCustomConfig();
					}
					plugin.getLogger().info("Reloaded WarpSuite");
					sender.sendMessage(Coloring.POSITIVE_PRIMARY + "WarpSuite Reloaded!");
					return true;
				}
				else
				{
					return true;
				}
			}
		}, overrideWarp);
		
		if(getServer().getPluginManager().getPlugin("Multiverse-Core") != null)
		{
			getLogger().info("Hooking into Multiverse");
			usingMultiverse = true;
		}
		else
		{
			usingMultiverse = false;
		}
		
//		getServer().getScheduler().runTaskTimer(this, playerManager, 72000L, 72000L);//every hour
		
		getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(this), this);
		getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
		getServer().getPluginManager().registerEvents(playerManager, this);
	}
	
	@Override
	public void onDisable()
	{
		playerManager.clearPlayers();
		cs.close();
	}
	
	//you will never need it
	public PublicWarpManager getPublicWarpManager()
	{
		return publicWarpManager;
	}
	
	public PlayerManager getPlayerManager()
	{
		return playerManager;
	}
	
	public boolean isUsingMultiverse()
	{
		return usingMultiverse;
	}
	
	public PendingWarpManager getPendingWarpManager()
	{
		return pendingWarpManager;
	}
}
