/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager.permissions;

import org.anjocaido.groupmanager.utils.StringPermissionComparator;
import com.nijiko.permissions.Control;
import com.nijiko.permissions.PermissionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.dataholder.DataHolder;
import org.anjocaido.groupmanager.data.User;
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
        return g.getVariables().getVarString("prefix");
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
        return g.getVariables().getVarString("suffix");
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
        return g.getVariables().getVarBoolean("build");
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
        return result.getVariables().getVarString(variable);
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
        return result.getVariables().getVarInteger(variable);
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
        return result.getVariables().getVarBoolean(variable);
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
        return result.getVariables().getVarDouble(variable);
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
            return "";
        }
        return auser.getVariables().getVarString(variable);
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
        return auser.getVariables().getVarInteger(variable);
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
        return auser.getVariables().getVarBoolean(variable);
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
        return auser.getVariables().getVarDouble(variable);
    }

    /**
     *
     * @param string
     * @param variable
     * @return
     */
    @Override
    public String getPermissionString(String user, String variable) {
        User auser = ph.getUser(user);
        if (auser == null) {
            return "";
        }
        if(auser.getVariables().hasVar(variable)){
            return auser.getVariables().getVarString(variable);
        }
        Group start = auser.getGroup();
        if (start == null) {
            return "";
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return "";
        }
        return result.getVariables().getVarString(variable);
        //return getUserPermissionString(user, variable);
    }

    /**
     *
     * @param user
     * @param variable
     * @return
     */
    @Override
    public int getPermissionInteger(String user, String variable) {
        User auser = ph.getUser(user);
        if (auser == null) {
            return -1;
        }
        if(auser.getVariables().hasVar(variable)){
            return auser.getVariables().getVarInteger(variable);
        }
        Group start = auser.getGroup();
        if (start == null) {
            return -1;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return -1;
        }
        return result.getVariables().getVarInteger(variable);
        //return getUserPermissionInteger(string, string1);
    }

    /**
     *
     * @param user
     * @param string1
     * @return
     */
    @Override
    public boolean getPermissionBoolean(String user, String variable) {
        User auser = ph.getUser(user);
        if (auser == null) {
            return false;
        }
        if(auser.getVariables().hasVar(variable)){
            return auser.getVariables().getVarBoolean(variable);
        }
        Group start = auser.getGroup();
        if (start == null) {
            return false;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return false;
        }
        return result.getVariables().getVarBoolean(variable);
        //return getUserPermissionBoolean(user, string1);
    }

    /**
     *
     * @param user
     * @param variable
     * @return
     */
    @Override
    public double getPermissionDouble(String user, String variable) {
        User auser = ph.getUser(user);
        if (auser == null) {
            return -1.0D;
        }
        if(auser.getVariables().hasVar(variable)){
            return auser.getVariables().getVarDouble(variable);
        }
        Group start = auser.getGroup();
        if (start == null) {
            return -1.0D;
        }
        Group result = nextGroupWithVariable(start, variable, null);
        if (result == null) {
            return -1.0D;
        }
        return result.getVariables().getVarDouble(variable);
        //return getUserPermissionDouble(string, string1);
    }

    /**
     * Does not include User's group permission
     * @param user
     * @param permission
     * @return
     */
    public String checkUserOnlyPermission(User user, String permission) {
        Collections.sort(user.permissions, new StringPermissionComparator());
        for (String access : user.permissions) {
            if (comparePermissionString(access, permission)) {
                if(access.startsWith("-")){
                    return null;
                }
                return access;
            }
        }
        return null;
    }

    /**
     * Returns the node responsible for that permission.
     * Does not include User's group permission.
     * @param user
     * @param permission
     * @return the node if permission is found. if not found, return null
     */
    public String checkGroupOnlyPermission(Group group, String permission) {
        Collections.sort(group.permissions, new StringPermissionComparator());
        for (String access : group.permissions) {
            if (comparePermissionString(access, permission)) {
                if(access.startsWith("-")){
                    return null;
                }
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
        if (start.getVariables().hasVar(variable)) {
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
        Collections.sort(start.permissions, new StringPermissionComparator());
        for (String availablePerm : start.permissions) {
            if (comparePermissionString(availablePerm, permission)) {
                //System.out.println("WIN!");
                if(availablePerm.startsWith("-")){
                    return null;
                }
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
     *
     * Please sort permissions before sending them here. So negative tokens
     * get priority.
     *
     * You must test if it start with negative after this, so you return
     * the !result of this method.
     * 
     * @param userAcessLevel
     * @param fullPermissionName
     * @return true if found a matching token. false if not.
     */
    public boolean comparePermissionString(String userAcessLevel, String fullPermissionName) {
        if (userAcessLevel == null || fullPermissionName == null) {
            return false;
        }

        if(userAcessLevel.startsWith("+")){
            userAcessLevel = userAcessLevel.substring(1);
        } else if(userAcessLevel.startsWith("-")){
            userAcessLevel = userAcessLevel.substring(1);
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
