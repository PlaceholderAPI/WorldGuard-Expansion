/*
 *
 * WorldGuard-Expansion
 * Copyright (C) 2018 Ryan McCarthy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.extendedclip.papi.expansion.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;

import java.util.*;

public class WorldGuardExpansion extends PlaceholderExpansion {

  private final String NAME = "WorldGuard";
  private final String IDENTIFIER = NAME.toLowerCase();
  private final String VERSION = getClass().getPackage().getImplementationVersion();

  private WorldGuardWrapper worldguard;

  @Override
  public boolean canRegister() {
    if (Bukkit.getServer().getPluginManager().getPlugin(NAME) == null) return false;
    worldguard = WorldGuardWrapper.getInstance();
    return worldguard != null;
  }

  @Override
  public String onRequest(OfflinePlayer offlinePlayer, String params) {

    IWrappedRegion r;

    if (params.contains(":")) {
      String[] args = params.split(":");
      params = args[0];
      r = getRegion(deserializeLoc(args[1]));
    } else {
      if (offlinePlayer == null || !offlinePlayer.isOnline()) {
        return "";
      }
      r = getRegion(((Player) offlinePlayer).getLocation());
    }

    if (r == null) {
      return "";
    }

    switch (params) {
      case "region_name":
        return r.getId();
      case "region_owner":
        Set<String> o = new HashSet<>();
        r.getOwners().getPlayers().forEach(u -> o.add(Bukkit.getOfflinePlayer(u).getName()));
        return o.isEmpty() ? "" : String.join(", ", o);
      case "region_owner_groups":
        return this.toGroupsString(r.getOwners().getGroups());
      case "region_members":
        Set<String> m = new HashSet<>();
        r.getMembers().getPlayers().forEach(u -> m.add(Bukkit.getOfflinePlayer(u).getName()));
        return m.isEmpty() ? "" : String.join(", ", m);
      case "region_members_groups":
        return this.toGroupsString(r.getMembers().getGroups());
      case "region_flags":
        return r.getFlags().entrySet().toString();
    }

    return null;
  }

  private IWrappedRegion getRegion(Location loc) {
    if (loc == null) return null;

    try {

      Optional<IWrappedRegion> region = worldguard.getRegion(loc.getWorld(),((IWrappedRegion)worldguard.getRegions(loc).toArray()[0]).getId());


      return region.isPresent() ? region.get() : null;

    } catch (IndexOutOfBoundsException e) {

      return null;

    }
  }

  // world,x,y,z
  private Location deserializeLoc(String locString) {
    if (!locString.contains(",")) {
      return null;
    }
    String[] s = locString.split(",");
    try {
      return new Location(
          Bukkit.getWorld(s[0]),
          Double.parseDouble(s[1]),
          Double.parseDouble(s[2]),
          Double.parseDouble(s[3]));
    } catch (Exception e) {
    }
    return null;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getAuthor() {
    return "clip";
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public String getIdentifier() {
    return IDENTIFIER;
  }

  private String toGroupsString(Set<String> groups) {
    StringBuilder str = new StringBuilder();
    Iterator it = groups.iterator();

    while(it.hasNext()) {
      str.append("*");
      str.append((String)it.next());
      if (it.hasNext()) {
        str.append(", ");
      }
    }

    return str.toString();
  }
}
