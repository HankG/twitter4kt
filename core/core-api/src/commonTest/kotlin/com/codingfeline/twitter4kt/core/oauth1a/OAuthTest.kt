/**
 * Copyright 2020 Shimizu Yasuhiro (yshrsmz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.codingfeline.twitter4kt.core.oauth1a

import kotlin.test.Test
import kotlin.test.assertEquals

class OAuthTest {

    @Test
    fun hmacSha1() {
        val result = hmacSha1("A", "B")

        assertEquals(result, "O6I323rTZhS8%2BWqOzLVYS7AmZhQ%3D")
    }
}
