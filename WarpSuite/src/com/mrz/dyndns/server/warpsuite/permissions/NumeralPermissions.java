package com.mrz.dyndns.server.warpsuite.permissions;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.mrz.dyndns.server.warpsuite.util.Config;

public enum NumeralPermissions
{
	COUNT("warpsuite.count.");
	
	private NumeralPermissions(String node)
	{
		this.node = node;
	}
	
	private final String node;
	
	public int getAmount(Player p) throws NumberFormatException
	{
		if(Permissions.COUNT_INFINITE.checkNode(p))
		{
			return -1;
		}
		
		for(PermissionAttachmentInfo perm : p.getEffectivePermissions())
		{
			if(perm.getPermission().startsWith(node))
			{
				String[] permParts = perm.getPermission().split("\\.");
				String amount = permParts[permParts.length - 1];
				return Integer.parseInt(amount.substring(0, amount.length() - 1));
			}
		}
		
		return Config.defaultMaxWarps;
	}
}
