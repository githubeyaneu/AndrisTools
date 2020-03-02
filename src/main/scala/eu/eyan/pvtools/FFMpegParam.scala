package eu.eyan.pvtools

import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.log.Log
import java.io.File
import eu.eyan.util.java.lang.RuntimePlus

object FFMPegPlus {
  def runFFMpeg(ffmpegPath: String, in: File, out: File, bytesDoneProgress: Long => Unit, params: FFMpegParam*): Int = {
    val parameters = params.map(_.param).flatten
    val ffmpeg = s"""$ffmpegPath"""
    val mini = "-i"
    val inn = s""""$in""""
    val nostdin = "-nostdin"
    val miny = "-y"
    val outt = s""""$out""""
    val cmd = List(ffmpeg, mini, inn, nostdin) ++ parameters ++ List(miny, outt)
    Log.info("Executing " + cmd)

    def computeProgress(s: String): String = { s.findGroup("size= *(\\d*)kB".r).foreach(kB => bytesDoneProgress(kB.toLong * 1024)); s }

    val exitCode = RuntimePlus.execAndProcessOutputs(cmd, computeProgress(_).println: Unit, computeProgress(_).printlnErr: Unit)
    Log.info("ExitCode=" + exitCode)
    exitCode
  }
}

object FFMpegParam {

  def VCODEC_MPEG4 = FFMpegParam("-vcodec", "mpeg4")

  def BITRATE_VIDEO_17M = FFMpegParam("-b:v", "17M")
  def BITRATE_AUDIO_192K = FFMpegParam("-b:a", "192k")

  def AUDIO_CODEC_MP3 = FFMpegParam("-acodec", "libmp3lame")

  def CONSTANT_RATE_FACTOR_VISUALLY_LOSSLESS = FFMpegParam("-crf", "18")
  def PRESET_MEDIUM = FFMpegParam("-preset", "medium")

  def CODEC_VIDEO_LIBX264 = FFMpegParam("-c:v", "libx264")

  def CUT(from: String, to:String) = FFMpegParam("-ss", from, "-t", to)
  def CODEC_AUDIO_COPY = FFMpegParam("-vcodec", "copy")
  def CODEC_VIDEO_COPY = FFMpegParam("-acodec", "copy")
  
  def CROP(x:Int,y:Int,w:Int,h:Int) = FFMpegParam("-filter:v", s""""crop=$w:$h:$x:$y"""")

  def VF(videoParams: FFMpegVideoParam*) = FFMpegParam("-vf", "\""+videoParams.map(_.param).mkString(", ")+"\"")
}

object FFMpegVideoParam {
  def YADIF = FFMpegVideoParam("yadif")
  def TRANSPOSE(transpose: Int) = FFMpegVideoParam(s"""transpose=$transpose""")
  def SCALE_HEIGHT(height: Int) = FFMpegVideoParam(s"""scale=-1:$height""")
  def SCALE_HEIGHT_NOOVERSIZE(height: Int) = FFMpegVideoParam(s"""scale=-1:'min($height,ih)'""")
  def DESHAKE = FFMpegVideoParam("deshake")
}

case class FFMpegVideoParam(param: String)
case class FFMpegParam(param: String*)