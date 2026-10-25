package com.fieldnotes.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.fieldnotes.AppContainer
import com.fieldnotes.FieldNotesApp

class TestFieldNotesApp : FieldNotesApp() {
    override fun createContainer(): AppContainer = AppContainer(this, inMemory = true)
}

class FieldNotesTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, TestFieldNotesApp::class.java.name, context)
}
