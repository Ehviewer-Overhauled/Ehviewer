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
package com.hippo.ehviewer.ui.scene

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.hippo.ehviewer.R
import com.hippo.ehviewer.widget.SimpleRatingView
import com.hippo.widget.LoadImageView

internal class GalleryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @JvmField
    val thumb: LoadImageView = itemView.findViewById(R.id.thumb)

    @JvmField
    val title: TextView? = itemView.findViewById(R.id.title)

    @JvmField
    val uploader: TextView? = itemView.findViewById(R.id.uploader)

    @JvmField
    val rating: SimpleRatingView? = itemView.findViewById(R.id.rating)

    @JvmField
    val category: TextView = itemView.findViewById(R.id.category)

    @JvmField
    val posted: TextView? = itemView.findViewById(R.id.posted)

    @JvmField
    val pages: TextView? = itemView.findViewById(R.id.pages)

    @JvmField
    val simpleLanguage: TextView = itemView.findViewById(R.id.simple_language)

    @JvmField
    val favourited: ImageView? = itemView.findViewById(R.id.favourited)

    @JvmField
    val downloaded: ImageView? = itemView.findViewById(R.id.downloaded)

    @JvmField
    val card: MaterialCardView = itemView.findViewById(R.id.card)
}