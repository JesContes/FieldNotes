package com.fieldnotes

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.fieldnotes.data.FieldNotesRepository
import com.fieldnotes.data.local.FieldNotesDatabase
import com.fieldnotes.data.prefs.SettingsStore
import com.fieldnotes.data.transfer.TransferFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

open class FieldNotesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
    }

    /** 测试子类覆盖此方法以注入 in-memory 数据库。 */
    protected open fun createContainer(): AppContainer = AppContainer(this)
}

class AppContainer(
    context: Context,
    inMemory: Boolean = false,
) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    internal val database: FieldNotesDatabase =
        if (inMemory) {
            Room.inMemoryDatabaseBuilder(appContext, FieldNotesDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        } else {
            Room.databaseBuilder(appContext, FieldNotesDatabase::class.java, "fieldnotes.db").build()
        }

    val settingsStore = SettingsStore(appContext)
    val repository = FieldNotesRepository(database)
    val transferFiles = TransferFiles
    val applicationContext: Context get() = appContext
    val todayFlow = MutableStateFlow(LocalDate.now())

    init {
        appScope.launch { repository.seedIfEmpty() }
    }

    fun onAppResumed() {
        val today = LocalDate.now()
        if (todayFlow.value != today) todayFlow.value = today
    }
}
