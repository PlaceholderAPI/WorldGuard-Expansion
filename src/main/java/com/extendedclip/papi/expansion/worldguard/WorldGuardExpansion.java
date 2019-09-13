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

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;
import org.codemc.worldguardwrapper.selection.ICuboidSelection;

import java.util.*;

public class WorldGuardExpansion extends PlaceholderExpansion {

    private final String NAME = "WorldGuard";
    private final String IDENTIFIER = NAME.toLowerCase();
    private final String VERSION = getClass().getPackage().getImplementationVersion();

    private WorldGuardWrapper worldguard;

    /**
     * Since this expansion requires api access to the plugin "SomePlugin"
     * we must check if said plugin is on the server or not.
     *
     * @return true or false depending on if the required plugin is installed.
     */
    @Override
    public boolean canRegister() {
        if (Bukkit.getServer().getPluginManager().getPlugin(NAME) == null) return false;
        worldguard = WorldGuardWrapper.getInstance();
        return worldguard != null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * The name of the person who created this expansion should go here.
     *
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor() {
        return "clip";
    }

    /**
     * This is the version of this expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion() {
        return VERSION;
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>This must be unique and can not contain % or _
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }


    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {

        // Get the wrapper from input location
        IWrappedRegion region;
        ICuboidSelection selection;

        // Check if it contains this symbol
        if (params.contains(":")) {
            // Split by symbol
            String[] args = params.split(":");
            // Set placeholder to first args
            params = args[0];
            // Set region to second args
            region = getRegion(stringToLocation(args[1]));
        } else {
            // Check to make sure offline player is online
            if (offlinePlayer == null || !offlinePlayer.isOnline()) {
                // If not, return empty
                return "";
            }
            // Return the region
            region = getRegion(((Player) offlinePlayer).getLocation());
        }

        // Make sure it's not null
        if (region == null) {
            return "";
        }

        if (params.startsWith("region_has_flag_")) {
            final String[] rg = params.split("region_has_flag_");
            if (rg.length < 1) return null;

            return region.getFlags().keySet().stream().anyMatch(f ->
                    f.getName().equalsIgnoreCase(rg[1])) ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        }

        // Defined as a switch statement to keep thinks clean
        switch (params) {
            // Check the name of the region the player is in
            case "region_name":
                return region.getId();
            // Because some people are stubborn, let's have it also provide capitalization
            case "region_name_capitalized":
                return Character.isLetter(region.getId().charAt(0)) ? StringUtils.capitalize(region.getId()) : region.getId();
            case "region_owner": {
                // Create a set of owners
                Set<String> owners = new HashSet<>();
                // Add them to set
                region.getOwners().getPlayers().forEach(u -> owners.add(Bukkit.getOfflinePlayer(u).getName()));
                // Return list of them
                return owners.isEmpty() ? "" : String.join(", ", owners);
            }
            case "region_owner_groups":
                // Turn the owner groups to a string
                return toGroupString(region.getOwners().getGroups());
            case "region_members":
                // Create set for members
                Set<String> members = new HashSet<>();
                // Add all members to the region
                region.getMembers().getPlayers().forEach(u -> members.add(Bukkit.getOfflinePlayer(u).getName()));
                // Return list
                return members.isEmpty() ? "" : String.join(", ", members);
            case "region_members_groups":
                // Turn member groups to a string
                return toGroupString(region.getMembers().getGroups());
            case "region_flags":
                Map<String, Object> flags = new HashMap<>();
                region.getFlags().forEach((key, value) -> flags.put(key.getName(), value));

                // Turn the list of flags to a string
                return flags.entrySet().toString();
        }

        if (params.startsWith("region_min_point_") || params.startsWith("region_max_point_")) {
            try {
                selection = (ICuboidSelection) region.getSelection();
            } catch (ClassCastException e) {
                return "";
            }

            switch (params) {
                case "region_min_point_x":
                    return String.valueOf(selection.getMinimumPoint().getBlockX());
                case "region_min_point_y":
                    return String.valueOf(selection.getMinimumPoint().getBlockY());
                case "region_min_point_z":
                    return String.valueOf(selection.getMinimumPoint().getBlockZ());
                case "region_max_point_x":
                    return String.valueOf(selection.getMaximumPoint().getBlockX());
                case "region_max_point_y":
                    return String.valueOf(selection.getMaximumPoint().getBlockY());
                case "region_max_point_z":
                    return String.valueOf(selection.getMaximumPoint().getBlockZ());
            }
        }

        return null;
    }

    /**
     * Get a wrapped region from a location
     *
     * @param location the location to check
     * @return the wrapped region
     */
    private IWrappedRegion getRegion(Location location) {
        if (location == null) {
            return null;
        }
        try {
            Optional<IWrappedRegion> region = worldguard.getRegion(location.getWorld(), ((IWrappedRegion) worldguard.getRegions(location).toArray()[0]).getId());
            return region.orElse(null);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    /**
     * Convert a string to a location
     *
     * @param loc the location to convert to
     * @return location
     */
    private Location stringToLocation(String loc) {
        if (!loc.contains(",")) {
            return null;
        }
        String[] s = loc.split(",");
        try {
            return new Location(
                    Bukkit.getWorld(s[0]),
                    Double.parseDouble(s[1]),
                    Double.parseDouble(s[2]),
                    Double.parseDouble(s[3])
            );
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Get a list of groups
     *
     * @param groups groups
     * @return list
     */
    private String toGroupString(Set<String> groups) {
        StringBuilder sb = new StringBuilder();
        Iterator it = groups.iterator();

        while (it.hasNext()) {
            sb.append("*");
            sb.append((String) it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
