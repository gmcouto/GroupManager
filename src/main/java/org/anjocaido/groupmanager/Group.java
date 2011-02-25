/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author gabrielcouto
 */
public class Group implements Cloneable{
    private DataHolder source;

    private String name;
    /**
     * The group it inherits DIRECTLY!
     */
    private ArrayList<String> inherits = new ArrayList<String>();
    /**
     *This one holds the fields in INFO node.
     * like prefix = 'c'
     * or build = false
     */
    private GroupVariables variables = new GroupVariables(this);
    /**
     * These are the complete name of the permissions nodes
     * like essentials.motd
     * or essentials.admin.kill
     *
     */
    public ArrayList<String> permissions = new ArrayList<String>();



    /**
     *
     * @param name
     */
    protected Group(DataHolder source, String name) {
        this.source = source;
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     *  Every group is matched only by their names and DataSources.
     * @param o
     * @return true if they are equal. false if not.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Group) {
            Group go = (Group) o;
            if (this.getName().equalsIgnoreCase(go.getName()) && this.getDataSource()==go.getDataSource()) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
    /**
     *  Clone this group
     * @return a clone of this group
     */
    @Override
    public Group clone(){
        Group clone = new Group(getDataSource(),name);
        clone.inherits = ((ArrayList<String>) this.getInherits().clone());
        clone.permissions = (ArrayList<String>) this.permissions.clone();
        clone.variables = ((GroupVariables)variables).clone(clone);
        return clone;
    }
    /**
     * Use this to deliver a group from a different dataSource to another
     * @param dataSource
     * @return
     */
    public Group clone(DataHolder dataSource){
        if(dataSource.groups.containsKey(name)){
            return null;
        }
        Group clone = getDataSource().createGroup(name);
        clone.inherits = ((ArrayList<String>) this.getInherits().clone());
        clone.permissions = (ArrayList<String>) this.permissions.clone();
        clone.variables = variables.clone(clone);
        return clone;
    }

    /**
     * a COPY of inherits list
     * You can't manage the list by here
     * Lol... version 0.6 had a problem because this.
     * @return the inherits
     */
    public ArrayList<String> getInherits() {
        return (ArrayList<String>) inherits.clone();
    }

    /**
     * @param inherits the inherits to set
     */
    public void addInherits(Group inherit) {
        //System.out.println("Adding inheritance:" + inherit.getName()+ "for "+ this.getName());
        if(!source.groups.containsKey(inherit)){
            getDataSource().addGroup(inherit);
        }
        if(!inherits.contains(inherit.getName())){
            inherits.add(inherit.getName());
        }
    }
    public boolean removeInherits(String inherit){
        if(this.inherits.contains(inherit)){
            this.inherits.remove(inherit);
            return true;
        }
        return false;
    }

    /**
     * @return the source
     */
    public DataHolder getDataSource() {
        return source;
    }

    /**
     * @return the variables
     */
    public GroupVariables getVariables() {
        return variables;
    }
    /**
     * 
     * @param varList
     */
    public void setVariables(Map<String, Object> varList) {
        variables = new GroupVariables(this,varList);
    }
}
