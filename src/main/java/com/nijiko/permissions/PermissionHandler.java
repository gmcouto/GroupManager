package com.nijiko.permissions;

import org.bukkit.entity.Player;
import org.bukkit.entity.Player;

/**
 *
 * @author gabrielcouto
 */
public abstract class PermissionHandler {

    /**
     *
     */
    public PermissionHandler() {
        //compiled code
        //throw new RuntimeException("Compiled Code");
    }

    /**
     *
     */
    public abstract void load();

    /**
     *
     * @param player
     * @param string
     * @return
     */
    public abstract boolean has(Player player, String string);

    /**
     *
     * @param player
     * @param string
     * @return
     */
    public abstract boolean permission(Player player, String string);

    /**
     *
     * @param string
     * @return
     */
    public abstract String getGroup(String string);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract boolean inGroup(String string, String string1);

    /**
     *
     * @param string
     * @return
     */
    public abstract String getGroupPrefix(String string);

    /**
     *
     * @param string
     * @return
     */
    public abstract String getGroupSuffix(String string);

    /**
     *
     * @param string
     * @return
     */
    public abstract boolean canGroupBuild(String string);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract String getGroupPermissionString(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract int getGroupPermissionInteger(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract boolean getGroupPermissionBoolean(String string, String string1);

    /**
     * 
     * @param string
     * @param string1
     * @return
     */
    public abstract double getGroupPermissionDouble(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract String getUserPermissionString(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract int getUserPermissionInteger(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract boolean getUserPermissionBoolean(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract double getUserPermissionDouble(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract String getPermissionString(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract int getPermissionInteger(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract boolean getPermissionBoolean(String string, String string1);

    /**
     *
     * @param string
     * @param string1
     * @return
     */
    public abstract double getPermissionDouble(String string, String string1);
}
