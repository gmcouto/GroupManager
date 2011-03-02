/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager.data;

import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author gabrielcouto
 */
public class User extends DataUnit implements Cloneable {

    /**
     *
     */
    private String group = null;
    /**
     *This one holds the fields in INFO node.
     * like prefix = 'c'
     * or build = false
     */
    private UserVariables variables = new UserVariables(this);
    

    /**
     *
     * @param name
     */
    public User(WorldDataHolder source, String name) {
        super(source,name);
        this.group = source.getDefaultGroup().getName();
    }

    /**
     *
     * @return
     */
    @Override
    public User clone() {
        User clone = new User(getDataSource(), this.getName());
        clone.setGroup(this.getGroup());
        for(String perm: this.getPermissionList()){
            clone.addPermission(perm);
        }
        //clone.variables = this.variables.clone();
        clone.flagAsChanged();
        return clone;
    }

    /**
     * Use this to deliver a user from one WorldDataHolder to another
     * @param dataSource
     * @return null if given dataSource already contains the same user
     */
    public User clone(WorldDataHolder dataSource) {
        if (dataSource.isUserDeclared(this.getName())) {
            return null;
        }
        User clone = dataSource.createUser(this.getName());
        if (dataSource.getGroup(group) == null) {
            clone.setGroup(dataSource.getDefaultGroup());
        } else {
            clone.setGroup(this.getGroupName());
        }
        for(String perm: this.getPermissionList()){
            clone.addPermission(perm);
        }
        //clone.variables = this.variables.clone();
        clone.flagAsChanged();
        return clone;
    }

    public Group getGroup() {
        Group result = getDataSource().getGroup(group);
        if (result == null) {
            this.setGroup(getDataSource().getDefaultGroup());
            result = getDataSource().getDefaultGroup();
        }
        return result;
    }

    /**
     * @return the group
     */
    public String getGroupName() {
        Group result = getDataSource().getGroup(group);
        if (result == null) {
            group = getDataSource().getDefaultGroup().getName();
        }
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
        flagAsChanged();
    }

    /**
     * @param group the group to set
     */
    public void setGroup(Group group) {
        if (!this.getDataSource().groupExists(group.getName())) {
            getDataSource().addGroup(group);
        }
        this.group = group.getName();
        flagAsChanged();
    }

    /**
     * @return the variables
     */
    public UserVariables getVariables() {
        return variables;
    }

    /**
     *
     * @param varList
     */
    public void setVariables(Map<String, Object> varList) {
        UserVariables temp = new UserVariables(this, varList);
        variables.clearVars();
        for(String key: temp.getVarKeyList()){
            variables.addVar(key, temp.getVarObject(key));
        }
        flagAsChanged();
    }
}
