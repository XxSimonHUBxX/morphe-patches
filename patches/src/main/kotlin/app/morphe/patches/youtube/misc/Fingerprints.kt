/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.youtube.misc.queue

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Matches the method that builds the video context (flyout) menu.
 * Called when the user taps the three-dot menu on a video.
 */
internal object VideoFlyoutMenuItemFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "L"),
    strings = listOf("menu_item_id")
)

/**
 * Matches the method fired when a video finishes playback.
 */
internal object VideoEndFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("on_end_screen")
)

/**
 * Matches YouTube's internal navigate-to-video method.
 */
internal object VideoNavigationFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("watch?v=")
)
