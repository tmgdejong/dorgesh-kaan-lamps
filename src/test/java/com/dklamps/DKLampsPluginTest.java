package com.dklamps;

import com.dklamps.DKLampsPlugin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DKLampsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DKLampsPlugin.class);
		RuneLite.main(args);
	}
}