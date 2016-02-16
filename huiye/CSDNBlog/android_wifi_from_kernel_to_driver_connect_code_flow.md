---
title: android wifi从kernel到driver connect流程 
---
## 一、supplicant部分

定义了CONFIG_SME发送NL80211_CMD_ASSOCIATE，不支持SME的wlan driver发送NL80211_CMD_CONNECT完成auth和assoc。
wpa_supplicant_connect -> 
wpa_supplicant_associate(struct wpa_driver_associate_params) -> wpa_driver_nl80211_associate -> 
wpa_driver_nl80211_connect(NL80211_CMD_CONNECT) 

## 二、kernel部分

## 三、driver部分
### 3.1 Connect/Assoc阶段
#### 3.1.1 HDD层
wlan_hdd_cfg80211.c::__wlan_hdd_cfg80211_connect(struct cfg80211_connect_params) ->
wlan_hdd_cfg80211.c::wlan_hdd_cfg80211_connect_start -> 
#### 3.1.2 SME层
csrApiRoam.c::csrRoamConnect ->
csrApiRoam.c::csrRoamIssueJoin(eWNI_SME_JOIN_REQ) ->
limProcessSmeReqMessages.c::__limProcessSmeJoinReq(LIM_MLM_JOIN_REQ) ->
#### 3.1.3 PE层
1. Join阶段
limProcessMlmReqMessages.c::limProcessMlmJoinReq//处理逻辑 
-> limProcessMlmReqMessages.c::limSetChannel(WDA_CHNL_SWITCH_REQ) ->
limProcessMlmReqMessages.c::limProcessSwitchChannelJoinReq(gLimJoinFailureTimer) -> 
limAssocUtils.c::limCheckAndAnnounceJoinSuccess(LIM_MLM_JOIN_CNF)//检查Probe Response是否来自要加入的BSS
-> limAssocUtils.c::limStaSendAddBssPreAssoc(WDA_ADD_BSS_REQ) ->
limProcessMlmRspMessages.c::limProcessStaMlmAddBssRspPreAssoc(LIM_MLM_AUTH_REQ) -> 
limProcessMlmReqMessages.c::limProcessMlmAuthReq(gLimAuthFailureTimer) ->
2. Auth阶段
limProcessAuthFrame.c::limProcessAuthFrame -> 
limSecurityUtils.c::limRestoreFromAuthState(LIM_MLM_AUTH_CNF) -> 
limProcessMlmRspMessages.c::limProcessMlmAuthCnf(LIM_MLM_ASSOC_REQ) ->
limProcessMlmReqMessages.c::limProcessMlmAssocReq(gLimAssocFailureTimer) ->
3. Assoc阶段
limProcessAssocRspFrame.c::limProcessAssocRspFrame//失败时发送LIM_MLM_ASSOC_CNF 
-> limAssocUtils.c::limStaSendAddBss(WDA_ADD_BSS_REQ)//Join阶段有PreAssoc 
-> limAssocUtils.c::limAddStaSelf(WDA_ADD_STA_REQ) -> 
limProcessMlmRspMessages.c::limProcessStaMlmAddStaRsp(LIM_MLM_ASSOC_CNF)//systemrole=3 
-> limProcessMlmRspMessages.c::limHandleSmeJoinResult(eWNI_SME_JOIN_RSP) ->
csrApiRoam.c::csrRoamComplete(eCsrJoinSuccess) -> 
csrApiRoam.c::csrRoamCallCallback(eCSR_ROAM_ASSOCIATION_COMPLETION)
/csrApiRoam.c::csrRoamStartWaitForKeyTimer(hTimerWaitForKey)
/pmc.c::pmcStartTrafficTimer(BMPS_TRAFFIC_TIMER_ALLOW_SECURITY_DHCP) ->
wlan_hdd_assoc.c::hdd_AssociationCompletionHandler ->
sme.c::__cfg80211_connect_result

### 3.2 AddKey阶段
NL80211_CMD_NEW_KEY -> 
wlan_hdd_cfg80211.c::__wlan_hdd_cfg80211_add_key//单播密钥 
-> csrApiRoam.c::csrRoamIssueSetKeyCommand(eSmeCommandSetKey) -> 
csrApiRoam.c::csrSendMBSetContextReqMsg(eWNI_SME_SETCONTEXT_REQ) -> 
limProcessSmeReqMessages.c::__limProcessSmeSetContextReq(LIM_MLM_SETKEYS_REQ) -> 
limSecurityUtils.c::limSendSetStaKeyReq(WDA_SET_STAKEY_REQ) ->
limProcessMlmRspMessages.c::limProcessMlmSetStaKeyRsp(LIM_MLM_SETKEYS_CNF) -> 
limSendSmeRspMessages.c::limSendSmeSetContextRsp(eWNI_SME_SETCONTEXT_RSP) ->
csrApiRoam.c::csrRoamCallCallback(eCSR_ROAM_SET_KEY_COMPLETE) ->
wlan_hdd_assoc.c::hdd_RoamSetKeyCompleteHandler
接着会安装组播密钥
疑问add_key和set_key是什么关系？

### 3.3 DHCP阶段
sme_Api.c::sme_DHCPStartInd(WDA_DHCP_START_IND)

## 三、涉及到的重要结构体

struct cfg80211_connect_params    kernel传给driver的连接参数    
struct tCsrRoamProfile            Roam切换的参数    
struct tpPESession                PE层的参数    
struct tpAddBssParams             Add Bss的参数    
struct tAddStaParams              Add Sta的结构体参数    
struct tpSirAssocRsp              AssocRsp Frame对应的结构体    

## 四、对应的kernel log

//kernel调用wlan_hdd_cfg80211_connect    
[11:08:52.828310] wlan: [20318:I :HDD] __wlan_hdd_cfg80211_connect: device_mode =   WLAN_HDD_INFRA_STATION (0)  
//根据struct cfg80211_connect_params设置连接参数  
[11:08:52.828319] wlan: [20318:I :HDD] Enter:wlan_hdd_cfg80211_set_privacy  
[11:08:52.828347] wlan: [20318:I :HDD] wlan_hdd_set_akm_suite: setting key mgmt type to PSK  
[11:08:52.828439] wlan: [20318:I :HDD] wlan_hdd_cfg80211_connect_start: 10768: Connect to SSID: 2.4GHz opertating Channel: 0  
//SME层
[11:08:52.828522] wlan: [20318:I :SME] csrRoamConnect: 6662: called  BSSType = eSIR_INFRA_AP_MODE (1)   authtype = 8 encryType = 6  
[11:08:52.829096] wlan: [20233:I :SME] Attempting to Join Bssid= c0:61:18:3b:82:4f  
[11:08:52.829489] wlan: [20233:I :PE ] __limProcessSmeJoinReq: 2024: SME JoinReq:  
Sessionid 0 SSID len 6 SSID : 2.4GHz Channel 13, BSSID c0:61:18:3b:82:4f  
//PE层  
//Join阶段  
//这句log表示非offchannel  
[11:08:52.829509] wlan: [20233:I :PE ] SessionId:0 Join request on current channel  
//limProcessMlmPostJoinSuspendLink  
[11:08:52.829545] wlan: [20233:I :PE ] limProcessMlmPostJoinSuspendLink: 2204: [limProcessMlmJoinReq]:   suspend link success(0) on sessionid: 0 setting channel to:     
13 with secChanOffset:0 and maxtxPower: 20  
//切换到AP所在的channel  
//limSendSwitchChnlParams  
[11:08:52.829554] wlan: [20233:I :PE ] SessionId:0 WDA_CHNL_SWITCH_REQ for SSID:2.4GHz  
[11:08:52.854984] wlan: [20233:I :PE ] limProcessSwitchChannelJoinReq: 3947: Sessionid: 0    
Send Probe req on channel 13 ssid: 2.4GHz BSSID: c0:61:18:3b:82:4f   
[11:08:52.860865] wlan: [20233:I :PE ] SessionId:0 ProbeRsp Frame is received  
[11:08:52.860914] wlan: [20233:I :PE ] limCheckAndAnnounceJoinSuccess: 3284:   
Received Beacon/PR with matching BSSIDc0:61:18:3b:82:4fPESessionID 0  
//Add Bss，对比Assoc阶段的Add Sta  
[11:08:52.860969] wlan: [20233:I :PE ] limStaSendAddBssPreAssoc: 4008: BSSID: c0:61:18:3b:82:4f  
[11:08:52.861101] wlan: [20233:I :PE ] limStaSendAddBssPreAssoc: 4278: fShortGI40Mh: 1  maxAmpduSize: 3   htLdpcCapable: 1 vhtLdpcCapable: 0  
//WDA_ADD_BSS_REQ与WDA_ADD_BSS_RSP  
[11:08:52.861249] wlan: [20233:I :PE ] limStaSendAddBssPreAssoc: 4372: SessionId:0   
Sending SIR_HAL_ADD_BSS_REQ  
//limProcessStaMlmAddBssRspPreAssoc  
[11:08:52.888823] wlan: [20233:I :PE ] SessionId:0 limPostMlmMessage LIM_MLM_AUTH_REQwith limSmeState:5  
[11:08:52.888837] wlan: [20233:I :PE ] limProcessMlmAuthReq: 2385: Process Auth Req on sessionID 0   Systemrole 3mlmstate 5 from: c0:61:18:3b:82:4f with authtype 0  
//发送Authentication Request  
[11:08:52.888882] wlan: [20233:I :PE ] limSendAuthMgmtFrame: 3781: Sending Auth Frame over WQ5  
with waitForAck 1 to c0:61:18:3b:82:4f From 00:0a:f5:77:72:8c  
//能否从此处看出发射功率为多少  
[11:08:52.955498] wlan: [20233:I :PE ] limAuthTxCompleteCnf: 3468: txCompleteSuccess= 1  
//接收Authentication Responce  
[11:08:52.956608] wlan: [20233:I :PE ] limProcessAuthFrame: 190: Sessionid: 0 System role : 3  
limMlmState: 7 :Auth Frame Received: BSSID: c0:61:18:3b:82:4f (RSSI 57)  
[11:08:52.956653] wlan: [20233:I :PE ] limProcessMlmAuthCnf: 732: Clearing Immed Blk Ack:  
no AP support  
//Assoc阶段，旅游回来接着看，尝试从后面臃肿的部分开始  
//解决Assoc Reject问题可以增加AssocFailureTimer时间，   
//参见wniCfgSta.h中ASSOC_FAILURE_TIMEOUT_STADEF   
//发送Association Request   
[11:08:52.956682] wlan: [20233:I :PE ] limProcessMlmAssocReq: 2603: Process Assoc Req on sessionID 0   Systemrole 3mlmstate 5 from: c0:61:18:3b:82:4f  
[11:08:52.956797] wlan: [20233:I :PE ] limSendAssocReqMgmtFrame: 2579: Sending Assoc req   
over WQ5 to c0:61:18:3b:82:4f From 00:0a:f5:77:72:8c  
//接收Association Responce  
[11:08:52.980276] wlan: [20233:I :PE ] limProcessAssocRspFrame: 332: received   
Re/Assoc(0) resp on sessionid: 0 with systemrole: 3 and mlmstate: 12 RSSI 58 from  
c0:61:18:3b:82:4f  
//将AssocRsp Frame转化为tpSirAssocRsp结构体  
[11:08:52.980335] wlan: [20233:I :PE ] sirConvertAssocRespFrame2Struct: 2465: WMM Parameter  
Element present in Association Response Frame!  
//Add Bss  
[11:08:52.989871] wlan: [20233:I :PE ] limStaSendAddBss: 3549: BSSID: c0:61:18:3b:82:4f  
[11:08:52.990991] wlan: [20233:I :PE ] limStaSendAddBss: 3933: SessionId:0 Sending SIR_HAL_ADD_BSS_REQ  
[11:08:53.009181] wlan: [20233:W :PE ] limProcessStaMlmAddBssRsp: 3199: SessionId:0 On STA:  
ADD_BSS was successful  
//Add Sta  
[11:08:53.015286] wlan: [20233:I :PE ] limAddStaSelf: 2816: SGI 20 0  
[11:08:53.015311] wlan: [20233:I :PE ] limAddStaSelf: 2829: 00:0a:f5:77:72:8c:  
[11:08:53.016245] wlan: [20233:W :PE ] limAddStaSelf: 3027: 00:0a:f5:77:72:8c:Sessionid 0 :  
Sending SIR_HAL_ADD_STA_REQ... (aid 32)  
//limHandleSmeJoinResult调用limSendSmeJoinReassocRsp  
//发送eWNI_SME_JOIN_RSP给SME层  
[11:08:53.035477] wlan: [20233:I :PE ] limSendSmeJoinReassocRsp: 378: maxRateFlags: a  
//SME层Assoc成功  
[11:08:53.035518] wlan: [20233:W :SME] csrRoamProcessResults: 5353: receives association indication  
[11:08:53.035540] wlan: [20233:I :SME] csrRoamStateChange: 1063: CSR RoamState[0]: [  
eCSR_ROAMING_STATE_JOINED <== eCSR_ROAMING_STATE_JOINING ]  
[11:08:53.035566] wlan: [20233:I :SME] csrRoamSubstateChange: 1048: CSR RoamSubstate: [  
eCSR_ROAM_SUBSTATE_WAIT_FOR_KEY <== eCSR_ROAM_SUBSTATE_NONE ]  
//等待supplicant完成认证生成key  
[11:08:53.035576] wlan: [20233:I :SME]  csrScanStartWaitForKeyTimer  
[11:08:53.036368] wlan: [20233:W :SME]  Assoc complete result = 2 statusCode = 0 reasonCode = 0  
//HDD层  
[11:08:53.036378] wlan: [20233:IH:HDD] CSR Callback: status= 7 result= 2 roamID=1  
[11:08:53.036387] wlan: [20233:I :HDD] ****eCSR_ROAM_ASSOCIATION_COMPLETION****  
[11:08:53.036399] wlan: [20233:I :HDD] hdd_AssociationCompletionHandler: Set HDD connState to  
eConnectionState_Associated  
[11:08:53.036420] wlan: [20233:IL:HDD] hdd_wmm_connect: Entered  
[11:08:53.055433] wlan: [20233:I :HDD] hdd_AssociationCompletionHandler: sending connect  
indication to nl80211: for bssid c0:61:18:3b:82:4f reason:2 and Status:7  
[11:08:53.055597] wlan: [20233:IL:HDD] hdd_wmm_assoc: Entered  
//4个EAPOL是四步握手  
[11:08:53.055861] wlan: [20235:I :HDD] STA RX EAPOL  
[11:08:53.063562] wlan: [20233:I :SME] csrRoamProcessResults: 5647: Set remainInPowerActiveTillDHCP to   make sure we wait until keys are set  
[11:08:53.063593] wlan: [20234:I :HDD] STA TX EAPOL  
[11:08:53.086168] wlan: [20235:I :HDD] STA RX EAPOL  
[11:08:53.089502] wlan: [20234:I :HDD] STA TX EAPOL  
//单播秘钥  
[11:08:53.090054] wlan: [20318:I :HDD] __wlan_hdd_cfg80211_add_key- 8674: setting pairwise key  
[11:08:53.090093] wlan: [20318:IM:HDD] __wlan_hdd_cfg80211_add_key: set key   
for peerMac c0:61:18:3b:82:4f, direction 2     
//组播秘钥  
[11:08:53.098144] wlan: [20318:I :HDD] __wlan_hdd_cfg80211_add_key- 8665: setting Broadcast key  
[11:08:53.098161] wlan: [20318:IM:HDD] __wlan_hdd_cfg80211_add_key: set key   
for peerMac ff:ff:ff:ff:ff:ff, direction 1   
[11:08:53.100084] wlan: [20233:IH:HDD] CSR Callback: status= 18 result= 0 roamID=1  
[11:08:53.100104] wlan: [20233:IH:HDD] Set Key completion roamStatus =18 roamResult=0 c0:61:18:3b:82:4f  
//DHCP过程  
//由DHCP REQUEST到DHCP ACK  
[11:08:53.174417] wlan: [20318:I :HDD] hdd_driver_command: 3778: send DHCP START indication  
[11:08:53.496972] wlan: [0:I :HDD] hdd_dhcp_pkt_info: 683: DHCP Dest Addr: ff:ff:ff:ff:ff:ff Src Addr   00:0a:f5:77:72:8c  source port : 68, dest port : 67  
[11:08:53.496980] wlan: [0:I :HDD] hdd_dhcp_pkt_info: 693: DHCP REQUEST  
[11:08:53.700768] wlan: [20235:I :HDD] hdd_dhcp_pkt_info: 683: DHCP Dest Addr: 00:0a:f5:77:72:8c   
Src Addr c0:61:18:3b:82:50  source port : 67, dest port : 68  
[11:08:53.700774] wlan: [20235:I :HDD] hdd_dhcp_pkt_info: 699: DHCP ACK  
[11:08:53.982172] wlan: [20318:I :HDD] hdd_driver_command: 3795: send DHCP STOP indication  












