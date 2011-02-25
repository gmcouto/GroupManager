/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.anjocaido.groupmanager;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author gabrielcouto
 */
public class User implements Cloneable {
    private DataHolder source;
    private String name;
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
     */
    public ArrayList<String> permissions = new ArrayList<String>();
    /**
     *
     * @param name
     */
    protected User(DataHolder source, String name){
        this.source = source;
        this.name = name.toLowerCase();
        this.group = group;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    /**
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o){
        if(o instanceof User){
            if(this.getName().equalsIgnoreCase(((User)o).getName()))
                return true;
        }
        return false;
    }
    /**
     *
     * @return
     */
    @Override
    public User clone(){
        User clone = new User(getDataSource(),this.name);
        clone.setGroup(this.getGroup());
        clone.permissions = (ArrayList<String>) this.permissions.clone();
        //clone.variables = this.variables.clone();
        return clone;
    }

    /**
     * Use this to deliver a user from one DataHolder to another
     * @param dataSource
     * @return null if given dataSource already contains the same user
     */
    public User clone(DataHolder dataSource){
        if(dataSource.users.containsKey(name)){
            return null;
        }
        User clone = dataSource.createUser(name);
        if(dataSource.getGroup(group)==null){
            clone.setGroup(dataSource.getDefaultGroup());
        } else {
            clone.setGroup(this.getGroupName());
        }
        clone.permissions = (ArrayList<String>) this.permissions.clone();
        //clone.variables = this.variables.clone();
        return clone;
    }

    public Group getGroup(){
        return getDataSource().getGroup(group);
    }
    /**
     * @return the group
     */
    public String getGroupName() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }
    /**
     * @param group the group to set
     */
    public void setGroup(Group group) {
        if(!source.groups.containsValue(group)){
            getDataSource().addGroup(group);
        }
        this.group = group.getName();
    }

    /**
     * The DataHolder that contains this user
     * @return the source
     */
    public DataHolder getDataSource() {
        return source;
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
        variables = new UserVariables(this,varList);
    }
}
