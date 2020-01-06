package com.bdl.airecovery.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "help_divided_by_torque")
public class HelpDividedByTorque {
    /**
     * 力矩值从0~5,界面上对应5~10
     * 大于cancelHelpTorque以后不再提供助力
     */
    @Column(name = "torque")
    int torque;
     /**
     * 力矩值大于该值以后不再提供助力
     */
    @Column(name = "cancelHelpTorque")
    int cancelHelpTorque;

    public int getTorque() {
        return torque;
    }

    public void setTorque(int torque) {
        this.torque = torque;
    }

    public int getCancelHelpTorque() {
        return cancelHelpTorque;
    }

    public void setCancelHelpTorque(int cancelHelpTorque) {
        this.cancelHelpTorque = cancelHelpTorque;
    }

    @Override
    public String toString() {
        return "HelpDividedByTorque{" +
                ", torque=" + torque +
                ", cancelHelpTorque=" + cancelHelpTorque +
                '}';
    }
}
