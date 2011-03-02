/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager.dataholder.worlds;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.anjocaido.groupmanager.utils.Tasks;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 *
 * @author gabrielcouto
 */
public class WorldsHolder {

    private Map<String, OverloadedWorldHolder> worldsData = new HashMap<String, OverloadedWorldHolder>();
    private OverloadedWorldHolder defaultWorld;
    private String serverDefaultWorldName;
    private GroupManager plugin;
    private File worldsFolder;

    public WorldsHolder(GroupManager plugin) {
        this.plugin = plugin;
        verifyFirstRun();
        initialLoad();
        if (defaultWorld == null) {
            throw new IllegalStateException("There is no default group! OMG!");
        }
    }

    private void initialLoad() {
        //LOAD EVERY WORLD POSSIBLE
        loadWorld(serverDefaultWorldName);
        defaultWorld = worldsData.get(serverDefaultWorldName);

        for(File folder: worldsFolder.listFiles()){
            if(folder.getName().equalsIgnoreCase(serverDefaultWorldName))
                continue;
            if(folder.isDirectory()){
                loadWorld(folder.getName());
            }
        }
        Map<String, Object> mirrorsMap = plugin.getConfig().getMirrorsMap();
        if (mirrorsMap != null) {
            for (String source : mirrorsMap.keySet()) {
                if (mirrorsMap.get(source) instanceof ArrayList) {
                    ArrayList mirrorList = (ArrayList) mirrorsMap.get(source);
                    for (Object o : mirrorList) {
                        try {
                            worldsData.remove(o.toString().toLowerCase());
                        } catch (Exception e) {
                        }
                        worldsData.put(o.toString().toLowerCase(), getWorldData(source));
                    }
                } else if (mirrorsMap.get(source) instanceof Object) {
                    String mirror = (String) mirrorsMap.get(source);
                    worldsData.put(mirror.toLowerCase(), getWorldData(source));
                }
            }
        } 
    }

    public void reloadAll() {
        ArrayList<WorldDataHolder> alreadyDone = new ArrayList<WorldDataHolder>();
        for (WorldDataHolder w : worldsData.values()) {
            if (alreadyDone.contains(w)) {
                continue;
            }
            w.reload();
            alreadyDone.add(w);
        }
    }

    public void reloadWorld(String worldName) {
        getWorldData(worldName).reload();
    }

    public void saveChanges() {
        ArrayList<WorldDataHolder> alreadyDone = new ArrayList<WorldDataHolder>();
        for (OverloadedWorldHolder w : worldsData.values()) {
            if (alreadyDone.contains(w)) {
                continue;
            }
            Tasks.removeOldFiles(plugin.getBackupFolder());
            if(w==null){
                GroupManager.logger.severe("WHAT HAPPENED?");
                continue;
            }
            if (w.haveGroupsChanged()) {
                String groupsFolderName = w.getGroupsFile().getParentFile().getName();
                File backupGroups = new File(plugin.getBackupFolder(), "bkp_" + w.getName() + "_g_" + Tasks.getDateString() + ".yml");
                try {
                    Tasks.copy(w.getGroupsFile(), backupGroups);
                } catch (IOException ex) {
                    GroupManager.logger.log(Level.SEVERE, null, ex);
                }
                WorldDataHolder.writeGroups(w, w.getGroupsFile());
                w.removeGroupsChangedFlag();
            }
            if (w.haveUsersChanged()) {
                File backupUsers = new File(plugin.getBackupFolder(), "bkp_" + w.getName() + "_u_" + Tasks.getDateString() + ".yml");
                try {
                    Tasks.copy(w.getUsersFile(), backupUsers);
                } catch (IOException ex) {
                    GroupManager.logger.log(Level.SEVERE, null, ex);
                }
                WorldDataHolder.writeUsers(w, w.getUsersFile());
                w.removeUsersChangedFlag();
            }
            alreadyDone.add(w);
        }
    }

    /**
     * Returns the dataHolder for the given world.
     * If the world is not on the worlds list, returns the default world
     * holder.
     *
     * (WHEN A WORLD IS CONFIGURED TO MIRROR, IT WILL BE ON THE LIST, BUT
     * POINTING TO ANOTHER WORLD HOLDER)
     *
     * @param worldName
     * @return
     */
    public OverloadedWorldHolder getWorldData(String worldName) {
        OverloadedWorldHolder data = worldsData.get(worldName.toLowerCase());
        if (data == null) {
            GroupManager.logger.finest("Requested world "+worldName+" not found. Returning default world...");
            data = getDefaultWorld();
        }
        return data;
    }

    public OverloadedWorldHolder getWorldDataByPlayerName(String playerName){
        List<Player> matchPlayer = plugin.getServer().matchPlayer(playerName);
        if(matchPlayer.size()==1){
            return getWorldData(matchPlayer.get(0));
        }
        return null;
    }

    public OverloadedWorldHolder getWorldData(Player p){
        return getWorldData(p.getWorld().getName());
    }

    public AnjoPermissionsHandler getWorldPermissions(String worldName) {
        return getWorldData(worldName).getPermissionsHandler();
    }

    public AnjoPermissionsHandler getWorldPermissions(Player p){
        return getWorldData(p).getPermissionsHandler();
    }

    public AnjoPermissionsHandler getWorldPermissionsByPlayerName(String playerName){
        List<Player> matchPlayer = plugin.getServer().matchPlayer(playerName);
        if(matchPlayer.size()==1){
            return getWorldPermissions(matchPlayer.get(0));
        }
        return null;
    }

    private void verifyFirstRun() {
        worldsFolder = new File(plugin.getDataFolder(), "worlds");
        if (!worldsFolder.exists()) {
            worldsFolder.mkdirs();
        }
        Properties server = new Properties();
        try {
            server.load(new FileInputStream(new File("server.properties")));
        } catch (IOException ex) {
            GroupManager.logger.log(Level.SEVERE, null, ex);
        }
        serverDefaultWorldName = server.getProperty("level-name").toLowerCase();
        File defaultWorldFolder = new File(worldsFolder, serverDefaultWorldName);
        if (!defaultWorldFolder.exists()) {
            defaultWorldFolder.mkdirs();
        }
        if (defaultWorldFolder.exists()) {
            File groupsFile = new File(defaultWorldFolder, "groups.yml");
            File usersFile = new File(defaultWorldFolder, "users.yml");
            if (!groupsFile.exists()) {
                InputStream template = plugin.getResourceAsStream("groups.yml");
                try {
                    Tasks.copy(template, groupsFile);
                } catch (IOException ex) {
                    GroupManager.logger.log(Level.SEVERE, null, ex);
                }
            }
            if (!usersFile.exists()) {
                InputStream template = plugin.getResourceAsStream("users.yml");
                try {
                    Tasks.copy(template, usersFile);
                } catch (IOException ex) {
                    GroupManager.logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Copies the specified world data 
     * @param fromWorld
     * @param toWorld
     */
    public void cloneWorld(String fromWorld, String toWorld){

    }

    private void loadWorld(String worldName) {
        GroupManager.logger.finest("Trying to load world "+worldName+"...");
        File thisWorldFolder = new File(worldsFolder, worldName);
        if (thisWorldFolder.exists() && thisWorldFolder.isDirectory()) {
            File groupsFile = new File(thisWorldFolder, "groups.yml");
            File usersFile = new File(thisWorldFolder, "users.yml");
            if (!groupsFile.exists()) {
                throw new IllegalArgumentException("Groups file for world '" + worldName + "' doesnt exist: " + groupsFile.getPath());
            }
            if (!usersFile.exists()) {
                throw new IllegalArgumentException("Users file for world '" + worldName + "' doesnt exist: " + usersFile.getPath());
            }
            try {
                OverloadedWorldHolder thisWorldData = new OverloadedWorldHolder(WorldDataHolder.load(worldName,groupsFile, usersFile));
                if (thisWorldData != null) {
                    GroupManager.logger.finest("Successful load of world "+worldName+"...");
                    worldsData.put(worldName.toLowerCase(), thisWorldData);
                    return;
                }
            } catch (FileNotFoundException ex) {
                GroupManager.logger.log(Level.SEVERE, null,ex);
                return;
            } catch (IOException ex) {
                GroupManager.logger.log(Level.SEVERE, null,ex);
                return;
            }
            GroupManager.logger.severe("Failed to load world "+worldName+"...");
        }
    }

    /**
     * @return the defaultWorld
     */
    public OverloadedWorldHolder getDefaultWorld() {
        return defaultWorld;
    }
    
    public ArrayList<OverloadedWorldHolder> allWorldsDataList(){
        ArrayList<OverloadedWorldHolder> list = new ArrayList<OverloadedWorldHolder>();
        for(OverloadedWorldHolder data: worldsData.values()){
            if(!list.contains(data)){
                list.add(data);
            }
        }
        return list;
    }
}
