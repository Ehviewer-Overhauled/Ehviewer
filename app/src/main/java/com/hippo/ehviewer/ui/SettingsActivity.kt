/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ActivityNavigator
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.ui.fragment.SettingsFragment
import com.hippo.ehviewer.ui.scene.BaseScene
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext

class SettingsActivity : EhActivity() {
    private var mFab: FloatingActionButton? = null
    private var mContentText: TextView? = null
    private var mAppbarLayout: AppBarLayout? = null
    var mLimits: HomeParser.Limits? = null
        private set
    var mFunds: HomeParser.Funds? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (EhCookieStore.hasSignedIn()) {
            lifecycleScope.launchIO {
                runCatching {
                    getImageLimits()
                }.exceptionOrNull()?.printStackTrace()
            }
        }
        setContentView(R.layout.activity_preference)
        setSupportActionBar(findViewById(R.id.toolbar))
        mContentText = findViewById(R.id.tip)
        mFab = findViewById(R.id.fab)
        mAppbarLayout = findViewById(R.id.appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
                .replace(R.id.fragment, SettingsFragment())
                .commitAllowingStateLoss()
        }
    }

    suspend fun getImageLimits() = EhEngine.getImageLimits().run {
        mLimits = limits
        mFunds = funds
    }

    suspend fun resetImageLimits() {
        mLimits =
            EhEngine.resetImageLimits() ?: HomeParser.Limits(maximum = mLimits!!.maximum).also {
                withUIContext {
                    showTip(R.string.reset_limits_succeed, BaseScene.LENGTH_SHORT)
                }
            }
    }

    fun showTip(@StringRes id: Int, length: Int) {
        showTip(getString(id), length)
    }

    fun showTip(message: CharSequence?, length: Int) {
        Snackbar.make(
            findViewById(R.id.snackbar),
            message!!,
            if (length == BaseScene.LENGTH_LONG) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT,
        ).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun getMFab(): FloatingActionButton {
        return mFab!!
    }

    fun getMContentText(): TextView {
        return mContentText!!
    }

    fun resetAppbarLiftStatus() {
        mAppbarLayout?.setExpanded(true)
    }

    override fun finish() {
        super.finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
}
