package com.hippo.ehviewer.ui.scene

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.google.accompanist.themeadapter.material3.Mdc3Theme

class FooterAdapter(private val onClick: () -> Unit) :
    RecyclerView.Adapter<FooterAdapter.ViewHolder>() {
    var text by mutableStateOf("")

    inner class ViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ComposeView(parent.context))
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.composeView.setContent {
            Mdc3Theme {
                Column {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            onClick = onClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.6666667F)
                        ) {}
                        Text(text)
                    }
                    Text("")
                }
            }
        }
    }
}