/*
 * Copyright 2018 Google, Inc.
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

package io.plaidapp.base.designernews.data.api.comments

import io.plaidapp.base.designernews.data.api.DesignerNewsService
import io.plaidapp.base.designernews.data.api.model.Comment
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Unconfined
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import retrofit2.Response

/**
 * Tests for [DesignerNewsCommentsRepository] with mocked service
 */
class DesignerNewsCommentsRepositoryTest {

    private val service = Mockito.mock(DesignerNewsService::class.java)
    private val dataSource = DesignerNewsCommentsRemoteDataSource(service, Unconfined)
    private val repository = DesignerNewsCommentsRepository(dataSource, Unconfined, Unconfined)

    @Test
    fun getComments_noReplies_whenRequestSuccessful() {
        // Given that the service responds with success
        val apiResult = Response.success(listOf(reply1))
        Mockito.`when`(service.getComments("11")).thenReturn(CompletableDeferred(apiResult))
        var result = emptyList<Comment>()

        // When getting the replies
        repository.getComments(listOf(11L), { list -> result = list }, { Assert.fail() })

        // Then the correct list of comments was requested from the API
        Mockito.verify(service).getComments("11")
        // Then the correct list is received
        assertEquals(listOf(reply1), result)
    }

    @Test
    fun getComments_noReplies_whenRequestFailed() {
        // Given that the service responds with failure
        val apiResult = Response.error<List<Comment>>(400, errorResponseBody)
        Mockito.`when`(service.getComments("11")).thenReturn(CompletableDeferred(apiResult))
        var errorCalled = false

        // When getting the comments
        repository.getComments(listOf(11L), { Assert.fail() }, { errorCalled = true })

        // Then the error lambda is called
        assertTrue(errorCalled)
    }

    @Test
    fun getComments_multipleReplies_whenRequestSuccessful() {
        // Given that:
        // When requesting replies for ids 1 from service we get the parent comment but
        // without replies embedded (since that's what the next call is doing)
        val resultParent = Response.success(listOf(parentCommentWithoutReplies))
        Mockito.`when`(service.getComments("1")).thenReturn(CompletableDeferred(resultParent))
        // When requesting replies for ids 11 and 12 from service we get the children
        val resultChildren = Response.success(replies)
        Mockito.`when`(service.getComments("11,12"))
                .thenReturn(CompletableDeferred(resultChildren))
        var result = emptyList<Comment>()

        // When getting the comments from the repository
        repository.getComments(listOf(1L), { list -> result = list }, { Assert.fail() })

        // Then  API requests were triggered
        Mockito.verify(service).getComments("1")
        Mockito.verify(service).getComments("11,12")
        // Then the correct result is received
        assertEquals(arrayListOf(parentCommentWithReplies), result)
    }

    @Test
    fun getComments_multipleReplies_whenRepliesRequestFailed() {
        // Given that
        // When requesting replies for ids 1 from service we get the parent comment
        val resultParent = Response.success(listOf(parentCommentWithReplies))
        Mockito.`when`(service.getComments("1")).thenReturn(CompletableDeferred(resultParent))
        // When requesting replies for ids 11 and 12 from service we get an error
        val resultChildrenError = Response.error<List<Comment>>(400, errorResponseBody)
        Mockito.`when`(service.getComments("11,12"))
                .thenReturn(CompletableDeferred(resultChildrenError))
        var result = emptyList<Comment>()

        // When getting the comments from the repository
        repository.getComments(listOf(1L), { list -> result = list }, { Assert.fail() })

        // Then  API requests were triggered
        Mockito.verify(service).getComments("1")
        Mockito.verify(service).getComments("11,12")
        // Then the correct result is received
        assertEquals(arrayListOf(parentCommentWithReplies), result)
    }
}
