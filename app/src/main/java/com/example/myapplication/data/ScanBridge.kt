package com.example.myapplication.data

import com.example.myapplication.data.model.ScanResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ScanBridge {
    // A pipeline that holds up to 10 recent scans while the UI wakes up to read them
    private val _incomingScans = MutableSharedFlow<ScanResult>(extraBufferCapacity = 10)
    val incomingScans = _incomingScans.asSharedFlow()

    // The SmsReceiver will call this function
    fun reportScan(result: ScanResult) {
        _incomingScans.tryEmit(result)
    }
}