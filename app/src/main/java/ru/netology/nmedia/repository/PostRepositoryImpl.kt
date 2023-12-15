package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.Post
import java.io.IOException
import java.util.concurrent.TimeUnit

class PostRepositoryImpl() : PostRepository {

    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
    private val gson = Gson()
    private val postsType = object : TypeToken<List<Post>>() {}.type
    private val postType = object : TypeToken<Post>() {}.type

    private companion object {
        const val GET = ""
        const val BASE_URL = "http://10.0.2.2:9999"
        val jsonType = "application/json".toMediaType()
    }

    override fun getAllAsync(callback: PostRepository.RepositoryCallback<List<Post>>) {
        PostsApi.retrofitService.getAll().enqueue(object : Callback<List<Post>> {
            override fun onResponse(
                call: Call<List<Post>>, response: Response<List<Post>>
            ) {
                println(Thread.currentThread().name)
                if (response.isSuccessful) {
                    callback.onSuccess(
                        //
                        response.body() ?: throw java.lang.RuntimeException("empty body")
                    )
                } else {
                    callback.onError(java.lang.RuntimeException("error code: ${response.code()} with ${response.message()}"))
                }
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                callback.onError(java.lang.Exception(t))
            }
        })
    }

    override fun getByIdAsync(id: Long, callback: PostRepository.RepositoryCallback<Post>) {
        PostsApi.retrofitService.getById(id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    callback.onSuccess(
                        response.body() ?: throw java.lang.RuntimeException("empty body")
                    )
                } else {
                    callback.onError(java.lang.RuntimeException("error code: ${response.code()} with ${response.message()}"))
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onError(java.lang.Exception(t))
            }

        })
    }

    override fun likePostAsync(id: Long, callback: PostRepository.RepositoryCallback<Boolean>) {
        PostsApi.retrofitService.likePost(id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    callback.onSuccess(true)
                } else {
                    callback.onError(java.lang.RuntimeException("Ошибка сервера"))
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onError(java.lang.RuntimeException("Ошибка сервера"))
            }

        })
    }

    override fun unlikePostAsync(id: Long, callback: PostRepository.RepositoryCallback<Boolean>) {
        PostsApi.retrofitService.unlikePost(id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    callback.onSuccess(false)
                } else {
                    callback.onError(java.lang.RuntimeException("Ошибка сервера"))
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onError(java.lang.RuntimeException("Ошибка сервера"))
            }
        })
    }

    override fun removeByIdAsync(id: Long, callback: PostRepository.RepositoryCallback<Boolean>) {
        PostsApi.retrofitService.removeById(id).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    callback.onSuccess(true)
                } else {
                    callback.onError(java.lang.RuntimeException("Ошибка сервера"))
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                callback.onError(java.lang.Exception(t))
            }
        })
    }

    override fun saveAsync(post: Post, callback: PostRepository.RepositoryCallback<Post>) {
        PostsApi.retrofitService.save(post).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    callback.onSuccess(post)
                } else {
                    callback.onError(RuntimeException("error code: ${response.code()} with ${response.message()}"))
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onError(java.lang.Exception(t))
            }
        })
    }


    override fun share(id: Long) {
        TODO("Not yet implemented")
    }
}