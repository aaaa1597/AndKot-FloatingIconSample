package com.aaa.floatingiconsample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageButton
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        /* 上端/下端算出のための補助変数を取得 */
        val metrics = windowManager.currentWindowMetrics
        val insets = metrics.windowInsets
        val systemBars = insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        topInset = systemBars.top
        btmInset = systemBars.bottom
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /* 1. フォアグラウンドサービスを開始するための通知を作成 */
        startForegroundServiceWithNotification()
        /* 2. オーバーレイを表示 */
        showOverlay()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "overlay_service_channel"
        val channelName = "Overlay Service"

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Icon動作中")
            .setContentText("Floating Iconがバックグラウンドで動作しています")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        /* 通知と一緒にフォアグラウンドを開始 */
        startForeground(1, notification)
    }

    private fun showOverlay() {
        if (overlayView != null) return /* 既に表示されていればスキップ */

        /* 表示するViewの生成 (今回は簡単なレイアウトファイルをインフレート) */
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        /* Android 26(Oreo)以降は TYPE_APPLICATION_OVERLAY を使用 */
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,   /* タッチイベントを後ろに透過させる場合はこれ */
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START  /* 画面上部中央に配置 */
            x = 100
            y = 200
        }

        /* 閉じるボタンの挙動などを実装 */
        overlayView?.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
            stopSelf()              /* サービス自体を終了させる */
        }

        /* 画面にビューを追加 */
        windowManager.addView(overlayView, layoutParams)
        startMoving()
    }

    override fun onDestroy() {
        super.onDestroy()
        /* ビュー削除(これを忘れるとリークする) */
        overlayView?.let {
            handler.removeCallbacksAndMessages(null)
            windowManager.removeView(it)
            overlayView = null
        }
    }

    private var topInset = 0
    private var btmInset = 0
    private var dx = 5              /* X方向速度 */
    private var dy = 3              /* Y方向速度 */
    private fun startMoving() {
       handler.post(object : Runnable {
            override fun run() {
                val lp = layoutParams

                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight= displayMetrics.heightPixels

                val viewWidth = overlayView?.width  ?: 0
                val viewHeight= overlayView?.height ?: 0

                val bottomLimit = screenHeight - topInset - btmInset - viewHeight

                lp.x += dx
                lp.y += dy

                /* 左右反転 */
                if (lp.x <= 0 || lp.x >= screenWidth - viewWidth) {
                    dx = -dx
                }

                /* 上下反転 */
                if (lp.y <= 0 || lp.y >= bottomLimit) {
                    dy = -dy
                }

                windowManager.updateViewLayout(overlayView, lp)

                handler.postDelayed(this, 16) // 約60fps
            }
        })
    }
}