import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


fun main() {
    CoroutineScope(EmptyCoroutineContext).launch {
        val posts = getPosts().map { post ->
            async {
                PostsWithComments(post, getAuthors(post.authorId), getComments(post.id).map { comment ->
                    CommentsWithAuthor(comment, getAuthors(comment.authorId))
                })
            }
        }.awaitAll()
        println(posts)
    }
    Thread.sleep(1000)
}
val okHttpclient = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor(::println).apply { level = HttpLoggingInterceptor.Level.BODY})
    .build()

suspend fun getPosts() = makeRequest("http://127.0.0.1:9999/api/slow/posts", okHttpclient, object : TypeToken<List<Post>>(){})

suspend fun getComments(id: Long) = makeRequest("http://127.0.0.1:9999/api/slow/posts/$id/comments", okHttpclient, object : TypeToken<List<Comment>>(){})

suspend fun getAuthors(id: Long) = makeRequest("http://127.0.0.1:9999/api/slow/authors/$id", okHttpclient, object : TypeToken<Author>(){})

suspend fun <T> makeRequest(url: String, okHttpclient: OkHttpClient, typeToken: TypeToken<T>): T =
    okHttpclient.apiCall(url).let { response ->
        if (!response.isSuccessful) {
            throw RuntimeException("error response")
        }
        val result = response.body ?: throw RuntimeException("No body")
        Gson().fromJson(result.charStream(), typeToken.type)
    }


suspend fun OkHttpClient.apiCall(url: String) = suspendCoroutine<Response> { continuation ->
    Request.Builder().url(url).build().let (::newCall).enqueue(object : Callback{
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

    })
}

