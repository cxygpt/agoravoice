package com.scifate.moudle_voice.rtc

import android.app.Application
import android.widget.FrameLayout

/**
 * rtc管理类
 * Created by wenhao 2022/12/14
 */
class RTCSDKManager {
    companion object {
        private var sdkManager: RTCSDKManager? = null
        fun instance(): RTCSDKManager? {
            if (sdkManager == null) {
                synchronized(RTCSDKManager::class.java) {
                    if (sdkManager == null) {
                        sdkManager = RTCSDKManager()
                    }
                }
            }
            return sdkManager
        }

        //error callback
        var mErrorCallback: OnErrorCallback? = null

        //room login callback
        var mMeJoinedCallback: OnMeJoinedCallback? = null

        //rejoin channel success callback
        var mRejoinChannelSuccessCallback: OnRejoinChannelSuccessCallback? = null

        //client role changed callback
        var mClientRoleChangedCallback: OnClientRoleChangedCallback? = null

        //user joined callback
        var mUserJoinedCallback: OnUserJoinedCallback? = null

        //user left callback
        var mUserLeftCallback: OnUserLeftCallback? = null

        //user offline callback
        var mUserOfflineCallback: OnUserOfflineCallback? = null

        //connection lost callback
        var mConnectionLostCallback: OnConnectionLostCallback? = null
    }

    /**
     * 创建引擎
     */
    fun createEngine(application: Application, appID: String, logFile: String) {
        AgoraRtcSdkManager.instance()?.createEngine(application, appID, logFile)
    }

    /**
     * 登录房间
     */
    fun loginRoom(
        userID: Int,
        channel: String,
        token: String,
        role: Int,
    ) {
        AgoraRtcSdkManager.instance()?.loginRoom(userID, channel, token, role)
    }

    /**
     * 离开房间
     */
    fun leaveRoom() {
        AgoraRtcSdkManager.instance()?.leaveRoom()
    }

    /**
     * 销毁引擎
     */
    fun destroy(){
        AgoraRtcSdkManager.instance()?.destroy()
    }

    /**
     * 切换前置后置摄像头
     */
    fun setSwitchCamera(){
        AgoraRtcSdkManager.instance()?.setSwitchCamera()
    }

    /**
     * 开启视频
     */
    fun startEnableVideo() {
        AgoraRtcSdkManager.instance()?.startEnableVideo()
    }

    /**
     * role 1 主播 2 观众
     * 设置角色
     */
    fun setIdentity(role: Int){
        AgoraRtcSdkManager.instance()?.setIdentity(role)
    }


    /**
     * 开始推流
     */
    fun startPublish(application: Application, userID: Int, localView: FrameLayout?) {
        AgoraRtcSdkManager.instance()?.startPublish(application, userID, localView)
    }

    /**
     * 开始拉流
     */
    fun startPlaying(application: Application, userID: Int, remoteView: FrameLayout?) {
        AgoraRtcSdkManager.instance()?.startPlaying(application, userID, remoteView)
    }

    /**
     * 设置是否静音
     */
    fun setMuteLocalAudio(muted: Boolean): Boolean {
        return AgoraRtcSdkManager.instance()?.setMuteLocalAudio(muted) == 0
    }

    /**
     * 设置是否外放
     */
    fun setEnableSpeakerphone(enabled: Boolean): Boolean {
        return AgoraRtcSdkManager.instance()?.setEnableSpeakerphone(enabled) == 0
    }

    /**
     * 设置人声
     */
    fun setVoiceConversionPreset(preset:Int){
        AgoraRtcSdkManager.instance()?.setVoiceConversionPreset(preset)
    }

    fun playMusic(filepath: String?, cycle: Int) {
        AgoraRtcSdkManager.instance()?.playMusic(filepath,cycle)
    }

    fun stopMusic() {
        AgoraRtcSdkManager.instance()?.stopMusic()
    }

    fun pauseMusic() {
        AgoraRtcSdkManager.instance()?.pauseMusic()
    }

    fun resumeMusic() {
        AgoraRtcSdkManager.instance()?.resumeMusic()
    }

    fun setMusicVolumeRtc(volume: Int) {
        AgoraRtcSdkManager.instance()?.setMusicVolumeRtc(volume)
    }

    fun getMusicDuration(): Float? {
        return AgoraRtcSdkManager.instance()?.getMusicDuration()
    }

    fun getMusicCurrentPosition(): Float? {
        return AgoraRtcSdkManager.instance()?.getMusicCurrentPosition()
    }

    fun setMusicPosition(startPos: Long) {
        AgoraRtcSdkManager.instance()?.setMusicPosition(startPos)
    }

    interface OnErrorCallback {
        fun onError(err: Int)
    }

    interface OnRejoinChannelSuccessCallback {
        fun onRejoinSuccess()
    }

    interface OnClientRoleChangedCallback {
        fun onClientRoleChangedAnchor()
        fun onClientRoleChangedAudience()
        fun onClientRoleChangedFail(errorCode: Int)
    }

    interface OnUserJoinedCallback {
        fun onUserJoined(uid: String)
    }

    interface OnUserLeftCallback {
        fun onUserLeft(uid: String)
    }

    interface OnUserOfflineCallback {
        fun onUserOffline(uid: String)
    }

    interface OnConnectionLostCallback {
        fun onConnectionLost()
        fun onNetworkRestore(mode:Int)
        fun onNetworkDisconnect()
    }

    interface OnMeJoinedCallback {
        fun onMeJoined()
    }

}