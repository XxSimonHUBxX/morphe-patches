/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.youtube.misc.queue

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/youtube/patches/queue/VideoQueuePatch;"

@Suppress("unused")
val videoQueuePatch = bytecodePatch(
    name = "YouTube - Video queue",
    description = "Adds an 'Add to queue list' option in the video three-dot flyout menu. " +
        "The queue is temporary and clears when the app is closed.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_YOUTUBE)

    // Hook the flyout menu builder to inject "Add to queue list"
    execute {
        VideoFlyoutMenuItemFingerprint.method.apply {
            val insertIndex = implementation!!.instructions.size - 1
            addInstruction(
                insertIndex,
                "invoke-static { p0, p1 }, " +
                    "$EXTENSION_CLASS_DESCRIPTOR->addQueueMenuItem(" +
                    "Ljava/lang/Object;Ljava/lang/Object;)V",
            )
        }
    }

    // Hook video-end event to auto-play next queued video
    execute {
        VideoEndFingerprint.method.apply {
            val insertIndex = implementation!!.instructions.size - 1
            addInstruction(
                insertIndex,
                "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->onVideoEnd()V",
            )
        }
    }

    // Capture the navigation method reference at class-load time
    execute {
        VideoNavigationFingerprint.method.apply {
            addInstruction(
                0,
                "invoke-static { p0 }, " +
                    "$EXTENSION_CLASS_DESCRIPTOR->resolveNavigationMethod(" +
                    "Ljava/lang/reflect/Method;)V",
            )
        }
    }
}
