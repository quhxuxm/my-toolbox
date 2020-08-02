package com.quhxuxm.quh.mytoolbox.qqtools.audio

import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.CollectResourceType
import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.MyCollectionResourceType
import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.QQInfo
import com.quhxuxm.quh.mytoolbox.qqtools.common.collector.MyCollectionResourceCollector
import com.quhxuxm.quh.mytoolbox.qqtools.common.exception.QQToolsException
import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*

object AudioTool {
    private const val FFMPEG_EXE: String = "ffmpeg.exe"
    private const val SILK_V3_DECODER_EXE: String = "silk_v3_decoder.exe"

    fun collectAudio(qqUsername: String, osUserDir: Path, targetPath: Path, historyFilePath: Path) {
        val targetDirFile = targetPath.toFile();
        if (!targetDirFile.exists()) {
            targetDirFile.mkdirs()
        }
        val convertingDir = targetPath.resolve(".converting")
        val convertingDirFile = convertingDir.toFile()
        if (!convertingDirFile.exists()) {
            convertingDirFile.mkdirs()
        }
        val ffmpegExeAbsolutePath = convertingDir.resolve(FFMPEG_EXE)
        val silkV3DecoderExeAbsolute = convertingDir.resolve(SILK_V3_DECODER_EXE)
        val ffmpegExeInputStream = AudioTool::class.java.classLoader.getResourceAsStream(FFMPEG_EXE)
        if (ffmpegExeInputStream == null) {
            throw QQToolsException()
        }
        val silkV3DecoderExeInputStream = AudioTool::class.java.classLoader.getResourceAsStream(SILK_V3_DECODER_EXE)
        if (silkV3DecoderExeInputStream == null) {
            throw QQToolsException()
        }
        Files.copy(ffmpegExeInputStream, ffmpegExeAbsolutePath, StandardCopyOption.REPLACE_EXISTING)
        Files.copy(silkV3DecoderExeInputStream, silkV3DecoderExeAbsolute, StandardCopyOption.REPLACE_EXISTING)
        val randomTmpAmrFileDirPath = convertingDir.resolve(UUID.randomUUID().toString().replace("-", ""))
        val randomTmpPcmFileDirPath = convertingDir.resolve(UUID.randomUUID().toString().replace("-", ""))
        randomTmpAmrFileDirPath.toFile().mkdirs()
        randomTmpPcmFileDirPath.toFile().mkdirs()
        val qqInfo = QQInfo(qqUsername, osUserDir)
        MyCollectionResourceCollector.collectFiles(qqInfo, MyCollectionResourceType.AUDIO, CollectResourceType.AMR,
                randomTmpAmrFileDirPath, historyFilePath) {
            val amrFilesFolder = it.toFile()
            amrFilesFolder.listFiles()?.forEach { amrFile ->
                val targetPcmFilePath = randomTmpPcmFileDirPath.resolve("${amrFile.name}.pcm")
                val silkV3Command =
                        "${silkV3DecoderExeAbsolute} ${amrFile.absolutePath} ${targetPcmFilePath} -Fs_API 16000"
                println(silkV3Command)
                Runtime.getRuntime().exec(silkV3Command)
                val targetMp3FilePath = targetPath.resolve("${targetPcmFilePath.fileName}.mp3")
                val ffmpegCommand =
                        "${ffmpegExeAbsolutePath} -y -f s16le -ar 16000 -ac 1 -i ${targetPcmFilePath} ${targetMp3FilePath}"
                println(ffmpegCommand)
                Runtime.getRuntime().exec(ffmpegCommand)
            }
        }
        FileUtils.deleteDirectory(convertingDirFile)
    }
}