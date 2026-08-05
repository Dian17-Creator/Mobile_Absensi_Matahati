package id.my.matahati.absensi.data

data class TodoResponse(
    val success: Boolean,
    val my_task: List<TodoItem>,
    val incoming_task: List<TodoItem>
)
