package com.aaa.floatingiconsample.permissions

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.aaa.floatingiconsample.MainFragment
import com.aaa.floatingiconsample.databinding.FragmentPermissionsBinding
import com.aaa.floatingiconsample.R

val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.INTERNET)

class PermissionsFragment : Fragment() {
    private lateinit var _binding: FragmentPermissionsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("aaaaa", "onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d("aaaaa", "onCreateView")
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("aaaaa", "onViewCreated 39")
        super.onViewCreated(view, savedInstanceState)

        Log.d("aaaaa", "onViewCreated 42")
        /* 権限承認済ならメイン処理のFragmentへ(移行このFragmentには戻らない) */
        if (checkPermissionsGranted()) {
            Log.d("aaaaa", "onViewCreated 45")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, MainFragment())
                .commit()
        }

        Log.d("aaaaa", "onViewCreated 50")
        permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun checkPermissionsGranted(): Boolean {
        var result = true
        for (permission in REQUIRED_PERMISSIONS) {
            result = result && ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
        return result
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            isGranted: Map<String, Boolean> ->
        Log.d("aaaaa", "registerForActivityResult 64")
        /* 権限チェック */
        if (isGranted.isNotEmpty() && isGranted.all({it.value == true})) {
            Log.d("aaaaa", "registerForActivityResult 67 count=${isGranted.count()} isGranted=${isGranted.entries.joinToString(separator = ","){ "${it.key}=${it.value}" }}")
            /* 権限承認済ならメイン処理のFragmentへ(移行このFragmentには戻らない) */
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, MainFragment())
                .commit()
        }
        else {
            Log.d("aaaaa", "registerForActivityResult 73")
            /* ひとつでも権限不足ありならアラートダイアログ→Shutdown */
            PermissionDialogFragment.show(requireActivity())
        }
    }
}

class PermissionDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()


        val msgstr = activity.getString(R.string.wording_permission) +
                REQUIRED_PERMISSIONS.joinToString(separator = ",\n") +
                activity.getString(R.string.wording_permission2)

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
        fun show(activity: FragmentActivity) {
            val fragment = PermissionDialogFragment()
            fragment.show(activity.supportFragmentManager, "PermissionDialog")
        }
    }
}
