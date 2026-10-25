package ru.familycinema.app

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/** Installed only in the instrumentation APK. Exposes solely the owned CC0 fixture. */
class TestMediaProvider:ContentProvider() {
 override fun onCreate()=true
 override fun getType(uri:Uri)="video/mp4"
 override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor {
  require(mode=="r")
  val file=File(context!!.cacheDir,"owned-fixture.mp4")
  if(!file.exists())context!!.assets.open("quiet-evening.mp4").use {src->file.outputStream().use {src.copyTo(it)}}
  return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
 }
 override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor?=null
 override fun insert(uri:Uri,values:ContentValues?):Uri?=null
 override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?)=0
 override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?)=0
}
