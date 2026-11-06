package com.fredy.fondecran

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SlideWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        try {
            Slides.applyNext(applicationContext)
        } catch (e: Exception) {
            // on réessaiera à la prochaine période
        }
        return Result.success()
    }
}
