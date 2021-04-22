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

    @Override
    public @NotNull String getRequiredPlugin() { return "WorldGuard"; }

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
        Integer priority = null;

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
            if (rg.length < 1) {
                return null;
            }

            for (Flag<?> flag : region.getFlags().keySet()) {
                if (flag.getName().equalsIgnoreCase(rg[1])) {
                    return PlaceholderAPIPlugin.booleanTrue();
                }
            }
            return PlaceholderAPIPlugin.booleanFalse();
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
     * Get a region from a location
     *
     * @param location the location to check
     * @param priority the priority wanted
     * @return the region
     */
    private ProtectedRegion getRegion(Location location, Integer priority) {
        if (location == null) {
            return null;
        }

        //Query regions
        RegionContainer container = worldguard.getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet applicableRegionSet = query.getApplicableRegions(BukkitAdapter.adapt(location));

        //Get regions from our query
        Set<ProtectedRegion> regions = applicableRegionSet.getRegions();


        Set<ProtectedRegion> selectedRegions = new HashSet<>();
        if (priority != null) {
            //We have been given a priority, now we want to select all regions with that priority.
            for(ProtectedRegion region : regions) {
                if (region.getPriority() == priority) {
                    //Region is equal to chosen priority and we will select it.
                    selectedRegions.add(region);
                }
            }
            if (selectedRegions.size() == 1) {
                //We have only selected one region in our prio search, that's the one we want.
                return selectedRegions.stream().findFirst().get();
            }
        }

        if (selectedRegions.size() == 0) {
            selectedRegions.addAll(regions);
        }

        //We have selected more than one region, now we want to eliminate the parent.
        List<ProtectedRegion> parents = new ArrayList<>();
        for (ProtectedRegion region : new ArrayList<>(selectedRegions)) {
            if (region.getParent() == null) {
                //Current region is a parent, back it up and remove it.
                parents.add(region);
                selectedRegions.remove(region);
            }
        }
        if (selectedRegions.size() == 0) {
            //No children in location, select parents again
            selectedRegions.addAll(parents);
        }

        if (selectedRegions.size() == 1) {
            //Only 1 region exists, return it
            return selectedRegions.stream().findFirst().get();
        }

        //At this point, we now want to sort through our selected regions for the highest prio one.
        List<ProtectedRegion> highestRegions = new ArrayList<>();
        for (ProtectedRegion region : selectedRegions) {
            //Set highestRegion to highest priority region
            if (highestRegions.size() == 0) {
                highestRegions.add(region);
                continue;
            }
            if (highestRegions.get(0).getPriority() == region.getPriority()) {
                highestRegions.add(region);
            } else if (highestRegions.get(0).getPriority() < region.getPriority()) {
                highestRegions.clear();
                highestRegions.add(region);
            }
        }

        if (highestRegions.size() == 1) {
            return highestRegions.get(0);
        } else if (highestRegions.size() > 1) {
            Random rand = new Random();
            return highestRegions.get(rand.nextInt(highestRegions.size()));
        } else {
            return null;
        }
    }

    /**
     * Get the comma separated names of all players in a domain.
     *
     * @param domain the domain to get players from
     * @return comma separated list of all players' names
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
     * Get the comma separated list of groups from domain
     *
     * @param domain the domain to get groups from
     * @return comma separated list of all groups' names
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
