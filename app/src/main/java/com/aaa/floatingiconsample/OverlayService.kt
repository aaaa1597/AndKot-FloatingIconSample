package com.aaa.floatingiconsample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
        if (overlayView != null) return // 既に表示されていればスキップ

        // 表示するViewの生成 (今回は簡単なレイアウトファイルをインフレート)
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        // Android 26(Oreo)以降は TYPE_APPLICATION_OVERLAY を使用する
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // タッチイベントを後ろに透過させる場合はこれ
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL // 画面上部中央に配置
            y = 200 // 上からのマージン
        }

        // 閉じるボタンの挙動などを実装
        overlayView?.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
            stopSelf() // サービス自体を終了させる
        }

        // 画面にビューを追加
        windowManager.addView(overlayView, layoutParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        /* ビュー削除(これを忘れるとリークする) */
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}