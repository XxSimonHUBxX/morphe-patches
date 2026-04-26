/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.queue;

import static app.morphe.extension.shared.StringRef.str;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;

/**
 * Runtime extension for the Video Queue patch.
 *
 * Maintains an in-memory queue of video IDs (cleared on process death).
 * Injects "Add to queue list" into YouTube's flyout menu.
 * Auto-plays the next queued video when the current one ends.
 */
@SuppressWarnings({"unused", "ConstantConditions"})
public final class VideoQueuePatch {

    // In-memory, session-only video queue. Head = next to play.
    private static final Deque<String> queue = new ArrayDeque<>();

    // Reference to YouTube's navigate-to-video method, captured once at class-load time.
    @Nullable
    private static volatile Method navigationMethod = null;

    /**
     * Called once when YouTube's navigation method is first loaded.
     * Stores the method reference for later use in playNext().
     */
    public static void resolveNavigationMethod(@Nullable final Method method) {
        if (method != null) {
            method.setAccessible(true);
            navigationMethod = method;
        }
    }

    /**
     * Injected at the end of YouTube's flyout menu builder.
     * Appends "Add to queue list" to the video context menu.
     */
    public static void addQueueMenuItem(
            @NonNull final Object menuBuilderContext,
            @NonNull final Object videoContext
    ) {
        try {
            final String videoId = extractVideoId(videoContext);
            if (videoId == null || videoId.isEmpty()) return;

            final Method addItemMethod = findAddItemMethod(menuBuilderContext.getClass());
            if (addItemMethod == null) {
                Logger.printDebug(() -> "VideoQueuePatch: could not locate addItem method");
                return;
            }

            addItemMethod.invoke(
                    menuBuilderContext,
                    0, // no icon
                    "Add to queue list",
                    (Runnable) () -> addToQueue(videoId)
            );

        } catch (final Exception ex) {
            Logger.printException(() -> "VideoQueuePatch: addQueueMenuItem failed", ex);
        }
    }

    /**
     * Appends a video ID to the queue and shows a confirmation toast.
     */
    public static void addToQueue(@NonNull final String videoId) {
        queue.addLast(videoId);
        Utils.showToastShort("Added to queue (" + queue.size() + " videos)");
        Logger.printDebug(() -> "VideoQueuePatch: queued " + videoId + " (size=" + queue.size() + ")");
    }

    /**
     * Returns the next video ID from the queue, or null if empty.
     */
    @Nullable
    public static String pollNext() {
        return queue.pollFirst();
    }

    /**
     * Returns true if there is at least one video in the queue.
     */
    public static boolean hasNext() {
        return !queue.isEmpty();
    }

    /**
     * Clears the entire queue.
     */
    public static void clearQueue() {
        queue.clear();
        Logger.printDebug(() -> "VideoQueuePatch: queue cleared");
    }

    /**
     * Injected by the Kotlin patch at YouTube's video-end callback.
     * Plays the next video in the queue if one exists.
     */
    public static void onVideoEnd() {
        if (!hasNext()) return;
        playNext();
    }

    private static void playNext() {
        final String nextId = pollNext();
        if (nextId == null) return;

        try {
            if (navigationMethod != null) {
                navigationMethod.invoke(null, nextId);
                Logger.printDebug(() -> "VideoQueuePatch: playing next -> " + nextId);
            } else {
                Logger.printException(() -> "VideoQueuePatch: navigationMethod is null");
            }
        } catch (final Exception ex) {
            Logger.printException(() -> "VideoQueuePatch: playNext failed for " + nextId, ex);
        }
    }

    /**
     * Extracts the video ID (11 chars) from YouTube's opaque video-context object via reflection.
     */
    @Nullable
    private static String extractVideoId(@NonNull final Object videoContext) {
        try {
            for (final java.lang.reflect.Field field : videoContext.getClass().getDeclaredFields()) {
                if (field.getType() != String.class) continue;
                field.setAccessible(true);
                final String value = (String) field.get(videoContext);
                if (value != null && value.length() == 11) return value;
            }
        } catch (final Exception ex) {
            Logger.printException(() -> "VideoQueuePatch: extractVideoId failed", ex);
        }
        return null;
    }

    /**
     * Finds YouTube's menu-item factory by its stable parameter signature: (int, CharSequence, Runnable).
     */
    @Nullable
    private static Method findAddItemMethod(@NonNull final Class<?> builderClass) {
        for (final Method method : builderClass.getDeclaredMethods()) {
            final Class<?>[] params = method.getParameterTypes();
            if (params.length == 3
                    && params[0] == int.class
                    && params[1] == CharSequence.class
                    && params[2] == Runnable.class) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
