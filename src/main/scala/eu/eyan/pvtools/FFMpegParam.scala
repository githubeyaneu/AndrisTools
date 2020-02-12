package eu.eyan.pvtools

import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.log.Log
import java.io.File

object FFMPegPlus {
  def runFFMpeg(ffmpegPath: String, in: File, out: File, bytesDoneProgress: Long => Unit, params: FFMpegParam*): Int = {
    val parameters = params.map(_.param).mkString(" ")
    val cmd = s""" $ffmpegPath -i "$in" $parameters -y "$out" """
    Log.info("Executing " + cmd)
    val exitCode = cmd.executeAsProcessWithResultAndOutputLineCallback(s => {
      println(s)
      s.findGroup("size= *(\\d*)kB".r).foreach(kB => bytesDoneProgress(kB.toLong * 1024))
    })
    Log.info("ExitCode=" + exitCode)
    exitCode
  }
}

object FFMpegParam {

  def VCODEC_MPEG4 = FFMpegParam("-vcodec mpeg4")

  def BITRATE_VIDEO_17M = FFMpegParam("-b:v 17M")
  def BITRATE_AUDIO_192K = FFMpegParam("-b:a 192k")

  def AUDIO_CODEC_MP3 = FFMpegParam("-acodec libmp3lame")

  def CONSTANT_RATE_FACTOR_VISUALLY_LOSSLESS = FFMpegParam("-crf 18")
  def PRESET_MEDIUM = FFMpegParam("-preset medium")

  def CODEC_VIDEO_LIBX264 = FFMpegParam("-c:v libx264")
  def CODEC_AUDIO_COPY = FFMpegParam("-c:a copy")

  def VF_YADIF = FFMpegParam("-vf yadif")
  def TRANSPOSE(transpose: Int) = FFMpegParam(s"""-vf "transpose=$transpose" """)
  def SCALE_HEIGHT(height: Int) = FFMpegParam(s""" -vf scale=-1:$height """)
  def SCALE_HEIGHT_NOOVERSIZE(height: Int) = FFMpegParam(s""" -vf scale=-1:'min($height,ih)' """)
  def DESHAKE = FFMpegParam(s""" -vf deshake """)
}
case class FFMpegParam(param: String)