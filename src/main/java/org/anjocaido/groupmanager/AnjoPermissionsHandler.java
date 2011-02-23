/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager;

import com.nijiko.permissions.Control;
import com.nijiko.permissions.PermissionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.anjocaido.groupmanager.Group;
import org.anjocaido.groupmanager.DataHolder;
import org.anjocaido.groupmanager.User;
import org.bukkit.entity.Player;

/**
 *  Everything here maintains the model created by Nijikokun
 * 
 * But implemented to use GroupManager system. Which provides instant changes,
 * without file access.
 *
 * @author gabrielcouto
 */
public class AnjoPermissionsHandler extends Control {

    DataHolder ph = null;

    /**
     *
     * @param holder
     */
    public AnjoPermissionsHandler(DataHolder holder) {
        super(null);
        ph = holder;
    }

    /**
     * Does nothing. It's not used by GroupManager.
     * But still here to fool Permissions dependent plugins.
     */
    @Override
    public void load() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *
     * @param player
     * @param permission
     * @return
     */
    @Override
    public boolean has(Player player, String permission) {
        return permission(player, permission);
    }

    /**
     *
     * @param player
     * @param permission
     * @return
     */
    @Override
    public boolean permission(Player player, String permission) {
        return checkUserPermission(ph.getUser(player.getName()), permission);
    }

    /**
     *
     * @param group
     * @return
     */
    @Override
    public String getGroup(String group) {
        return ph.getUser(group).getGroup().getName();
    }

    /**
     *
     * @param name
     * @param group
     * @return
     */
    @Override
    public boolean inGroup(String name, String group) {
        //System.out.println("Verifying inGroup user "+name+" with "+group);

        return searchGroupInInheritance(ph.getUser(name).getGroup(), group, null);
    }

    /**
     *
     * @param string
     * @return
     */
    @Override
    public String getGroupPrefix(String string) {
        Group g = ph.getGroup(string);
        if (g == null) {
            return null;
        }
        return g.variables.getVarString("prefix");
    }

    /**
     *
     * @param string
     * @return
     */
    @Override
    public String getGroupSuffix(String string) {
        Group g = ph.getGroup(string);
        if (g == null) {
            return null;
        }
        return g.variables.getVarString("suffix");
    }

    /**
     *
     * @param string
     * @return
     */
    @Override
    public boolean canGroupBuild(String string) {
        Group g = ph.getGroup(string);
        if (g == null) {
            return false;
        }
        return g.variables.getVarBoolean("build");
    }

    /**
     *
     * @param group
     * @param variable
     * @return
     */
    @Override
    public String getGroupPermissionString(String group, String variable) {
        Group start = ph.getGroup(group);
        if (start == null) {
            return null;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return null;
        }
        return result.variables.getVarString(variable);
    }

    /**
     *
     * @param group
     * @param variable
     * @return
     */
    @Override
    public int getGroupPermissionInteger(String group, String variable) {
        Group start = ph.getGroup(group);
        if (start == null) {
            return -1;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return -1;
        }
        return result.variables.getVarInteger(variable);
    }

    /**
     * 
     * @param group
     * @param variable
     * @return
     */
    @Override
    public boolean getGroupPermissionBoolean(String group, String variable) {
        Group start = ph.getGroup(group);
        if (start == null) {
            return false;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return false;
        }
        return result.variables.getVarBoolean(variable);
    }

    /**
     *
     * @param group
     * @param variable
     * @return
     */
    @Override
    public double getGroupPermissionDouble(String group, String variable) {
        Group start = ph.getGroup(group);
        if (start == null) {
            return -1;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return -1;
        }
        return result.variables.getVarDouble(variable);
    }

    /**
     *
     * @param user
     * @param variable
     * @return
     */
    @Override
    public String getUserPermissionString(String user, String variable) {
        User auser = ph.getUser(user);
        if (auser == null) {
            return null;
        }
        Group start = auser.getGroup();
        if (start == null) {
            return null;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return null;
        }
        return result.variables.getVarString(variable);
    }

    /**
     *
     * @param user
     * @param variable
     * @return
     */
    @Override
    public int getUserPermissionInteger(String user, String variable) {
        User auser = ph.getUser(user);
        if (auser == null) {
            return -1;
        }
        Group start = auser.getGroup();
        if (start == null) {
            return -1;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return -1;
        }
        return result.variables.getVarInteger(variable);
    }

    /**
     *
     * @param user
     * @param variable
     * @return
     */
    @Override
    public boolean getUserPermissionBoolean(String user, String variable) {
        User auser = ph.getUser(user);
        if (auser == null) {
            return false;
        }
        Group start = auser.getGroup();
        if (start == null) {
            return false;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return false;
        }
        return result.variables.getVarBoolean(variable);
    }

    /**
     *
     * @param user
     * @param variable
     * @return
     */
    @Override
    public double getUserPermissionDouble(String user, String variable) {
        User auser = ph.getUser(user);
        if (auser == null) {
            return -1;
        }
        Group start = auser.getGroup();
        if (start == null) {
            return -1;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return -1;
        }
        return result.variables.getVarDouble(variable);
    }

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    @Override
    public String getPermissionString(String string, String string1) {
        return getUserPermissionString(string, string1);
    }

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    @Override
    public int getPermissionInteger(String string, String string1) {
        return getUserPermissionInteger(string, string1);
    }

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    @Override
    public boolean getPermissionBoolean(String string, String string1) {
        return getUserPermissionBoolean(string, string1);
    }

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    @Override
    public double getPermissionDouble(String string, String string1) {
        return getUserPermissionDouble(string, string1);
    }

    /**
     * Does not include User's group permission
     * @param user
     * @param permission
     * @return
     */
    public String checkUserOnlyPermission(User user, String permission) {
        for (String access : user.permissions) {
            if (comparePermissionString(access, permission)) {
                return access;
            }
        }
        return null;
    }

    /**
     * Does not include User's group permission
     * @param user
     * @param permission
     * @return
     */
    public String checkGroupOnlyPermission(Group group, String permission) {
        for (String access : group.permissions) {
            if (comparePermissionString(access, permission)) {
                return access;
            }
        }
        return null;
    }

    /**
     * Check permissions, including it's group and inheritance.
     * @param user
     * @param permission
     * @return
     */
    public boolean checkUserPermission(User user, String permission) {
        if (user == null || permission == null) {
            return false;
        }
        if (checkUserOnlyPermission(user, permission) != null) {
            return true;
        }
        if (checkGroupPermissionWithInheritance(user.getGroup(), permission, null)) {
            return true;
        }
        return false;
    }

    /**
     * Verifies if a given group has a variable. Including it's inheritance.
     * @param start
     * @param variable
     * @param alreadyChecked
     * @return returns the closest inherited group with the variable.
     */
    public Group nextGroupWithVariable(Group start, String variable, List<Group> alreadyChecked) {
        if (start == null || variable == null) {
            return null;
        }
        if (alreadyChecked == null) {
            alreadyChecked = new ArrayList<Group>();
        }
        if (alreadyChecked.contains(start)) {
            return null;
        }
        if (start.variables.hasVar(variable)) {
            return start;
        }

        alreadyChecked.add(start);

        for (String inherited : start.getInherits()) {
            Group groupInh = ph.getGroup(inherited);
            Group result = nextGroupWithVariable(groupInh, variable, alreadyChecked);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Check if given group inherits another group.
     * @param start The group to start the search.
     * @param askedGroup Name of the group you're looking for
     * @param alreadyChecked groups to ignore(pass null on it, please)
     * @return true if it inherits the group.
     */
    public boolean searchGroupInInheritance(Group start, String askedGroup, List<Group> alreadyChecked) {
        if (start == null || askedGroup == null) {
            return false;
        }
        if (alreadyChecked == null) {
            alreadyChecked = new ArrayList<Group>();
        }
        if (alreadyChecked.contains(start)) {
            return false;
        }
        if (start.getName().equalsIgnoreCase(askedGroup)) {
            return true;
        }
        alreadyChecked.add(start);
        for (String inherited : start.getInherits()) {
            Group groupInh = ph.getGroup(inherited);
            if (searchGroupInInheritance(groupInh, askedGroup, alreadyChecked)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the group has given permission. Including it's inheritance
     * @param start
     * @param permission
     * @param alreadyChecked
     * @return
     */
    public boolean checkGroupPermissionWithInheritance(Group start, String permission, List<Group> alreadyChecked) {
        if (nextGroupWithPermission(start, permission, alreadyChecked) != null) {
            return true;
        }
        return false;
    }

    /**
     * Return the group that passes on the permission test. Checking whole
     * inheritance chain.
     * @param start
     * @param permission
     * @param alreadyChecked
     * @return the group that passed on test. null if no group passed.
     */
    public Group nextGroupWithPermission(Group start, String permission, List<Group> alreadyChecked) {
        if (start == null || permission == null) {
            return null;
        }
        if (alreadyChecked == null) {
            alreadyChecked = new ArrayList<Group>();
        }
        if (alreadyChecked.contains(start)) {
            return null;
        }
        //System.out.println("Testing permission inh group "+start.getName());
        for (String availablePerm : start.permissions) {
            if (comparePermissionString(availablePerm, permission)) {
                //System.out.println("WIN!");
                return start;
            }
        }
        //System.out.println("FAIL!");
        alreadyChecked.add(start);
        for (String inherited : start.getInherits()) {
            Group groupInh = ph.getGroup(inherited);
            Group result = nextGroupWithPermission(groupInh, permission, alreadyChecked);
            if (result != null) {
                return result;
            }
        }
        //System.out.println("No more to check!");
        return null;
    }

    /**
     * Return the group that passes on the permission test. Checking whole
     * inheritance chain.
     * @param start
     * @param permission
     * @param alreadyChecked
     * @return the group that passed on test. null if no group passed.
     */
    public ArrayList<Group> listAllGroupsInherited(Group start, ArrayList<Group> alreadyChecked) {
        if (start == null) {
            return null;
        }
        if (alreadyChecked == null) {
            alreadyChecked = new ArrayList<Group>();
        }
        if (alreadyChecked.contains(start)) {
            return alreadyChecked;
        }
        alreadyChecked.add(start);
        for (String grp : start.getInherits()) {
            Group g = ph.getGroup(grp);
            if (g != null) {
                listAllGroupsInherited(g, alreadyChecked);
            }
        }
        //System.out.println("No more to check!");
        return alreadyChecked;
    }

    /**
     * Compare a user permission like 'myplugin.*' against a full plugin
     * permission name, like 'myplugin.dosomething'.
     * As the example above, will return true.
     * @param userAcessLevel
     * @param fullPermissionName
     * @return
     */
    public boolean comparePermissionString(String userAcessLevel, String fullPermissionName) {
        if (userAcessLevel == null || fullPermissionName == null) {
            return false;
        }
        //System.out.println("Comparing acess "+userAcessLevel+" with "+fullPermissionName);
        StringTokenizer levelATokenizer = new StringTokenizer(userAcessLevel, ".");
        StringTokenizer levelBTokenizer = new StringTokenizer(fullPermissionName, ".");
        while (levelATokenizer.hasMoreTokens() && levelBTokenizer.hasMoreTokens()) {
            String levelA = levelATokenizer.nextToken();
            String levelB = levelBTokenizer.nextToken();
            //System.out.println("Comparing tokens "+levelA+" with "+ levelB);
            if (levelA.contains("*")) {
                //System.out.println("WIN");
                return true;
            }
            if (levelA.equalsIgnoreCase(levelB)) {
                if (!levelATokenizer.hasMoreTokens() && !levelBTokenizer.hasMoreTokens()) {
                    //System.out.println("WIN");
                    return true;
                }
                //System.out.println("NEXT");
                continue;
            } else {
                //System.out.println("FAIL");
                return false;
            }

        }
        //System.out.println("FAIL");
        return false;
    }

    @Override
    public String[] getGroups(String userName) {
        ArrayList<Group> listAllGroupsInherited = listAllGroupsInherited(ph.getUser(userName).getGroup(), null);
        String[] arr = new String[listAllGroupsInherited.size()];
        return listAllGroupsInherited.toArray(arr);
    }
}
