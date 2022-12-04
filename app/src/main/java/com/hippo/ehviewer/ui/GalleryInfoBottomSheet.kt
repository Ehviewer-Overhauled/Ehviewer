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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.UrlOpener
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.util.addTextToClipboard

class GalleryInfoBottomSheet(detail: GalleryDetail) : BottomSheetDialogFragment() {
    private var mKeys: ArrayList<String> = arrayListOf()
    private var mValues: ArrayList<String> = arrayListOf()
    private var mRecyclerView: RecyclerView? = null
    private val mDetail: GalleryDetail = detail

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        parseDetail()
        val view = inflater.inflate(R.layout.scene_gallery_info, container, false)
        val recyclerView = view.findViewById(R.id.recycler_view) as RecyclerView
        val adapter = InfoAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recyclerView.clipToPadding = false
        recyclerView.setHasFixedSize(true)
        mRecyclerView = recyclerView
        return view
    }

    private fun parseDetail() {
        mKeys.add(getString(R.string.header_key))
        mValues.add(getString(R.string.header_value))
        mKeys.add(getString(R.string.key_gid))
        mValues.add(mDetail.gid.toString())
        mKeys.add(getString(R.string.key_token))
        mValues.add(mDetail.token)
        mKeys.add(getString(R.string.key_url))
        mValues.add(EhUrl.getGalleryDetailUrl(mDetail.gid, mDetail.token))
        mKeys.add(getString(R.string.key_title))
        mValues.add(mDetail.title)
        mKeys.add(getString(R.string.key_title_jpn))
        mValues.add(mDetail.titleJpn)
        mKeys.add(getString(R.string.key_thumb))
        mValues.add(mDetail.thumb)
        mKeys.add(getString(R.string.key_category))
        mValues.add(EhUtils.getCategory(mDetail.category))
        mKeys.add(getString(R.string.key_uploader))
        mValues.add(mDetail.uploader)
        mKeys.add(getString(R.string.key_posted))
        mValues.add(mDetail.posted)
        mKeys.add(getString(R.string.key_parent))
        mValues.add(mDetail.parent)
        mKeys.add(getString(R.string.key_visible))
        mValues.add(mDetail.visible)
        mKeys.add(getString(R.string.key_language))
        mValues.add(mDetail.language)
        mKeys.add(getString(R.string.key_pages))
        mValues.add(mDetail.pages.toString())
        mKeys.add(getString(R.string.key_size))
        mValues.add(mDetail.size)
        mKeys.add(getString(R.string.key_favorite_count))
        mValues.add(mDetail.favoriteCount.toString())
        mKeys.add(getString(R.string.key_favorited))
        mValues.add(java.lang.Boolean.toString(mDetail.isFavorited))
        mKeys.add(getString(R.string.key_rating_count))
        mValues.add(mDetail.ratingCount.toString())
        mKeys.add(getString(R.string.key_rating))
        mValues.add(mDetail.rating.toString())
        mKeys.add(getString(R.string.key_torrents))
        mValues.add(mDetail.torrentCount.toString())
        mKeys.add(getString(R.string.key_torrent_url))
        mValues.add(mDetail.torrentUrl)
        mKeys.add(getString(R.string.favorite_name))
        mValues.add(mDetail.favoriteName)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.findViewById(R.id.title) as TextView).setText(R.string.gallery_info)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRecyclerView?.stopScroll()
    }

    fun onItemClick(position: Int): Boolean {
        val context = context
        return if (null != context && 0 != position) {
            if (position == INDEX_PARENT) {
                UrlOpener.openUrl(context, mValues[position], true)
            } else {
                activity?.addTextToClipboard(
                    mValues[position],
                    false,
                )
                if (position == INDEX_URL) {
                    // Save it to avoid detect the gallery
                    Settings.putClipboardTextHashCode(mValues[position].hashCode())
                }
            }
            true
        } else {
            false
        }
    }

    private class InfoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val key: TextView
        val value: TextView

        init {
            key = itemView.findViewById(R.id.key)
            value = itemView.findViewById(R.id.value)
        }
    }

    private inner class InfoAdapter : RecyclerView.Adapter<InfoHolder>() {
        private val mInflater: LayoutInflater = layoutInflater

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) {
                TYPE_HEADER
            } else {
                TYPE_DATA
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoHolder {
            return InfoHolder(
                mInflater.inflate(
                    if (viewType == TYPE_HEADER) R.layout.item_gallery_info_header else R.layout.item_gallery_info_data,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: InfoHolder, position: Int) {
            holder.key.text = mKeys[position]
            holder.value.text = mValues[position]
            holder.itemView.isEnabled = position != 0
            holder.itemView.setOnClickListener { onItemClick(position) }
        }

        override fun getItemCount(): Int {
            return mKeys.size.coerceAtMost(mValues.size)
        }
    }

    companion object {
        const val TAG = "GalleryInfoBottomSheet"
        private const val INDEX_URL = 3
        private const val INDEX_PARENT = 10
        private const val TYPE_HEADER = 0
        private const val TYPE_DATA = 1
    }
}