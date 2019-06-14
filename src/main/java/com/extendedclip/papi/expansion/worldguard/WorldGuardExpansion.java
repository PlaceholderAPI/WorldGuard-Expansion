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

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;

import java.util.Optional;

public class WorldGuardExpansion extends PlaceholderExpansion {

    private final String NAME = "WorldGuard";
    private final String IDENTIFIER = NAME.toLowerCase();
    private final String VERSION = getClass().getPackage().getImplementationVersion();

    private WorldGuardWrapper worldguard;

    /**
     * This expansion requires WorldGuard to work, so we have to check for it here.
     *
     * @return true if WorldGuard is installed and active.
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
     * The name of the person who created this expansion.
     *
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor() {
        return "clip";
    }

    /**
     * The Version of this expansion.
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion() {
        return VERSION;
    }

    /**
     * The identifier "worldguard".
     *
     * @return "worldguard".
     */
    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }


    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {

        // Get the wrapper from input location
        IWrappedRegion region = getRegion(((Player) offlinePlayer).getLocation());

        // Make sure it's not null
        if (region == null) {
            return "";
        }

        // Defined as a switch statement to keep thinks clean
        switch (params) {
            // Check the name of the region the player is in
            case "region_name":
                return region.getId();
                // Because some people are stubborn, let's have it also provide capitalization
            case "region_name_capitalized":
                return Character.isLetter(region.getId().charAt(0)) ? StringUtils.capitalize(region.getId()) : region.getId();
        }

        return null;
    }

    /**
     * Get a wrapped region from a location.
     *
     * @param location The location to check
     *
     * @return The wrapped region
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
}
