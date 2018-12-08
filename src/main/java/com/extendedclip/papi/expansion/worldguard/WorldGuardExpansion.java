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
import org.codemc.worldguardwrapper.selection.ICuboidSelection;
import org.codemc.worldguardwrapper.selection.IPolygonalSelection;
import org.codemc.worldguardwrapper.selection.ISelection;

import java.util.*;

public class WorldGuardExpansion extends PlaceholderExpansion {

    private final String NAME = "WorldGuard";
    private final String AUTHOR = "clip";
    private final String IDENTIFIER = NAME.toLowerCase();
    private final String VERSION = getClass().getPackage().getImplementationVersion();

    private WorldGuardWrapper worldguard;

    @Override
    public boolean canRegister() {
        if (Bukkit.getServer().getPluginManager().getPlugin(NAME) == null)
            return false;
        worldguard = WorldGuardWrapper.getInstance();
        return worldguard != null;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        IWrappedRegion region;
        if (params.contains(":")) {
            String[] args = params.split(":");
            params = args[0];
            region = getRegion(deserializeLoc(args[1]));
        } else {
            if (offlinePlayer == null || !offlinePlayer.isOnline()) {
                return "";
            }
            region = getRegion(((Player) offlinePlayer).getLocation());
        }

        if (region == null) {
            return "";
        }
        return parseParam(params,region);
    }

    private String parseParam(String params,IWrappedRegion region){
        Location minLoc = ((ICuboidSelection)region.getSelection()).getMinimumPoint();
        Location maxLoc = ((ICuboidSelection)region.getSelection()).getMaximumPoint();
        switch (params) {
            case "region_name":
                return region.getId();
            case "region_owner":
                Set<String> owners = new HashSet<>();
                region.getOwners().getPlayers().forEach(uuid -> owners.add(Bukkit.getOfflinePlayer(uuid).getName()));
                return owners.isEmpty() ? "" : String.join(", ", owners);
            case "region_owner_groups":
                return this.toGroupsString(region.getOwners().getGroups());
            case "region_members":
                Set<String> members = new HashSet<>();
                region.getMembers().getPlayers().forEach(uuid -> members.add(Bukkit.getOfflinePlayer(uuid).getName()));
                return members.isEmpty() ? "" : String.join(", ", members);
            case "region_members_groups":
                return this.toGroupsString(region.getMembers().getGroups());
            case "region_flags":
                return region.getFlags().entrySet().toString();
            case "region_min_point_x":
                return String.valueOf(minLoc.getBlockX());
            case "region_min_point_y":
                return String.valueOf(minLoc.getBlockY());
            case "region_min_point_z":
                return String.valueOf(minLoc.getBlockZ());
            case "region_max_point_x":
                return String.valueOf(maxLoc.getBlockX());
            case "region_max_point_y":
                return String.valueOf(maxLoc.getBlockY());
            case "region_max_point_z":
                return String.valueOf(maxLoc.getBlockZ());
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


    private Location deserializeLoc(String locString) {
        if (!locString.contains(",")) {
            return null;
        }
        String[] locations = locString.split(",");
        try {
            return new Location(
                    Bukkit.getWorld(locations[0]),
                    Double.parseDouble(locations[1]),
                    Double.parseDouble(locations[2]),
                    Double.parseDouble(locations[3])
            );
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
        return AUTHOR;
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
        StringBuilder groupsString = new StringBuilder();
        Iterator it = groups.iterator();
        while(it.hasNext()) {
            groupsString.append("*");
            groupsString.append((String)it.next());
            if (it.hasNext()) {
                groupsString.append(", ");
            }
        }
        return groupsString.toString();
    }
}
