package com.example.emptys

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.ClipboardManager
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.emptys.databinding.ActivityMainBinding
import com.googlecode.tesseract.android.TessBaseAPI
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import id.zelory.compressor.Compressor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


public lateinit var bitmaps: Bitmap // текущая картинка, нужна для передачи в другую форму
public lateinit var rusLng: RadioButton   // содержит состояние переключателя русского языка

class MainActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imgView: ImageView
    private lateinit var btnCrop: Button
    private lateinit var btnCopy: Button
    private lateinit var cropImageView: CropImageView
    private lateinit var tvResult: TextView
    private lateinit var localBitmap: Bitmap

    // при запуске приложения
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater, null, false)

        setContentView(binding.root)
        tvResult = findViewById(R.id.tvResult)
        imgView = findViewById(R.id.imgView)
        btnCrop = findViewById(R.id.crop_button)
        btnCopy = findViewById(R.id.copy_button)
        cameraExecutor = Executors.newSingleThreadExecutor()
        requestPermission()
        
        btnCrop.setOnClickListener()
        {
            val intent = Intent(this, Crop_img::class.java)
            bitmaps = localBitmap
            startActivity(intent)
        }

        btnCopy.setOnClickListener()
        {
            setClipboard(this,tvResult.text.toString());
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            var result = CropImage.getActivityResult(data)
            imgView.setImageURI(result.uri)
        }
    }

    // запрос разрешений приложением, и начало съемки камерой
    private fun requestPermission()
    {
        requestPermissionMissing { granted ->
            if (granted)
                startCamera()
            else
                Toast.makeText(this, "allow necessary permision", Toast.LENGTH_SHORT).show()
        }
    }

    // отсутсвуют разрешения
    private fun requestPermissionMissing(onResult: ((Boolean) -> Unit)) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
            onResult(true)
        else
            registerForActivityResult(ActivityResultContracts.RequestPermission())
            {
                onResult(it)
            }.launch(Manifest.permission.CAMERA)
    }

    // начало съемки изображений с камеры
    private fun startCamera()
    {
        val processCameraProvider = ProcessCameraProvider.getInstance(this)
        processCameraProvider.addListener({
            try
            {
                val cameraProvider = processCameraProvider.get()
                val previewUseCase = buildProvideUse()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase
                )
                imageRefresh()
            } catch (e: java.lang.Exception) {
            }
        }, ContextCompat.getMainExecutor(this))

    }

    // соединение камеры и картинки
    private fun buildProvideUse(): Preview
    {
        return Preview.Builder().build()
            .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
    }

    // при обновлении изображения
    private fun imageRefresh()
    {

        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {

                var image: ImageView
                var surf: PreviewView
                image = findViewById(R.id.imgView)
                surf = findViewById(R.id.previewView)
                image.setImageBitmap(surf.bitmap)

                var emptyBitmap: Bitmap = surf.bitmap ?: createBitmap(100, 100)

                localBitmap = emptyBitmap
                var Filesdir = this@MainActivity.filesDir;
                var ThisActivity = this@MainActivity;
                rusLng = findViewById(R.id.rusLng)
                tvResult = findViewById(R.id.tvResult)

                var Tess = ConvertTask();
                Tess.ParamInit(ThisActivity, tvResult, localBitmap, Filesdir)
                Tess.LangSetup(rusLng)
                Tess.execute()

                handler.postDelayed(this, 1000)//1 sec delay
            }
        }, 0)
    }

}


// публичный класс для распознавания текста
public class ConvertTask : AsyncTask<File, Void, String>()
{
    internal var tesseract = TessBaseAPI()
    private lateinit var Filesdir: File
    private lateinit var ThisActivity: AppCompatActivity
    private lateinit var tvResult: TextView
    private lateinit var bitmapsClass: Bitmap
    private lateinit var rusLng: RadioButton

    // метод для передачи параметров
    public fun ParamInit(thisAct: AppCompatActivity, outext: TextView, bit: Bitmap, fdir: File)
    {
        ThisActivity = thisAct;
        tvResult = outext;
        bitmapsClass = bit;
        Filesdir = fdir;
    }

    // метод для изменения языка
    public fun LangSetup(rus: RadioButton)
    {
        rusLng = rus;
    }

    // передача параметров и выполнение распознавания
    override fun onPreExecute()
    {
        super.onPreExecute()
        val datapath = "$Filesdir/tesseract/";

        var lng: String = "rus"
        if(rusLng.isChecked)
        {
            lng = "rus"
        }
        else
        {
            lng = "eng"
        }

        FileUtil.langs = lng
        ImageCompressor.langs = lng

        FileUtil.checkFile(
            ThisActivity,
            datapath.toString(),
            File(datapath + "tessdata/")
       )

        tesseract.init(datapath, lng)
    }

    // завершение распознавания
    override fun doInBackground(vararg files: File): String
    {
        val options = BitmapFactory.Options()
        options.inSampleSize = 4
        tesseract.setImage(bitmapsClass)
        val result = tesseract.utF8Text
        tesseract.end()
        return result
    }

    // после распознавания показать результат
    override fun onPostExecute(result: String)
    {
        super.onPostExecute(result)
        tvResult.text = result
    }
}


// дополнительные функции
object FileUtil
{
    var langs: String = "rus"

    // проверка файлов в директории приложения
    fun checkFile(context: Context, datapath: String, dir: File)
    {
        if (!dir.exists() && dir.mkdirs())
        {
            copyFiles(context, datapath)
        }

        if (dir.exists())
        {
            val datafilepath = "$datapath/tessdata/" + langs + ".traineddata"
            val datafile = File(datafilepath)
            if (!datafile.exists()) {
                copyFiles(context, datapath)
            }
        }
    }

    // переносит файлы в директорию приложения
    public fun copyFiles(context: Context, DATA_PATH: String)
    {

        try {
            val path = "tessdata"
            val fileList = context.assets.list(path)

            for (fileName in fileList!!) {
                val pathToDataFile = "$DATA_PATH$path/$fileName"
                if (!File(pathToDataFile).exists()) {
                    val inputStream = context.assets.open("$path/$fileName")
                    val out = FileOutputStream(pathToDataFile)

                    val buf = ByteArray(1024)
                    var len: Int
                    len = inputStream.read(buf)
                    while (len > 0) {
                        out.write(buf, 0, len)
                        len = inputStream.read(buf)
                    }
                    inputStream.close()
                    out.close()

                    Log.d("copyFiles", "Copied " + fileName + "to tessdata")
                }
            }
        } catch (e: IOException) {
            Log.e("copyFiles", "Unable to copy files to tessdata $e")
        }

    }

}

// сжатие изображений, если они слишком большие
object ImageCompressor
{
    var langs: String = "rus"

    fun compress(context: Context, imageFile: File?): File {
        val path = "${context.filesDir}/tesseract/tessdata/" + langs + ".traineddata"
        Log.d("tesseract", path)
        return Compressor(context)
            .setMaxWidth(1024)
            .setMaxHeight(1024)
            .setDestinationDirectoryPath(path)
            .compressToFile(imageFile)
    }
}

// копирование в буфер обмена
public fun setClipboard(context: Context, text: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.text = text
    } else {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }
}