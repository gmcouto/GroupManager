/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *A class that holds variables of a user/group.
 * In groups, it holds the contents of INFO node.
 * Like:
 * prefix
 * suffix
 * build
 *
 * @author gabrielcouto
 */
public class Variables implements Cloneable{

    private Map<String, Object> variables = new HashMap<String, Object>();

    /**
     *
     */
    public Variables(){
        variables = new HashMap<String, Object>();
    }

    /**
     *
     * @param varList
     */
    public Variables(Map<String, Object> varList){
        variables = varList;
    }

    /**
     * Add var to the the INFO node.
     * examples:
     * addVar("build",true);
     * addVar("prefix","c");
     * @param name key name of the var
     * @param o the object value of the var
     */
    public void addVar(String name, Object o) {
        if (variables.containsKey(name)) {
            variables.remove(name);
        }
        variables.put(name, o);
    }

    /**
     *  Returns the object inside the var
     * @param name
     * @return a Object if exists. null if doesn't exists
     */
    public Object getVarObject(String name) {
        return variables.get(name);
    }

    /**
     * Get the String value for the given var name
     * @param name the var key name
     * @return "" if null. or the toString() value of object
     */
    public String getVarString(String name) {
        Object o = variables.get(name);
        return o == null ? "" : o.toString();
    }

    /**
     *
     * @param name
     * @return false if null. or a Boolean.parseBoolean of the string
     */
    public Boolean getVarBoolean(String name) {
        Object o = variables.get(name);
        return o == null ? false : Boolean.parseBoolean(o.toString());
    }

    /**
     *
     * @param name
     * @return -1 if null. or a parseInt of the string
     */
    public Integer getVarInteger(String name) {
        Object o = variables.get(name);
        return o == null ? -1 : Integer.parseInt(o.toString());
    }

    /**
     *
     * @param name
     * @return -1 if null. or a parseDouble of the string
     */
    public Double getVarDouble(String name) {
        Object o = variables.get(name);
        return o == null ? -1 : Double.parseDouble(o.toString());
    }

    /**
     *  All variable keys this is holding
     * @return
     */
    public Set<String> getVarKeyList() {
        return variables.keySet();
    }

    /**
     *  verify is a var exists
     * @param name the key name of the var
     * @return true if that var exists
     */
    public boolean hasVar(String name) {
        return variables.containsKey(name);
    }

    /**
     * Returns the quantity of vars this is holding
     * @return the number of vars
     */
    public int getVarSize() {
        return variables.size();
    }

    /**
     * Remove a var from the list
     * @param name
     */
    public void removeVar(String name) {
        try {
            variables.remove(name);
        } catch (Exception e) {
        }
    }
    /**
     *  A clone of all vars here.
     * @return
     */
    @Override
    public Variables clone(){
        Variables clone = new Variables();
        for(String key: this.variables.keySet()){
            clone.variables.put(key, this.variables.get(key));
        }
        return clone;
    }
}
