package com.quhxuxm.quh.mytoolbox.qqtools.audio

import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.CollectResourceType
import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.MyCollectionResourceType
import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.QQInfo
import com.quhxuxm.quh.mytoolbox.qqtools.common.collector.MyCollectionResourceCollector
import com.quhxuxm.quh.mytoolbox.qqtools.common.exception.QQToolsException
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

object AudioTool {
    private const val FFMPEG_EXE: String = "ffmpeg.exe"
    private const val SILK_V3_DECODER_EXE: String = "silk_v3_decoder.exe"
    private val threadPool = Executors.newScheduledThreadPool(10)

    private fun printProcessMessage(input: InputStream) {
        val reader = InputStreamReader(input)
        val bf = BufferedReader(reader);
        var line: String? = bf.readLine();
        while (line != null) {
            println(line)
            line = bf.readLine()
        }
    }

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
        val randomDirName = UUID.randomUUID().toString().replace("-", "")
        val randomTmpAmrFileDirPath =
                convertingDir.resolve(randomDirName).resolve("amr")
        val randomTmpPcmFileDirPath =
                convertingDir.resolve(randomDirName).resolve("pcm")
        val randomTmpMp3FileDirPath =
                convertingDir.resolve(randomDirName).resolve("mp3")
        val randomTmpMp3MergeDirPath =
                convertingDir.resolve(randomDirName).resolve("merge_mp3")
        randomTmpAmrFileDirPath.toFile().mkdirs()
        randomTmpPcmFileDirPath.toFile().mkdirs()
        randomTmpMp3FileDirPath.toFile().mkdirs()
        randomTmpMp3MergeDirPath.toFile().mkdirs()
        val qqInfo = QQInfo(qqUsername, osUserDir)
        MyCollectionResourceCollector.collectFiles(qqInfo, MyCollectionResourceType.AUDIO, CollectResourceType.AMR,
                randomTmpAmrFileDirPath, historyFilePath) {
            val amrFilesFolder = it.toFile()
            val allAmrFiles = amrFilesFolder.listFiles() as Array<File>
            val allAmrFilesNumber = allAmrFiles.size
            val amrFileConvertCountDownLatch = CountDownLatch(allAmrFilesNumber)
            allAmrFiles.asSequence().forEach { amrFile ->
                threadPool.submit {
                    val pcmFilePath = randomTmpPcmFileDirPath.resolve("${amrFile.name}.pcm")
                    val silkV3Command =
                            "${silkV3DecoderExeAbsolute} ${amrFile.absolutePath} ${pcmFilePath} -Fs_API 16000"
                    println(silkV3Command)
                    val silkCommendProcess = Runtime.getRuntime().exec(silkV3Command)
                    printProcessMessage(silkCommendProcess.errorStream)
                    silkCommendProcess.waitFor()
                    val mp3FilePath = randomTmpMp3FileDirPath.resolve("${pcmFilePath.fileName}.mp3")
                    val ffmpegCommand =
                            "${ffmpegExeAbsolutePath} -y -f s16le -ar 16000 -ac 1 -i ${pcmFilePath} ${mp3FilePath}"
                    println(ffmpegCommand)
                    val ffmpegCommandProcess = Runtime.getRuntime().exec(ffmpegCommand)
                    printProcessMessage(ffmpegCommandProcess.errorStream)
                    ffmpegCommandProcess.waitFor()
                    pcmFilePath.toFile().delete()
                    amrFileConvertCountDownLatch.countDown()
                }
            }
            amrFileConvertCountDownLatch.await()
            val mp3FilesToMerge = randomTmpMp3FileDirPath.toFile().listFiles() as Array<File>
            if (mp3FilesToMerge.isEmpty()) {
                return@collectFiles
            }
            val finalMp3File = mp3FilesToMerge.asSequence().reduce { f1, f2 ->
                val tmpMergedFileName = UUID.randomUUID().toString().replace("-", "")
                val mergedMp3FilePath = randomTmpMp3MergeDirPath.resolve("${tmpMergedFileName}.mp3")
                val mergeCommand =
                        """$ffmpegExeAbsolutePath -i "concat:${f1.absolutePath}|${f2.absolutePath}" -acodec copy $mergedMp3FilePath"""
                println(mergeCommand)
                val mergeCommandProcess = Runtime.getRuntime().exec(mergeCommand)
                printProcessMessage(mergeCommandProcess.errorStream)
                mergeCommandProcess.waitFor()
                f1.delete()
                f2.delete()
                return@reduce mergedMp3FilePath.toFile()
            }
            Files.copy(finalMp3File.toPath(), targetPath.resolve(finalMp3File.name),
                    StandardCopyOption.REPLACE_EXISTING)
        }
    }
}