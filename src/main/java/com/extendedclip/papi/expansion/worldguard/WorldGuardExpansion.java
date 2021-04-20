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
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import static java.util.stream.Collectors.toMap;

public class WorldGuardExpansion extends PlaceholderExpansion {

    private final String NAME = "WorldGuard";
    private final String IDENTIFIER = NAME.toLowerCase();
    private final String VERSION = getClass().getPackage().getImplementationVersion();

    private WorldGuard worldguard;

    /**
     * Since this expansion requires api access to the plugin "SomePlugin"
     * we must check if said plugin is on the server or not.
     *
     * @return true or false depending on if the required plugin is installed.
     */
    @Override
    public boolean canRegister() {
        if (Bukkit.getServer().getPluginManager().getPlugin(NAME) == null) return false;
        worldguard = WorldGuard.getInstance();
        return worldguard != null;
    }

    @Override
    public @NotNull String getName() {
        return NAME;
    }

    /**
     * The name of the person who created this expansion should go here.
     *
     * @return The name of the author as a String.
     */
    @Override
    public @NotNull String getAuthor() {
        return "clip";
    }

    /**
     * This is the version of this expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * @return The version as a String.
     */
    @Override
    public @NotNull String getVersion() {
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
    public @NotNull String getIdentifier() {
        return IDENTIFIER;
    }


    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {

        // Initialise region & create a default priority.
        ProtectedRegion region;
        int priority = 0;

        //Set priority to the one supplied by the placeholder string, if one exists.
        if (params.matches("(.*_)([1-9]\\d*)(.*)")) {
            priority = Integer.parseInt(params.replaceAll("(.*_)([1-9]\\d*)(.*)", "$2"));
            params = params.replace("_" + priority, "");
        }

        //Create a default location.
        Location location;

        //Check if a placeholder contains a ":", and therefore query the location given in the placeholder.
        if (params.contains(":")) {
            String[] args = params.split(":");
            params = args[0];
            location = stringToLocation(args[1]);
        } else {
            if (offlinePlayer == null || !offlinePlayer.isOnline()) {
                //Ensure we can cast offlinePlayer to Player
                return "";
            }
            location = ((Player) offlinePlayer).getLocation();
        }

        region = getRegion(location, priority);

        // If no region exists, return nothing
        if (region == null) {
            return "";
        }

        if (params.startsWith("region_has_flag_")) {
            final String[] rg = params.split("region_has_flag_");
            if (rg.length < 1) return null;

            return region.getFlags().keySet().stream().anyMatch(f ->
                    f.getName().equalsIgnoreCase(rg[1])) ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        }

        // Defined as a switch statement to keep things clean
        switch (params) {
            // Check the name of the region the player is in
            case "region_name":
                return region.getId();
            // Because some people are stubborn, let's have it also provide capitalization
            case "region_name_capitalized":
                return Character.isLetter(region.getId().charAt(0)) ? StringUtils.capitalize(region.getId()) : region.getId();
            case "region_owner": {
                return getNamesFromDomain(region.getOwners());
            }
            case "region_owner_groups":
                // Turn the owner groups to a string
                return getGroupsFromDomain(region.getOwners());
            case "region_members":
                return getNamesFromDomain(region.getMembers());
            case "region_members_groups":
                // Turn member groups to a string
                return getGroupsFromDomain(region.getMembers());
            case "region_flags":
                Map<String, Object> flags = new HashMap<>();
                region.getFlags().forEach((key, value) -> flags.put(key.getName(), value));

                // Turn the list of flags to a string
                return flags.entrySet().toString();
        }

        if (params.startsWith("region_min_point_") || params.startsWith("region_max_point_")) {
            BlockVector3 minimumPoint = region.getMinimumPoint();
            BlockVector3 maximumPoint = region.getMaximumPoint();

            switch (params) {
                case "region_min_point_x":
                    return String.valueOf(minimumPoint.getBlockX());
                case "region_min_point_y":
                    return String.valueOf(minimumPoint.getBlockY());
                case "region_min_point_z":
                    return String.valueOf(minimumPoint.getBlockZ());
                case "region_max_point_x":
                    return String.valueOf(maximumPoint.getBlockX());
                case "region_max_point_y":
                    return String.valueOf(maximumPoint.getBlockY());
                case "region_max_point_z":
                    return String.valueOf(maximumPoint.getBlockZ());
            }
        }

        return null;
    }

    /**
     * Get a wrapped region from a location
     *
     * @param location the location to check
     * @return the region
     */
    private ProtectedRegion getRegion(Location location, int priority) {
        if (location == null) {
            return null;
        }

        RegionContainer container = worldguard.getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet applicableRegionSet = query.getApplicableRegions(BukkitAdapter.adapt(location));

        for(ProtectedRegion region : applicableRegionSet.getRegions()) {
            if (region.getPriority() == priority) {
                return region;
            }
        }
        return null;
    }

    /**
     * Get the comma seperated names of all players in a domain.
     *
     * @param domain the domain to get players from
     * @return comma seperated list of all players' names
     */

    private String getNamesFromDomain(DefaultDomain domain) {
        Set<String> playerNames = new HashSet<>();
        Set<UUID> playerUUIDs = domain.getPlayerDomain().getUniqueIds();
        for (UUID uuid : playerUUIDs) {
            playerNames.add(Bukkit.getOfflinePlayer(uuid).getName());
        }
        return playerNames.isEmpty() ? "" : String.join(", ", playerNames);
    }

    /**
     * Get the comma seperated list of groups from doamin
     *
     * @param domain the domain to get groups from
     * @return comma seperated list of all groups' names
     */

    private String getGroupsFromDomain(DefaultDomain domain) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> groupsIterator = domain.getGroups().iterator();

        while (groupsIterator.hasNext()) {
            stringBuilder.append("*");
            stringBuilder.append(groupsIterator.next());
            if (groupsIterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }

        return stringBuilder.toString();
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
}
