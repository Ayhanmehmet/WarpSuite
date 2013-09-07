package com.mrz.dyndns.server.warpsuite.commands;

import static com.mrz.dyndns.server.warpsuite.util.Coloring.*;

import java.util.List;

import com.mrz.dyndns.server.warpsuite.WarpSuite;
import com.mrz.dyndns.server.warpsuite.WarpSuitePlayer;
import com.mrz.dyndns.server.warpsuite.permissions.Permissions;
import com.mrz.dyndns.server.warpsuite.util.Config;
import com.mrz.dyndns.server.warpsuite.util.SimpleLocation;
import com.mrz.dyndns.server.warpsuite.util.Util;

public class GoPlayersOwnWarp extends WarpSuiteCommand
{

	public GoPlayersOwnWarp(WarpSuite plugin)
	{
		super(plugin);
	}

	@Override
	public boolean warpPlayerExecute(final WarpSuitePlayer player, List<String> args, List<String> variables)
	{
		if(args.size() == 0)
		{
			player.sendMessage(NEGATIVE_PRIMARY + "Invalid usage!" + POSITIVE_PRIMARY + " Correct usage: " + USAGE + "/warp " + USAGE_ARGUMENT + " [warpName]");
			if(Permissions.HELP.check(player))
			{
				player.sendMessage(POSITIVE_PRIMARY + "Or, if you want to view all of the warp help, issue " + USAGE + "/warp help");
			}
			return true;
		}
		
		String warpName = args.get(0);
		if(player.getWarpManager().warpIsSet(warpName))
		{
			final SimpleLocation sLoc = player.getWarpManager().loadWarp(warpName);
			boolean canGoToWorld = sLoc.tryLoad(plugin);
			if(canGoToWorld)
			{
				//it is time to teleport!
				if(Permissions.DELAY_BYPASS.check(player) || Util.areTherePlayersInRadius(player))
				{
					player.teleport(plugin, sLoc);
					return true;
				}
				else
				{
					//TODO: my gosh this needs testing!
					StringBuilder sb = new StringBuilder();
					sb.append(POSITIVE_PRIMARY + "You will be warped in " + POSITIVE_SECONDARY + Config.timer + POSITIVE_PRIMARY + "seconds.");
					boolean thingsToSay = (Config.cancelOnMobDamage || Config.cancelOnMove || Config.cancelOnPvp);
					if(thingsToSay)
					{
						sb.append(" Don\'t ");
						
						if(Config.cancelOnPvp)
						{
							sb.append("engage in pvp");
							if(Config.cancelOnMobDamage && Config.cancelOnMove)
							{
								sb.append(", ");
							}
							else if(!Config.cancelOnMobDamage || !Config.cancelOnMobDamage)
							{
								sb.append("or ");
							}
							else
							{
								sb.append(".");
							}
						}
						if(Config.cancelOnMobDamage)
						{
							sb.append("get hurt by mobs");
							if(Config.cancelOnMove)
							{
								sb.append(" or");
							}
							else
							{
								sb.append(".");
							}
						}
						if(Config.cancelOnMove)
						{
							sb.append("move around.");
						}
						
						player.sendMessage(sb.toString());
					}
					else
					{
						player.sendMessage(sb.toString());
					}
					plugin.getPendingWarpManager().addPlayer(player.getName());
					plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
						@Override
						public void run()
						{
							plugin.getPendingWarpManager().removePlayer(player.getName());
							player.teleport(plugin, sLoc);
						}
					}, Config.timer * 20L);
					return true;
				}
			}
			else
			{
				player.sendMessage(NEGATIVE_PRIMARY + "The world warp \'" + NEGATIVE_SECONDARY + "\' is located in either no longer exists, or isn't loaded");
				return true;
			}
		}
		else
		{
			player.sendMessage(NEGATIVE_PRIMARY + "Warp \'" + NEGATIVE_SECONDARY + warpName + NEGATIVE_PRIMARY + "\' is not set!");
			return true;
		}
	}

	@Override
	public String getUsage()
	{
		//I'll do this myself above (see line (around) 25)
		return null;
	}

}
