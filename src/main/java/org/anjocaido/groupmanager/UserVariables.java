/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.anjocaido.groupmanager;

import java.util.Map;

/**
 *
 * @author gabrielcouto
 */
public class UserVariables extends Variables{
    private User owner;
    public UserVariables(User owner){
        this.owner = owner;
    }
    public UserVariables(User owner, Map<String, Object> varList) {
        this.variables = varList;
        this.owner = owner;
    }
    /**
     *  A clone of all vars here.
     * @return
     */
    protected UserVariables clone(User newOwner) {
        UserVariables clone = new UserVariables(newOwner);
        for (String key : variables.keySet()) {
            clone.variables.put(key, variables.get(key));
        }
        return clone;
    }
    /**
     * @return the owner
     */
    public User getOwner() {
        return owner;
    }

}
