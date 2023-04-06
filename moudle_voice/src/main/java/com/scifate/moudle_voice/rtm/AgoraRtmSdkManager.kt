package com.scifate.moudle_voice.rtm

import android.app.Application
import android.util.Log
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mCallInvitationAcceptedCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mCallInvitationCancelledCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mCallInvitationFailureCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mCallInvitationReceivedCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mCallInviteesAnsweredFailureCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mLocalInvitationCanceledCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mLocalInvitationRefusedCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mLoginConnectCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mLoginStateCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mReceiveRoomMessageCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mRoomMemberJoinedCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mRoomMemberLeftCallback
import com.scifate.moudle_voice.rtm.RTMSDKManager.Companion.mTokenWillExpireCallback
import io.agora.rtm.*
import io.agora.rtm.RtmStatusCode.ConnectionChangeReason.*
import io.agora.rtm.RtmStatusCode.ConnectionState.*
import java.io.File

/**
 * 声网 rtm 管理类
 * Created by wenhao 2022/12/13
 */
class AgoraRtmSdkManager {

    companion object {
        private const val TAG = "RtmSdkManager"
        private var agoraRtmSdkManager: AgoraRtmSdkManager? = null
        fun instance(): AgoraRtmSdkManager? {
            if (agoraRtmSdkManager == null) {
                synchronized(AgoraRtmSdkManager::class.java) {
                    if (agoraRtmSdkManager == null) {
                        agoraRtmSdkManager = AgoraRtmSdkManager()
                    }
                }
            }
            return agoraRtmSdkManager
        }
    }

    private var rtmClient: RtmClient? = null
    private var rtmChannel: RtmChannel? = null
    private var rtmCallManager: RtmCallManager? = null
    private var mLocalInvitation: LocalInvitation? = null
    private var mRemoteInvitation: RemoteInvitation? = null

    /**
     * 初始化 RTM 实例
     */
    fun createInstance(application: Application, appID: String, logFile: String) {
        if (rtmClient == null) {
            rtmClient = RtmClient.createInstance(application, appID, object : RtmClientListener {
                override fun onConnectionStateChanged(state: Int, reason: Int) {
                    mLoginStateCallback?.onConnectionStateChanged(state, reason)
                    when (state) {
                        CONNECTION_STATE_DISCONNECTED -> { //初始状态。SDK 未连接到 Agora RTM 系统
                            when (reason) {
                                CONNECTION_CHANGE_REASON_LOGIN_FAILURE -> { //3: SDK 登录 Agora RTM 系统失败。
                                    mLoginStateCallback?.onConnectFail()
                                }
                                CONNECTION_CHANGE_REASON_LOGIN_TIMEOUT -> { //4: SDK 无法登录 Agora RTM 系统超过 12 秒，停止登录。
                                    mLoginStateCallback?.onConnectTimeOut()
                                }
                                CONNECTION_CHANGE_REASON_INTERRUPTED -> { //5: SDK 与 Agora RTM 系统的连接被中断。
                                    mLoginStateCallback?.onConnectInterrupted()
                                }
                                CONNECTION_CHANGE_REASON_LOGOUT -> { //6: 用户已调用 logout() 方法登出 Agora RTM 系统。
                                    mLoginStateCallback?.onConnectLogout()
                                }
                                CONNECTION_CHANGE_REASON_BANNED_BY_SERVER -> { //7：SDK 被服务器禁止登录 Agora RTM 系统。
                                    mLoginStateCallback?.onConnectBanned()
                                }
                                CONNECTION_CHANGE_REASON_REMOTE_LOGIN -> { //8：另一个用户正以相同的用户 ID 登陆 Agora RTM 系统。
                                    mLoginStateCallback?.onConnectRemoteLogin()
                                }
                                CONNECTION_CHANGE_REASON_TOKEN_EXPIRED -> { //9：用户当前使用的 Token 已过期。
                                    mLoginStateCallback?.onConnectTokenExpired()
                                }
                            }
                        }
                        CONNECTION_STATE_ABORTED -> { //SDK 停止登录 Agora RTM 系统。
                            mLoginStateCallback?.onConnectAborted()
                            mLoginConnectCallback?.onConnectAborted()
                        }
                        CONNECTION_STATE_CONNECTED -> {
                            mLoginStateCallback?.onConnectSuccess()
                            mLoginConnectCallback?.onConnected()
                        }
                        CONNECTION_STATE_RECONNECTING ->{
                            mLoginConnectCallback?.onReconnecting()
                        }
                    }
                }

                override fun onMessageReceived(message: RtmMessage?, peerId: String?) {

                }

                override fun onTokenExpired() {
                    mTokenWillExpireCallback?.onTokenWillExpire()
                }

                override fun onTokenPrivilegeWillExpire() {

                }

                override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {

                }

            })
        }

        setLogFile(logFile)
        rtmCallManager = rtmClient?.rtmCallManager

        rtmCallManager?.setEventListener(object : RtmCallEventListener {
            override fun onLocalInvitationReceivedByPeer(p0: LocalInvitation?) {

            }

            /**
             * 邀请者收到接受邀请回调
             */
            override fun onLocalInvitationAccepted(p0: LocalInvitation?, p1: String?) {
                mCallInvitationAcceptedCallback?.onCallInvitationAccepted()
            }

            /**
             * 邀请者收到拒绝邀请回调
             */
            override fun onLocalInvitationRefused(p0: LocalInvitation?, p1: String?) {
                mLocalInvitationRefusedCallback?.onCallInvitationRejected()
            }

            /**
             * 邀请者收到取消邀请回调
             */
            override fun onLocalInvitationCanceled(p0: LocalInvitation?) {
                mLocalInvitationCanceledCallback?.onCallInvitationCancel()
            }

            /**
             * 邀请者收到邀请进程失败回调
             */
            override fun onLocalInvitationFailure(p0: LocalInvitation?, p1: Int) {
                mCallInviteesAnsweredFailureCallback?.onCallInviteesAnsweredFailure(p1)
            }

            /**
             * 被邀请者收到呼叫邀请回调
             */
            override fun onRemoteInvitationReceived(p0: RemoteInvitation?) {
                setRemoteInvitation(p0)
            }

            override fun onRemoteInvitationAccepted(p0: RemoteInvitation?) {}

            override fun onRemoteInvitationRefused(p0: RemoteInvitation?) {}

            /**
             * 被邀请者收到取消呼叫邀请成功回调
             */
            override fun onRemoteInvitationCanceled(p0: RemoteInvitation?) {
                mCallInvitationCancelledCallback?.onCallInvitationCancelled()
            }

            /**
             * 被邀请者收到邀请进程失败回调
             */
            override fun onRemoteInvitationFailure(p0: RemoteInvitation?, p1: Int) {
                mCallInvitationFailureCallback?.onCallInvitationFailure(p1)
            }
        })
    }

    /**
     * 设置日志存储路径
     */
    private fun setLogFile(logFile: String) {
        val file = File(logFile)
        if (!file.exists()) {
            file.mkdirs()
        }
        rtmClient?.setLogFile(file.absolutePath + "/agorartm.log")
    }

    /**
     * 设置远程邀请回调数据
     */
    private fun setRemoteInvitation(p0: RemoteInvitation?) {
        this.mRemoteInvitation = p0
        if (mRemoteInvitation != null) {
            mCallInvitationReceivedCallback?.onCallInvitationReceived(
                mRemoteInvitation!!.callerId,
                mRemoteInvitation!!.channelId, mRemoteInvitation!!.content
            )
        }
    }

    /**
     * 登录 RTM 系统
     */
    fun login(token: String, uid: String, callback: RTMSDKManager.OnLoggedInCallback) {
        rtmClient?.login(token, uid, object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                callback.onLoggedSuccess()
            }

            override fun onFailure(p0: ErrorInfo?) {
                if (p0 != null) {
                    callback.onLoggedFail(p0.errorCode, p0.errorDescription)
                }
            }
        })
    }

    /**
     * 发送呼叫邀请
     */
    fun callInvite(
        userID: String?,
        channel: String?,
        paramsContent: String,
        callback: RTMSDKManager.OnCallInvitationSentCallback,
    ) {
        //创建 LocalInvitation
        mLocalInvitation = rtmCallManager?.createLocalInvitation(userID)!!
        mLocalInvitation?.channelId = channel
        mLocalInvitation?.content = paramsContent
        //发送呼叫邀请
        rtmCallManager?.sendLocalInvitation(mLocalInvitation, object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                userID?.let { callback.onCallInvitationSentSuccess(it) }
            }

            override fun onFailure(p0: ErrorInfo?) {
                if (p0 != null) {
                    callback.onCallInvitationSentFail(p0.errorCode)
                }
            }

        })
    }

    /**
     * 接受呼叫邀请
     */
    fun callAccept(callback: RTMSDKManager.OnCallAcceptanceSentCallback) {
        if (rtmCallManager != null && mRemoteInvitation != null) {
            rtmCallManager?.acceptRemoteInvitation(mRemoteInvitation,
                object : ResultCallback<Void> {
                    override fun onSuccess(p0: Void?) {
                        callback.onCallAcceptanceSentSuccess()
                    }

                    override fun onFailure(p0: ErrorInfo?) {
                        if (p0 != null) {
                            callback.onCallAcceptanceSentFail(p0.errorCode)
                        }
                    }

                })
        }
    }

    /**
     * 取消呼叫邀请
     */
    fun callCancel(callback: RTMSDKManager.OnCallCancelSentCallback) {
        if (rtmCallManager != null && mLocalInvitation != null) {
            rtmCallManager?.cancelLocalInvitation(mLocalInvitation, object : ResultCallback<Void> {
                override fun onSuccess(p0: Void?) {
                    callback.onCallCancelSentSuccess()
                }

                override fun onFailure(p0: ErrorInfo?) {
                    if (p0 != null) {
                        callback.onCallCancelSentFail(p0.errorCode)
                    }
                }

            })
        }
    }

    /**
     * 拒绝呼叫邀请
     */
    fun callReject(callback: RTMSDKManager.OnCallRejectionSentCallback) {
        if (rtmCallManager != null && mRemoteInvitation != null) {
            rtmCallManager?.refuseRemoteInvitation(mRemoteInvitation,
                object : ResultCallback<Void> {
                    override fun onSuccess(p0: Void?) {
                        callback.onCallRejectionSentSuccess()
                    }

                    override fun onFailure(p0: ErrorInfo?) {
                        if (p0 != null) {
                            callback.onCallRejectionSentFail(p0.errorCode)
                        }
                    }

                })
        }
    }

    /**
     * 创建房间
     */
    fun createRoom(channel: String, callback: RTMSDKManager.OnRoomEnteredCallback) {
        rtmChannel = rtmClient?.createChannel(channel, object : RtmChannelListener {
            override fun onMemberCountUpdated(p0: Int) {

            }

            override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {

            }

            override fun onMessageReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
                if (p0 != null) {
                    mReceiveRoomMessageCallback?.onReceiveRoomMessage(p0.text)
                }
            }

            override fun onMemberJoined(p0: RtmChannelMember?) {
                if (p0 != null) {
                    mRoomMemberJoinedCallback?.onRoomMemberJoined(p0.userId)
                }
            }

            /**
             * 频道成员离开频道回调
             */
            override fun onMemberLeft(p0: RtmChannelMember?) {
                if (p0 != null) {
                    mRoomMemberLeftCallback?.onRoomMemberLeft(p0.userId)
                }
            }

        })
        rtmChannel?.join(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                callback.onSuccess()
            }

            override fun onFailure(p0: ErrorInfo?) {
                if (p0 != null) {
                    callback.onFailure(p0.errorCode)
                }
            }

        })
    }

    /**
     * 发送自定义消息
     */
    fun sendChannelMessage(
        message: String,
        mMessageSentCallback: RTMSDKManager.OnMessageSentCallback,
    ) {
        val rtmMessage = rtmClient?.createMessage()
        rtmMessage?.text = message
        rtmChannel?.sendMessage(rtmMessage, object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                mMessageSentCallback.onMessageSentSuccess()
            }

            override fun onFailure(p0: ErrorInfo?) {
                if (p0 != null) {
                    mMessageSentCallback.onMessageSentFail(p0.errorCode)
                }
            }

        })
    }

    /**
     * 离开房间
     */
    fun leaveRoom() {
        rtmChannel?.leave(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                Log.e(TAG,"leaveRoom--->onSuccess")
            }

            override fun onFailure(p0: ErrorInfo?) {
                if (p0 != null) {
                    Log.e("TAG","leaveRoom--->onFailure:"+p0.errorCode+"----"+p0.errorDescription)
                }
            }

        })
        rtmChannel?.release()
    }

    /**
     * 退出登录
     */
    fun logout() {
        rtmClient?.logout(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
               Log.e(TAG,"logout--->onSuccess")
            }

            override fun onFailure(p0: ErrorInfo?) {
                Log.e(TAG,"logout--->onFailure:"+p0?.errorCode+"---"+p0?.errorDescription)
            }

        })
    }

}