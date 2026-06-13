package com.aaa.floatingiconsample

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity

val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

class MainActivity : AppCompatActivity() {
    /* オーバーレイ設定画面から戻ってきたときの判定用ランチャー */
    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        /* 設定画面から戻った時点で、両方の権限が揃っているか最終確認 */
        if (checkAllPermissionsGranted()) {
            startOverlayService()
        }
        else {
            /* オーバーレイ権限をONにしてもらえなかったらアラートを出して終了 */
            PermissionDialogFragment.show(this, isOverlayError = true)
        }
    }

    /* 通常のパーミッション（通知など）を要求するランチャー */
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        isGranted: Map<String, Boolean> ->
            /* 権限チェック */
            if (isGranted.isNotEmpty() && isGranted.all { it.value }) {
                /* 通知権限がOKなら、次はオーバーレイ権限をチェック */
                checkOverlayPermission()
            } else {
                /* 通知権限が拒否されたらアラートダイアログ→Shutdown */
                PermissionDialogFragment.show(this, isOverlayError = false)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* 起動時にすでにすべての権限（通知＋オーバーレイ）があるか */
        if (checkAllPermissionsGranted()) {
            /* OKならサービス起動してActivityを閉じる */
            startOverlayService()
        }
        else {
            /* 権限不足の場合は、まず通常のパーミッション(通知)から要求を開始 */
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    /* 全ての権限（通知 ＋ オーバーレイ）が揃っているかチェックする関数 */
    private fun checkAllPermissionsGranted(): Boolean {
        /* 1. 通常のパーミッションチェック */
        val normalGranted = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        /* 2. オーバーレイ権限チェック */
        val overlayGranted = Settings.canDrawOverlays(this)

        return normalGranted && overlayGranted
    }

    /* オーバーレイ権限があるか確認し、無ければ設定画面へ飛ばす関数 */
    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            /* 設定画面（他のアプリの上に重ねて表示）へ遷移するインテント */
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            overlayPermissionLauncher.launch(intent)
        } else {
            /* すでに権限があればサービス起動 */
            startOverlayService()
        }
    }

    /* 安全にフォアグラウンドサービスを起動し、Activityを終了する共通関数 */
    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
        finish() /* サービスが起動したら、このActivityは閉じる */
    }
}

/* --- ダイアログ用のクラス（MainActivityクラスの外に配置） --- */
class PermissionDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()

        /* どちらの権限エラーかで文言を切り替える(引数で判定) */
        val isOverlayError = arguments?.getBoolean(ARG_IS_OVERLAY, false) ?: false

        val msgstr = if (isOverlayError) {
            /* オーバーレイ（重ね合わせ表示）が拒否された場合のメッセージ */
            "アプリの動作には「他のアプリの上に重ねて表示」の許可が必要です。設定を確認してください。"
        } else {
            /* 通知などが拒否された場合のメッセージ */
            activity.getString(R.string.wording_permission) +
                    REQUIRED_PERMISSIONS.joinToString(separator = ",\n") +
                    activity.getString(R.string.wording_permission2)
        }

        return AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.req_permission))
            .setMessage(msgstr)
            .setPositiveButton("OK") { _, _ ->
                activity.finish()
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().finish()
    }

    companion object {
        private const val ARG_IS_OVERLAY = "arg_is_overlay"

        fun show(activity: FragmentActivity, isOverlayError: Boolean) {
            val fragment = PermissionDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_OVERLAY, isOverlayError)
                }
            }
            fragment.show(activity.supportFragmentManager, "PermissionDialog")
        }
    }
}