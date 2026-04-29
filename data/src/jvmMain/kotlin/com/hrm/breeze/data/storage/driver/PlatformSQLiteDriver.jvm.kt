package com.hrm.breeze.data.storage.driver

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun createPlatformSQLiteDriver(): SQLiteDriver = BundledSQLiteDriver()
