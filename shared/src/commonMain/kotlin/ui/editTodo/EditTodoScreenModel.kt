package ui.editTodo

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import data.Todo
import data.TodoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditTodoScreenModel(
    private val todoRepository: TodoRepository,
    private val id: Long?,
) : ScreenModel, EditTodoStateUpdater {
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()
    private val _done = MutableStateFlow(false)
    val done = _done.asStateFlow()
    private val _deadline =
        MutableStateFlow(EditTodoUiState.Deadline.createCurrentDate())
    val deadline = _deadline.asStateFlow()
    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker = _showDatePicker.asStateFlow()
    private val _backEvent = Channel<Unit>()
    val backEvent = _backEvent.receiveAsFlow()

    init {
        screenModelScope.launch {
            withContext(dispatcher) {
                id?.let {
                    val todo = todoRepository.get(it)
                    _title.update { todo.title }
                    _description.update { todo.description }
                    _done.update { todo.done }
                    _deadline.update { todo.deadline.toDeadline() }
                }
            }
        }
    }

    override fun setTitle(title: String) {
        _title.update { title }
    }

    override fun setDescription(description: String) {
        _description.update { description }
    }

    override fun setDone(done: Boolean) {
        _done.update { done }
    }

    override fun setDeadline(deadline: Long) {
        _deadline.update {
            EditTodoUiState.Deadline(milliseconds = deadline)
        }
    }

    override fun showDatePicker() {
        _showDatePicker.update { true }
    }

    override fun dismissDatePicker() {
        _showDatePicker.update { false }
    }

    override fun save() {
        screenModelScope.launch {
            withContext(dispatcher) {
                todoRepository.save(
                    Todo(
                        id = id,
                        title = _title.value,
                        description = _description.value,
                        done = _done.value,
                        deadline = _deadline.value.toLocalDate(),
                    )
                )
            }
        }
        onBack()
    }

    override fun delete() {
        screenModelScope.launch {
            withContext(dispatcher) {
                id?.let { todoRepository.delete(id = it) }
            }
        }
        onBack()
    }

    override fun onBack() {
        _backEvent.trySend(Unit)
    }
}