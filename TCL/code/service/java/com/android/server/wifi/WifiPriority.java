/******************************************************************************/
/*                                                               Date:10/2012 */
/*                                PRESENTATION                                */
/*                                                                            */
/*       Copyright 2012 TCL Communication Technology Holdings Limited.        */
/*                                                                            */
/* This material is company confidential, cannot be reproduced in any form    */
/* without the written permission of TCL Communication Technology Holdings    */
/* Limited.                                                                   */
/*                                                                            */
/* -------------------------------------------------------------------------- */
/*  Author :  Jinbo.Zhang                                                     */
/*  Email  :  Jinbo.Zhang@tcl.com                                             */
/*  Role   :                                                                  */
/*  Reference documents :                                                     */
/* -------------------------------------------------------------------------- */
/*  Comments : Feature of Wifi priority, SSIDs can set different grade of pri */
/*             ority, when wifi enable to work on WIFI_FULL_MODE, device will */
/*              connect to the AP which is in range and has the highest prior */
/*             ity grade. This class depends on IPriority interface and contr */
/*             olled by pls file (wifi_priority_strategy).                    */
/*  File     :                                                                */
/*  Labels   : Wifi Priority                                                  */
/* -------------------------------------------------------------------------- */
/* ========================================================================== */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* ----------|----------------------|----------------------|----------------- */
/* 10/22/2014|Zhang.Jinbo           |FR342229              |Add ATTSpyderImpl */
/* ----------|----------------------|----------------------|----------------- */
/******************************************************************************/

package com.android.server.wifi;

import java.util.List;
import android.os.Binder;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.provider.Settings;
import android.net.wifi.priority.IPriority;
import android.net.wifi.priority.PriorityImplOrange;
import android.net.wifi.priority.PriorityImplTMobile;
import android.net.wifi.priority.PriorityImplSFR;
import android.net.wifi.priority.PriorityImplATTSpyder;

public class WifiPriority {

    private static final String TAG = "WifiPriority";
    private WifiConfigStore mWifiConfigStore;
    private IPriority priorityImpl = null;
    private int WIFI_PRIORITY_STRATEGY = 0;
    /**
     * related pls value is 0x00
     */
    private static final int WIFI_PRIORITY_NONE = 0;
    /**
     * related pls value is 0x01
     */
    private static final int WIFI_PRIORITY_ORANGE = 1;
    /**
     * related pls value is 0x02
     */
    private static final int WIFI_PRIORITY_TMOBILE = 2;

    private static final int WIFI_PRIORITY_SFR = 3;

//[FEATTURE]-Add-BEGIN by TCTNB.Yu.Feng,11/13/2012,342229,
//Support ATT Priority Requirement

    private static final int WIFI_PRIORITY_ATTSPYDER = 4;

//[FEATTURE]-Add-END by TCTNB.Yu.Feng

    public WifiPriority(Context context,WifiConfigStore wifiConfigStore) {
        mWifiConfigStore = wifiConfigStore;
        WIFI_PRIORITY_STRATEGY = Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.WIFI_PRIORITY_STRATEGY, 0);
        setPriorityImpl();
    }

    /**
     * set the implements class of IPriority interface. Use wifi priority
     * strategy defined in that class.
     */
     private void setPriorityImpl() {
        switch (WIFI_PRIORITY_STRATEGY) {
        case WIFI_PRIORITY_NONE:
            priorityImpl = null;
            break;
        case WIFI_PRIORITY_ORANGE:
            priorityImpl = new PriorityImplOrange();
            break;
        case WIFI_PRIORITY_TMOBILE:
            priorityImpl = new PriorityImplTMobile();
            break;
        case WIFI_PRIORITY_SFR:
            priorityImpl = new PriorityImplSFR();
            break;

        case WIFI_PRIORITY_ATTSPYDER:
            priorityImpl = new PriorityImplATTSpyder();
            break;

        default:
            priorityImpl = null;
            break;
        }
    }

    /**
     * When Connect the SSID, need to update the last SSID
     * @param config
     */
    public void updatePriority(WifiConfiguration mConfig) {
        if (mConfig == null || mConfig.networkId == -1 || priorityImpl == null) {
            return;
        }
        
        WifiConfiguration updatedConfig = new WifiConfiguration();
        updatedConfig.networkId = mConfig.networkId;
        updatedConfig.priority = priorityImpl.getPriorityGrade(mConfig);
        mWifiConfigStore.addOrUpdateNetwork(updatedConfig,Binder.getCallingUid());
        mWifiConfigStore.saveConfig();
    }

    /**
     * update priority of all configuredNetworks
     * happend when disable the wifi.
     */
    public void sortPriority() {
        if (priorityImpl == null) {
            return;
        }
        List<WifiConfiguration> configs = mWifiConfigStore.getConfiguredNetworks();
        if(configs != null) {
            int num = 0;
            if (configs != null) {
                for (WifiConfiguration config : configs) {
                    WifiConfiguration updatedConfig = new WifiConfiguration();
                    updatedConfig.networkId = config.networkId;
                    updatedConfig.priority = priorityImpl.getPriorityGrade(config);
                    mWifiConfigStore.addOrUpdateNetwork(updatedConfig,Binder.getCallingUid());
                    num ++;
                }
                if(num > 0){
                    mWifiConfigStore.saveConfig();
                }
            }
        }
    }
}
