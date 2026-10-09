package com.jones.winggo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod


class WingGoModule(
    private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {


    // ============================================================
    // INSTANCE
    // ============================================================

    companion object {

        var instance: WingGoModule? = null


        fun emitNotificationEvent() {
            instance?.sendNotificationEvent()
        }
    }


    init {
        instance = this
    }


    // ============================================================
    // MODULE NAME
    // ============================================================

    override fun getName(): String {
        return "WingGoNotifications"
    }


    // ============================================================
    // NOTIFICATION EVENT
    // ============================================================

    private fun sendNotificationEvent() {

        reactContext
            .getJSModule(
                com.facebook.react.modules.core
                    .DeviceEventManagerModule
                    .RCTDeviceEventEmitter::class.java
            )
            .emit(
                "WingGoNotification",
                null
            )
    }


    // ============================================================
    // OPEN NOTIFICATION ACCESS SETTINGS
    // ============================================================

    @ReactMethod
    fun openNotificationAccessSettings() {

        try {

            val intent =
                Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                )

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            reactContext.startActivity(
                intent
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }


    // ============================================================
    // CHECK NOTIFICATION ACCESS
    // ============================================================

    @ReactMethod
    fun checkNotificationAccess(
        promise: Promise
    ) {

        try {

            val enabledListeners =
                Settings.Secure.getString(
                    reactContext.contentResolver,
                    "enabled_notification_listeners"
                )


            val componentName =
                "${reactContext.packageName}/${NotificationListener::class.java.name}"


            val granted =
                enabledListeners
                    ?.split(":")
                    ?.any {
                        it.equals(
                            componentName,
                            ignoreCase = true
                        )
                    } == true


            promise.resolve(
                granted
            )

        } catch (e: Exception) {

            promise.reject(
                "NOTIFICATION_ACCESS_CHECK_FAILED",
                e
            )
        }
    }


    // ============================================================
    // OPEN OVERLAY SETTINGS
    // ============================================================

    @ReactMethod
    fun openOverlaySettings() {

        try {

            val intent =
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse(
                        "package:${reactContext.packageName}"
                    )
                )

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            reactContext.startActivity(
                intent
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }


    // ============================================================
    // CHECK OVERLAY PERMISSION
    // ============================================================

    @ReactMethod
    fun checkOverlayPermission(
        promise: Promise
    ) {

        try {

            val granted =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M
                ) {

                    Settings.canDrawOverlays(
                        reactContext
                    )

                } else {

                    true
                }


            promise.resolve(
                granted
            )

        } catch (e: Exception) {

            promise.reject(
                "OVERLAY_PERMISSION_CHECK_FAILED",
                e
            )
        }
    }


    // ============================================================
    // START FLOATING WINGGO
    //
    // JS:
    //
    // startFloatingWingGo("winggo")
    // startFloatingWingGo("Vijay")
    // startFloatingWingGo("CM_1")
    // startFloatingWingGo("CM_2")
    //
    // The selected character ID is passed to the service.
    // ============================================================

    @ReactMethod
    fun startFloatingWingGo(
        characterId: String,
        promise: Promise
    ) {

        try {

            // ----------------------------------------------------
            // CHECK OVERLAY PERMISSION
            // ----------------------------------------------------

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(
                    reactContext
                )
            ) {

                promise.reject(
                    "OVERLAY_PERMISSION_REQUIRED",
                    "Overlay permission is required."
                )

                return
            }


            // ----------------------------------------------------
            // NORMALIZE CHARACTER ID
            //
            // Keep the IDs compatible with your React Native code.
            // ----------------------------------------------------

            val normalizedCharacterId =
                when (
                    characterId.trim().lowercase()
                ) {

                    "winggo" ->
                        "winggo"

                    "vijay" ->
                        "Vijay"

                    "cm_1" ->
                        "CM_1"

                    "cm_2" ->
                        "CM_2"

                    else ->
                        "winggo"
                }


            // ----------------------------------------------------
            // SERVICE INTENT
            // ----------------------------------------------------

            val intent =
                Intent(
                    reactContext,
                    WingGoForegroundService::class.java
                )


            intent.putExtra(
                "character_id",
                normalizedCharacterId
            )


            // ----------------------------------------------------
            // START SERVICE
            // ----------------------------------------------------

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                reactContext.startForegroundService(
                    intent
                )

            } else {

                reactContext.startService(
                    intent
                )
            }


            promise.resolve(
                true
            )

        } catch (e: Exception) {

            promise.reject(
                "FLOATING_WINGGO_START_FAILED",
                e
            )
        }
    }


    // ============================================================
    // STOP FLOATING WINGGO
    // ============================================================

    @ReactMethod
    fun stopFloatingWingGo(
        promise: Promise
    ) {

        try {

            val intent =
                Intent(
                    reactContext,
                    WingGoForegroundService::class.java
                )


            reactContext.stopService(
                intent
            )


            promise.resolve(
                true
            )

        } catch (e: Exception) {

            promise.reject(
                "STOP_FLOATING_WINGGO_FAILED",
                e
            )
        }
    }


    // ============================================================
    // SET FLOATING CHARACTER
    //
    // Used when floating mode is ALREADY ON.
    //
    // This changes:
    //
    // WingGO -> Vijay
    // Vijay  -> CM_1
    // CM_1   -> CM_2
    //
    // without creating another floating character.
    // ============================================================

    @ReactMethod
    fun setFloatingCharacter(
        characterId: String,
        promise: Promise
    ) {

        try {

            val normalizedCharacterId =
                when (
                    characterId.trim().lowercase()
                ) {

                    "winggo" ->
                        "winggo"

                    "vijay" ->
                        "Vijay"

                    "cm_1" ->
                        "CM_1"

                    "cm_2" ->
                        "CM_2"

                    else ->
                        "winggo"
                }


            WingGoForegroundService
                .setFloatingCharacter(
                    normalizedCharacterId
                )


            promise.resolve(
                true
            )

        } catch (e: Exception) {

            promise.reject(
                "FLOATING_CHARACTER_CHANGE_FAILED",
                e
            )
        }
    }


    // ============================================================
    // SET WINGGO SIZE
    // ============================================================

    @ReactMethod
    fun setWingGoSize(
        size: String,
        promise: Promise
    ) {

        try {

            val normalizedSize =
                size.trim().lowercase()


            // ----------------------------------------------------
            // VALIDATE SIZE
            // ----------------------------------------------------

            if (
                normalizedSize != "small" &&
                normalizedSize != "medium" &&
                normalizedSize != "big"
            ) {

                promise.reject(
                    "INVALID_WINGGO_SIZE",
                    "Size must be small, medium, or big."
                )

                return
            }


            // ----------------------------------------------------
            // SAVE SIZE
            // ----------------------------------------------------

            val preferences =
                reactContext.getSharedPreferences(
                    "winggo_settings",
                    Context.MODE_PRIVATE
                )


            preferences
                .edit()
                .putString(
                    "winggo_size",
                    normalizedSize
                )
                .apply()


            // ----------------------------------------------------
            // UPDATE RUNNING SERVICE
            // ----------------------------------------------------

            WingGoForegroundService
                .setCharacterSize(
                    normalizedSize
                )


            promise.resolve(
                true
            )

        } catch (e: Exception) {

            promise.reject(
                "WINGGO_SIZE_FAILED",
                e
            )
        }
    }


    // ============================================================
    // GET WINGGO SIZE
    // ============================================================

    @ReactMethod
    fun getWingGoSize(
        promise: Promise
    ) {

        try {

            val preferences =
                reactContext.getSharedPreferences(
                    "winggo_settings",
                    Context.MODE_PRIVATE
                )


            val size =
                preferences.getString(
                    "winggo_size",
                    "medium"
                ) ?: "medium"


            promise.resolve(
                size
            )

        } catch (e: Exception) {

            promise.reject(
                "WINGGO_SIZE_GET_FAILED",
                e
            )
        }
    }


    // ============================================================
    // INVALIDATE
    // ============================================================

    override fun invalidate() {

        instance =
            null

        super.invalidate()
    }
}