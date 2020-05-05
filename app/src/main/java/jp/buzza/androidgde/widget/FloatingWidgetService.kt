package jp.buzza.androidgde.widget

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import com.airbnb.lottie.LottieAnimationView
import jp.buzza.androidgde.R
import kotlin.math.abs

class FloatingWidgetService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        // Often, there will be a slight, unintentional, drag when the user taps the FAB, so we need to account for this.
        private const val CLICK_DRAG_TOLERANCE = 10f
    }

    var initialX: Int = 0
    var initialY: Int = 0
    var initialTouchX: Float = 0.0f
    var initialTouchY: Float = 0.0f
    val negativeBoundLimit: Int by lazy { endCallAnimation.width.div(4) }
    val positiveBoundLimit: Int by lazy { endCallAnimation.width.times(3).div(4) }

    private val floatingWidgetView: ViewGroup by lazy {
        LayoutInflater.from(this)
            .inflate(R.layout.layout_floating_widget, null) as ViewGroup
    }

    private val endCallAnimation: View by lazy {
        floatingWidgetView.findViewById(R.id.end_call_animation) as LottieAnimationView
    }

    private val windowManager: WindowManager by lazy {
        applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        createLayoutFlag(),
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    )

    private val displaySize: Point by lazy {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        size
    }

    override fun onCreate() {
        super.onCreate()
        floatingWidgetView.findViewById<View>(R.id.root_container)
            .setOnTouchListener(clickListener())
        floatingWidgetView.findViewById<View>(R.id.root_container).setOnClickListener {
            Log.d("Hello", "clicked")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        params.apply {
            gravity = Gravity.BOTTOM or Gravity.END
        }
        windowManager.addView(floatingWidgetView, params)
        return START_STICKY
    }


    private fun createLayoutFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun clickListener() = object : View.OnTouchListener, ActionCallBack {

        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            return when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("Widget", "action_down")
                    initialX = params.x
                    initialY = params.y
                    Log.d("initialX", initialX.toString())
                    Log.d("initialY", initialY.toString())
                    //Get the touch location
                    initialTouchX = motionEvent.rawX
                    initialTouchY = motionEvent.rawY
                    Log.d("initialTouchX", initialTouchX.toString())
                    Log.d("initialTouchY", initialTouchY.toString())
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.d("Widget", "action_up")
                    setAction(Action.TOUCH_UP)
                    val xDiff: Int = (initialTouchX - motionEvent.rawX).toInt()
                    val yDiff: Int = (initialTouchY - motionEvent.rawY).toInt()
                    Log.d("touchUPX", motionEvent.rawX.toString())
                    Log.d("touchUPY", motionEvent.rawX.toString())
                    Log.d("screenX", displaySize.x.toString())
                    Log.d("screenY", displaySize.y.toString())
                    if (abs(xDiff) < CLICK_DRAG_TOLERANCE && abs(yDiff) < CLICK_DRAG_TOLERANCE) {
                        // Click
                        view.performClick()
                    } else {
                        // Drag
                        true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    Log.d("Widget", "action_move")
                    setAction(Action.MOVING)
                    val xDiff: Int = (initialTouchX - motionEvent.rawX).toInt()
                    val yDiff: Int = (initialTouchY - motionEvent.rawY).toInt()

                    Log.d("MOVE-initialTouchX", initialTouchX.toString())
                    Log.d("MOVE-initialTouchY", initialTouchY.toString())

                    Log.d("MOVE-rawX", motionEvent.rawX.toString())
                    Log.d("MOVE-rawY", motionEvent.rawY.toString())

                    var newPositionX = initialX.plus(xDiff)
                    var newPositionY = initialY.plus(yDiff)

                    Log.d("MOVE-newPositionX", newPositionX.toString())
                    Log.d("MOVE-newPositionY", newPositionY.toString())
                    if (newPositionX < negativeBoundLimit.unaryMinus()) {
                        newPositionX = negativeBoundLimit.unaryMinus()
                    } else if (newPositionX > displaySize.x.minus(positiveBoundLimit)) {
                        newPositionX = displaySize.x.minus(positiveBoundLimit)
                    }
                    if (newPositionY < negativeBoundLimit.unaryMinus()) {
                        newPositionY = negativeBoundLimit.unaryMinus()
                    } else if (newPositionY > displaySize.y.minus(positiveBoundLimit)) {
                        newPositionY = displaySize.y.minus(positiveBoundLimit)
                    }
                    Log.d("MOVE-EndCallX", endCallAnimation.width.toString())
                    Log.d("MOVE-EndCallY", endCallAnimation.height.toString())

                    params.x = newPositionX
                    params.y = newPositionY

                    Log.d("MOVE-param x", params.x.toString())
                    Log.d("MOVE-param y", params.y.toString())
                    windowManager.updateViewLayout(floatingWidgetView, params)
                    true
                }
                else -> {
                    false
                }
            }
        }

        override fun setAction(action: Action) {
            when (action) {
                Action.TOUCH_UP -> {
                    endCallAnimation.visibility = View.VISIBLE
                }
                Action.MOVING -> {
                    endCallAnimation.visibility = View.INVISIBLE
                }
            }
        }
    }

    private interface ActionCallBack {
        fun setAction(action: Action)
    }

    enum class Action {
        TOUCH_UP,
        MOVING,
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        windowManager.removeView(floatingWidgetView)
        stopSelf()
    }
}


