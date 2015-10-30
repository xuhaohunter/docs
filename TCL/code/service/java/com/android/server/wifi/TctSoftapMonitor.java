/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/******************************************************************************/
/*                                                               Date:11/2012 */
/*                                PRESENTATION                                */
/*                                                                            */
/*       Copyright 2012 TCL Communication Technology Holdings Limited.        */
/*                                                                            */
/* This material is company confidential, cannot be reproduced in any form    */
/* without the written permission of TCL Communication Technology Holdings    */
/* Limited.                                                                   */
/*                                                                            */
/* -------------------------------------------------------------------------- */
/*  Author :  ZhangJinbo                                                      */
/*  Email  :  jinbo.zhang@tcl-mobile.com                                      */
/*  Role   :                                                                  */
/*  Reference documents :                                                     */
/* -------------------------------------------------------------------------- */
/*  Comments : WPS Push Button                                                */
/*  File     :frameworks/base/wifi/java/android/net/wifi/TctSoftapMonitor.java*/
/*  Labels   :                                                                */
/* -------------------------------------------------------------------------- */
/* ========================================================================== */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* ----------|----------------------|----------------------|----------------- */
/* 10/30/2014|       ZhangJinbo     |        720095        |Add WiFi WPS Push */
/*           |                      |                      | Button Feature   */
/* ----------|----------------------|----------------------|----------------- */
/******************************************************************************/

package com.android.server.wifi;

import android.net.NetworkInfo;
import com.android.server.wifi.StateChangeResult;
import android.os.Message;


import com.android.internal.util.Protocol;
import com.android.internal.util.StateMachine;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.android.server.wifi.WifiNative;

/**
 * Listens for events from the wpa_supplicant server, and passes them on
 * to the {@link StateMachine} for handling. Runs in its own thread.
 *
 * @hide
 */
public class TctSoftapMonitor {

    private static final String TAG = "TctSoftapMonitor";

    /** Events we receive from the supplicant daemon */

    private static final int CONNECTED    = 1;
    private static final int DISCONNECTED = 2;
    private static final int STATE_CHANGE = 3;
    private static final int SCAN_RESULTS = 4;
    private static final int LINK_SPEED   = 5;
    private static final int TERMINATING  = 6;
    private static final int DRIVER_STATE = 7;
    private static final int EAP_FAILURE  = 8;
    private static final int UNKNOWN      = 9;

    /** All events coming from the supplicant start with this prefix */
    private static final String EVENT_PREFIX_STR = "CTRL-EVENT-";
    private static final int EVENT_PREFIX_LEN_STR = EVENT_PREFIX_STR.length();

    /** All WPA events coming from the supplicant start with this prefix */
    private static final String WPA_EVENT_PREFIX_STR = "WPA:";
    private static final String PASSWORD_MAY_BE_INCORRECT_STR =
       "pre-shared key may be incorrect";

    /* WPS events */
    private static final String WPS_OVERLAP_STR = "WPS-OVERLAP-DETECTED";
    /* WPS registration failed after M2/M2D */
    private static final String WPS_FAIL_STR = "WPS-FAIL ";
    /* WPS registration completed successfully */
    private static final String WPS_SUCCESS_STR = "WPS-SUCCESS ";
    /* WPS enrollment attempt timed out and was terminated */
    private static final String WPS_TIMEOUT_STR = "WPS-TIMEOUT ";

    /**
     * Names of events from wpa_supplicant (minus the prefix). In the
     * format descriptions, * &quot;<code>x</code>&quot;
     * designates a dynamic value that needs to be parsed out from the event
     * string
     */
    /**
     * <pre>
     * CTRL-EVENT-CONNECTED - Connection to xx:xx:xx:xx:xx:xx completed
     * </pre>
     * <code>xx:xx:xx:xx:xx:xx</code> is the BSSID of the associated access point
     */
    private static final String CONNECTED_STR =    "CONNECTED";
    /**
     * <pre>
     * CTRL-EVENT-DISCONNECTED - Disconnect event - remove keys
     * </pre>
     */
    private static final String DISCONNECTED_STR = "DISCONNECTED";
    /**
     * <pre>
     * CTRL-EVENT-STATE-CHANGE x
     * </pre>
     * <code>x</code> is the numerical value of the new state.
     */
    private static final String STATE_CHANGE_STR =  "STATE-CHANGE";
    /**
     * <pre>
     * CTRL-EVENT-SCAN-RESULTS ready
     * </pre>
     */
    private static final String SCAN_RESULTS_STR =  "SCAN-RESULTS";

    /**
     * <pre>
     * CTRL-EVENT-LINK-SPEED x Mb/s
     * </pre>
     * {@code x} is the link speed in Mb/sec.
     */
    private static final String LINK_SPEED_STR = "LINK-SPEED";
    /**
     * <pre>
     * CTRL-EVENT-TERMINATING - signal x
     * </pre>
     * <code>x</code> is the signal that caused termination.
     */
    private static final String TERMINATING_STR =  "TERMINATING";
    /**
     * <pre>
     * CTRL-EVENT-DRIVER-STATE state
     * </pre>
     * <code>state</code> can be HANGED
     */
    private static final String DRIVER_STATE_STR = "DRIVER-STATE";
    /**
     * <pre>
     * CTRL-EVENT-EAP-FAILURE EAP authentication failed
     * </pre>
     */
    private static final String EAP_FAILURE_STR = "EAP-FAILURE";

    /**
     * This indicates an authentication failure on EAP FAILURE event
     */
    private static final String EAP_AUTH_FAILURE_STR = "EAP authentication failed";

    /**
     * Regex pattern for extracting an Ethernet-style MAC address from a string.
     * Matches a strings like the following:<pre>
     * CTRL-EVENT-CONNECTED - Connection to 00:1e:58:ec:d5:6d completed (reauth) [id=1 id_str=]</pre>
     */
    private static Pattern mConnectedEventPattern =
        Pattern.compile("((?:[0-9a-f]{2}:){5}[0-9a-f]{2}) .* \\[id=([0-9]+) ");

    /** P2P events */
    private static final String P2P_EVENT_PREFIX_STR = "P2P";

    /* P2P-DEVICE-FOUND fa:7b:7a:42:02:13 p2p_dev_addr=fa:7b:7a:42:02:13 pri_dev_type=1-0050F204-1
       name='p2p-TEST1' config_methods=0x188 dev_capab=0x27 group_capab=0x0 */
    private static final String P2P_DEVICE_FOUND_STR = "P2P-DEVICE-FOUND";

    /* P2P-DEVICE-LOST p2p_dev_addr=42:fc:89:e1:e2:27 */
    private static final String P2P_DEVICE_LOST_STR = "P2P-DEVICE-LOST";

    /* P2P-GO-NEG-REQUEST 42:fc:89:a8:96:09 dev_passwd_id=4 */
    private static final String P2P_GO_NEG_REQUEST_STR = "P2P-GO-NEG-REQUEST";

    private static final String P2P_GO_NEG_SUCCESS_STR = "P2P-GO-NEG-SUCCESS";

    private static final String P2P_GO_NEG_FAILURE_STR = "P2P-GO-NEG-FAILURE";

    private static final String P2P_GROUP_FORMATION_SUCCESS_STR =
            "P2P-GROUP-FORMATION-SUCCESS";

    private static final String P2P_GROUP_FORMATION_FAILURE_STR =
            "P2P-GROUP-FORMATION-FAILURE";

    /* P2P-GROUP-STARTED p2p-wlan0-0 [client|GO] ssid="DIRECT-W8" freq=2437
       [psk=2182b2e50e53f260d04f3c7b25ef33c965a3291b9b36b455a82d77fd82ca15bc|passphrase="fKG4jMe3"]
       go_dev_addr=fa:7b:7a:42:02:13 */
    private static final String P2P_GROUP_STARTED_STR = "P2P-GROUP-STARTED";

    /* P2P-GROUP-REMOVED p2p-wlan0-0 [client|GO] reason=REQUESTED */
    private static final String P2P_GROUP_REMOVED_STR = "P2P-GROUP-REMOVED";

    /* P2P-INVITATION-RECEIVED sa=fa:7b:7a:42:02:13 go_dev_addr=f8:7b:7a:42:02:13
        bssid=fa:7b:7a:42:82:13 unknown-network */
    private static final String P2P_INVITATION_RECEIVED_STR = "P2P-INVITATION-RECEIVED";

    /* P2P-INVITATION-RESULT status=1 */
    private static final String P2P_INVITATION_RESULT_STR = "P2P-INVITATION-RESULT";

    /* P2P-PROV-DISC-PBC-REQ 42:fc:89:e1:e2:27 p2p_dev_addr=42:fc:89:e1:e2:27
       pri_dev_type=1-0050F204-1 name='p2p-TEST2' config_methods=0x188 dev_capab=0x27
       group_capab=0x0 */
    private static final String P2P_PROV_DISC_PBC_REQ_STR = "P2P-PROV-DISC-PBC-REQ";
    /* P2P-PROV-DISC-ENTER-PIN 42:fc:89:e1:e2:27 p2p_dev_addr=42:fc:89:e1:e2:27
       pri_dev_type=1-0050F204-1 name='p2p-TEST2' config_methods=0x188 dev_capab=0x27
       group_capab=0x0 */
    private static final String P2P_PROV_DISC_ENTER_PIN_STR = "P2P-PROV-DISC-ENTER-PIN";
    /* P2P-PROV-DISC-SHOW-PIN 42:fc:89:e1:e2:27 44490607 p2p_dev_addr=42:fc:89:e1:e2:27
       pri_dev_type=1-0050F204-1 name='p2p-TEST2' config_methods=0x188 dev_capab=0x27
       group_capab=0x0 */
    private static final String P2P_PROV_DISC_SHOW_PIN_STR = "P2P-PROV-DISC-SHOW-PIN";

    private static final String P2P_PROV_DISC_FAILURE_STR ="P2P-PROV-DISC-FAILURE";

    private static final String HOST_AP_EVENT_PREFIX_STR = "AP";
    /* AP-STA-CONNECTED 42:fc:89:a8:96:09 */
    private static final String AP_STA_CONNECTED_STR = "AP-STA-CONNECTED";
    /* AP-STA-DISCONNECTED 42:fc:89:a8:96:09 */
    private static final String AP_STA_DISCONNECTED_STR = "AP-STA-DISCONNECTED";

    private final StateMachine mStateMachine;

    /* Supplicant events reported to a state machine */
    private static final int BASE = Protocol.BASE_WIFI_MONITOR;

    /* Connection to supplicant established */
    public static final int SUP_CONNECTION_EVENT                 = BASE + 1;
    /* Connection to supplicant lost */
    public static final int SUP_DISCONNECTION_EVENT              = BASE + 2;
    /* Network connection completed */
    public static final int NETWORK_CONNECTION_EVENT             = BASE + 3;
    /* Network disconnection completed */
    public static final int NETWORK_DISCONNECTION_EVENT          = BASE + 4;
    /* Scan results are available */
    public static final int SCAN_RESULTS_EVENT                   = BASE + 5;
    /* Supplicate state changed */
    public static final int SUPPLICANT_STATE_CHANGE_EVENT        = BASE + 6;
    /* Password failure and EAP authentication failure */
    public static final int AUTHENTICATION_FAILURE_EVENT         = BASE + 7;
    /* WPS success detected */
    public static final int WPS_SUCCESS_EVENT                    = BASE + 8;
    /* WPS failure detected */
    public static final int WPS_FAIL_EVENT                       = BASE + 9;
    /* WPS overlap detected */
    public static final int WPS_OVERLAP_EVENT                    = BASE + 10;
    /* WPS timeout detected */
    public static final int WPS_TIMEOUT_EVENT                    = BASE + 11;
    /* Driver was hung */
    public static final int DRIVER_HUNG_EVENT                    = BASE + 12;


    /* P2P events */
    public static final int P2P_DEVICE_FOUND_EVENT               = BASE + 21;
    public static final int P2P_DEVICE_LOST_EVENT                = BASE + 22;
    public static final int P2P_GO_NEGOTIATION_REQUEST_EVENT     = BASE + 23;
    public static final int P2P_GO_NEGOTIATION_SUCCESS_EVENT     = BASE + 25;
    public static final int P2P_GO_NEGOTIATION_FAILURE_EVENT     = BASE + 26;
    public static final int P2P_GROUP_FORMATION_SUCCESS_EVENT    = BASE + 27;
    public static final int P2P_GROUP_FORMATION_FAILURE_EVENT    = BASE + 28;
    public static final int P2P_GROUP_STARTED_EVENT              = BASE + 29;
    public static final int P2P_GROUP_REMOVED_EVENT              = BASE + 30;
    public static final int P2P_INVITATION_RECEIVED_EVENT        = BASE + 31;
    public static final int P2P_INVITATION_RESULT_EVENT          = BASE + 32;
    public static final int P2P_PROV_DISC_PBC_REQ_EVENT          = BASE + 33;
    public static final int P2P_PROV_DISC_ENTER_PIN_EVENT        = BASE + 34;
    public static final int P2P_PROV_DISC_SHOW_PIN_EVENT         = BASE + 35;
    public static final int P2P_PROV_DISC_FAILURE_EVENT          = BASE + 36;

    /* hostap events */
    public static final int AP_STA_DISCONNECTED_EVENT            = BASE + 41;
    public static final int AP_STA_CONNECTED_EVENT               = BASE + 42;

    public static final int AP_WPS_SUCCESS_EVENT                 = BASE + 50;

    /**
     * This indicates the supplicant connection for the monitor is closed
     */
    private static final String MONITOR_SOCKET_CLOSED_STR = "connection closed";

    /**
     * This indicates a read error on the monitor socket conenction
     */
    private static final String WPA_RECV_ERROR_STR = "recv error";

    /**
     * Tracks consecutive receive errors
     */
    private int mRecvErrors = 0;

    /**
     * Max errors before we close supplicant connection
     */
    private static final int MAX_RECV_ERRORS    = 10;

    private WifiNative mWifiNative;

    public TctSoftapMonitor(String iface, StateMachine wifiStateMachine) {
        mStateMachine = wifiStateMachine;
        mWifiNative = new WifiNative(iface);
    }

    public void startMonitoring() {
        new MonitorThread().start();
    }

    class MonitorThread extends Thread {
        public MonitorThread() {
            super("TctSoftapMonitor");
        }

        public void run() {

            if (connectToSupplicant()) {
                // Send a message indicating that it is now possible to send commands
                // to the supplicant
                //mStateMachine.sendMessage(SUP_CONNECTION_EVENT);
                mWifiNative.startWpsPbcCommand();
            } else {
                //mStateMachine.sendMessage(SUP_DISCONNECTION_EVENT);
                return;
            }

            //noinspection InfiniteLoopStatement
            for (;;) {
                String eventStr = mWifiNative.waitForEvent();

                // Skip logging the common but mostly uninteresting scan-results event
                if (false && eventStr.indexOf(SCAN_RESULTS_STR) == -1) {
                }
                if (!eventStr.startsWith(EVENT_PREFIX_STR)) {
                    if (eventStr.startsWith(WPA_EVENT_PREFIX_STR) &&
                            0 < eventStr.indexOf(PASSWORD_MAY_BE_INCORRECT_STR)) {
                        //mStateMachine.sendMessage(AUTHENTICATION_FAILURE_EVENT);
                    } else if (eventStr.startsWith(WPS_OVERLAP_STR)) {
                        mStateMachine.sendMessage(WPS_OVERLAP_EVENT);
                    } else if (eventStr.startsWith(WPS_FAIL_STR)) {
                        mStateMachine.sendMessage(WPS_FAIL_EVENT);
                    } else if (eventStr.startsWith(WPS_TIMEOUT_STR)) {
                        //add here
                        mStateMachine.sendMessage(WPS_TIMEOUT_EVENT);
                        mWifiNative.closeSupplicantConnection();
                        break;
                    } else if (eventStr.startsWith(WPS_SUCCESS_STR)) {
                        //add here
                        mStateMachine.sendMessage(WPS_SUCCESS_EVENT);
                        mWifiNative.closeSupplicantConnection();
                        break;
                    }
                    continue;
                }

                String eventName = eventStr.substring(EVENT_PREFIX_LEN_STR);
                int nameEnd = eventName.indexOf(' ');
                if (nameEnd != -1)
                    eventName = eventName.substring(0, nameEnd);
                if (eventName.length() == 0) {
                    continue;
                }
                /*
                 * Map event name into event enum
                 */
                int event;
                if (eventName.equals(CONNECTED_STR))
                    event = CONNECTED;
                else if (eventName.equals(DISCONNECTED_STR))
                    event = DISCONNECTED;
                else if (eventName.equals(STATE_CHANGE_STR))
                    event = STATE_CHANGE;
                else if (eventName.equals(SCAN_RESULTS_STR))
                    event = SCAN_RESULTS;
                else if (eventName.equals(LINK_SPEED_STR))
                    event = LINK_SPEED;
                else if (eventName.equals(TERMINATING_STR))
                    event = TERMINATING;
                else if (eventName.equals(DRIVER_STATE_STR))
                    event = DRIVER_STATE;
                else if (eventName.equals(EAP_FAILURE_STR))
                    event = EAP_FAILURE;
                else
                    event = UNKNOWN;

                String eventData = eventStr;
                if (event == DRIVER_STATE || event == LINK_SPEED)
                    eventData = eventData.split(" ")[1];
                else if (event == STATE_CHANGE || event == EAP_FAILURE) {
                    int ind = eventStr.indexOf(" ");
                    if (ind != -1) {
                        eventData = eventStr.substring(ind + 1);
                    }
                } else {
                    int ind = eventStr.indexOf(" - ");
                    if (ind != -1) {
                        eventData = eventStr.substring(ind + 3);
                    }
                }

                if (event == TERMINATING) {
                    /**
                    * If monitor socket is closed, we have already
                    * stopped the supplicant, simply exit the monitor thread
                    */
                    if (eventData.startsWith(MONITOR_SOCKET_CLOSED_STR)) {
                        break;
                    }

                    /**
                    * Close the supplicant connection if we see
                    * too many recv errors
                    */
                    if (eventData.startsWith(WPA_RECV_ERROR_STR)) {
                        if (++mRecvErrors > MAX_RECV_ERRORS) {
                        } else {
                            continue;
                        }
                    }

                    // notify and exit
                    //mStateMachine.sendMessage(SUP_DISCONNECTION_EVENT);
                    break;
                }
                mRecvErrors = 0;
            }
        }

        private boolean connectToSupplicant() {
            int connectTries = 0;

            while (true) {
                if (mWifiNative.connectTosoftap()) {
                    return true;
                }
                if (connectTries++ < 5) {
                    nap(1);
                } else {
                    break;
                }
            }
            return false;
        }
    }
    /**
     * Sleep for a period of time.
     * @param secs the number of seconds to sleep
     */
    private static void nap(int secs) {
        try {
            Thread.sleep(secs * 1000);
        } catch (InterruptedException ignore) {
        }
    }
}
