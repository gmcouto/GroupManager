/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager;

import com.nijiko.permissions.PermissionHandler;
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

    /**
     *
     * @param pluginLoader
     * @param instance
     * @param desc
     * @param folder
     * @param plugin
     * @param cLoader
     */
    public GroupManager(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
        super(pluginLoader, instance, desc, folder, plugin, cLoader);
        configFile = new File(this.getDataFolder(), "config.yml");
        permissionsFile = new File(this.getDataFolder(), "data.yml");
        nijikokunPermissionsFile = new File(this.getDataFolder().getParentFile(), "Permissions" + permissionsFile.separator + "config.yml");
        backupFolder = new File(this.getDataFolder(), "backup");
        //
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
        //
        config = new Configuration(configFile);
        if(!configFile.exists()){
            config.setProperty("settings.data.save.minutes", 10);
            config.save();
        }
        config.load();
        //
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
        commiter = new Runnable() {

            @Override
            public void run() {
                GroupManager.this.commit();
            }
        };
        permissionHandler = new AnjoPermissionsHandler(dataHolder);

    }

    public void onDisable() {
        //throw new UnsupportedOperationException("Not supported yet.");
        if (dataHolder != null) {
            dataHolder.commit();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }

        scheduler = null;

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!");
    }

    public void onEnable() {
        PluginDescriptionFile pdfFile = this.getDescription();
        if (dataHolder == null) {
            System.out.println("Can't enable " + pdfFile.getName() + " version " + pdfFile.getVersion() + ", bad loading!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        doBackup();
        dataHolder.reload();
        scheduler = new ScheduledThreadPoolExecutor(1);
        int minutes = config.getInt("settings.data.save.minutes", 10);
        scheduler.scheduleAtFixedRate(commiter, minutes, minutes, TimeUnit.MINUTES);

        //
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
    }

    /**
     * Saves the data on file
     */
    public void commit() {
        if (dataHolder != null) {
            doBackup();
            dataHolder.commit();
        }
    }

    /**
     * Reloads the data
     */
    public void reload() {
        if (dataHolder != null) {
            dataHolder.reload();
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
        File backup = new File(backupFolder, "bkpperm" + System.currentTimeMillis() + ".yml");
        if (backup.exists() && backup.isFile()) {
            backup.delete();
        }
        try {
            copy(permissionsFile, backup);
        } catch (IOException ex) {
            Logger.getLogger(GroupManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    /**
     * The new permission handler in the old model... it extends the old, by Nijikokun
     * It is compatible with the plugins made for Permissions
     * @return a permission handler
     */
    public PermissionHandler getPermissionHandler() {
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
        Group playerGroup = null;
        if (sender instanceof Player) {
            Player p = (Player) sender;
            playerGroup = dataHolder.getUser(p.getName()).getGroup();
            if (p.isOp() || permissionHandler.has(p, "groupmanager." + cmd.getName())) {
                playerCanDo = true;
            }
        } else if(sender instanceof ConsoleCommandSender){
            isConsole = true;
        }



        int count;
        ArrayList<User> removeList;
        String aux;
        List<Player> match;
        User victim;
        Group destGroup;

        if (isConsole || playerCanDo) {
            switch (GroupManagerPermissions.valueOf(cmd.getName())) {
                case mangroup:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "Review your arguments count!");
                        return false;
                    }
                    match = this.getServer().matchPlayer(args[0]);
                    if (match.size() != 1) {
                        sender.sendMessage(ChatColor.RED + "Player not found!");
                        return false;
                    }
                    victim = dataHolder.getUser(match.get(0).getName());
                    destGroup = dataHolder.getGroup(args[1]);
                    if (destGroup == null) {
                        sender.sendMessage(ChatColor.RED + "Group not found!");
                        return false;
                    }
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (match.get(0).isOp() || playerGroup != null ? permissionHandler.inGroup(victim.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    //PARECE OK
                    victim.setGroup(destGroup);
                    sender.sendMessage(ChatColor.YELLOW + "You changed that player group.");
                    return true;
                //break;
                case addpermission:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 2) {
                        return false;
                    }
                    match = this.getServer().matchPlayer(args[0]);
                    if (match.size() != 1) {
                        return false;
                    }
                    victim = dataHolder.getUser(match.get(0).getName());
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (match.get(0).isOp() || playerGroup != null ? permissionHandler.inGroup(victim.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    //PARECE OK
                    victim.permissions.add(args[1]);
                    sender.sendMessage(ChatColor.YELLOW + "You changed that player permissions.");
                    return true;
                //break;
                case manwhois:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        return false;
                    }
                    match = this.getServer().matchPlayer(args[0]);
                    if (match.size() != 1) {
                        return false;
                    }
                    victim = dataHolder.getUser(match.get(0).getName());
                    //PARECE OK
                    sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.GREEN + victim.getName());
                    sender.sendMessage(ChatColor.YELLOW + "Group: " + ChatColor.GREEN + victim.getGroup().getName());
                    sender.sendMessage(ChatColor.YELLOW + "Overloaded: " + ChatColor.GREEN + dataHolder.isOverloaded(victim.getName()));
                    destGroup = dataHolder.surpassOverload(victim.getName()).getGroup();
                    if (!destGroup.equals(victim.getGroup())) {
                        sender.sendMessage(ChatColor.YELLOW + "Original Group: " + ChatColor.GREEN + destGroup);
                    }
                    //victim.permissions.add(args[1]);
                    return true;
                //break;
                case overload:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        return false;
                    }
                    match = this.getServer().matchPlayer(args[0]);
                    if (match.size() != 1) {
                        return false;
                    }
                    victim = dataHolder.getUser(match.get(0).getName());
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (match.get(0).isOp() || playerGroup != null ? permissionHandler.inGroup(victim.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    //PARECE OK
                    dataHolder.overloadUser(victim.getName());
                    overloadedUsers.add(dataHolder.getUser(victim.getName()));
                    sender.sendMessage(ChatColor.YELLOW + "Player overloaded!");
                    return true;
                //break;
                case underload:
                    //VALIDANDO ARGUMENTOS
                    if (args.length != 1) {
                        return false;
                    }
                    match = this.getServer().matchPlayer(args[0]);
                    if (match.size() != 1) {
                        return false;
                    }
                    victim = dataHolder.getUser(match.get(0).getName());
                    //VALIDANDO PERMISSAO
                    if (!isConsole && (match.get(0).isOp() || playerGroup != null ? permissionHandler.inGroup(victim.getName(), playerGroup.getName()) : false)) {
                        sender.sendMessage(ChatColor.RED + "Can't modify player with same permissions than you, or higher.");
                        return false;
                    }
                    //PARECE OK
                    dataHolder.removeOverload(victim.getName());
                    if (overloadedUsers.contains(victim)) {
                        overloadedUsers.remove(victim);
                    }
                    sender.sendMessage(ChatColor.YELLOW + "You removed that player overload. He's back to normal!");
                    return true;
                //break;
                case listoverload:
                    aux = "";
                    removeList = new ArrayList<User>();
                    count = 0;
                    for (User u : overloadedUsers) {
                        if (!dataHolder.isOverloaded(u.getName())) {
                            removeList.add(u);
                        } else {
                            aux += u.getName() + ", ";
                            count++;
                        }
                    }
                    if (count == 0) {
                        sender.sendMessage(ChatColor.YELLOW + "There is no users in overload mode");
                        return true;
                    }
                    aux = aux.substring(0, aux.lastIndexOf(","));
                    overloadedUsers.removeAll(removeList);
                    sender.sendMessage(ChatColor.YELLOW + " " + count + " Users in overload mode: " + ChatColor.WHITE + aux);
                    return true;
                case underloadall:
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
                case mancommit:
                    commit();
                    sender.sendMessage(ChatColor.YELLOW + " Permissions saved.");
                    return true;
                case manreload:
                    reload();
                    sender.sendMessage(ChatColor.YELLOW + " Permissions reloaded.");
                    return true;
                case listgroups:
                    aux = "";
                    for(Group g: dataHolder.getGroupList()){
                        aux += g.getName() + ", ";
                    }
                    if(aux.lastIndexOf(",")>0){
                        aux = aux.substring(0, aux.lastIndexOf(","));
                    }
                    sender.sendMessage(ChatColor.YELLOW + " Groups Available: "+ChatColor.WHITE +aux);
                default:
                    break;
            }
        }
        return false;
    }
}
