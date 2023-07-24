package com.example.emptys

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.theartofdev.edmodo.cropper.CropImageView


class Crop_img : AppCompatActivity()
{

    private lateinit var cropImageView: CropImageView
    private lateinit var btnCrops: Button
    private lateinit var btnAnalyz: Button
    private lateinit var btnCopy: Button

    // при запуске второй формы
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_img)

        //actionbar
        val actionbar = supportActionBar
        actionbar!!.title = "crop activity"
        actionbar.setDisplayHomeAsUpEnabled(true)

        cropImageView = findViewById(R.id.cropImageView)
        cropImageView.setImageBitmap(bitmaps);

        var tvResult: TextView
        tvResult = findViewById(R.id.tvResult)

        // обрезка
        btnCrops = findViewById(R.id.btn)
        btnCrops.setOnClickListener()
        {
            val cropped = cropImageView.croppedImage
            cropImageView.setImageBitmap(cropped);
        }

        // распознавание
        btnAnalyz = findViewById(R.id.Tesser)
        btnAnalyz.setOnClickListener()
        {
            var localBitmap = cropImageView.croppedImage;
            var ThisActivity = this@Crop_img;
            var Filesdir = this@Crop_img.filesDir;

            var Tess = ConvertTask();
            Tess.ParamInit(ThisActivity, tvResult, localBitmap, Filesdir)
            Tess.LangSetup(rusLng)
            Tess.execute()
        }

        // копировать в буфер обмена
        btnCopy = findViewById(R.id.copyBTN)
        btnCopy.setOnClickListener()
        {
            setClipboard(this,tvResult.text.toString());
        }

    }

    // возвращение назад с помощью стрелочки в хедере
    override fun onSupportNavigateUp(): Boolean
    {
        onBackPressed()
        return true
    }

}
