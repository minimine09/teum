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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.teum.app.debug.TeumLogger
import com.teum.app.session.OutcomeType
import com.teum.app.ui.intervention.IntentCheckScreen
import com.teum.app.ui.intervention.ReopenCheckScreen
import com.teum.app.ui.intervention.SessionBrakeScreen
import com.teum.app.ui.theme.TeumTheme

class OverlayController(context: Context) {
    private val overlayContext = context
    private val windowManager = overlayContext.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null
    private var currentOverlayType: OverlayType? = null
    private var currentOverlayOwner: OverlayViewTreeOwner? = null

    val overlayShowing: Boolean
        get() = overlayView?.isAttachedToWindow == true

    val currentOverlayName: String
        get() = currentOverlayType?.name ?: "NONE"

    fun showIntentCheck(
        packageName: String,
        mode: IntentCheckMode = IntentCheckMode.NORMAL,
        reopenGapMillis: Long? = null,
        debugSessionId: Long? = null,
        source: String = "target_enter",
        onIntentConfirmed: (IntentChoice, Long) -> Unit,
        onCloseNowSelected: () -> Unit,
        onDismissed: () -> Unit
    ) {
        removeOverlayIfAttached()

        val view = createIntentCheckComposeView(
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

        addOverlayView(view, "intent check", OverlayType.INTENT_CHECK)
        TeumLogger.overlay("SHOW_INTENT", "package=$packageName mode=$mode source=$source")
    }

    fun showSessionBrake(
        packageName: String,
        elapsedMillis: Long,
        targetDurationMillis: Long,
        debugSessionId: Long? = null,
        source: String = "session_brake",
        onBrakeChoice: (BrakeChoice) -> Unit,
        onExtendDurationSelected: (Long) -> Unit
    ) {
        removeOverlayIfAttached()

        val view = createSessionBrakeComposeView(
            packageName = packageName,
            elapsedMillis = elapsedMillis,
            targetDurationMillis = targetDurationMillis,
            debugSessionId = debugSessionId,
            onBrakeChoice = { choice ->
                Log.d(BRAKE_TAG, "choice selected ${choice.name} package=$packageName")
                onBrakeChoice(choice)
                dismiss(reason = "brake_choice")
            },
            onExtendDurationSelected = { durationMillis ->
                Log.d(BRAKE_TAG, "choice selected ${BrakeChoice.EXTEND_3_MIN.name} package=$packageName duration=$durationMillis")
                onExtendDurationSelected(durationMillis)
                dismiss(reason = "brake_choice")
            }
        )

        addOverlayView(view, "session brake", OverlayType.SESSION_BRAKE)
        TeumLogger.overlay("SHOW_BRAKE", "package=$packageName source=$source")
        Log.d(
            BRAKE_TAG,
            "brake shown package=$packageName elapsed=$elapsedMillis target=$targetDurationMillis"
        )
    }

    fun showOutcomeCheck(
        packageName: String,
        debugSessionId: Long,
        durationMillis: Long,
        intentChoice: IntentChoice,
        source: String = "target_exit",
        onOutcomeSelected: (OutcomeType) -> Unit,
        onDismissedWithoutChoice: () -> Unit
    ) {
        removeOverlayIfAttached()

        val view = createOutcomeCheckView(
            packageName = packageName,
            durationMillis = durationMillis,
            onOutcomeSelected = { outcomeType ->
                try {
                    onOutcomeSelected(outcomeType)
                } finally {
                    dismiss(reason = "outcome_selected")
                }
            },
            onDismissedWithoutChoice = {
                try {
                    onDismissedWithoutChoice()
                } finally {
                    dismiss(reason = "outcome_dismissed")
                }
            }
        )

        addOverlayView(view, "outcome check", OverlayType.OUTCOME_CHECK)
        TeumLogger.overlay(
            event = "SHOW_OUTCOME",
            detail = "package=$packageName source=$source session=S#$debugSessionId duration=$durationMillis intent=${intentChoice.name}"
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
            currentOverlayType = null
            currentOverlayOwner?.onDestroy()
            currentOverlayOwner = null
        }
    }

    private fun addOverlayView(view: View, overlayName: String, overlayType: OverlayType) {
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
            currentOverlayType = overlayType
            windowManager.addView(view, params)
        } catch (exception: RuntimeException) {
            overlayView = null
            currentOverlayType = null
            currentOverlayOwner?.onDestroy()
            currentOverlayOwner = null
            Log.e(TAG, "failed to show $overlayName overlay", exception)
        }
    }

    private fun createIntentCheckComposeView(
        packageName: String,
        mode: IntentCheckMode,
        reopenGapMillis: Long?,
        debugSessionId: Long?,
        onIntentConfirmed: (IntentChoice, Long) -> Unit,
        onCloseNowSelected: () -> Unit
    ): View {
        val owner = OverlayViewTreeOwner().apply {
            onCreate()
            onStart()
            onResume()
        }
        currentOverlayOwner = owner

        return ComposeView(overlayContext).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                var selectedIntentChoice by remember { mutableStateOf<IntentChoice?>(null) }
                var selectedDurationChoice by remember {
                    mutableStateOf<TargetDurationChoice?>(TargetDurationChoice.TEST_FIVE_SECONDS)
                }

                fun selectIntent(choice: IntentChoice) {
                    if (selectedIntentChoice == choice) return
                    selectedIntentChoice = choice
                    Log.d(TAG, "intent choice selected: ${choice.name}")
                    debugSessionId?.let {
                        TeumLogger.session(
                            debugSessionId = it,
                            event = "INTENT_SELECTED",
                            detail = "intent=${choice.name}"
                        )
                    }
                }

                fun selectDuration(choice: TargetDurationChoice) {
                    if (selectedDurationChoice == choice) return
                    selectedDurationChoice = choice
                    Log.d(TAG, "target duration selected: ${choice.durationMillis}")
                    debugSessionId?.let {
                        TeumLogger.session(
                            debugSessionId = it,
                            event = "TARGET_SELECTED",
                            detail = "target=${choice.durationMillis}"
                        )
                    }
                }

                fun startIntentSession() {
                    val intentChoice = selectedIntentChoice ?: return
                    if (intentChoice == IntentChoice.CLOSE_NOW) {
                        onCloseNowSelected()
                        return
                    }

                    val targetDurationMillis = selectedDurationChoice?.durationMillis ?: return
                    Log.d(TAG, "start clicked intent=${intentChoice.name} target=$targetDurationMillis package=$packageName")
                    onIntentConfirmed(intentChoice, targetDurationMillis)
                }

                TeumTheme {
                    if (mode == IntentCheckMode.FAST_REOPEN) {
                        ReopenCheckScreen(
                            appName = displayNameForPackage(packageName),
                            reopenGapMillis = reopenGapMillis,
                            selectedIntent = selectedIntentChoice,
                            selectedDuration = selectedDurationChoice,
                            onIntentSelected = { selectIntent(it) },
                            onDurationSelected = { selectDuration(it) },
                            onStartClick = { startIntentSession() },
                            onCloseClick = onCloseNowSelected
                        )
                    } else {
                        IntentCheckScreen(
                            appName = displayNameForPackage(packageName),
                            selectedIntent = selectedIntentChoice,
                            selectedDuration = selectedDurationChoice,
                            onIntentSelected = { selectIntent(it) },
                            onDurationSelected = { selectDuration(it) },
                            onStartClick = { startIntentSession() },
                            onCloseClick = onCloseNowSelected
                        )
                    }
                }
            }
        }
    }

    private fun createSessionBrakeComposeView(
        packageName: String,
        elapsedMillis: Long,
        targetDurationMillis: Long,
        debugSessionId: Long?,
        onBrakeChoice: (BrakeChoice) -> Unit,
        onExtendDurationSelected: (Long) -> Unit
    ): View {
        val owner = OverlayViewTreeOwner().apply {
            onCreate()
            onStart()
            onResume()
        }
        currentOverlayOwner = owner

        return ComposeView(overlayContext).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                fun chooseBrake(choice: BrakeChoice) {
                    debugSessionId?.let {
                        TeumLogger.session(
                            debugSessionId = it,
                            event = "BRAKE_CHOICE",
                            detail = "choice=${choice.name}"
                        )
                    }
                    onBrakeChoice(choice)
                }

                fun extendBrake(durationChoice: TargetDurationChoice) {
                    debugSessionId?.let {
                        TeumLogger.session(
                            debugSessionId = it,
                            event = "BRAKE_CHOICE",
                            detail = "choice=${BrakeChoice.EXTEND_3_MIN.name} duration=${durationChoice.durationMillis}"
                        )
                    }
                    onExtendDurationSelected(durationChoice.durationMillis)
                }

                TeumTheme {
                    SessionBrakeScreen(
                        appName = displayNameForPackage(packageName),
                        elapsedMillis = elapsedMillis,
                        targetDurationMillis = targetDurationMillis,
                        onEndClick = { chooseBrake(BrakeChoice.END_NOW) },
                        onExtendClick = { extendBrake(it) }
                    )
                }
            }
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

    private fun createOutcomeCheckView(
        packageName: String,
        durationMillis: Long,
        onOutcomeSelected: (OutcomeType) -> Unit,
        onDismissedWithoutChoice: () -> Unit
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
                text = "\uC774\uBC88 \uC0AC\uC6A9\uC740 \uC5B4\uB560\uB098\uC694?",
                textSize = 20f,
                textColor = Color.rgb(30, 38, 35),
                topMargin = 18
            )
        )
        card.addView(
            textView(
                text = "\uCC98\uC74C \uC758\uB3C4\uD55C \uBAA9\uC801\uC744 \uB2EC\uC131\uD588\uB098\uC694?",
                textSize = 16f,
                textColor = Color.rgb(63, 73, 69),
                topMargin = 10
            )
        )
        card.addView(
            textView(
                text = "\uBC29\uAE08 \uC0AC\uC6A9\uC744 \uC9E7\uAC8C \uC815\uB9AC\uD574\uB450\uBA74 \uB2E4\uC74C \uC2E4\uD589\uC744 \uB9C9\uC744 \uC218 \uC788\uC5B4\uC694.",
                textSize = 13f,
                textColor = Color.rgb(89, 98, 94),
                topMargin = 10
            )
        )
        card.addView(
            textView(
                text = "\uAC10\uC9C0\uB41C \uC571: $packageName / \uC0AC\uC6A9 ${formatDuration(durationMillis)}",
                textSize = 13f,
                textColor = Color.rgb(89, 98, 94),
                topMargin = 10
            )
        )

        card.addView(sectionLabel(text = "\uACB0\uACFC \uC120\uD0DD", topMargin = 18))

        card.addView(outcomeButton("\uBAA9\uC801 \uB2EC\uC131") {
            onOutcomeSelected(OutcomeType.NECESSARY_USE)
        })
        card.addView(outcomeButton("\uD544\uC694\uD55C \uC0AC\uC6A9\uC774\uC5C8\uC74C") {
            onOutcomeSelected(OutcomeType.NECESSARY_USE)
        })
        card.addView(outcomeButton("\uBAA9\uC801\uC5D0\uC11C \uBC97\uC5B4\uB0AC\uB2E4") {
            onOutcomeSelected(OutcomeType.PURPOSE_DRIFT)
        })
        card.addView(outcomeButton("\uADF8\uB0E5 \uC885\uB8CC") {
            onOutcomeSelected(OutcomeType.ENDED)
        })

        val dismissButton = choiceButton(text = "\uB2EB\uAE30").apply {
            setOnClickListener {
                onDismissedWithoutChoice()
            }
        }
        card.addView(dismissButton)

        return createOverlayRoot(card)
    }

    private fun outcomeButton(text: String, onClick: () -> Unit): Button {
        return choiceButton(text = text).apply {
            setOnClickListener {
                onClick()
            }
        }
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

    private fun displayNameForPackage(packageName: String): String {
        return when (packageName) {
            "com.google.android.youtube" -> "YouTube"
            "com.instagram.android" -> "Instagram"
            else -> packageName
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

    private enum class OverlayType {
        INTENT_CHECK,
        SESSION_BRAKE,
        OUTCOME_CHECK
    }

    private class OverlayViewTreeOwner :
        LifecycleOwner,
        SavedStateRegistryOwner,
        ViewModelStoreOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()
        private var destroyed = false

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        override val viewModelStore: ViewModelStore
            get() = store

        fun onCreate() {
            savedStateRegistryController.performAttach()
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        fun onStart() {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        fun onResume() {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun onDestroy() {
            if (destroyed) return
            destroyed = true
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        }
    }
}
