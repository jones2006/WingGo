package com.jones.winggo

class WingGoCharacter {

    // Position
    var posX = 0f
    var posY = 20f

    // Velocity
    var velocityX = 0.7f
    var velocityY = 0.45f

    // Idle state
    var idleState = 0
    var idleTimer = 0L
    var lookDirection = 0

    // Animation
    var wingAnimation = 0f
    var bodyFloat = 0f

    // Blink
    var blinkTimer = 0L
    var isBlinking = false

    // Notification reaction
    var notificationReaction = false
    var notificationTimer = 0L
    var notificationLook = 0f
    var notificationRotation = 0f
}