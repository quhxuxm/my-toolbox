import com.quhxuxm.quh.mytoolbox.qqtools.audio.AudioTool
import java.nio.file.Path

fun main() {
    AudioTool.collectAudio(
            qqUsername = "1355784643",
            osUserDir = Path.of("C:\\Users\\quhxu"),
            targetPath = Path.of("D:\\tmp"),
            historyFilePath = Path.of("D:\\.history")
    )
}