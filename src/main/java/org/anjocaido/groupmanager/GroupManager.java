/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager;

import com.nijiko.permissions.PermissionHandler;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

/**
 *
 * @author gabrielcouto
 */
public class GroupManager extends JavaPlugin {

    private File permissionsFile;
    private File configFile;
    private File backupFolder;
    private File nijikokunPermissionsFile;
    private OverloadedDataHolder dataHolder;
    private Runnable commiter;
    private ScheduledThreadPoolExecutor scheduler;
    private AnjoPermissionsHandler permissionHandler;
    private ArrayList<User> overloadedUsers = new ArrayList<User>();
    private Configuration config;
    private boolean validateOnlinePlayer = true;

    @Override
    public void onDisable() {
        //throw new UnsupportedOperationException("Not supported yet.");
        if (dataHolder != null) {
            dataHolder.commit();
        }
        disableScheduler();
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!");
    }

    @Override
    public void onEnable() {
        prepareFileFields();
        prepareConfig();
        doBackup();
        prepareData();

        PluginDescriptionFile pdfFile = this.getDescription();
        if (dataHolder == null) {
            System.out.println("Can't enable " + pdfFile.getName() + " version " + pdfFile.getVersion() + ", bad loading!");
            this.getServer().getPluginManager().disablePlugin(this);
            throw new IllegalStateException("An error ocurred while loading GroupManager");
        }

        enableScheduler();
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
    }

    private void prepareData() {
        if (dataHolder == null) {
            if (!permissionsFile.exists()) {
                if (nijikokunPermissionsFile.exists()) {
                    try {
                        copy(nijikokunPermissionsFile, permissionsFile);
                    } catch (IOException ex) {
                        Logger.getLogger(GroupManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (!permissionsFile.exists()) {
                    this.getPluginLoader().disablePlugin(this);
                    dataHolder = null;
                    throw new IllegalStateException("There is no permissions file! " + permissionsFile.getPath());
                }
            }
            try {
                dataHolder = new OverloadedDataHolder(DataHolder.load(permissionsFile));
            } catch (Exception ex) {
                dataHolder = null;
                Logger.getLogger(GroupManager.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalArgumentException("Permissions file is in wrong format");
            }
        } else {
            dataHolder.reload();
        }
        if (permissionHandler == null) {
            permissionHandler = new AnjoPermissionsHandler(dataHolder);
        }
    }

    private void prepareFileFields() {
        configFile = new File(this.getDataFolder(), "config.yml");
        permissionsFile = new File(this.getDataFolder(), "data.yml");
        nijikokunPermissionsFile = new File(this.getDataFolder().getParentFile(), "Permissions" + permissionsFile.separator + "config.yml");
        backupFolder = new File(this.getDataFolder(), "backup");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    private void prepareConfig() {
        config = new Configuration(configFile);
        if (!configFile.exists()) {
            config.setProperty("settings.data.save.minutes", 10);
            config.save();
        }
        config.load();
    }

    public void enableScheduler() {
        if (dataHolder != null) {
            disableScheduler();
            commiter = new Runnable() {

                @Override
                public void run() {
                    GroupManager.this.commit();
                }
            };
            scheduler = new ScheduledThreadPoolExecutor(1);
            int minutes = config.getInt("settings.data.save.minutes", 10);
            scheduler.scheduleAtFixedRate(commiter, minutes, minutes, TimeUnit.MINUTES);
            System.out.println(this.getDescription().getName() + " - Scheduled Data Saving is set for every " + minutes + " minutes!");
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
            System.out.println(this.getDescription().getName() + " - Scheduled Data Saving has been disabled!");
        }
    }

    /**
     * Saves the data on file
     */
    public void commit() {
        if (dataHolder != null) {
            doBackup();
            System.out.println(this.getDescription().getName() + " - Saving your data...");
            dataHolder.commit();
            System.out.println(this.getDescription().getName() + " - Saving done!");
        }
    }

    /**
     * Reloads the data
     */
    public void reload() {
        if (dataHolder != null) {
            System.out.println(this.getDescription().getName() + " - Reloading your data...");
            dataHolder.reload();
            System.out.println(this.getDescription().getName() + " - Roload done!");
        }
    }

    private void copy(InputStream src, File dst) throws IOException {
        InputStream in = src;
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
    }

    private void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        copy(in, dst);
        in.close();
    }

    private void doBackup() {
        System.out.println(this.getDescription().getName() + " - Backing up your data...");
        File backup = new File(backupFolder, "bkpperm" + System.currentTimeMillis() + ".yml");
        if (backup.exists() && backup.isFile()) {
            backup.delete();
        }
        try {
            copy(permissionsFile, backup);
        } catch (IOException ex) {
            Logger.getLogger(GroupManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(this.getDescription().getName() + " - Backup done!");
    }

    /**
     * The new permission handler in the old interface... it extends the old, by Nijikokun
     * It is compatible with the plugins made for Permissions
     * @return a permission handler
     */
    public PermissionHandler getPermissionHandler() {
        return permissionHandler;
    }

    /**
     * The handler in the interface created by AnjoCaido
     * @return
     */
    public AnjoPermissionsHandler getHandler() {
        return permissionHandler;
    }

    /**
     *  A simple interface, for ones that don't want to mess with overloading.
     * Yet it is affected by overloading. But seamless.
     * @return the dataholder with all information
     */
    public DataHolder getData() {
        return dataHolder;
    }

    /**
     *  Use this if you want to play with overloading.
     * @return  a dataholder with overloading interface
     */
    public OverloadedDataHolder getOverloadedClassData() {
        return dataHolder;
    }

    /**
     * Called when a command registered by this plugin is received.
     * @param sender 
     * @param cmd 
     * @param args
     */
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        boolean playerCanDo = false;
        boolean isConsole = false;
        Player p = null;
        Group playerGroup = null;
        User playerUser = null;
        if (sender instanceof Player) {
            p = (Player) sender;
            playerUser = dataHolder.getUser(p.getName());
            playerGroup = playerUser.getGroup();
            if (permissionHandler.has(p, "groupmanager." + cmd.getName())) {
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
            System.out.println("===================================================");
            System.out.println("=              ERROR REPORT START                 =");
            System.out.println("===================================================");
            System.out.println("=  COPY AND PASTE THIS TO GROUPMANAGER DEVELOPER  =");
            System.out.println("===================================================");
            System.out.println(this.getDescription().getName());
            System.out.println(this.getDescription().getVersion());
            System.out.println("An error occured while trying to execute command:");
            System.out.println(cmd.getName());
            System.out.println("With " + args.length + " arguments:");
            for (String ar : args) {
                System.out.println(ar);
            }
            System.out.println("The field '" + cmd.getName() + "' was not found in enum.");
            System.out.println("And could not be parsed.");
            System.out.println("FIELDS FOUND IN ENUM:");
            for (GroupManagerPermissions val : GroupManagerPermissions.values()) {
                System.out.println(val);
            }
            System.out.println("===================================================");
            System.out.println("=              ERROR REPORT ENDED                 =");
            System.out.println("===================================================");
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
                    auxUser.permissions.add(args[1]);
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
                    if (!auxUser.permissions.contains(args[1])) {
                        sender.sendMessage(ChatColor.RED + "This permission node doesn't match any node.");
                        sender.sendMessage(ChatColor.RED + "But matches node: " + auxString);
                        return false;
                    }
                    //PARECE OK
                    auxUser.permissions.remove(args[1]);
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
                    for (String perm : auxUser.permissions) {
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
                    auxGroup.permissions.add(args[1]);
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
                    if (!auxGroup.permissions.contains(args[1])) {
                        sender.sendMessage(ChatColor.RED + "That group own the permission. But there is no equal node there.");
                        sender.sendMessage(ChatColor.RED + "The node might be: '" + permissionHandler.checkGroupOnlyPermission(auxGroup, args[1]) + "'");
                        return false;
                    }
                    //PARECE OK
                    auxGroup.permissions.remove(args[1]);
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
                    for (String perm : auxGroup.permissions) {
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
                    auxGroup.variables.addVar(args[1], Variables.parseVariableValue(auxString));
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
                    if (!auxGroup.variables.hasVar(args[1])) {
                        sender.sendMessage(ChatColor.RED + "The group doesn't have directly that variable!");
                    }
                    //PARECE OK
                    auxGroup.variables.removeVar(args[1]);
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
                    for (String varKey : auxGroup.variables.getVarKeyList()) {
                        Object o = auxGroup.variables.getVarObject(varKey);
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
                    sender.sendMessage(ChatColor.YELLOW + "The value of variable '" + ChatColor.GOLD + args[1] + ChatColor.YELLOW + "' is: '" + ChatColor.GREEN + auxGroup2.variables.getVarObject(args[1]).toString() + ChatColor.WHITE + "'");
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
                    overloadedUsers.add(dataHolder.getUser(auxUser.getName()));
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
                    if (overloadedUsers.contains(auxUser)) {
                        overloadedUsers.remove(auxUser);
                    }
                    sender.sendMessage(ChatColor.YELLOW + "You removed that player overload. He's back to normal!");

                    return true;
                //break;
                case templist:
                    auxString = "";
                    removeList = new ArrayList<User>();
                    count = 0;
                    for (User u : overloadedUsers) {
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
                    overloadedUsers.removeAll(removeList);
                    sender.sendMessage(ChatColor.YELLOW + " " + count + " Users in overload mode: " + ChatColor.WHITE + auxString);
                    return true;
                case tempdelall:
                    removeList = new ArrayList<User>();
                    count = 0;
                    for (User u : overloadedUsers) {
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
                default:
                    break;
            }
        }
        sender.sendMessage(ChatColor.RED + "You are not allowed to use that command.");
        return false;
    }
}
