package ru.ivan.mobchat

import android.app.Dialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class FullscreenImageDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        Dialog(requireContext(), R.style.Theme_MobChat_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.dialog_image, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val path = requireArguments().getString(ARG_PATH).orEmpty()
        val progress = view.findViewById<View>(R.id.image_progress)
        val retry = view.findViewById<View>(R.id.image_retry)
        val close = view.findViewById<View>(R.id.close_image_button)
        val image = view.findViewById<ImageView>(R.id.full_image)
        fun load() {
            progress.visibility = View.VISIBLE
            retry.visibility = View.GONE
            Glide.with(this).load(imageUrl(path, thumbnail = false))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?, target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progress.visibility = View.GONE
                        retry.visibility = View.VISIBLE
                        image.contentDescription = getString(R.string.image_failed)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable, model: Any, target: Target<Drawable>,
                        dataSource: DataSource, isFirstResource: Boolean
                    ): Boolean {
                        progress.visibility = View.GONE
                        image.contentDescription = getString(R.string.image_message)
                        return false
                    }
                }).into(image)
        }
        load()
        retry.setOnClickListener { load() }
        close.setOnClickListener { dismiss() }
        TooltipCompat.setTooltipText(close, getString(R.string.close_image))
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    companion object {
        private const val ARG_PATH = "path"
        fun newInstance(path: String) = FullscreenImageDialog().apply {
            arguments = Bundle().apply { putString(ARG_PATH, path) }
        }
    }
}
