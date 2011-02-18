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
    public Variables variables = new Variables();
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
        variables.addVar("prefix", "");
        variables.addVar("suffix", "");
        variables.addVar("build", false);
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
     *  Get a list with all groups names in inherits field.
     * Not recursive.
     * @return the names of groups in inherits field
     */
    public ArrayList<String> getInheritanceKeyNameList() {
        return (ArrayList<String>) inherits.clone();
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
        clone.variables = this.variables.clone();
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
        clone.variables = this.variables.clone();
        return clone;
    }

    /**
     * @return the inherits
     */
    public ArrayList<String> getInherits() {
        return (ArrayList<String>) inherits.clone();
    }

    /**
     * @param inherits the inherits to set
     */
    public void addInherits(Group inherit) {
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
}
