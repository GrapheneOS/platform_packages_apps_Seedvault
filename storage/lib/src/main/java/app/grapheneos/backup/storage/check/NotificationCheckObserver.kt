/*
 * SPDX-FileCopyrightText: 2021 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package app.grapheneos.backup.storage.check

import android.content.Context
import app.grapheneos.backup.storage.api.CheckObserver
import app.grapheneos.backup.storage.ui.Notifications

public open class NotificationCheckObserver internal constructor(
    private val n: Notifications,
    private val reportActivityClass: Class<*>,
) : CheckObserver {

    public constructor(
        context: Context,
        reportActivityClass: Class<*>,
    ) : this(Notifications(context), reportActivityClass)

    override fun onStartChecking() {
        n.showCheckStartedNotification()
    }

    override fun onCheckUpdate(speed: Long, thousandth: Int) {
        n.showCheckNotification(speed, thousandth)
    }

    override fun onCheckSuccess(size: Long, speed: Long) {
        n.onCheckComplete(reportActivityClass, size, speed)
    }

    override fun onCheckFoundErrors(size: Long, speed: Long) {
        n.onCheckFinishedWithError(reportActivityClass, size, speed)
    }

}
