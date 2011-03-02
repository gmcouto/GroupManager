/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager;

import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.anjocaido.groupmanager.utils.GroupManagerPermissions;
import org.anjocaido.groupmanager.data.Variables;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
import org.anjocaido.groupmanager.utils.GMLoggerHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author gabrielcouto
 */
public class GroupManager extends JavaPlugin {

    private File configFile;
    private File backupFolder;
    private Runnable commiter;
    private ScheduledThreadPoolExecutor scheduler;
    private Map<String, ArrayList<User>> overloadedUsers = new HashMap<String, ArrayList<User>>();
    private Map<CommandSender, String> selectedWorlds = new HashMap<CommandSender, String>();
    private WorldsHolder worldsHolder;
    private boolean validateOnlinePlayer = true;
    private boolean isReady = false;
    private GMConfiguration config;
    public static final Logger logger = Logger.getLogger(GroupManager.class.getName());

    @Override
    public void onDisable() {
        if (worldsHolder != null) {
            worldsHolder.saveChanges();
        }
        disableScheduler();
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!");
    }

    @Override
    public void onEnable() {
        GroupManager.logger.setUseParentHandlers(false);
        GMLoggerHandler ch = new GMLoggerHandler();
        GroupManager.logger.addHandler(ch);
        logger.setLevel(Level.ALL);
        if (worldsHolder == null) {
            prepareFileFields();
            prepareConfig();
            worldsHolder = new WorldsHolder(this);
        }

        PluginDescriptionFile pdfFile = this.getDescription();
        if (worldsHolder == null) {
            GroupManager.logger.severe("Can't enable " + pdfFile.getName() + " version " + pdfFile.getVersion() + ", bad loading!");
            this.getServer().getPluginManager().disablePlugin(this);
            throw new IllegalStateException("An error ocurred while loading GroupManager");
        }

        enableScheduler();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
    }
    

    public InputStream getResourceAsStream(String fileName){
        return this.getClassLoader().getResourceAsStream(fileName);
    }

    private void prepareFileFields() {
        configFile = new File(this.getDataFolder(), "config.yml");
        backupFolder = new File(this.getDataFolder(), "backup");
        if (!backupFolder.exists()) {
            getBackupFolder().mkdirs();
        }
    }

    private void prepareConfig() {
        config = new GMConfiguration(this);
    }

    public void enableScheduler() {
        if (worldsHolder != null) {
            disableScheduler();
            commiter = new Runnable() {

                @Override
                public void run() {
                    GroupManager.this.worldsHolder.saveChanges();
                }
            };
            scheduler = new ScheduledThreadPoolExecutor(1);
            int minutes = getConfig().getSaveInterval();
            scheduler.scheduleAtFixedRate(commiter, minutes, minutes, TimeUnit.MINUTES);
            GroupManager.logger.info("Scheduled Data Saving is set for every "+minutes+" minutes!");
        }
    }

    public void disableScheduler() {
        if (scheduler != null) {
            try {
                scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
                scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                scheduler.shutdown();
            } catch (Exception e) {
            }
            scheduler = null;
            GroupManager.logger.info("Scheduled Data Saving is disabled!");
        }
    }

    /**
     * Use the WorldsHolder saveChanges directly instead
     * Saves the data on file
     */
    @Deprecated
    public void commit() {
        if (worldsHolder != null) {
            worldsHolder.saveChanges();
        }
    }

    /**
     * Use worlds holder to reload a specific world
     * Reloads the data
     */
    @Deprecated
    public void reload() {
        worldsHolder.reloadAll();
    }

    public WorldsHolder getWorldsHolder(){
        return worldsHolder;
    }
    
    /**
     * The handler in the interface created by AnjoCaido
     * @return
     */
    @Deprecated
    public AnjoPermissionsHandler getPermissionHandler() {
        return worldsHolder.getDefaultWorld().getPermissionsHandler();
    }

    /**
     *  A simple interface, for ones that don't want to mess with overloading.
     * Yet it is affected by overloading. But seamless.
     * @return the dataholder with all information
     */
    @Deprecated
    public WorldDataHolder getData() {
        return worldsHolder.getDefaultWorld();
    }

    /**
     *  Use this if you want to play with overloading.
     * @return  a dataholder with overloading interface
     */
    @Deprecated
    public OverloadedWorldHolder getOverloadedClassData() {
        return worldsHolder.getDefaultWorld();
    }

    /**
     * Called when a command registered by this plugin is received.
     * @param sender 
     * @param cmd 
     * @param args
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        boolean playerCanDo = false;
        boolean isConsole = false;
        Player p = null;
        Group playerGroup = null;
        User playerUser = null;

        if(!selectedWorlds.containsKey(sender)){
            selectedWorlds.put(sender, worldsHolder.getDefaultWorld().getName());
        }
        
        OverloadedWorldHolder dataHolder = worldsHolder.getWorldData(selectedWorlds.get(sender));
        AnjoPermissionsHandler permissionHandler = dataHolder.getPermissionsHandler();
        if(overloadedUsers.get(dataHolder.getName().toLowerCase())==null){
            overloadedUsers.put(dataHolder.getName().toLowerCase(), new ArrayList<User>());
        }

        if (sender instanceof Player) {
            p = (Player) sender;
            playerUser = dataHolder.getUser(p.getName());
            playerGroup = playerUser.getGroup();
            if (worldsHolder.getWorldPermissions(p).has(p, "groupmanager." + cmd.getName())) {
                playerCanDo = true;
            }
        } else if (sender instanceof ConsoleCommandSender) {
            isConsole = true;
        }



        int count;
        ArrayList<User> removeList = null;
        String auxString = null;
        List<Player> match = null;
        User auxUser = null;
        Group auxGroup = null;
        Group auxGroup2 = null;

        GroupManagerPermissions execCmd = null;
        try {
            execCmd = GroupManagerPermissions.valueOf(cmd.getName());
        } catch (Exception e) {
            //this error happened once with someone. now im prepared... i think
            GroupManager.logger.severe("===================================================");
            GroupManager.logger.severe("=              ERROR REPORT START                 =");
            GroupManager.logger.severe("===================================================");
            GroupManager.logger.severe("=  COPY AND PASTE THIS TO GROUPMANAGER DEVELOPER  =");
            GroupManager.logger.severe("===================================================");
            GroupManager.logger.severe(this.getDescription().getName());
            GroupManager.logger.severe(this.getDescription().getVersion());
            GroupManager.logger.severe("An error occured while trying to execute command:");
            GroupManager.logger.severe(cmd.getName());
            GroupManager.logger.severe("With "+args.length+" arguments:");
            for (String ar : args) {
                GroupManager.logger.severe(ar);
            }
            GroupManager.logger.severe("The field '"+ cmd.getName()+"' was not found in enum.");
            GroupManager.logger.severe("And could not be parsed.");
            GroupManager.logger.severe("FIELDS FOUND IN ENUM:");
            for (GroupManagerPermissions val : GroupManagerPermissions.values()) {
                GroupManager.logger.severe(val.name());
            }
            GroupManager.logger.severe("===================================================");
            GroupManager.logger.severe("=              ERROR REPORT ENDED                 =");
            GroupManager.logger.severe("===================================================");
            sender.sendMessage("An error occurred. Ask the admin to take a look at the console.");
        }

        if (isConsole || playerCanDo) {
            switch (execCmd) {
                case manuadd:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    auxGroup = dataHolder.getGroup(args[1]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group not found!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (playerGroup != null ? permissionHandler.inGroup(auxUser.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    //PARECE OK
                    auxUser.setGroup(auxGroup);
                    sender.sendMessage(ChatColor.YELLOW + "You changed that player group.");

                    return true;
                //break;
                case manudel:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (playerGroup != null ? permissionHandler.inGroup(auxUser.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    //PARECE OK
                    dataHolder.removeUser(auxUser.getName());
                    sender.sendMessage(ChatColor.YELLOW + "You changed that player to default group.");

                    return true;
                case mangadd:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup != null) {
                        sender.sendMessage(ChatColor.RED + "Group already exists!");
                        return false;
                    }
                    //PARECE OK
                    auxGroup = dataHolder.createGroup(args[0]);
                    sender.sendMessage(ChatColor.YELLOW + "You created a group named: " + auxGroup.getName());

                    return true;
                case mangdel:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group not exists!");
                        return false;
                    }
                    //PARECE OK
                    dataHolder.removeGroup(auxGroup.getName());
                    sender.sendMessage(ChatColor.YELLOW + "You deleted a group named " + auxGroup.getName() + ", it's users are default group now.");

                    return true;
                case manuaddp:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (playerGroup != null ? permissionHandler.inGroup(auxUser.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same group than you, or higher.");
                        return false;
                    }
                    if (!isConsole && (!permissionHandler.has(p, args[1]))) {
                        sender.sendMessage(ChatColor.RED + "Can't add a permission you don't have.");
                        return false;
                    }
                    auxString = permissionHandler.checkUserOnlyPermission(auxUser, args[1]);
                    if (auxString != null) {
                        sender.sendMessage(ChatColor.RED + "The user already has direct access to that permission.");
                        sender.sendMessage(ChatColor.RED + "Node: " + auxString);
                        return false;
                    }
                    //PARECE OK
                    auxUser.addPermission(args[1]);
                    sender.sendMessage(ChatColor.YELLOW + "You added '" + args[1] + "' to player '" + auxUser.getName() + "' permissions.");

                    return true;
                //break;
                case manudelp:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (playerGroup != null ? permissionHandler.inGroup(auxUser.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same group than you, or higher.");
                        return false;
                    }
                    if (!isConsole && (!permissionHandler.has(p, args[1]))) {
                        sender.sendMessage(ChatColor.RED + "Can't remove a permission you don't have.");
                        return false;
                    }
                    auxString = permissionHandler.checkUserOnlyPermission(auxUser, args[1]);
                    if (auxString == null) {
                        sender.sendMessage(ChatColor.RED + "The user doesn't have direct access to that permission.");
                        return false;
                    }
                    if (!auxUser.hasSamePermissionNode(args[1])) {
                        sender.sendMessage(ChatColor.RED + "This permission node doesn't match any node.");
                        sender.sendMessage(ChatColor.RED + "But matches node: " + auxString);
                        return false;
                    }
                    //PARECE OK
                    auxUser.removePermission(args[1]);
                    sender.sendMessage(ChatColor.YELLOW + "You removed '" + args[1] + "' from player '" + auxUser.getName() + "' permissions.");

                    return true;
                //break;
                case manulistp:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    //PARECE OK
                    auxString = "";
                    for (String perm : auxUser.getPermissionList()) {
                        auxString += perm + ", ";
                    }
                    if (auxString.lastIndexOf(",") > 0) {
                        auxString = auxString.substring(0, auxString.lastIndexOf(","));
                        sender.sendMessage(ChatColor.YELLOW + "The player '" + auxUser.getName() + "' has following permissions: " + ChatColor.WHITE + auxString);
                        sender.sendMessage(ChatColor.YELLOW + "And all permissions from group: " + auxUser.getGroupName());
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "The player '" + auxUser.getName() + "' has no specific permissions.");
                        sender.sendMessage(ChatColor.YELLOW + "Only all permissions from group: " + auxUser.getGroupName());
                    }
                    return true;
                case manucheckp:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    if (!permissionHandler.checkUserPermission(auxUser, args[1])) {
                        sender.sendMessage(ChatColor.RED + "The player doesn't have access to that permission");
                        return false;
                    }
                    //PARECE OK    
                    auxString = permissionHandler.checkUserOnlyPermission(auxUser, args[1]);
                    if (auxString != null) {
                        sender.sendMessage(ChatColor.YELLOW + "The user has directly this permission.");
                        sender.sendMessage(ChatColor.YELLOW + "Permission Node: " + auxString);
                    }
                    auxGroup = permissionHandler.nextGroupWithPermission(auxUser.getGroup(), args[1], null);
                    if (auxGroup != null) {
                        auxString = permissionHandler.checkGroupOnlyPermission(auxGroup, args[1]);
                        if (auxString != null) {
                            sender.sendMessage(ChatColor.YELLOW + "The user inherits the permission from group: " + auxGroup.getName());
                            sender.sendMessage(ChatColor.YELLOW + "Permission Node: " + auxString);
                        }
                    }
                    return true;
                case mangaddp:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (permissionHandler.checkGroupPermissionWithInheritance(auxGroup, args[1], null)) {
                        sender.sendMessage(ChatColor.RED + "That group already has access to that permission");
                        return false;
                    }
                    //PARECE OK
                    auxGroup.addPermission(args[1]);
                    sender.sendMessage(ChatColor.YELLOW + "You added '" + args[1] + "' to group '" + auxGroup.getName() + "' permissions.");

                    return true;
                case mangdelp:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (!permissionHandler.checkGroupPermissionWithInheritance(auxGroup, args[1], null)) {
                        sender.sendMessage(ChatColor.RED + "That group doesn't has access to that permission");
                        return false;
                    }
                    if (permissionHandler.checkGroupOnlyPermission(auxGroup, args[1]) == null) {
                        sender.sendMessage(ChatColor.RED + "That group doesn't own directly the permission");
                        return false;
                    }
                    if (!auxGroup.hasSamePermissionNode(args[1])) {
                        sender.sendMessage(ChatColor.RED + "That group own the permission. But there is no equal node there.");
                        sender.sendMessage(ChatColor.RED + "The node might be: '" + permissionHandler.checkGroupOnlyPermission(auxGroup, args[1]) + "'");
                        return false;
                    }
                    //PARECE OK
                    auxGroup.removePermission(args[1]);
                    sender.sendMessage(ChatColor.YELLOW + "You removed '" + args[1] + "' from group '" + auxGroup.getName() + "' permissions.");

                    return true;
                case manglistp:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO

                    //PARECE OK
                    auxString = "";
                    for (String perm : auxGroup.getPermissionList()) {
                        auxString += perm + ", ";
                    }
                    if (auxString.lastIndexOf(",") > 0) {
                        auxString = auxString.substring(0, auxString.lastIndexOf(","));
                        sender.sendMessage(ChatColor.YELLOW + "The group '" + auxGroup.getName() + "' has following permissions: " + ChatColor.WHITE + auxString);
                        auxString = "";
                        for (String grp : auxGroup.getInherits()) {
                            auxString += grp + ", ";
                        }
                        if (auxString.lastIndexOf(",") > 0) {
                            auxString = auxString.substring(0, auxString.lastIndexOf(","));
                            sender.sendMessage(ChatColor.YELLOW + "And all permissions from groups: " + auxString);
                        }

                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "The grpup '" + auxGroup.getName() + "' has no specific permissions.");
                        auxString = "";
                        for (String grp : auxGroup.getInherits()) {
                            auxString += grp + ", ";
                        }
                        if (auxString.lastIndexOf(",") > 0) {
                            auxString = auxString.substring(0, auxString.lastIndexOf(","));
                            sender.sendMessage(ChatColor.YELLOW + "Only all permissions from groups: " + auxString);
                        }

                    }
                    return true;
                case mangcheckp:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (!permissionHandler.checkGroupPermissionWithInheritance(auxGroup, args[1], null)) {
                        sender.sendMessage(ChatColor.YELLOW + "The group doesn't have access to that permission");
                        return false;
                    }
                    //PARECE OK
                    auxString = permissionHandler.checkGroupOnlyPermission(auxGroup, args[1]);
                    if (auxString != null) {
                        sender.sendMessage(ChatColor.YELLOW + "The group has directly this permission.");
                        sender.sendMessage(ChatColor.YELLOW + "Permission Node: " + auxString);
                        return true;
                    }
                    auxGroup = permissionHandler.nextGroupWithPermission(auxGroup, args[1], null);
                    if (auxGroup != null) {
                        auxString = permissionHandler.checkGroupOnlyPermission(auxGroup, args[1]);
                        if (auxString != null) {
                            sender.sendMessage(ChatColor.YELLOW + "The user inherits the permission from group: " + auxGroup.getName());
                            sender.sendMessage(ChatColor.YELLOW + "Permission Node: " + auxString);
                            return true;
                        }
                    }
                    sender.sendMessage(ChatColor.RED + "Somehow the group has access, but I can't find where!");
                    return false;
                case mangaddi:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group 1 does not exists!");
                        return false;
                    }
                    auxGroup2 = dataHolder.getGroup(args[1]);
                    if (auxGroup2 == null) {
                        sender.sendMessage(ChatColor.RED + "Group 2 does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (permissionHandler.searchGroupInInheritance(auxGroup, auxGroup2.getName(), null)) {
                        sender.sendMessage(ChatColor.RED + "Group " + auxGroup.getName() + " already inherits " + auxGroup2.getName() + " (might not be directly)");
                        return false;
                    }
                    //PARECE OK
                    auxGroup.addInherits(auxGroup2);
                    sender.sendMessage(ChatColor.RED + "Group  " + auxGroup2.getName() + " is now in " + auxGroup.getName() + " inheritance list.");

                    return true;
                case mangdeli:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group 1 does not exists!");
                        return false;
                    }
                    auxGroup2 = dataHolder.getGroup(args[1]);
                    if (auxGroup2 == null) {
                        sender.sendMessage(ChatColor.RED + "Group 2 does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (!permissionHandler.searchGroupInInheritance(auxGroup, auxGroup2.getName(), null)) {
                        sender.sendMessage(ChatColor.RED + "Group " + auxGroup.getName() + " does not inherits " + auxGroup2.getName() + ".");
                        return false;
                    }
                    if (!auxGroup.getInherits().contains(auxGroup2.getName())) {
                        sender.sendMessage(ChatColor.RED + "Group " + auxGroup.getName() + " does not inherits " + auxGroup2.getName() + " directly.");
                        return false;
                    }
                    //PARECE OK
                    auxGroup.removeInherits(auxGroup2.getName());
                    sender.sendMessage(ChatColor.RED + "Group  " + auxGroup2.getName() + " was removed from " + auxGroup.getName() + " inheritance list.");

                    return true;
                case manuaddv:
                    //VALIDANDO ARGUMENTOS
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    //PARECE OK
                    auxString = "";
                    for (int i = 2; i < args.length; i++) {
                        auxString += args[i];
                        if ((i + 1) < args.length) {
                            auxString += " ";
                        }
                    }
                    auxUser.getVariables().addVar(args[1], Variables.parseVariableValue(auxString));
                    sender.sendMessage(ChatColor.YELLOW + "Variable " + ChatColor.GOLD + args[1] + ChatColor.YELLOW + ":'" + ChatColor.GREEN + auxString + ChatColor.YELLOW + "' added to the user " + auxUser.getName());
                    return true;
                case manudelv:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    if (!auxUser.getVariables().hasVar(args[1])) {
                        sender.sendMessage(ChatColor.RED + "The user doesn't have directly that variable!");
                    }
                    //PARECE OK
                    auxUser.getVariables().removeVar(args[1]);
                    sender.sendMessage(ChatColor.YELLOW + "Variable " + ChatColor.GOLD + args[1] + ChatColor.YELLOW + " removed from the user " + ChatColor.GREEN + auxUser.getName());
                    return true;
                case manulistv:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    //PARECE OK
                    auxString = "";
                    for (String varKey : auxUser.getVariables().getVarKeyList()) {
                        Object o = auxUser.getVariables().getVarObject(varKey);
                        auxString += ChatColor.GOLD + varKey + ChatColor.WHITE + ":'" + ChatColor.GREEN + o.toString() + ChatColor.WHITE + "', ";
                    }
                    if (auxString.lastIndexOf(",") > 0) {
                        auxString = auxString.substring(0, auxString.lastIndexOf(","));
                    }
                    sender.sendMessage(ChatColor.YELLOW + "Variables of user " + auxUser.getName() + ": ");
                    sender.sendMessage(auxString + ".");
                    sender.sendMessage(ChatColor.YELLOW + "Plus all variables from group: " + auxUser.getGroupName());
                    return true;
                case manucheckv:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    auxGroup = auxUser.getGroup();
                    auxGroup2 = permissionHandler.nextGroupWithVariable(auxGroup, args[1], null);

                    if (!auxUser.getVariables().hasVar(args[1])) {
                        if (auxGroup2 == null) {
                            sender.sendMessage(ChatColor.RED + "The user doesn't have access to that variable!");
                        }
                    }
                    //PARECE OK
                    if (auxUser.getVariables().hasVar(auxString)) {
                        sender.sendMessage(ChatColor.YELLOW + "The value of variable '" + ChatColor.GOLD + args[1] + ChatColor.YELLOW + "' is: '" + ChatColor.GREEN + auxUser.getVariables().getVarObject(args[1]).toString() + ChatColor.WHITE + "'");
                        sender.sendMessage(ChatColor.YELLOW + "This user own directly the variable");
                    }
                    sender.sendMessage(ChatColor.YELLOW + "The value of variable '" + ChatColor.GOLD + args[1] + ChatColor.YELLOW + "' is: '" + ChatColor.GREEN + auxGroup2.getVariables().getVarObject(args[1]).toString() + ChatColor.WHITE + "'");
                    if (!auxGroup.equals(auxGroup2)) {
                        sender.sendMessage(ChatColor.YELLOW + "And the value was inherited from group: " + ChatColor.GREEN + auxGroup2.getName());
                    }
                    return true;
                case mangaddv:
                    //VALIDANDO ARGUMENTOS
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    //PARECE OK
                    auxString = "";
                    for (int i = 2; i < args.length; i++) {
                        auxString += args[i];
                        if ((i + 1) < args.length) {
                            auxString += " ";
                        }
                    }
                    auxGroup.getVariables().addVar(args[1], Variables.parseVariableValue(auxString));
                    sender.sendMessage(ChatColor.YELLOW + "Variable " + ChatColor.GOLD + args[1] + ChatColor.YELLOW + ":'" + ChatColor.GREEN + auxString + ChatColor.YELLOW + "' added to the group " + auxGroup.getName());

                    return true;
                case mangdelv:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (!auxGroup.getVariables().hasVar(args[1])) {
                        sender.sendMessage(ChatColor.RED + "The group doesn't have directly that variable!");
                    }
                    //PARECE OK
                    auxGroup.getVariables().removeVar(args[1]);
                    sender.sendMessage(ChatColor.YELLOW + "Variable " + ChatColor.GOLD + args[1] + ChatColor.YELLOW + " removed from the group " + ChatColor.GREEN + auxGroup.getName());

                    return true;
                case manglistv:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    //PARECE OK
                    auxString = "";
                    for (String varKey : auxGroup.getVariables().getVarKeyList()) {
                        Object o = auxGroup.getVariables().getVarObject(varKey);
                        auxString += ChatColor.GOLD + varKey + ChatColor.WHITE + ":'" + ChatColor.GREEN + o.toString() + ChatColor.WHITE + "', ";
                    }
                    if (auxString.lastIndexOf(",") > 0) {
                        auxString = auxString.substring(0, auxString.lastIndexOf(","));
                    }
                    sender.sendMessage(ChatColor.YELLOW + "Variables of group " + auxGroup.getName() + ": ");
                    sender.sendMessage(auxString + ".");
                    auxString = "";
                    for (String grp : auxGroup.getInherits()) {
                        auxString += grp + ", ";
                    }
                    if (auxString.lastIndexOf(",") > 0) {
                        auxString = auxString.substring(0, auxString.lastIndexOf(","));
                        sender.sendMessage(ChatColor.YELLOW + "Plus all variables from groups: " + auxString);
                    }
                    return true;
                case mangcheckv:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    auxGroup = dataHolder.getGroup(args[0]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group does not exists!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    auxGroup2 = permissionHandler.nextGroupWithVariable(auxGroup, args[1], null);
                    if (auxGroup2 == null) {
                        sender.sendMessage(ChatColor.RED + "The group doesn't have access to that variable!");
                    }
                    //PARECE OK
                    sender.sendMessage(ChatColor.YELLOW + "The value of variable '" + ChatColor.GOLD + args[1] + ChatColor.YELLOW + "' is: '" + ChatColor.GREEN + auxGroup2.getVariables().getVarObject(args[1]).toString() + ChatColor.WHITE + "'");
                    if (!auxGroup.equals(auxGroup2)) {
                        sender.sendMessage(ChatColor.YELLOW + "And the value was inherited from group: " + ChatColor.GREEN + auxGroup2.getName());
                    }
                    return true;
                case manwhois:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //PARECE OK
                    sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.GREEN + auxUser.getName());
                    sender.sendMessage(ChatColor.YELLOW + "Group: " + ChatColor.GREEN + auxUser.getGroup().getName());
                    sender.sendMessage(ChatColor.YELLOW + "Overloaded: " + ChatColor.GREEN + dataHolder.isOverloaded(auxUser.getName()));
                    auxGroup = dataHolder.surpassOverload(auxUser.getName()).getGroup();
                    if (!auxGroup.equals(auxUser.getGroup())) {
                        sender.sendMessage(ChatColor.YELLOW + "Original Group: " + ChatColor.GREEN + auxGroup);
                    }
                    //victim.permissions.add(args[1]);
                    return true;
                //break;
                case tempadd:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (playerGroup != null ? permissionHandler.inGroup(auxUser.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    //PARECE OK
                    dataHolder.overloadUser(auxUser.getName());
                    overloadedUsers.get(dataHolder.getName()).add(dataHolder.getUser(auxUser.getName()));
                    sender.sendMessage(ChatColor.YELLOW + "Player overloaded!");

                    return true;
                //break;
                case tempdel:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (playerGroup != null ? permissionHandler.inGroup(auxUser.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    //PARECE OK
                    dataHolder.removeOverload(auxUser.getName());
                    if (overloadedUsers.get(dataHolder.getName().toLowerCase()).contains(auxUser)) {
                        overloadedUsers.get(dataHolder.getName()).remove(auxUser);
                    }
                    sender.sendMessage(ChatColor.YELLOW + "You removed that player overload. He's back to normal!");

                    return true;
                //break;
                case templist:
                    auxString = "";
                    removeList = new ArrayList<User>();
                    count = 0;
                    for (User u : overloadedUsers.get(dataHolder.getName())) {
                        if (!dataHolder.isOverloaded(u.getName())) {
                            removeList.add(u);
                        } else {
                            auxString += u.getName() + ", ";
                            count++;
                        }
                    }
                    if (count == 0) {
                        sender.sendMessage(ChatColor.YELLOW + "There is no users in overload mode");
                        return true;
                    }
                    auxString = auxString.substring(0, auxString.lastIndexOf(","));
                    overloadedUsers.get(dataHolder.getName()).removeAll(removeList);
                    sender.sendMessage(ChatColor.YELLOW + " " + count + " Users in overload mode: " + ChatColor.WHITE + auxString);
                    return true;
                case tempdelall:
                    removeList = new ArrayList<User>();
                    count = 0;
                    for (User u : overloadedUsers.get(dataHolder.getName())) {
                        if (dataHolder.isOverloaded(u.getName())) {
                            dataHolder.removeOverload(u.getName());
                            count++;
                        }
                    }
                    if (count == 0) {
                        sender.sendMessage(ChatColor.YELLOW + "There is no users in overload mode");
                        return true;
                    }
                    overloadedUsers.clear();
                    sender.sendMessage(ChatColor.YELLOW + " " + count + " Users in overload mode. Now they are normal again.");

                    return true;
                case mansave:
                    commit();
                    sender.sendMessage(ChatColor.YELLOW + " Permissions saved.");

                    return true;
                case manload:
                    reload();
                    sender.sendMessage(ChatColor.YELLOW + " Permissions reloaded.");
                    return true;
                case listgroups:
                    auxString = "";
                    for (Group g : dataHolder.getGroupList()) {
                        auxString += g.getName() + ", ";
                    }
                    if (auxString.lastIndexOf(",") > 0) {
                        auxString = auxString.substring(0, auxString.lastIndexOf(","));
                    }
                    sender.sendMessage(ChatColor.YELLOW + " Groups Available: " + ChatColor.WHITE + auxString);
                    return true;
                case manpromote:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    auxGroup = dataHolder.getGroup(args[1]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group not found!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (playerGroup != null ? permissionHandler.inGroup(auxUser.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    if (!isConsole && (permissionHandler.searchGroupInInheritance(auxGroup, playerGroup.getName(), null))) {
                        sender.sendMessage(ChatColor.RED + "The destination group can't be the same as yours, or higher.");
                        return false;
                    }
                    if (!isConsole && (!permissionHandler.inGroup(playerUser.getName(), auxUser.getGroupName()) || !permissionHandler.inGroup(playerUser.getName(), auxGroup.getName()))) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player involving a group of different heritage line.");
                        return false;
                    }
                    if (!isConsole && (!permissionHandler.searchGroupInInheritance(auxGroup, auxUser.getGroupName(), null))) {
                        sender.sendMessage(ChatColor.RED + "The new group must be a higher rank.");
                        return false;
                    }
                    //PARECE OK
                    auxUser.setGroup(auxGroup);
                    sender.sendMessage(ChatColor.YELLOW + "You changed that player group.");

                    return true;
                //break;
                case mandemote:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    if (validateOnlinePlayer) {
                        match = this.getServer().matchPlayer(args[0]);
                        if (match.size() != 1) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return false;
                        }
                    }
                    if (match != null) {
                        auxUser = dataHolder.getUser(match.get(0).getName());
                    } else {
                        auxUser = dataHolder.getUser(args[0]);
                    }
                    auxGroup = dataHolder.getGroup(args[1]);
                    if (auxGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group not found!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (playerGroup != null ? permissionHandler.inGroup(auxUser.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    if (!isConsole && (permissionHandler.searchGroupInInheritance(auxGroup, playerGroup.getName(), null))) {
                        sender.sendMessage(ChatColor.RED + "The destination group can't be the same as yours, or higher.");
                        return false;
                    }
                    if (!isConsole && (!permissionHandler.inGroup(playerUser.getName(), auxUser.getGroupName()) || !permissionHandler.inGroup(playerUser.getName(), auxGroup.getName()))) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player involving a group of different heritage line.");
                        return false;
                    }
                    if (!isConsole && (permissionHandler.searchGroupInInheritance(auxGroup, auxUser.getGroupName(), null))) {
                        sender.sendMessage(ChatColor.RED + "The new group must be a lower rank.");
                        return false;
                    }
                    //PARECE OK
                    auxUser.setGroup(auxGroup);
                    sender.sendMessage(ChatColor.YELLOW + "You changed that player group.");

                    return true;
                //break;
                case mantogglevalidate:
                    validateOnlinePlayer = !validateOnlinePlayer;
                    sender.sendMessage(ChatColor.YELLOW + "Validade if player is online, now set to: " + Boolean.toString(validateOnlinePlayer));
                    if (!validateOnlinePlayer) {
                        sender.sendMessage(ChatColor.GOLD + "From now on you can edit players not connected... BUT:");
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "From now on you should type the whole name of the player, correctly.");
                    }
                    return true;
                case mantogglesave:
                    if (scheduler == null) {
                        enableScheduler();
                        sender.sendMessage(ChatColor.YELLOW + "The auto-saving is enabled!");
                    } else {
                        disableScheduler();
                        sender.sendMessage(ChatColor.YELLOW + "The auto-saving is disabled!");
                    }
                    return true;
                case manworld:
                    sender.sendMessage(ChatColor.YELLOW + "You have the world '"+dataHolder.getName()+"' in your selection.");
                    return true;
                case manselect:
                    if (args.length != 1) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    dataHolder = worldsHolder.getWorldData(args[0]);
                    permissionHandler = dataHolder.getPermissionsHandler();
                    selectedWorlds.put(sender, dataHolder.getName());
                    sender.sendMessage(ChatColor.YELLOW + "You have selected world '"+dataHolder.getName()+"'.");
                    return true;
                default:
                    break;
            }
        }
        sender.sendMessage(ChatColor.RED + "You are not allowed to use that command.");
        return false;
    }

    /**
     * @return the config
     */
    public GMConfiguration getConfig() {
        return config;
    }

    /**
     * @return the backupFolder
     */
    public File getBackupFolder() {
        return backupFolder;
    }
}
