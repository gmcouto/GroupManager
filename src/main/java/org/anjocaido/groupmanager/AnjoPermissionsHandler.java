/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager;

import com.nijiko.permissions.PermissionHandler;
import java.util.ArrayList;
import java.util.List;
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
public class AnjoPermissionsHandler extends PermissionHandler {

    DataHolder ph = null;

    /**
     *
     * @param holder
     */
    public AnjoPermissionsHandler(DataHolder holder) {
        ph = holder;
    }

    /**
     *
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
        Group hisGroup;
        User user = ph.getUser(player.getName());
        hisGroup = user.getGroup();
        //System.out.println("Verifying permission for "+player.getName()+"(group "+ hisGroup.getName() +") with "+permission);
        if (checkPermissionWithInheritance(hisGroup, permission, null)) {
            return true;
        }
        for (String availablePerm : user.permissions) {
            if(comparePermissionString(availablePerm, permission)){
                return true;
            }
        }
        return false;
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

        return checkGroupWithInheritance(ph.getUser(name).getGroup(),group,null);
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
        if(start == null)
            return null;
        Group result = checkVariableWithInheritance(start, variable, null);
        if(result==null)
            return null;
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
        if(start == null)
            return -1;
        Group result = checkVariableWithInheritance(start, variable, null);
        if(result==null)
            return -1;
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
        if(start == null)
            return false;
        Group result = checkVariableWithInheritance(start, variable, null);
        if(result==null)
            return false;
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
        if(start == null)
            return -1;
        Group result = checkVariableWithInheritance(start, variable, null);
        if(result==null)
            return -1;
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
        if(auser == null)
            return null;
        Group start = auser.getGroup();
        if(start == null)
            return null;
        Group result = checkVariableWithInheritance(start, variable, null);
        if(result==null)
            return null;
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
        if(auser == null)
            return -1;
        Group start = auser.getGroup();
        if(start == null)
            return -1;
        Group result = checkVariableWithInheritance(start, variable, null);
        if(result==null)
            return -1;
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
        if(auser == null)
            return false;
        Group start = auser.getGroup();
        if(start == null)
            return false;
        Group result = checkVariableWithInheritance(start, variable, null);
        if(result==null)
            return false;
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
        if(auser == null)
            return -1;
        Group start = auser.getGroup();
        if(start == null)
            return -1;
        Group result = checkVariableWithInheritance(start, variable, null);
        if(result==null)
            return -1;
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
     *
     * @param start
     * @param variable
     * @param alreadyChecked
     * @return
     */
    public Group checkVariableWithInheritance(Group start, String variable, List<Group> alreadyChecked) {
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
            Group result = checkVariableWithInheritance(groupInh, variable, alreadyChecked);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private boolean checkGroupWithInheritance(Group start, String askedGroup, List<Group> alreadyChecked) {
        if (alreadyChecked == null) {
            alreadyChecked = new ArrayList<Group>();
        }
        if (alreadyChecked.contains(start)) {
            return false;
        }
        if(start.getName().equalsIgnoreCase(askedGroup)){
            return true;
        }
        alreadyChecked.add(start);
        for (String inherited : start.getInherits()) {
            Group groupInh = ph.getGroup(inherited);
            if (checkGroupWithInheritance(groupInh, askedGroup, alreadyChecked)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPermissionWithInheritance(Group start, String permission, List<Group> alreadyChecked) {
        if (alreadyChecked == null) {
            alreadyChecked = new ArrayList<Group>();
        }
        if (alreadyChecked.contains(start)) {
            return false;
        }
        //System.out.println("Testing permission inh group "+start.getName());
        for (String availablePerm : start.permissions) {
            if (comparePermissionString(availablePerm, permission)) {
                //System.out.println("WIN!");
                return true;
            }
        }
        //System.out.println("FAIL!");
        alreadyChecked.add(start);
        for (String inherited : start.getInherits()) {
            Group groupInh = ph.getGroup(inherited);
            if (checkPermissionWithInheritance(groupInh, permission, alreadyChecked)) {
                return true;
            }
        }
        //System.out.println("No more to check!");
        return false;
    }

    private boolean comparePermissionString(String userAcessLevel, String fullPermissionName) {
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
                //System.out.println("FAIL");
                continue;
            } else {
                //System.out.println("FAIL");
                return false;
            }

        }
        //System.out.println("FAIL");
        return false;
    }
}
