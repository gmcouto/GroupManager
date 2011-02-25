/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anjocaido.groupmanager;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author gabrielcouto
 */
public class GroupVariables extends Variables implements Cloneable {

    private Group owner;

    public GroupVariables(Group owner) {
        this.owner = owner;
        addVar("prefix", "");
        addVar("suffix", "");
        addVar("build", false);
    }

    public GroupVariables(Group owner, Map<String, Object> varList) {
        variables = varList;
        if (variables.get("prefix") == null) {
            variables.put("prefix", "");
        }
        //thisGrp.prefix = infoNode.get("prefix").toString();

        if (variables.get("suffix") == null) {
            variables.put("suffix", "");
        }
        //thisGrp.suffix = infoNode.get("suffix").toString();

        if (variables.get("build") == null) {
            variables.put("build", false);
        }
        this.owner = owner;
    }

    /**
     *  A clone of all vars here.
     * @return
     */
    protected GroupVariables clone(Group newOwner) {
        GroupVariables clone = new GroupVariables(newOwner);
        for (String key : variables.keySet()) {
            clone.variables.put(key, variables.get(key));
        }
        return clone;
    }

    /**
     * Remove a var from the list
     * @param name
     */
    @Override
    public void removeVar(String name) {
        try {
            this.variables.remove(name);
        } catch (Exception e) {
        }
        if (name.equals("prefix")) {
            addVar("prefix", "");
        } else if (name.equals("suffix")) {
            addVar("suffix", "");
        } else if (name.equals("build")) {
            addVar("build", false);
        }
    }

    /**
     * @return the owner
     */
    public Group getOwner() {
        return owner;
    }
}
