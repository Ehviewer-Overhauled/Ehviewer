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
package com.hippo.ehviewer.client.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class GalleryComment(
    // 0 for uploader comment. can't vote
    var id: Long = 0,
    var score: Int = 0,
    var editable: Boolean = false,
    var voteUpAble: Boolean = false,
    var voteUpEd: Boolean = false,
    var voteDownAble: Boolean = false,
    var voteDownEd: Boolean = false,
    var uploader: Boolean = false,
    var voteState: String? = null,
    var time: Long = 0,
    var user: String? = null,
    var comment: String? = null,
    var lastEdited: Long = 0,
) : Parcelable
