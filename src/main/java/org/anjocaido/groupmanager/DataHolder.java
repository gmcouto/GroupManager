/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;

/**
 *
 * @author gabrielcouto
 */
public class DataHolder {

    /**
     *  The actual groups holder
     */
    protected Map<String, Group> groups = new HashMap<String, Group>();
    /**
     * The actual users holder
     */
    protected Map<String, User> users = new HashMap<String, User>();
    /**
     *  Points to the default group
     */
    protected Group defaultGroup = null;
    /**
     * The file, which this class loads/save data from/to
     */
    protected File f;

    /**
     *  Prevent direct instantiation
     */
    protected DataHolder() {
    }

    /**
     * The main constructor for a new DataHolder
     *  Please don't set the default group as null
     * @param defaultGroup the default group. its good to start with one
     */
    public DataHolder(Group defaultGroup) {
        groups.put(defaultGroup.getName(), defaultGroup);
        this.defaultGroup = defaultGroup;
    }

    /**
     * Search for a user. If it doesn't exist, create a new one with
     * default group.
     *
     * @param userName the name of the user
     * @return class that manage that user permission
     */
    public User getUser(String userName) {
        if (users.containsKey(userName.toLowerCase())) {
            return users.get(userName.toLowerCase());
        }
        User newUser = createUser(userName);
        return newUser;
    }

    /**
     *  Add a user to the list. If it already exists, overwrite the old.
     * @param theUser the user you want to add to the permission list
     */
    public void addUser(User theUser) {
        if (theUser.getDataSource() != this) {
            theUser = theUser.clone(this);
        }
        if (theUser == null) {
            return;
        }
        if ((theUser.getGroup() == null) || (!groups.containsKey(theUser.getGroup()))) {
            theUser.setGroup(defaultGroup);
        }
        removeUser(theUser.getName());
        users.put(theUser.getName(), theUser);
    }

    /**
     * Removes the user from the list. (he might become a default user)
     * @param userName the username from the user to remove
     * @return true if it had something to remove
     */
    public boolean removeUser(String userName) {
        if (users.containsKey(userName.toLowerCase())) {
            users.remove(userName.toLowerCase());
            return true;
        }
        return false;
    }

    /**
     *  Change the default group of the file.
     * @param group the group you want make default.
     */
    public void setDefaultGroup(Group group) {
        if (!groups.containsKey(group.getName()) || (group.getDataSource() != this)) {
            addGroup(group);
        }
        defaultGroup = this.getGroup(group.getName());
    }

    /**
     *  Returns the default group of the file
     * @return the default group
     */
    public Group getDefaultGroup() {
        return defaultGroup;
    }

    /**
     *  Returns a group of the given name
     * @param groupName the name of the group
     * @return a group if it is found. null if not found.
     */
    public Group getGroup(String groupName) {
        for (String key : groups.keySet()) {
            if (groupName.equalsIgnoreCase(key)) {
                return groups.get(key);
            }
        }
        return null;
    }

    /**
     *  Check if a group exists.
     * Its the same of getGroup, but check if it is null.
     * @param groupName the name of the group
     * @return true if exists. false if not.
     */
    public boolean groupExists(String groupName) {
        for (String key : groups.keySet()) {
            if (groupName.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a group to the list
     * @param groupToAdd
     */
    public void addGroup(Group groupToAdd) {
        if (groupToAdd.getDataSource() != this) {
            groupToAdd = groupToAdd.clone(this);
        }
        removeGroup(groupToAdd.getName());
        groups.put(groupToAdd.getName(), groupToAdd);
    }

    /**
     *  Remove the group to the list
     * @param groupName
     * @return true if had something to remove. false the group was default or non-existant
     */
    public boolean removeGroup(String groupName) {
        if (groupName.equals(defaultGroup)) {
            return false;
        }
        for (String key : groups.keySet()) {
            if (groupName.equalsIgnoreCase(key)) {
                groups.remove(key);
                return true;
            }
        }
        return false;

    }

    /**
     * Creates a new User with the given name
     * and adds it to this holder.
     * @param userName the username you want
     * @return null if user already exists. or new User
     */
    public User createUser(String userName) {
        if (this.groups.containsKey(userName.toLowerCase())) {
            return null;
        }
        User newUser = new User(this, userName);
        newUser.setGroup(defaultGroup);
        this.addUser(newUser);
        return newUser;
    }

    /**
     * Creates a new Group with the given name
     * and adds it to this holder
     * @param groupName the groupname you want
     * @return null if group already exists. or new Group
     */
    public Group createGroup(String groupName) {
        if (this.groups.containsKey(groupName)) {
            return null;
        }
        Group newGroup = new Group(this, groupName);
        this.addGroup(newGroup);
        return newGroup;
    }

    /**
     *
     * @return a collection of the groups
     */
    public Collection<Group> getGroupList() {
        return groups.values();
    }

    /**
     *
     * @return a collection of the users
     */
    public Collection<User> getUserList() {
        return users.values();
    }

    /**
     *  reads the file again
     */
    public void reload() {
        try {
            DataHolder ph = load(f);
            this.defaultGroup = ph.defaultGroup;
            this.groups = ph.groups;
            this.users = ph.users;
        } catch (Exception ex) {
            Logger.getLogger(DataHolder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *  save to the file
     */
    public void commit() {
        write(this, f);
    }

    /**
     *  Returns a data holder for the given file
     * @param file
     * @return
     * @throws Exception
     */
    public static DataHolder load(File file) throws Exception {
        DataHolder ph = new DataHolder();
        ph.f = file;
        final Yaml yaml = new Yaml(new SafeConstructor());
        Map<String, Object> data1;
        if (!file.exists()) {
            throw new Exception("This server does not use Permissions.");
        }
        FileInputStream rx = new FileInputStream(file);
        try {
            data1 = (Map<String, Object>) yaml.load(new UnicodeReader(rx));
            if (data1 == null) {
                throw new NullPointerException();
            }
        } catch (Exception ex) {
            throw new Exception("This server does not have Permissions configured properly.", ex);
        } finally {
            rx.close();
        }
        Map<String, String> inheritance = new HashMap<String, String>();
        try {
            Map<String, Object> groupsNode = (Map<String, Object>) data1.get("groups");
            for (String groupKey : groupsNode.keySet()) {
                Map<String, Object> group = (Map<String, Object>) groupsNode.get(groupKey);
                Group thisGrp = new Group(ph, groupKey);
                if (group.get("default") == null) {
                    group.put("default", false);
                }
                if ((Boolean.parseBoolean(group.get("default").toString()))) {
                    ph.defaultGroup = thisGrp;
                }

                //PERMISSIONS NODE
                if (group.get("permissions") == null) {
                    group.put("permissions", new ArrayList<String>());
                }
                if (group.get("permissions") instanceof List) {
                    for (Object o : ((List) group.get("permissions"))) {
                        thisGrp.permissions.add(o.toString());
                    }
                } else if (group.get("permissions") instanceof String) {
                    thisGrp.permissions.add((String) group.get("permissions"));
                } else {
                    throw new Exception("Unknown type of permissions node(Should be String or List<String>): " + group.get("permissions").getClass().getName());
                }

                //INFO NODE
                Map<String, Object> infoNode = (Map<String, Object>) group.get("info");
                if (infoNode == null) {
                    infoNode = new HashMap<String, Object>();
                }
                if (infoNode.get("prefix") == null) {
                    infoNode.put("prefix", "");
                }
                //thisGrp.prefix = infoNode.get("prefix").toString();

                if (infoNode.get("suffix") == null) {
                    infoNode.put("suffix", "");
                }
                //thisGrp.suffix = infoNode.get("suffix").toString();

                if (infoNode.get("build") == null) {
                    infoNode.put("build", false);
                }
                //thisGrp.canbuild = Boolean.parseBoolean(infoNode.get("build").toString());
                thisGrp.variables = new Variables(infoNode);

                Object inheritNode = group.get("inheritance");
                if (inheritNode == null) {
                    group.put("inheritance", new ArrayList<String>());
                } else if (inheritNode instanceof List) {
                    List<String> groupsInh = (List<String>) inheritNode;
                    for (String grp : groupsInh) {
                        //System.out.println("Found inheritance "+grp+" for group"+groupKey);
                        inheritance.put(groupKey, grp);
                    }
                }
                ph.groups.put(thisGrp.getName(), thisGrp);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new Exception("Your Permissions config file is invalid.  See console for details.");
        }
        for (String groupKey : inheritance.keySet()) {
            String inherited = inheritance.get(groupKey);
            //System.out.println("Inserting inheritance "+inherited+" for group"+groupKey);
            if (ph.groups.containsKey(groupKey) && ph.groups.containsKey(inherited)) {
                Group grp = ph.groups.get(groupKey);
                Group inh = ph.groups.get(inherited);
                grp.addInherits(inh);
            }
        }
        // Process users to check for invalid value and determine group
        Map<String, Object> usersNode = (Map<String, Object>) data1.get("users");
        for (String usersKey : usersNode.keySet()) {
            Map<String, Object> node = (Map<String, Object>) usersNode.get(usersKey);
            User thisUser = new User(ph, usersKey);
            if (node.get("permissions") == null) {
                node.put("permissions", new ArrayList<String>());
            }
            thisUser.permissions = (ArrayList<String>) node.get("permissions");

            if (node.get("group") != null) {
                thisUser.setGroup(ph.groups.get(node.get("group")));
            } else {
                thisUser.setGroup(ph.defaultGroup);
            }

            ph.users.put(thisUser.getName(), thisUser);
        }
        return ph;
    }

    /**
     *  Write a dataHolder in a specified file
     * @param ph
     * @param file
     */
    public static void write(DataHolder ph, File file) {
        Map<String, Object> root = new HashMap<String, Object>();

        Map<String, Object> pluginMap = new HashMap<String, Object>();
        root.put("plugin", pluginMap);

        Map<String, Object> permissionsMap = new HashMap<String, Object>();
        pluginMap.put("permissions", permissionsMap);

        permissionsMap.put("system", "default");

        Map<String, Object> groupsMap = new HashMap<String, Object>();
        root.put("groups", groupsMap);
        for (String groupKey : ph.groups.keySet()) {
            Group group = ph.groups.get(groupKey);

            Map<String, Object> aGroupMap = new HashMap<String, Object>();
            groupsMap.put(group.getName(), aGroupMap);

            aGroupMap.put("default", group.equals(ph.defaultGroup));

            Map<String, Object> infoMap = new HashMap<String, Object>();
            aGroupMap.put("info", infoMap);

            for (String infoKey : group.variables.getVarKeyList()) {
                infoMap.put(infoKey, group.variables.getVarObject(infoKey));
            }

            //infoMap.put("prefix", group.prefix);

            //infoMap.put("suffix", group.suffix);

            //infoMap.put("build", group.canbuild);

            aGroupMap.put("inheritance", group.getInherits());

            aGroupMap.put("permissions", group.permissions);
        }

        Map<String, Object> usersMap = new HashMap<String, Object>();
        root.put("users", usersMap);
        for (String userKey : ph.users.keySet()) {
            User user = ph.users.get(userKey);
            if ((user.getGroup() == null || user.getGroup().equals(ph.defaultGroup)) && user.permissions.isEmpty()) {
                continue;
            }

            Map<String, Object> aUserMap = new HashMap<String, Object>();
            usersMap.put(user.getName(), aUserMap);

            if (user.getGroup() == null) {
                aUserMap.put("group", ph.defaultGroup.getName());
            } else {
                aUserMap.put("group", user.getGroup().getName());
            }

            aUserMap.put("permissions", user.permissions);
        }
        final Yaml yaml = new Yaml(new SafeConstructor());
        FileWriter tx = null;
        try {
            tx = new FileWriter(file, false);
            tx.write(yaml.dump(root));
            tx.flush();
        } catch (Exception e) {
        } finally {
            try {
                tx.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Don't use this. Unless you want to make this plugin to interact with original Nijikokun Permissions
     * This method is supposed to make the original one reload the file, and propagate the changes made here.
     *
     * Prefer to use the AnjoCaido's fake version of Nijikokun's Permission plugin.
     * The AnjoCaido's Permission can propagate the changes made on this plugin instantly,
     * without need to save the file.
     *
     * @param server the server that holds the plugin
     * @deprecated use only with the old Nijikokun's Original Permissions Plugin
     */
    @Deprecated
    public static void reloadOldPlugin(Server server) {
        // Only reload permissions
        PluginManager pm = server.getPluginManager();
        Plugin[] plugins = pm.getPlugins();
        for (int i = 0; i < plugins.length; i++) {
            plugins[i].getConfiguration().load();
            try {
                plugins[i].getClass().getMethod("setupPermissions").invoke(plugins[i]);
            } catch (Exception ex) {
                continue;
            }
        }
    }
}
