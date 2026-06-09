package com.helpmethen.chat.ui.screens.image

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.helpmethen.chat.R
import com.helpmethen.chat.data.remote.ImageLoader
import com.helpmethen.chat.data.repository.UnauthorizedException
import com.helpmethen.chat.data.session.ChatSession
import com.helpmethen.chat.data.session.SessionStore
import com.helpmethen.chat.databinding.FragmentImageBinding
import com.helpmethen.chat.ui.navigation.BackPressHandler
import com.helpmethen.chat.ui.screens.login.LoginFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageFragment : Fragment(), BackPressHandler {

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    private lateinit var imagePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePath = requireArguments().getString(ARG_IMAGE_PATH).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        loadImage()
    }

    private fun loadImage() {
        val url = imageUrl(imagePath)
        ChatSession.images[url]?.let { bitmap ->
            binding.imageView.setImageBitmap(bitmap)
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                ImageLoader.loadBitmap(url)
            }

            binding.progressBar.visibility = View.GONE
            result
                .onSuccess { bitmap ->
                    bitmap?.let {
                        ChatSession.images[url] = it
                        binding.imageView.setImageBitmap(it)
                    }
                }
                .onFailure { exception ->
                    if (exception is UnauthorizedException) {
                        restartLogin()
                    }
                }
        }
    }

    private fun imageUrl(imagePath: String): String {
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath
        }

        return "https://faerytea.name/img/${imagePath.trimStart('/').replace(" ", "%20")}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onBackPressed(): Boolean {
        parentFragmentManager.popBackStack()
        return true
    }

    private fun restartLogin() {
        ChatSession.clear()
        SessionStore.clear(requireContext())
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, LoginFragment())
        }
    }

    companion object {
        private const val ARG_IMAGE_PATH = "imagePath"

        fun newInstance(imagePath: String): ImageFragment {
            return ImageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_PATH, imagePath)
                }
            }
        }
    }
}
