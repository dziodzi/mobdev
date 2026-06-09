package com.helpmethen.chat

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import com.helpmethen.chat.ui.navigation.BackPressHandler
import com.helpmethen.chat.data.session.ChatSession
import com.helpmethen.chat.data.session.SessionStore
import com.helpmethen.chat.ui.screens.chats.ChatsFragment
import com.helpmethen.chat.ui.screens.login.LoginFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideSystemBars()
        setupBackNavigation()

        if (savedInstanceState == null) {
            val savedSession = SessionStore.restore(this)
            supportFragmentManager.commit {
                if (savedSession == null) {
                    replace(R.id.fragmentContainer, LoginFragment())
                } else {
                    ChatSession.setLogin(savedSession.token, savedSession.username)
                    replace(
                        R.id.fragmentContainer,
                        ChatsFragment.newInstance(savedSession.token, savedSession.username)
                    )
                }
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val currentFragment =
                        supportFragmentManager.findFragmentById(R.id.fragmentContainer)

                    if (currentFragment is BackPressHandler && currentFragment.onBackPressed()) {
                        return
                    }

                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
