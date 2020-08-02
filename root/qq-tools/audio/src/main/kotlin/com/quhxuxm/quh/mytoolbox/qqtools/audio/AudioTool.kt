package com.quhxuxm.quh.mytoolbox.qqtools.audio

import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.CollectResourceType
import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.MyCollectionResourceType
import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.QQInfo
import com.quhxuxm.quh.mytoolbox.qqtools.common.collector.MyCollectionResourceCollector
import java.nio.file.Path

object AudioTool {
    fun collectAudio() {
        val qqInfo = QQInfo("1355784643", Path.of("C:\\Users\\quhxu"))
        val targetPath = Path.of("D:\\tmp")
        val historyFilePath = Path.of("D:\\.history")
        MyCollectionResourceCollector.collectFiles(qqInfo, MyCollectionResourceType.AUDIO, CollectResourceType.AMR,
                targetPath, historyFilePath)
    }
}