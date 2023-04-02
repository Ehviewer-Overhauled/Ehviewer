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
package com.hippo.ehviewer.client.parser

import org.json.JSONObject

object VoteCommentParser {
    // {"comment_id":1253922,"comment_score":-19,"comment_vote":0}
    fun parse(body: String, expectVote: Int): Result {
        val jo = JSONObject(body)
        val id = jo.getLong("comment_id")
        val score = jo.getInt("comment_score")
        val vote = jo.getInt("comment_vote")
        return Result(id, score, vote, expectVote)
    }

    class Result(val id: Long, val score: Int, val vote: Int, val expectVote: Int)
}
