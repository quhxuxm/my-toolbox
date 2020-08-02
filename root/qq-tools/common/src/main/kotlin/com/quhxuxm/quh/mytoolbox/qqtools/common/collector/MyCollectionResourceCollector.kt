package com.quhxuxm.quh.mytoolbox.qqtools.common.collector

import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.CollectResourceType
import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.MyCollectionResourceType
import com.quhxuxm.quh.mytoolbox.qqtools.common.bo.QQInfo
import com.quhxuxm.quh.mytoolbox.qqtools.common.exception.QQToolsException
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import java.io.FileFilter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * A example C:\Users\quhxu\Documents\Tencent Files\1355784643\MyCollection\Audio
 */
object MyCollectionResourceCollector {
    private const val DOCUMENT_FOLDER_NAME = "Documents"
    private const val TENCENT_FILES_FOLDER_NAME = "Tencent Files"
    private const val MY_COLLECTION_FOLDER_NAME = "MyCollection"
    private val logger = LoggerFactory.getLogger(MyCollectionResourceCollector::class.java)
    fun collectFiles(qqInfo: QQInfo, myCollectionResourceType: MyCollectionResourceType,
                     targetCollectResourceType: CollectResourceType, targetFolderPath: Path,
                     historyFilePath: Path = Path.of("./tmp", ".history")) {
        val qqResourceFolderPath =
                qqInfo.osUserFolderPath.resolve(DOCUMENT_FOLDER_NAME).resolve(TENCENT_FILES_FOLDER_NAME)
                        .resolve(qqInfo.qqUsername).resolve(MY_COLLECTION_FOLDER_NAME)
                        .resolve(myCollectionResourceType.folderName)
        val qqResourceFolderFile = qqResourceFolderPath.toFile()
        if (!qqResourceFolderFile.isDirectory) {
            logger.error("The qq resource path: $qqResourceFolderPath is not a directory.")
            throw QQToolsException()
        }
        val targetFolderFile = targetFolderPath.toFile()
        if (!targetFolderFile.exists()) {
            if (!targetFolderPath.toFile().mkdirs()) {
                logger.error("Fail to create target folder: $targetFolderPath.")
                throw QQToolsException()
            }
        }
        val historyFile = historyFilePath.toFile()
        val historyFileLines: MutableList<String> = if (historyFile.exists()) {
            ArrayList(historyFile.readLines())
        } else {
            historyFile.createNewFile()
            ArrayList()
        }
        qqResourceFolderFile.listFiles(FileFilter {
            if (it.isDirectory) {
                return@FileFilter false
            }
            if (it.name.endsWith(targetCollectResourceType.suffix.toUpperCase())) {
                return@FileFilter true
            }
            if (it.name.endsWith(targetCollectResourceType.suffix.toLowerCase())) {
                return@FileFilter true
            }
            return@FileFilter false
        })?.forEach {
            val targetFilePath = targetFolderPath.resolve(it.name)
            val fileBytes = it.readBytes()
            val fileMd5Bytes = DigestUtils.md5(fileBytes)
            val fileMd5HexString = Hex.encodeHexString(fileMd5Bytes)
            logger.debug("Current file: $targetFilePath convert to md5 string: $fileMd5HexString")
            if (historyFileLines.contains(fileMd5HexString)) {
                return@forEach
            }
            Files.copy(it.toPath(), targetFilePath, StandardCopyOption.REPLACE_EXISTING)
            historyFileLines.add(fileMd5HexString)
        }
        historyFile.outputStream().channel.truncate(0)
        historyFileLines.forEach {
            historyFile.appendText("$it\n")
        }
    }
}