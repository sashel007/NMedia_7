package ru.netology.nmedia.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.core.content.contentValuesOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.activity.Event
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.SingleLiveEvent
import ru.netology.nmedia.recyclerview.OnInteractionListener
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import kotlin.concurrent.thread

private val empty = Post(
    id = 0L,
    author = "Евгений",
    authorAvatar = "",
    content = "",
    published = 0,
    likedByMe = false,
    likes = 0
//    sharings = 0,
//    video = ""
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository = PostRepositoryImpl()

    private val _data = MutableLiveData(FeedModelState())
    val data: LiveData<FeedModelState>
        get() = _data
    private val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private var interactionListener: OnInteractionListener? = null

    // Обработка ошибок на сервере, механизм Event
    private val _errorMessages = MutableLiveData<String?>()
    val errorMessages: LiveData<String?>
        get() = _errorMessages

    // Статус загрузки в ожидании ответа от сервера
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadPosts()
    }

    // Функции для установки обработчика взаимодействий и переменная для хранения
    fun setInteractionListener(listener: OnInteractionListener) {
        this.interactionListener = listener
    }

    fun getInteractionListener(): OnInteractionListener? {
        return interactionListener
    }

    fun loadPosts() {
        _data.value = FeedModelState(loading = true)
        repository.getAllAsync(object : PostRepository.RepositoryCallback<List<Post>> {
            override fun onSuccess(result: List<Post>) {
                _data.value = FeedModelState(
                    posts = result,
                    empty = result.isEmpty(),
                    loading = false
                )
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModelState(error = true))
            }
        })
    }

    fun like(id: Long) {
        // Получаем текущий пост
        val currentPosts = _data.value?.posts.orEmpty()
        val currentPost = currentPosts.find { it.id == id }

        currentPost?.let { post ->
            // Оптимистичное обновление UI
            val updatedPost = if (post.likedByMe) {
                post.copy(likes = post.likes - 1, likedByMe = false)
            } else {
                post.copy(likes = post.likes + 1, likedByMe = true)
            }

            // Обновляем список постов
            val updatedPosts = currentPosts.map {
                if (it.id == id) updatedPost else it
            }
            _data.postValue(_data.value?.copy(posts = updatedPosts))

            // Асинхронное обновление на сервере
            val callback = object : PostRepository.RepositoryCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    // Ничего не делаем, так как UI уже обновлен оптимистично
                }

                override fun onError(e: Exception) {
                    // Если запрос не удался, откатываем изменения в UI
                    _data.postValue(_data.value?.copy(posts = currentPosts))
                    val errorMessage = "Ошибка при нажатии кнопки \"Лайк\". Повторите снова"
                    _errorMessages.postValue(errorMessage)
                }
            }
            if (post.likedByMe) {
                repository.unlikePostAsync(id, callback)
            } else {
                repository.likePostAsync(id, callback)
            }
        }
    }

    fun share(id: Long) = thread { repository.share(id) }

    fun removeById(id: Long) = thread {
        // Оптимистичная модель: предполагаем, что пост удален
        val oldPosts = _data.value?.posts.orEmpty()
        val updatedPosts = oldPosts.filter { it.id != id }
        _data.postValue(_data.value?.copy(posts = updatedPosts))

        // Асинхронное удаление на сервере
        repository.removeByIdAsync(id, object : PostRepository.RepositoryCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                // Успешно удалено, ничего не делаем, так как UI уже обновлен
            }

            override fun onError(e: Exception) {
                // Если запрос не удался, откатываем изменения в UI
                _data.postValue(_data.value?.copy(posts = oldPosts))
            }
        })
    }

    private fun resetEditingState() {
        edited.postValue(empty)
    }

    fun addNewPost(content: String) {
        // Показываем индикатор загрузки
        _isLoading.value = true

        val newPost = empty.copy(content = content.trim(), id = 0L)

        // Оптимистичное обновление: добавляем новый пост в UI
        val currentPosts = _data.value?.posts.orEmpty()
        _data.postValue(_data.value?.copy(posts = listOf(newPost) + currentPosts))

        // Ассинхронное сохранение поста
        repository.saveAsync(newPost, object : PostRepository.RepositoryCallback<Post> {
            override fun onSuccess(result: Post) {
                // Обновление списка постов с сохраненным постом
                val updatedPosts = listOf(result) + currentPosts.filter { it.id != 0L }
                _data.postValue(_data.value?.copy(posts = updatedPosts))

                // оповещение об успешном создании
                _postCreated.postValue(Unit)
                resetEditingState()

                // Скрываем индикатор загрузки
                _isLoading.postValue(false)
            }

            override fun onError(e: Exception) {
                // Если запрос не удался, откатываем изменения в UI
                _data.postValue(_data.value?.copy(posts = currentPosts))
                _isLoading.postValue(false)
                _errorMessages.postValue("Повторите попытку")
            }
        })
    }

    fun updatePost(postId: Long, content: String) {
        // Показываем индикатор ожидания ответа от сервера
        _isLoading.value = true

        // Асинхронно получаем текущий пост
        repository.getByIdAsync(postId, object : PostRepository.RepositoryCallback<Post> {
            override fun onSuccess(result: Post) {
                // Создаем обновленный пост
                val updatedPost = result.copy(content = content.trim())

                // Асинхронно сохраняем обновленный пост
                repository.saveAsync(updatedPost, object : PostRepository.RepositoryCallback<Post> {
                    override fun onSuccess(result: Post) {
                        // Обновляем список постов в LiveData
                        val updatedPosts = _data.value?.posts?.map { post ->
                            if (post.id == result.id) result else post
                        }.orEmpty()
                        _data.postValue(_data.value?.copy(posts = updatedPosts))

                        // Оповещаем о завершении обновления
                        _postCreated.postValue(Unit)
                        _isLoading.postValue(false)
                    }

                    override fun onError(e: Exception) {
                        // Обработка ошибок при сохранении
                        _isLoading.postValue(false)
                        _errorMessages.postValue("Повторите попытку")
                    }
                })
            }

            override fun onError(e: Exception) {
                // Обработка ошибок при получении поста
                _isLoading.postValue(false)
                _errorMessages.postValue("Повторите попытку")
            }
        })

        // Сбрасываем состояние редактирования
        resetEditingState()
    }

    // Метод для сбрасывания значения об ошибке при прослушивании событий нажатия лайка
    fun clearErrorMessage() {
        _errorMessages.value = null
    }

}



