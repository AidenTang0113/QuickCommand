package com.quickcommand

import android.app.Application
import com.quickcommand.data.AppDatabase

class QuickCommandApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
}
