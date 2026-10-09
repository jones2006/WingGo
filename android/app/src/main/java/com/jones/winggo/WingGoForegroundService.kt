package com.jones.winggo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


class WingGoForegroundService : Service() {

    companion object {

        private const val CHANNEL_ID =
            "winggo_floating_channel"

        private const val NOTIFICATION_ID =
            1001

        private const val FLYING_TIME =
            6000L

        private const val IDLE_TIME =
            400L

        private const val LOOK_LEFT_TIME =
            300L

        private const val LOOK_RIGHT_TIME =
            300L

        private const val RETURN_TO_FLY_TIME =
            250L

        private var serviceInstance:
                WingGoForegroundService? = null


        // ========================================================
        // NOTIFICATION
        // ========================================================

        fun onNotificationReceived() {

            serviceInstance?.handler?.post {

                serviceInstance
                    ?.triggerNotificationReaction()
            }
        }


        // ========================================================
        // SIZE
        // ========================================================

        fun setCharacterSize(
            size: String
        ) {

            serviceInstance?.handler?.post {

                serviceInstance
                    ?.changeCharacterSize(size)
            }
        }


        // ========================================================
        // CHANGE FLOATING CHARACTER
        // ========================================================

        fun setFloatingCharacter(
            characterId: String
        ) {

            serviceInstance?.handler?.post {

                serviceInstance
                    ?.changeActiveCharacter(characterId)
            }
        }
    }


    // ============================================================
    // WINDOW
    // ============================================================

    private lateinit var windowManager: WindowManager

    private lateinit var overlayView:
            WingGoOverlayView

    private lateinit var layoutParams:
            WindowManager.LayoutParams


    // ============================================================
    // HANDLER
    // ============================================================

    private val handler =
        Handler(Looper.getMainLooper())


    // ============================================================
    // ACTIVE CHARACTER
    // ============================================================

    private var activeCharacterId =
        "winggo"


    // ============================================================
    // CHARACTER SIZE
    // ============================================================

    private var characterScale =
        1f

    private var characterSize =
        150

    private var characterWidth =
        166

    private var characterHeight =
        158

    private var characterOffsetX =
        8f

    private var characterOffsetY =
        6f


    // ============================================================
    // SCREEN
    // ============================================================

    private var screenWidth =
        0

    private var screenHeight =
        0


    // ============================================================
    // CHARACTERS
    // ============================================================

    private val characters =
        mutableListOf<WingGoCharacter>()


    // ============================================================
    // SERVICE STATE
    // ============================================================

    private var isRunning =
        false


    // ============================================================
    // MOVEMENT LOOP
    // ============================================================

    private val movementRunnable =
        object : Runnable {

            override fun run() {

                if (!isRunning) {
                    return
                }

                for (character in characters) {

                    updateMovement(character)
                }

                if (::overlayView.isInitialized) {

                    overlayView.invalidate()
                }

                handler.postDelayed(
                    this,
                    16L
                )
            }
        }


    // ============================================================
    // CHARACTER BITMAP HOLDER
    //
    // IMPORTANT:
    // This is outside WingGoOverlayView.
    // ============================================================

    private data class CharacterBitmaps(

        val body: Bitmap,

        val leftWing: Bitmap,

        val rightWing: Bitmap,

        val eyesOpen: Bitmap,

        val eyesClosed: Bitmap
    )


    // ============================================================
    // SERVICE CREATE
    // ============================================================

    override fun onCreate() {

        super.onCreate()

        serviceInstance = this


        // ========================================================
        // WINDOW MANAGER
        // ========================================================

        windowManager =
            getSystemService(
                Context.WINDOW_SERVICE
            ) as WindowManager


        // ========================================================
        // SCREEN SIZE
        // ========================================================

        val metrics =
            resources.displayMetrics

        screenWidth =
            metrics.widthPixels

        screenHeight =
            metrics.heightPixels


        // ========================================================
        // SAVED SIZE
        // ========================================================

        val preferences =
            getSharedPreferences(
                "winggo_settings",
                Context.MODE_PRIVATE
            )

        val savedSize =
            preferences.getString(
                "winggo_size",
                "medium"
            ) ?: "medium"


        applyCharacterSize(
            savedSize
        )


        // ========================================================
        // NOTIFICATION
        // ========================================================

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )


        // ========================================================
        // ONLY ONE CHARACTER
        // ========================================================

        if (characters.isEmpty()) {

            characters.add(
                WingGoCharacter()
            )
        }


        // ========================================================
        // START POSITION
        // ========================================================

        characters[0].posX =
            (screenWidth - characterSize) / 2f

        characters[0].posY =
            20f


        keepInsideScreen()


        // ========================================================
        // CREATE OVERLAY
        // ========================================================

        createOverlay()


        // ========================================================
        // START MOVEMENT
        // ========================================================

        isRunning = true

        handler.post(
            movementRunnable
        )
    }


    // ============================================================
    // START COMMAND
    // ============================================================

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val characterId =
            intent?.getStringExtra(
                "character_id"
            )

        if (!characterId.isNullOrEmpty()) {

            changeActiveCharacter(
                characterId
            )
        }

        return START_STICKY
    }


    // ============================================================
    // CHANGE ACTIVE CHARACTER
    // ============================================================

    private fun changeActiveCharacter(
        characterId: String
    ) {

        val normalizedId =
            when (
                characterId.trim().lowercase()
            ) {

                "winggo" ->
                    "winggo"

                "vijay" ->
                    "vijay"

                "cm_1" ->
                    "cm_1"

                "cm_2" ->
                    "cm_2"

                else ->
                    "winggo"
            }


        activeCharacterId =
            normalizedId


        // Reset blink whenever character changes

        if (characters.isNotEmpty()) {

            val character =
                characters[0]

            character.isBlinking =
                false

            character.blinkTimer =
                0L
        }


        // Reload bitmap immediately

        if (::overlayView.isInitialized) {

            overlayView
                .reloadCharacterBitmaps()

            overlayView.invalidate()
        }
    }


    // ============================================================
    // CHARACTER SIZE CHANGE
    // ============================================================

    private fun changeCharacterSize(
        size: String
    ) {

        val normalizedSize =
            when (
                size.trim().lowercase()
            ) {

                "small" ->
                    "small"

                "big" ->
                    "big"

                else ->
                    "medium"
            }


        getSharedPreferences(
            "winggo_settings",
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                "winggo_size",
                normalizedSize
            )
            .apply()


        applyCharacterSize(
            normalizedSize
        )


        if (::overlayView.isInitialized) {

            handler.post {

                try {

                    keepInsideScreen()

                    layoutParams.width =
                        characterWidth

                    layoutParams.height =
                        characterHeight

                    updateWindowPosition()

                    overlayView.invalidate()

                } catch (_: Exception) {
                }
            }
        }
    }


    // ============================================================
    // APPLY SIZE
    // ============================================================

    private fun applyCharacterSize(
        size: String
    ) {

        when (
            size.trim().lowercase()
        ) {

            "small" -> {

                characterScale =
                    0.8f

                characterSize =
                    (150f * characterScale)
                        .toInt()

                characterWidth =
                    (166f * characterScale)
                        .toInt()

                characterHeight =
                    (158f * characterScale)
                        .toInt()

                characterOffsetX =
                    8f * characterScale

                characterOffsetY =
                    6f * characterScale
            }


            "big" -> {

                characterScale =
                    1.2f

                characterSize =
                    (150f * characterScale)
                        .toInt()

                characterWidth =
                    (166f * characterScale)
                        .toInt()

                characterHeight =
                    (158f * characterScale)
                        .toInt()

                characterOffsetX =
                    8f * characterScale

                characterOffsetY =
                    6f * characterScale
            }


            else -> {

                characterScale =
                    1f

                characterSize =
                    150

                characterWidth =
                    166

                characterHeight =
                    158

                characterOffsetX =
                    8f

                characterOffsetY =
                    6f
            }
        }
    }


    // ============================================================
    // CREATE OVERLAY
    // ============================================================

    private fun createOverlay() {

        overlayView =
            WingGoOverlayView(this)


        layoutParams =
            WindowManager.LayoutParams(

                characterWidth,

                characterHeight,

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O
                ) {

                    WindowManager.LayoutParams
                        .TYPE_APPLICATION_OVERLAY

                } else {

                    WindowManager.LayoutParams
                        .TYPE_PHONE
                },

                WindowManager.LayoutParams
                    .FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams
                            .FLAG_LAYOUT_NO_LIMITS,

                PixelFormat.TRANSLUCENT
            )


        layoutParams.gravity =
            Gravity.TOP or Gravity.START


        layoutParams.x =
            (
                characters[0].posX -
                        characterOffsetX
                ).toInt()


        layoutParams.y =
            (
                characters[0].posY -
                        characterOffsetY
                ).toInt()


        windowManager.addView(
            overlayView,
            layoutParams
        )
    }


    // ============================================================
    // MOVEMENT
    // ============================================================

    private fun updateMovement(
        character: WingGoCharacter
    ) {

        // ========================================================
        // NOTIFICATION REACTION
        // ========================================================

        if (
            character.notificationReaction
        ) {

            updateNotificationReaction(
                character
            )

            updateWindowPosition()

            return
        }


        // ========================================================
        // DRAGGING
        // ========================================================

        if (
            overlayView.isDragging
        ) {

            return
        }


        // ========================================================
        // FLYING
        // ========================================================

        if (
            character.idleState == 0
        ) {

            character.posX +=
                character.velocityX

            character.posY +=
                character.velocityY

            handleEdges(
                character
            )
        }


        // ========================================================
        // IDLE TIMER
        // ========================================================

        character.idleTimer +=
            16L


        // ========================================================
        // IDLE STATE MACHINE
        // ========================================================

        when (
            character.idleState
        ) {

            // ----------------------------------------------------
            // 0 = FLYING
            // ----------------------------------------------------

            0 -> {

                if (
                    character.idleTimer >=
                    FLYING_TIME
                ) {

                    character.idleState =
                        1

                    character.idleTimer =
                        0L
                }
            }


            // ----------------------------------------------------
            // 1 = SLOW DOWN
            // ----------------------------------------------------

            1 -> {

                character.velocityX *=
                    0.93f

                character.velocityY *=
                    0.93f


                if (
                    abs(character.velocityX) <
                    0.08f &&
                    abs(character.velocityY) <
                    0.08f
                ) {

                    character.velocityX =
                        0f

                    character.velocityY =
                        0f

                    character.idleState =
                        2

                    character.idleTimer =
                        0L
                }
            }


            // ----------------------------------------------------
            // 2 = IDLE
            // ----------------------------------------------------

            2 -> {

                if (
                    character.idleTimer >=
                    IDLE_TIME
                ) {

                    character.idleState =
                        3

                    character.idleTimer =
                        0L

                    character.lookDirection =
                        -1
                }
            }


            // ----------------------------------------------------
            // 3 = LOOK LEFT
            // ----------------------------------------------------

            3 -> {

                if (
                    character.idleTimer >=
                    LOOK_LEFT_TIME
                ) {

                    character.idleState =
                        4

                    character.idleTimer =
                        0L

                    character.lookDirection =
                        1
                }
            }


            // ----------------------------------------------------
            // 4 = LOOK RIGHT
            // ----------------------------------------------------

            4 -> {

                if (
                    character.idleTimer >=
                    LOOK_RIGHT_TIME
                ) {

                    character.idleState =
                        5

                    character.idleTimer =
                        0L

                    character.lookDirection =
                        0
                }
            }


            // ----------------------------------------------------
            // 5 = RETURN TO FLYING
            // ----------------------------------------------------

            5 -> {

                if (
                    character.idleTimer >=
                    RETURN_TO_FLY_TIME
                ) {

                    character.idleState =
                        0

                    character.idleTimer =
                        0L

                    character.velocityX =
                        if (
                            Random.nextBoolean()
                        ) {
                            0.65f
                        } else {
                            -0.65f
                        }

                    character.velocityY =
                        if (
                            Random.nextBoolean()
                        ) {
                            0.4f
                        } else {
                            -0.4f
                        }
                }
            }
        }


        // ========================================================
        // WING ANIMATION
        // ========================================================

        character.wingAnimation +=
            16f * 0.015f


        if (
            character.wingAnimation >
            Math.PI.toFloat() * 2f
        ) {

            character.wingAnimation =
                0f
        }


        // ========================================================
        // BODY FLOAT
        // ========================================================

        character.bodyFloat +=
            16f * 0.004f


        if (
            character.bodyFloat >
            Math.PI.toFloat() * 2f
        ) {

            character.bodyFloat =
                0f
        }


        // ========================================================
        // BLINK
        //
        // ONLY WINGGO BLINKS
        // ========================================================

        if (
            activeCharacterId ==
            "winggo"
        ) {

            character.blinkTimer +=
                16L


            if (
                character.blinkTimer >=
                3200L &&
                !character.isBlinking
            ) {

                character.isBlinking =
                    true

                character.blinkTimer =
                    0L
            }


            if (
                character.isBlinking &&
                character.blinkTimer >=
                120L
            ) {

                character.isBlinking =
                    false

                character.blinkTimer =
                    0L
            }

        } else {

            character.isBlinking =
                false

            character.blinkTimer =
                0L
        }


        // ========================================================
        // WINDOW POSITION
        // ========================================================

        updateWindowPosition()
    }


    // ============================================================
    // EDGE BOUNCE
    // ============================================================

    private fun handleEdges(
        character: WingGoCharacter
    ) {

        val maxX =
            max(
                0,
                screenWidth -
                        characterSize
            )


        val maxY =
            max(
                0,
                screenHeight -
                        characterSize
            )


        // LEFT

        if (
            character.posX <=
            0f
        ) {

            character.posX =
                0f

            character.velocityX =
                abs(
                    character.velocityX
                )
        }


        // RIGHT

        if (
            character.posX >=
            maxX
        ) {

            character.posX =
                maxX.toFloat()

            character.velocityX =
                -abs(
                    character.velocityX
                )
        }


        // TOP

        if (
            character.posY <=
            0f
        ) {

            character.posY =
                0f

            character.velocityY =
                abs(
                    character.velocityY
                )
        }


        // BOTTOM

        if (
            character.posY >=
            maxY
        ) {

            character.posY =
                maxY.toFloat()

            character.velocityY =
                -abs(
                    character.velocityY
                )
        }
    }


    // ============================================================
    // KEEP INSIDE SCREEN
    // ============================================================

    private fun keepInsideScreen() {

        if (
            characters.isEmpty()
        ) {

            return
        }


        val maxX =
            max(
                0,
                screenWidth -
                        characterSize
            )


        val maxY =
            max(
                0,
                screenHeight -
                        characterSize
            )


        characters[0].posX =
            min(
                max(
                    0f,
                    characters[0].posX
                ),
                maxX.toFloat()
            )


        characters[0].posY =
            min(
                max(
                    0f,
                    characters[0].posY
                ),
                maxY.toFloat()
            )
    }


    // ============================================================
    // NOTIFICATION REACTION
    // ============================================================

    private fun triggerNotificationReaction() {

        if (
            !isRunning ||
            characters.isEmpty()
        ) {

            return
        }


        val character =
            characters[0]


        if (
            character.notificationReaction
        ) {

            return
        }


        character.notificationReaction =
            true

        character.notificationTimer =
            0L

        character.notificationRotation =
            0f


        character.notificationLook =
            when {

                character.velocityX > 0f ->
                    1f

                character.velocityX < 0f ->
                    -1f

                else ->
                    if (
                        Random.nextBoolean()
                    ) {
                        1f
                    } else {
                        -1f
                    }
            }


        character.idleState =
            0

        character.idleTimer =
            0L
    }


    // ============================================================
    // UPDATE NOTIFICATION REACTION
    // ============================================================

    private fun updateNotificationReaction(
        character: WingGoCharacter
    ) {

        character.notificationTimer +=
            16L


        when {

            // ----------------------------------------------------
            // 0 - 400ms
            // ----------------------------------------------------

            character.notificationTimer < 400L -> {

                character.velocityX *=
                    0.85f

                character.velocityY *=
                    0.85f

                character.notificationRotation +=
                    24f *
                            character.notificationLook

                character.wingAnimation +=
                    0.45f
            }


            // ----------------------------------------------------
            // 400 - 1200ms
            // ----------------------------------------------------

            character.notificationTimer < 1200L -> {

                character.notificationRotation +=
                    12f *
                            character.notificationLook

                character.wingAnimation +=
                    0.30f
            }


            // ----------------------------------------------------
            // 1200 - 1800ms
            // ----------------------------------------------------

            character.notificationTimer < 1800L -> {

                character.notificationRotation *=
                    0.85f

                character.wingAnimation +=
                    0.15f
            }


            // ----------------------------------------------------
            // END
            // ----------------------------------------------------

            else -> {

                character.notificationReaction =
                    false

                character.notificationTimer =
                    0L

                character.notificationRotation =
                    0f

                character.notificationLook =
                    0f


                character.velocityX =
                    if (
                        Random.nextBoolean()
                    ) {
                        0.65f
                    } else {
                        -0.65f
                    }


                character.velocityY =
                    if (
                        Random.nextBoolean()
                    ) {
                        0.4f
                    } else {
                        -0.4f
                    }
            }
        }
    }


    // ============================================================
    // WINDOW POSITION
    // ============================================================

    private fun updateWindowPosition() {

        if (
            !::layoutParams.isInitialized ||
            characters.isEmpty()
        ) {

            return
        }


        layoutParams.x =
            (
                characters[0].posX -
                        characterOffsetX
                ).toInt()


        layoutParams.y =
            (
                characters[0].posY -
                        characterOffsetY
                ).toInt()


        try {

            windowManager.updateViewLayout(
                overlayView,
                layoutParams
            )

        } catch (_: Exception) {
        }
    }


    // ============================================================
    // LOAD CHARACTER BITMAPS
    // ============================================================

    private fun loadBitmapsForCharacter(
        id: String
    ): CharacterBitmaps {

        /*
         * EXACT CHARACTER MAPPING
         *
         * winggo -> new_body.png
         * Vijay  -> vijay_body.png
         * CM_1   -> cm1_body.png
         * CM_2   -> cm2_body.png
         *
         * ALL characters use:
         *
         * new_left_wing.png
         * new_right_wing.png
         *
         * ONLY WingGO uses:
         *
         * winggo_eyes_open.png
         * winggo_eyes_closed.png
         */

        val bodyRes =
            when (id) {

                "winggo" ->
                    R.drawable.new_body

                "vijay" ->
                    R.drawable.vijay_body

                "cm_1" ->
                    R.drawable.cm1_body

                "cm_2" ->
                    R.drawable.cm2_body

                else ->
                    R.drawable.new_body
            }


        // SAME WINGS FOR EVERY CHARACTER

        val leftWingRes =
            R.drawable.new_left_wing

        val rightWingRes =
            R.drawable.new_right_wing


        // WingGO eyes only

        val eyesOpenRes =
            R.drawable.winggo_eyes_open

        val eyesClosedRes =
            R.drawable.winggo_eyes_closed


        return CharacterBitmaps(

            body =
                BitmapFactory.decodeResource(
                    resources,
                    bodyRes
                ),

            leftWing =
                BitmapFactory.decodeResource(
                    resources,
                    leftWingRes
                ),

            rightWing =
                BitmapFactory.decodeResource(
                    resources,
                    rightWingRes
                ),

            eyesOpen =
                BitmapFactory.decodeResource(
                    resources,
                    eyesOpenRes
                ),

            eyesClosed =
                BitmapFactory.decodeResource(
                    resources,
                    eyesClosedRes
                )
        )
    }


    // ============================================================
    // NOTIFICATION CHANNEL
    // ============================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "WingGo Floating Companion",
                    NotificationManager
                        .IMPORTANCE_LOW
                )


            val manager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager


            manager.createNotificationChannel(
                channel
            )
        }
    }


    // ============================================================
    // FOREGROUND NOTIFICATION
    // ============================================================

    private fun createNotification():
            Notification {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "WingGo is floating"
                )
                .setContentText(
                    "Your little companion is with you."
                )
                .setSmallIcon(
                    android.R.drawable
                        .ic_dialog_info
                )
                .setOngoing(true)
                .build()

        } else {

            Notification.Builder(
                this
            )
                .setContentTitle(
                    "WingGo is floating"
                )
                .setContentText(
                    "Your little companion is with you."
                )
                .setSmallIcon(
                    android.R.drawable
                        .ic_dialog_info
                )
                .setOngoing(true)
                .build()
        }
    }


    // ============================================================
    // OVERLAY VIEW
    // ============================================================

    inner class WingGoOverlayView(
        context: Context
    ) : View(context) {


        // ========================================================
        // PAINT
        // ========================================================

        private val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )


        // ========================================================
        // BITMAPS
        // ========================================================

        private var body: Bitmap

        private var leftWing: Bitmap

        private var rightWing: Bitmap

        private var eyesOpen: Bitmap

        private var eyesClosed: Bitmap


        // ========================================================
        // INIT
        // ========================================================

        init {

            val bitmaps =
                loadBitmapsForCharacter(
                    activeCharacterId
                )


            body =
                bitmaps.body

            leftWing =
                bitmaps.leftWing

            rightWing =
                bitmaps.rightWing

            eyesOpen =
                bitmaps.eyesOpen

            eyesClosed =
                bitmaps.eyesClosed
        }


        // ========================================================
        // RELOAD CHARACTER
        // ========================================================

        fun reloadCharacterBitmaps() {

            val bitmaps =
                loadBitmapsForCharacter(
                    activeCharacterId
                )


            body =
                bitmaps.body

            leftWing =
                bitmaps.leftWing

            rightWing =
                bitmaps.rightWing

            eyesOpen =
                bitmaps.eyesOpen

            eyesClosed =
                bitmaps.eyesClosed
        }


        // ========================================================
        // BODY SIZE
        // ========================================================

        private val bodyWidth =
            165f

        private val bodyHeight =
            150f

        private val bodyX =
            0f

        private val bodyY =
            0f


        // ========================================================
        // LEFT WING
        // ========================================================

        private val leftWingWidth =
            75f

        private val leftWingHeight =
            75f

        private val leftWingX =
            -12f

        private val leftWingY =
            20f


        // ========================================================
        // RIGHT WING
        // ========================================================

        private val rightWingWidth =
            75f

        private val rightWingHeight =
            75f

        private val rightWingRight =
            -22f

        private val rightWingY =
            20f


        // ========================================================
        // EYES
        // ========================================================

        private val eyesWidth =
            100f

        private val eyesHeight =
            100f

        private val eyesX =
            0f

        private val eyesY =
            -26f


        // ========================================================
        // DRAG
        // ========================================================

        var isDragging =
            false
            private set


        private var lastTouchX =
            0f

        private var lastTouchY =
            0f

        private var lastTime =
            0L

        private var dragVelocityX =
            0f

        private var dragVelocityY =
            0f


        // ========================================================
        // DRAW CHARACTER
        // ========================================================

        private fun drawCharacter(
            canvas: Canvas,
            character: WingGoCharacter
        ) {

            val baseCharacterSize =
                characterSize /
                        characterScale


            val drawX =
                characterOffsetX

            val drawY =
                characterOffsetY


            // ====================================================
            // BODY
            // ====================================================

            val bodyRect =
                RectF(

                    drawX + bodyX,

                    drawY + bodyY,

                    drawX +
                            bodyX +
                            bodyWidth,

                    drawY +
                            bodyY +
                            bodyHeight
                )


            // ====================================================
            // LEFT WING
            // ====================================================

            val leftWingRect =
                RectF(

                    drawX +
                            leftWingX,

                    drawY +
                            leftWingY,

                    drawX +
                            leftWingX +
                            leftWingWidth,

                    drawY +
                            leftWingY +
                            leftWingHeight
                )


            // ====================================================
            // RIGHT WING
            // ====================================================

            val rightWingX =
                drawX +
                        baseCharacterSize -
                        rightWingWidth -
                        rightWingRight


            val rightWingRect =
                RectF(

                    rightWingX,

                    drawY +
                            rightWingY,

                    rightWingX +
                            rightWingWidth,

                    drawY +
                            rightWingY +
                            rightWingHeight
                )


            // ====================================================
            // EYES
            // ====================================================

            val eyeX =
                drawX +
                        (
                            (
                                baseCharacterSize -
                                        eyesWidth
                                ) / 2f
                            ) +
                        eyesX +
                        getLookOffset(character)


            val eyesRect =
                RectF(

                    eyeX,

                    drawY +
                            eyesY,

                    eyeX +
                            eyesWidth,

                    drawY +
                            eyesY +
                            eyesHeight
                )


            // ====================================================
            // FLOATING ANIMATION
            // ====================================================

            val floatingOffset =
                kotlin.math.sin(
                    character.bodyFloat
                        .toDouble()
                ).toFloat() *
                        2f


            // ====================================================
            // WING FLAP
            // ====================================================

            val wingFlap =
                kotlin.math.sin(
                    character.wingAnimation
                        .toDouble()
                ).toFloat() *
                        18f


            val leftWingRotation =
                -18f +
                        wingFlap


            val rightWingRotation =
                18f -
                        wingFlap


            // ====================================================
            // CANVAS
            // ====================================================

            canvas.save()


            canvas.scale(
                characterScale,
                characterScale
            )


            canvas.translate(
                0f,
                floatingOffset
            )


            // ====================================================
            // NOTIFICATION ROTATION
            // ====================================================

            if (
                character.notificationReaction
            ) {

                val centerX =
                    drawX +
                            baseCharacterSize /
                            2f

                val centerY =
                    drawY +
                            baseCharacterSize /
                            2f


                canvas.rotate(
                    character.notificationRotation,
                    centerX,
                    centerY
                )
            }


            // ====================================================
            // WINGS
            // ====================================================

            drawBitmapRotated(

                canvas,

                leftWing,

                leftWingRect,

                leftWingRotation
            )


            drawBitmapRotated(

                canvas,

                rightWing,

                rightWingRect,

                rightWingRotation
            )


            // ====================================================
            // BODY
            // ====================================================

            canvas.drawBitmap(

                body,

                null,

                bodyRect,

                paint
            )


            // ====================================================
            // EYES
            //
            // ONLY WINGGO HAS EYES/Blink
            // ====================================================

            if (
                activeCharacterId ==
                "winggo"
            ) {

                val eyeBitmap =
                    if (
                        character.isBlinking
                    ) {

                        eyesClosed

                    } else {

                        eyesOpen
                    }


                canvas.drawBitmap(

                    eyeBitmap,

                    null,

                    eyesRect,

                    paint
                )
            }


            // ====================================================
            // RESTORE
            // ====================================================

            canvas.restore()
        }


        // ========================================================
        // DRAW
        // ========================================================

        override fun onDraw(
            canvas: Canvas
        ) {

            super.onDraw(canvas)


            for (
                character in characters
            ) {

                drawCharacter(
                    canvas,
                    character
                )
            }
        }


        // ========================================================
        // LOOK OFFSET
        // ========================================================

        private fun getLookOffset(
            character: WingGoCharacter
        ): Float {

            return when {

                character.notificationReaction ->

                    character.notificationLook *
                            3f


                character.idleState == 3 ->

                    -3f


                character.idleState == 4 ->

                    3f


                else ->

                    0f
            }
        }


        // ========================================================
        // ROTATED BITMAP
        // ========================================================

        private fun drawBitmapRotated(
            canvas: Canvas,
            bitmap: Bitmap,
            rect: RectF,
            rotation: Float
        ) {

            canvas.save()


            canvas.rotate(
                rotation,
                rect.centerX(),
                rect.centerY()
            )


            canvas.drawBitmap(
                bitmap,
                null,
                rect,
                paint
            )


            canvas.restore()
        }


        // ========================================================
        // TOUCH / DRAG
        // ========================================================

        override fun onTouchEvent(
            event: MotionEvent
        ): Boolean {

            if (
                characters.isEmpty()
            ) {

                return true
            }


            val activeCharacter =
                characters[0]


            when (
                event.actionMasked
            ) {


                // ==================================================
                // DOWN
                // ==================================================

                MotionEvent.ACTION_DOWN -> {

                    isDragging =
                        true


                    activeCharacter.idleState =
                        0

                    activeCharacter.idleTimer =
                        0L


                    activeCharacter.velocityX =
                        0f

                    activeCharacter.velocityY =
                        0f


                    lastTouchX =
                        event.rawX

                    lastTouchY =
                        event.rawY

                    lastTime =
                        System.currentTimeMillis()


                    dragVelocityX =
                        0f

                    dragVelocityY =
                        0f


                    return true
                }


                // ==================================================
                // MOVE
                // ==================================================

                MotionEvent.ACTION_MOVE -> {

                    if (
                        !isDragging
                    ) {

                        return true
                    }


                    val currentX =
                        event.rawX

                    val currentY =
                        event.rawY


                    val dx =
                        currentX -
                                lastTouchX

                    val dy =
                        currentY -
                                lastTouchY


                    val now =
                        System.currentTimeMillis()


                    val dt =
                        max(
                            1L,
                            now -
                                    lastTime
                        )


                    dragVelocityX =
                        dx /
                                dt *
                                16f


                    dragVelocityY =
                        dy /
                                dt *
                                16f


                    activeCharacter.posX +=
                        dx

                    activeCharacter.posY +=
                        dy


                    keepInsideScreen()

                    updateWindowPosition()


                    lastTouchX =
                        currentX

                    lastTouchY =
                        currentY

                    lastTime =
                        now


                    return true
                }


                // ==================================================
                // UP / CANCEL
                // ==================================================

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    isDragging =
                        false


                    activeCharacter.velocityX =
                        dragVelocityX

                    activeCharacter.velocityY =
                        dragVelocityY


                    if (
                        abs(
                            activeCharacter.velocityX
                        ) < 0.45f
                    ) {

                        activeCharacter.velocityX =
                            if (
                                dragVelocityX >= 0
                            ) {

                                0.45f

                            } else {

                                -0.45f
                            }
                    }
                    if (
                        abs(
                            activeCharacter.velocityY
                        ) < 0.3f
                    ) {
                        activeCharacter.velocityY =
                            if (
                                dragVelocityY >= 0
                            ) {
                                0.3f
                            } else {
                                -0.3f
                            }
                    }
                    activeCharacter.idleState = 0
                    activeCharacter.idleTimer = 0L
                    return true
                }
            }
            return true
        }
    }


    // ============================================================
    // DESTROY
    // ============================================================

    override fun onDestroy() {

        isRunning =
            false


        handler.removeCallbacks(
            movementRunnable
        )


        if (
            ::overlayView.isInitialized
        ) {

            try {

                windowManager.removeView(
                    overlayView
                )

            } catch (_: Exception) {
            }
        }


        serviceInstance =
            null


        super.onDestroy()
    }


    // ============================================================
    // BIND
    // ============================================================

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}