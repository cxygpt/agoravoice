package com.scifate.moudle_voice.rtc

import android.app.Application
import android.widget.FrameLayout
import com.scifate.moudle_voice.rtc.RTCSDKManager.Companion.mClientRoleChangedCallback
import com.scifate.moudle_voice.rtc.RTCSDKManager.Companion.mConnectionLostCallback
import com.scifate.moudle_voice.rtc.RTCSDKManager.Companion.mErrorCallback
import com.scifate.moudle_voice.rtc.RTCSDKManager.Companion.mMeJoinedCallback
import com.scifate.moudle_voice.rtc.RTCSDKManager.Companion.mRejoinChannelSuccessCallback
import com.scifate.moudle_voice.rtc.RTCSDKManager.Companion.mUserJoinedCallback
import com.scifate.moudle_voice.rtc.RTCSDKManager.Companion.mUserLeftCallback
import com.scifate.moudle_voice.rtc.RTCSDKManager.Companion.mUserOfflineCallback
import io.agora.rtc.Constants.*
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.RtcEngineConfig
import io.agora.rtc.video.VideoCanvas
import java.io.File


/**
 * 声网 rtc 管理类
 * Created by wenhao 2022/12/14
 */
class AgoraRtcSdkManager {
    companion object {
        private var agoraRtcSdkManager: AgoraRtcSdkManager? = null
        fun instance(): AgoraRtcSdkManager? {
            if (agoraRtcSdkManager == null) {
                synchronized(AgoraRtcSdkManager::class.java) {
                    if (agoraRtcSdkManager == null) {
                        agoraRtcSdkManager = AgoraRtcSdkManager()
                    }
                }
            }
            return agoraRtcSdkManager
        }
    }

    private var mRtcEngine: RtcEngine? = null

    /**
     * 创建RtcEngine
     */
    fun createEngine(application: Application, appID: String, logFile: String) {
        val config = RtcEngineConfig()
        config.mContext = application
        config.mAppId = appID
        config.mLogConfig = getLogConfig(logFile)
        config.mEventHandler = object : IRtcEngineEventHandler() {
            override fun onError(err: Int) {
                super.onError(err)
                if (mErrorCallback != null) {
                    mErrorCallback?.onError(err)
                }
            }

            /**
             * 加入频道成功
             */
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                super.onJoinChannelSuccess(channel, uid, elapsed)
                mMeJoinedCallback?.onMeJoined()
            }

            /**
             * 有时候由于网络原因，客户端可能会和服务器失去连接，SDK 会进行自动重连，自动重连成功后触发此回调方法
             */
            override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                super.onRejoinChannelSuccess(channel, uid, elapsed)
                mRejoinChannelSuccessCallback?.onRejoinSuccess()
            }

            /**
             * 直播场景下用户角色已切换回调
             */
            override fun onClientRoleChanged(oldRole: Int, newRole: Int) {
                super.onClientRoleChanged(oldRole, newRole)
                if (oldRole == 2 && newRole == 1) {//上麦成功
                    mClientRoleChangedCallback?.onClientRoleChangedAnchor()
                }
                if (oldRole == 1 && newRole == 2) {//下麦成功
                    mClientRoleChangedCallback?.onClientRoleChangedAudience()
                }
            }

            /**
             * 直播场景下切换用户角色失败回调
             */
            override fun onClientRoleChangeFailed(reason: Int, currentRole: Int) {
                super.onClientRoleChangeFailed(reason, currentRole)
                mClientRoleChangedCallback?.onClientRoleChangedFail(reason)
            }

            /**
             * 远端用户（通信场景）/主播（直播场景）加入当前频道回调
             * 用在1v1语音
             */
            override fun onUserJoined(uid: Int, elapsed: Int) {
                super.onUserJoined(uid, elapsed)
                mUserJoinedCallback?.onUserJoined(uid.toString())
            }

            /**
             * 远端用户（通信场景）/主播（直播场景）离开当前频道回调
             * 用在1v1语音和直播间连麦
             * reason 0 用户主动离开 1 掉线
             */
            override fun onUserOffline(uid: Int, reason: Int) {
                super.onUserOffline(uid, reason)
                if (reason == 0) { //用户主动离开
                    mUserLeftCallback?.onUserLeft(uid.toString())
                }
                if (reason == 1) { //用户掉线 toast提示
                    mUserOfflineCallback?.onUserOffline(uid.toString())
                }
            }

            /**
             * 网络连接中断，且 SDK 无法在 10 秒内连接服务器回调
             * 直接toast 不要做任何处理
             */
            override fun onConnectionLost() {
                super.onConnectionLost()
                mConnectionLostCallback?.onConnectionLost()
            }

            override fun onNetworkTypeChanged(type: Int) {
                super.onNetworkTypeChanged(type)
                if (type == -1 || type == 0 || type == 1) {
                    mConnectionLostCallback?.onNetworkDisconnect()
                } else {
                    mConnectionLostCallback?.onNetworkRestore(type)
                }

            }
        }
        mRtcEngine = RtcEngine.create(config)
    }

    private fun getLogConfig(logFile: String): RtcEngineConfig.LogConfig {
        val logConfig = RtcEngineConfig.LogConfig()
        val file = File(logFile)
        if (!file.exists()) {
            file.mkdirs()
        }
        logConfig.filePath = file.absolutePath + "/agorartc.log"
        return logConfig
    }

    /**
     * 加入房间
     *  role 0 1v1 1 主播
     */
    fun loginRoom(
        userID: Int,
        channel: String,
        token: String,
        role: Int,
    ) {
        mRtcEngine?.setChannelProfile(role)
        mRtcEngine?.joinChannel(token, channel, "", userID)
    }

    /**
     * 切换前置后置摄像头
     */
    fun setSwitchCamera(){
        mRtcEngine?.switchCamera()
    }

    /**
     * 开启视频
     */
    fun startEnableVideo() {
        mRtcEngine?.enableVideo()
    }

    /**
     * 设置身份
     */
    fun setIdentity(role: Int) {
        mRtcEngine?.setClientRole(role)
    }

    /**
     * 离开房间
     */
    fun leaveRoom() {
        mRtcEngine?.leaveChannel()
    }

    //预览并推流
    fun startPublish(application: Application, userID: Int, localView: FrameLayout?) {
        mRtcEngine?.setClientRole(CLIENT_ROLE_BROADCASTER)
        // 视频默认禁用，你需要调用 enableVideo 启用视频流。
        mRtcEngine?.enableVideo()
        // 开启本地视频预览。
        mRtcEngine?.startPreview()
        // 创建一个 SurfaceView 对象，并将其作为 FrameLayout 的子对象。
        val surfaceView = RtcEngine.CreateRendererView(application)
        surfaceView.setZOrderMediaOverlay(true)
        localView?.addView(surfaceView)
        // 将 SurfaceView 对象传入 Agora，以渲染本地视频。
        mRtcEngine!!.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FILL, userID))
    }

    //拉流
    fun startPlaying(application: Application, userID: Int, remoteView: FrameLayout?) {
        val surfaceView = RtcEngine.CreateRendererView(application)
        surfaceView.setZOrderMediaOverlay(true)
        remoteView?.addView(surfaceView)
        mRtcEngine!!.setupRemoteVideo(
            VideoCanvas(
                surfaceView,
                VideoCanvas.RENDER_MODE_FILL,
                userID
            )
        )
    }

    //设置是否静音
    fun setMuteLocalAudio(muted: Boolean): Int? {
        return mRtcEngine?.muteLocalAudioStream(muted)
    }

    //设置是否外放
    fun setEnableSpeakerphone(enabled: Boolean): Int? {
        return mRtcEngine?.setEnableSpeakerphone(enabled)
    }

    //设置预设的变声效果
    //VOICE_CONVERSION_OFF: 原声，即关闭变声效果。 0
    //VOICE_CHANGER_NEUTRAL: 中性。为避免音频失真，请确保仅对女声设置该效果。1
    //VOICE_CHANGER_SWEET: 甜美。为避免音频失真，请确保仅对女声设置该效果。2
    //VOICE_CHANGER_SOLID: 稳重。为避免音频失真，请确保仅对男声设置该效果。3
    //VOICE_CHANGER_BASS: 低沉。为避免音频失真，请确保仅对男声设置该效果。4
    fun setVoiceConversionPreset(preset: Int): Int? {
        val agoraPreset = when (preset) {
            1 -> {
                VOICE_CHANGER_NEUTRAL
            }
            2 -> {
                VOICE_CHANGER_SWEET
            }
            3 -> {
                VOICE_CHANGER_SOLID
            }
            4 -> {
                VOICE_CHANGER_BASS
            }
            else -> {
                VOICE_CONVERSION_OFF
            }
        }
        return mRtcEngine?.setVoiceConversionPreset(agoraPreset)
    }

    fun playMusic(filepath: String?, cycle: Int) {
        mRtcEngine?.startAudioMixing(filepath, false, false, cycle,0)
    }

    fun stopMusic() {
        mRtcEngine?.stopAudioMixing()
    }

    fun pauseMusic() {
        mRtcEngine?.pauseAudioMixing()
    }

    fun resumeMusic() {
        mRtcEngine?.resumeAudioMixing()
    }

    fun setMusicVolumeRtc(volume: Int) {
        mRtcEngine?.adjustAudioMixingVolume(volume)
    }

    fun getMusicDuration(): Float {
        return mRtcEngine?.audioMixingDuration?.toFloat() ?: 0f
    }

    fun getMusicCurrentPosition(): Float {
        return mRtcEngine?.audioMixingCurrentPosition?.toFloat() ?: 0f
    }

    fun setMusicPosition(position: Long) {
        mRtcEngine?.setAudioMixingPosition(position.toInt())
    }

    /**
     * 销毁RtcEngine
     */
    fun destroy() {
        RtcEngine.destroy()
    }
}