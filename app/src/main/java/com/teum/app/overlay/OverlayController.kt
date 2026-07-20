package com.teum.app.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.teum.app.debug.TeumLogger

class OverlayController(context: Context) {
    private val overlayContext = context
    private val windowManager = overlayContext.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null

    val overlayShowing: Boolean
        get() = overlayView?.isAttachedToWindow == true

    fun showIntentCheck(
        packageName: String,
        mode: IntentCheckMode = IntentCheckMode.NORMAL,
        reopenGapMillis: Long? = null,
        debugSessionId: Long? = null,
        onIntentConfirmed: (IntentChoice, Long) -> Unit,
        onCloseNowSelected: () -> Unit,
        onDismissed: () -> Unit
    ) {
        removeOverlayIfAttached()

        val view = createIntentCheckView(
            packageName = packageName,
            mode = mode,
            reopenGapMillis = reopenGapMillis,
            debugSessionId = debugSessionId,
            onIntentConfirmed = { choice, targetDurationMillis ->
                Log.d(TAG, "intent selected: ${choice.name} target=$targetDurationMillis for $packageName")
                onIntentConfirmed(choice, targetDurationMillis)
                dismiss(onDismissed = onDismissed, reason = "intent_confirmed")
            },
            onCloseNowSelected = {
                Log.d(TAG, "close now selected for $packageName")
                onCloseNowSelected()
                dismiss(onDismissed = onDismissed, reason = "close_now")
            }
        )

        addOverlayView(view, "intent check")
        TeumLogger.overlay("SHOW_INTENT", "package=$packageName mode=$mode")
    }

    fun showSessionBrake(
        packageName: String,
        elapsedMillis: Long,
        targetDurationMillis: Long,
        debugSessionId: Long? = null,
        onBrakeChoice: (BrakeChoice) -> Unit
    ) {
        removeOverlayIfAttached()

        val view = createSessionBrakeView(
            packageName = packageName,
            elapsedMillis = elapsedMillis,
            targetDurationMillis = targetDurationMillis,
            debugSessionId = debugSessionId,
            onBrakeChoice = { choice ->
                Log.d(BRAKE_TAG, "choice selected ${choice.name} package=$packageName")
                onBrakeChoice(choice)
                dismiss(reason = "brake_choice")
            }
        )

        addOverlayView(view, "session brake")
        TeumLogger.overlay("SHOW_BRAKE", "package=$packageName")
        Log.d(
            BRAKE_TAG,
            "brake shown package=$packageName elapsed=$elapsedMillis target=$targetDurationMillis"
        )
    }

    fun dismiss(onDismissed: () -> Unit = {}, reason: String = "unspecified") {
        val hadOverlay = overlayView != null
        removeOverlayIfAttached()
        if (hadOverlay) {
            Log.d(TAG, "overlay dismissed")
            TeumLogger.overlay("DISMISS", "reason=$reason")
            onDismissed()
        }
    }

    fun removeOverlayIfAttached() {
        val currentView = overlayView ?: return
        try {
            if (currentView.isAttachedToWindow) {
                windowManager.removeView(currentView)
            }
        } catch (exception: RuntimeException) {
            Log.w(TAG, "failed to remove overlay", exception)
        } finally {
            overlayView = null
        }
    }

    private fun addOverlayView(view: View, overlayName: String) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            overlayView = view
            windowManager.addView(view, params)
        } catch (exception: RuntimeException) {
            overlayView = null
            Log.e(TAG, "failed to show $overlayName overlay", exception)
        }
    }

    private fun createIntentCheckView(
        packageName: String,
        mode: IntentCheckMode,
        reopenGapMillis: Long?,
        debugSessionId: Long?,
        onIntentConfirmed: (IntentChoice, Long) -> Unit,
        onCloseNowSelected: () -> Unit
    ): View {
        var selectedIntentChoice: IntentChoice? = null
        var selectedDurationChoice: TargetDurationChoice? = null

        val card = createCardContent()

        card.addView(
            textView(
                text = "\uD2C8",
                textSize = 28f,
                textColor = Color.rgb(30, 38, 35),
                style = android.graphics.Typeface.BOLD
            )
        )

        if (mode == IntentCheckMode.FAST_REOPEN) {
            card.addView(
                textView(
                    text = "\uBC29\uAE08 \uC571\uC744 \uB098\uAC14\uB2E4\uAC00 \uB2E4\uC2DC \uC5F4\uC5C8\uC5B4\uC694.",
                    textSize = 20f,
                    textColor = Color.rgb(30, 38, 35),
                    topMargin = 18
                )
            )
            card.addView(
                textView(
                    text = "\uB2E4\uC2DC \uB4E4\uC5B4\uC628 \uC774\uC720\uAC00 \uC788\uB098\uC694?",
                    textSize = 16f,
                    textColor = Color.rgb(63, 73, 69),
                    topMargin = 10
                )
            )
            card.addView(
                textView(
                    text = "\uC774\uC804 \uC885\uB8CC \uD6C4 ${formatDuration(reopenGapMillis ?: 0L)} \uB9CC\uC5D0 \uB2E4\uC2DC \uC9C4\uC785",
                    textSize = 14f,
                    textColor = Color.rgb(89, 98, 94),
                    topMargin = 10
                )
            )
        } else {
            card.addView(
                textView(
                    text = "\uC9C0\uAE08 \uBAA9\uC801\uC744 \uB5A0\uC62C\uB838\uC5B4\uC694?",
                    textSize = 20f,
                    textColor = Color.rgb(30, 38, 35),
                    topMargin = 18
                )
            )
        }

        card.addView(
            textView(
                text = "\uAC10\uC9C0\uB41C \uC571: $packageName",
                textSize = 14f,
                textColor = Color.rgb(89, 98, 94),
                topMargin = 10
            )
        )

        card.addView(sectionLabel(text = "\uC0AC\uC6A9 \uBAA9\uC801", topMargin = 18))

        val intentButtons = mutableMapOf<IntentChoice, Button>()
        IntentChoice.entries.forEach { choice ->
            val button = choiceButton(text = choice.label)
            intentButtons[choice] = button
            card.addView(button)
        }

        card.addView(sectionLabel(text = "\uBAA9\uD45C \uC0AC\uC6A9 \uC2DC\uAC04", topMargin = 20))

        val durationButtons = mutableMapOf<TargetDurationChoice, Button>()
        TargetDurationChoice.entries.forEach { choice ->
            val button = choiceButton(text = choice.label)
            durationButtons[choice] = button
            card.addView(button)
        }

        val startButton = primaryButton(text = "\uC2DC\uC791").apply {
            isEnabled = false
        }

        fun refreshStartButton() {
            val intentChoice = selectedIntentChoice
            val hasRequiredChoices = intentChoice != null &&
                (intentChoice == IntentChoice.CLOSE_NOW || selectedDurationChoice != null)
            startButton.isEnabled = hasRequiredChoices
        }

        intentButtons.forEach { (choice, button) ->
            button.setOnClickListener {
                selectedIntentChoice = choice
                Log.d(TAG, "intent choice selected: ${choice.name}")
                debugSessionId?.let {
                    TeumLogger.session(
                        debugSessionId = it,
                        event = "INTENT_SELECTED",
                        detail = "intent=${choice.name}"
                    )
                }
                updateSelectionButtons(intentButtons, selectedIntentChoice)
                refreshStartButton()
            }
        }
        durationButtons.forEach { (choice, button) ->
            button.setOnClickListener {
                selectedDurationChoice = choice
                Log.d(TAG, "target duration selected: ${choice.durationMillis}")
                debugSessionId?.let {
                    TeumLogger.session(
                        debugSessionId = it,
                        event = "TARGET_SELECTED",
                        detail = "target=${choice.durationMillis}"
                    )
                }
                updateSelectionButtons(durationButtons, selectedDurationChoice)
                refreshStartButton()
            }
        }

        startButton.setOnClickListener {
            val intentChoice = selectedIntentChoice ?: return@setOnClickListener
            if (intentChoice == IntentChoice.CLOSE_NOW) {
                onCloseNowSelected()
                return@setOnClickListener
            }

            val targetDurationMillis = selectedDurationChoice?.durationMillis ?: return@setOnClickListener
            Log.d(TAG, "start clicked intent=${intentChoice.name} target=$targetDurationMillis package=$packageName")
            onIntentConfirmed(intentChoice, targetDurationMillis)
        }

        card.addView(startButton)

        card.addView(
            textView(
                text = "\uC774 \uD654\uBA74\uC740 \uC0AC\uC6A9\uC790\uAC00 \uC9C0\uC815\uD55C \uAD00\uB9AC \uB300\uC0C1 \uC571\uC5D0\uC11C\uB9CC \uD45C\uC2DC\uB429\uB2C8\uB2E4.",
                textSize = 12f,
                textColor = Color.rgb(98, 106, 102),
                topMargin = 18
            )
        )

        return createOverlayRoot(card)
    }

    private fun createSessionBrakeView(
        packageName: String,
        elapsedMillis: Long,
        targetDurationMillis: Long,
        debugSessionId: Long?,
        onBrakeChoice: (BrakeChoice) -> Unit
    ): View {
        val card = createCardContent()

        card.addView(
            textView(
                text = "\uD2C8",
                textSize = 28f,
                textColor = Color.rgb(30, 38, 35),
                style = android.graphics.Typeface.BOLD
            )
        )
        card.addView(
            textView(
                text = "\uC57D\uC18D\uD55C \uC2DC\uAC04\uC774 \uC9C0\uB0AC\uC5B4\uC694. \uACC4\uC18D \uC0AC\uC6A9\uD560\uAE4C\uC694?",
                textSize = 20f,
                textColor = Color.rgb(30, 38, 35),
                topMargin = 18
            )
        )
        card.addView(
            textView(
                text = "\uAC10\uC9C0\uB41C \uC571: $packageName",
                textSize = 14f,
                textColor = Color.rgb(89, 98, 94),
                topMargin = 14
            )
        )
        card.addView(
            textView(
                text = "\uC0AC\uC6A9 \uC2DC\uAC04: ${formatDuration(elapsedMillis)}",
                textSize = 14f,
                textColor = Color.rgb(89, 98, 94),
                topMargin = 6
            )
        )
        card.addView(
            textView(
                text = "\uBAA9\uD45C \uC2DC\uAC04: ${formatDuration(targetDurationMillis)}",
                textSize = 14f,
                textColor = Color.rgb(89, 98, 94),
                topMargin = 6
            )
        )

        card.addView(sectionLabel(text = "\uC120\uD0DD", topMargin = 20))

        BrakeChoice.entries.forEach { choice ->
            val button = if (choice == BrakeChoice.END_NOW) {
                primaryButton(text = choice.label)
            } else {
                choiceButton(text = choice.label)
            }
            button.setOnClickListener {
                debugSessionId?.let {
                    TeumLogger.session(
                        debugSessionId = it,
                        event = "BRAKE_CHOICE",
                        detail = "choice=${choice.name}"
                    )
                }
                onBrakeChoice(choice)
            }
            card.addView(button)
        }

        return createOverlayRoot(card)
    }

    private fun createOverlayRoot(card: LinearLayout): View {
        val root = LinearLayout(overlayContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(36), dp(24), dp(48))
            setBackgroundColor(Color.argb(190, 12, 18, 18))
        }

        val cardContainer = FrameLayout(overlayContext).apply {
            background = roundedBackground(Color.WHITE, dp(28).toFloat())
            clipToOutline = false
        }

        val scrollView = ScrollView(overlayContext).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        scrollView.addView(
            card,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        cardContainer.addView(
            scrollView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            cardContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                maxCardHeightPx()
            ).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
            }
        )

        return root
    }

    private fun createCardContent(): LinearLayout {
        return LinearLayout(overlayContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(30), dp(28), dp(28))
        }
    }

    private fun sectionLabel(text: String, topMargin: Int): TextView {
        return textView(
            text = text,
            textSize = 15f,
            textColor = Color.rgb(30, 38, 35),
            topMargin = topMargin,
            style = android.graphics.Typeface.BOLD
        )
    }

    private fun textView(
        text: String,
        textSize: Float,
        textColor: Int,
        topMargin: Int = 0,
        style: Int = android.graphics.Typeface.NORMAL
    ): TextView {
        return TextView(overlayContext).apply {
            this.text = text
            this.textSize = textSize
            setTextColor(textColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD.takeIf { style == android.graphics.Typeface.BOLD }
                ?: android.graphics.Typeface.DEFAULT
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                this.topMargin = dp(topMargin)
            }
        }
    }

    private fun choiceButton(text: String): Button {
        return Button(overlayContext).apply {
            this.text = text
            isAllCaps = false
            setTextColor(Color.rgb(30, 38, 35))
            background = roundedBackground(Color.rgb(238, 244, 241), dp(18).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        }
    }

    private fun primaryButton(text: String): Button {
        return Button(overlayContext).apply {
            this.text = text
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBackground(Color.rgb(47, 111, 102), dp(18).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
        }
    }

    private fun <T> updateSelectionButtons(buttons: Map<T, Button>, selected: T?) {
        buttons.forEach { (choice, button) ->
            val isSelected = choice == selected
            button.setTextColor(if (isSelected) Color.WHITE else Color.rgb(30, 38, 35))
            button.background = roundedBackground(
                color = if (isSelected) Color.rgb(47, 111, 102) else Color.rgb(238, 244, 241),
                radius = dp(18).toFloat()
            )
        }
    }

    private fun roundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (seconds == 0L) {
            "${minutes}\uBD84"
        } else {
            "${minutes}\uBD84 ${seconds}\uCD08"
        }
    }

    private fun dp(value: Int): Int {
        return (value * overlayContext.resources.displayMetrics.density).toInt()
    }

    private fun maxCardHeightPx(): Int {
        return (overlayContext.resources.displayMetrics.heightPixels * 0.85f).toInt()
    }

    companion object {
        private const val TAG = "TeumOverlay"
        private const val BRAKE_TAG = "TeumBrake"
    }
}
