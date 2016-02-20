---
title: android wifi 从kernel到driver scan流程 
---
```markdown
  supplicant:
  在supplicant中nl80211使用netlink，wext使用ioctl，效果是类似的。
  wpa_driver_nl80211_scan -> 
  nl80211_scan_common(NL80211_CMD_TRIGGER_SCAN）
  
  kernel: 
  nl80211.c::nl80211_ops[] -> nl80211.c::nl80211_trigger_scan -> 
  rdev-ops.h::rdev_scan -> 
  struct cfg80211_registered_device::struct cfg80211_ops->scan -> 
  
  driver:
  开始扫描
  (1)HDD层: 
    wlan_hdd_cfg80211.c::wlan_hdd_cfg80211_ops -> 
    wlan_hdd_cfg80211.c::__wlan_hdd_cfg80211_scan -> 
    csrApiScan.c::csrScanRequest(eSmeCommandScan) ->
  (2)SME层: 
    sme_Api.c::smeProcessCommand(eSmeCommandScan) -> 
    csrApiScan.c::csrProcessScanCommand(eCsrScanUserRequest) ->
    csrApiScan.c::csrSendMBScanReq(eWNI_SME_SCAN_REQ) -> 
  (3)PE层:
    (a)SME消息转换为MLM消息
    limProcessMessageQueue.c::limProcessMessages(eWNI_SME_SCAN_REQ) ->
    limProcessSmeReqMessages.c::limProcessNormalHddMsg(eWNI_SME_SCAN_REQ) ->
    limProcessSmeReqMessages.c::__limProcessSmeScanReq(LIM_MLM_SCAN_REQ) -> 
    (b)进入SCAN模式
    limProcessMlmReqMessages.c::limProcessMlmReqMessages(LIM_MLM_SCAN_REQ) ->  
    limProcessMlmReqMessages.c::limSetScanMode(eLIM_HAL_INIT_SCAN_WAIT_STATE) ->
    (c)从WDA_INIT_SCAN_REQ到WDA_INIT_SCAN_RSP
    limProcessMlmReqMessages.c::limSendHalInitScanReq(WDA_INIT_SCAN_REQ) -> 
    wlan_qct_wda.c::WDA_McProcessMsg(WDA_INIT_SCAN_REQ) -> 
    wlan_qct_wda.c::WDA_ProcessInitScanReq(tWDA_ReqParams) -> 
    wlan_qct_wda.c::WDA_InitScanReqCallback(WDA_INIT_SCAN_RSP) -> 
    limProcessMlmRspMessages.c::limProcessInitScanRsp(WDA_INIT_SCAN_RSP) ->
    limProcessMlmReqMessages.c::limContinueChannelScan(eLIM_HAL_START_SCAN_WAIT_STATE) -> 
    (d)从WDA_START_SCAN_REQ到WDA_START_SCAN_RSP
    limProcessMlmReqMessages.c::limSendHalStartScanReq(WDA_START_SCAN_REQ) ->
    wlan_qct_wda.c::WDA_ProcessStartScanReq(WDA_START_SCAN_REQ) ->
    wlan_qct_wda.c::WDA_StartScanReqCallback(WDA_START_SCAN_RSP) ->
    limProcessMlmRspMessages.c::limProcessStartScanRsp(WDA_START_SCAN_RSP) ->
    limProcessMlmReqMessages.c::limContinuePostChannelScan ->
    limSendManagementFrames.c::limSendProbeReqMgmtFrame/
    (e)触发minChannelTime定时器
    limTimerUtils.c::limDeactivateAndChangeTimer(eLIM_MIN_CHANNEL_TIMER) ->
    limTimerUtils.c::limCreateTimers(SIR_LIM_MIN_CHANNEL_TIMEOUT) ->
    limProcessMlmReqMessages.c::limProcessMinChannelTimeout(SIR_LIM_MIN_CHANNEL_TIMEOUT) -> 
    (f)从WDA_END_SCAN_REQ到WDA_END_SCAN_RSP
    limProcessMlmReqMessages.c::limSendHalEndScanReq(WDA_END_SCAN_REQ) ->
    wlan_qct_wda.c::WDA_EndScanReqCallback(WDA_END_SCAN_RSP) ->
    limProcessMlmRspMessages.c::limProcessEndScanRsp(WDA_END_SCAN_RSP) ->
    limProcessMlmReqMessages.c::limContinueChannelScan ->
    (g)从WDA_FINISH_SCAN_REQ到WDA_FINISH_SCAN_RSP
    limProcessMlmReqMessages.c::limSendHalFinishScanReq(WDA_FINISH_SCAN_REQ) ->
    wlan_qct_wda.c::WDA_FinishScanReqCallback(WDA_FINISH_SCAN_RSP) ->
    limProcessMlmRspMessages.c::limProcessFinishScanRsp(WDA_FINISH_SCAN_RSP) ->
    limProcessMlmReqMessages.c::limCompleteMlmScan(LIM_MLM_SCAN_CNF) ->
    limProcessMlmRspMessages.c::limProcessMlmScanCnf ->
    limSendSmeRspMessages.c::limSendSmeScanRsp(eWNI_SME_SCAN_RSP) ->
    
  接收扫描结果
  (1)driver
    csrApiScan.c::csrScanSmeScanResponse(eWNI_SME_SCAN_RSP) ->
    csrApiScan.c::csrScanGetScanChnInfo(eSmeCommandScan/eCsrScanGetScanChnInfo) ->
    csrApiScan.c::csrProcessScanCommand(eSmeCommandScan) ->
    csrApiScan.c::csrScanGetScanChannelInfo(eWNI_SME_GET_SCANNED_CHANNEL_REQ) -> 
    limProcessSmeReqMessages.c::
    limProcessSmeGetScanChannelInfo(eWNI_SME_GET_SCANNED_CHANNEL_RSP)->
    csrApiScan.c::csrScanSmeScanResponse(eWNI_SME_GET_SCANNED_CHANNEL_RSP) ->
    csrApiScan.c::csrReleaseScanCommand(eSIR_SME_SUCCESS) ->
    csrApiScan.c::csrScanCallCallback -> 
    wlan_hdd_cfg80211.c::hdd_cfg80211_scan_done_callback ->
  (2)kernel
    kernel/net/wireless/scan.c::cfg80211_scan_done ->
    core.c::INIT_WORK(&rdev->scan_done_wk, __cfg80211_scan_done) -> 
    scan.c::___cfg80211_scan_done -> 
    nl80211.c::nl80211_send_scan_done(NL80211_CMD_NEW_SCAN_RESULTS)
  
  涉及的重要结构体有：
  struct cfg80211_scan_request  kernel传给driver的扫描参数
  struct tagCsrScanRequest      SME层的扫描请求
  struct tagCsrScanResult       扫描结果
  struct cfg80211_bss           driver传给kernel的扫描结果
  
  对应的kernel log:
  //打印struct cfg80211_scan_request
  [11:08:33.428944] wlan: [4274:I :HDD] scan request for ssid = 1
  [11:08:33.428956] wlan: [4274:I :HDD] No of Scan Channels: 1
  [11:08:33.428967] wlan: [4274:I :HDD] Channel-List:  13
  //打印struct tCsrScanRequest
  [11:08:33.429041] wlan: [4274:IH:HDD] requestType 2, scanType 1, 
  minChnTime 20, maxChnTime 40,p2pSearch 0, skipDfsChnlInP2pSearch 0
  //从SME层的csr消息队列到PE层的lim消息队列
  [11:08:33.429102] wlan: [4274:I :SME] csrScanRequest: 885:  
  SId=0 scanId=466 Scan reason=9 numSSIDs=0 numChan=1 
  P2P search=0 minCT=20 maxCT=40 minCBtc=60 maxCBtx=120
  [11:08:33.429430] wlan: [4061:I :PE ] __limProcessSmeScanReq: 1217: 
  SME SCAN REQ numChan 1 min 20 max 40 IELen 0first 0 fresh 129 
  unique 1 type eSIR_ACTIVE_SCAN (1) 
  mode eSIR_AGGRESSIVE_BACKGROUND_SCAN (0)rsp 1
  [11:08:33.429471] wlan: [4061:I :PE ] __limProcessSmeScanReq: 1336: 
  Non Offload SCAN request
  [11:08:33.429484] wlan: [4061:I :PE ] __limProcessSmeScanReq: 1453: 
  Issue Scan request command to MLM
  //从WDA_INIT_SCAN_REQ到WDA_START_SCAN_REQ
  [11:08:33.429523] wlan: [4061:I :WDA] ------> WDA_ProcessInitScanReq
  [11:08:33.439702] wlan: [4061:I :PE ] limContinueChannelScan: 1310: 
  Current Channel to be scanned is 13
  [11:08:33.439735] wlan: [4061:I :WDA] ------> WDA_ProcessStartScanReq
  //发送Probe Request，接收Probe Response
  [11:08:33.441888] wlan: [4061:I :PE ] limContinuePostChannelScan: 
  449: sending ProbeReq number 0, for SSID  on channel: 13
  //底层报上的消息，由limHandle80211Frames处理
  [11:08:33.457829] wlan: [4061:I :PE ] SessionId:0 ProbeRsp Frame 
  is received
  //触发minChannelTime定时器，进入WDA_END_SCAN_REQ
  [11:08:33.457909] wlan: [4061:W :PE ] limProcessMinChannelTimeout: 
  3887: Sending End Scan req from MIN_CH_TIMEOUT in state 2 ch-13
  [11:08:33.457933] wlan: [4061:I :WDA] ------> WDA_ProcessEndScanReq
  //DUT以minChannelTime/2周期性发送Probe Request，直到min/maxChannelTime超时
  [11:08:33.457979] wlan: [4061:I :PE ] limProcessPeriodicProbeReqTimer: 
  4070: received unexpected Periodic scan timeout in state 2
  [11:08:33.458975] wlan: [4061:I :WDA] ------> WDA_ProcessFinishScanReq
  //PE层扫描结果
  [11:08:33.478133] wlan: [4061:I :PE ] limSendSmeScanRsp: 
  798: BssId
  [11:08:33.478146] wlan: [4061:I :PE ] limPrintMacAddr: 1314: 
  c0:61:18:3b:82:4f
  //SME层扫描结果
  [11:08:33.478215] wlan: [4061:I :SME]  Scan received 1 unique 
  BSS scan reason is 9
  [11:08:33.478232] wlan: [4061:W :SME] ...Bssid= c0:61:18:3b:82:4f 
  chan= 13, rssi = -47
  //获取channel扫描结果
  [11:08:33.478505] wlan: [4061:I :PE ] limProcessSmeGetScanChannelInfo: 
  3297: Sending message  with number of channels 1
  //HDD层扫描结果
  [11:08:33.478638] wlan: [4061:I :HDD] Enter:wlan_hdd_cfg80211_update_bss
  [11:08:33.478664] wlan: [4061:I :HDD] wlan_hdd_cfg80211_inform_bss_frame:
  BSSID:c0:61:18:3b:82:4f Channel:2472RSSI:-59


  




    
    
    
    
    
    
    
    
    
    
    
```

                       