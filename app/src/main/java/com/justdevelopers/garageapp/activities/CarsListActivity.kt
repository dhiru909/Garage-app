package com.justdevelopers.garageapp.activities

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.View.OVER_SCROLL_ALWAYS
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.justdevelopers.garageapp.R
import com.justdevelopers.garageapp.databinding.ActivityCarsListBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList

class CarsListActivity : AppCompatActivity() {
    private var carAdapter: CarAdapter? = null
    lateinit var currentPhotoPath: String
    private var saveImageToInternalStorage: Uri? = null
    var binding: ActivityCarsListBinding? = null
    private var mUpdateId=0
    companion object{
        private const val CAMERA_PERMISSION_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
        private const val GALLERY_REQUEST_CODE = 3
        private const val IMAGE_DIRECTORY="GarageAppImages"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityCarsListBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        val carDao = (application as CarApp).db.carDao()
        binding?.btnAddCar?.setOnClickListener {
            addRecords(carDao = carDao)
        }
        binding?.btnLogout?.setOnClickListener {
            finish()
        }
        lifecycleScope.launch{
            carDao.fetchAllCars().collect{
                val list = ArrayList(it)
                runOnUiThread {
                    setupListOfCarIntoRecyclerView(list,carDao)

                }
            }
        }
    }

    private fun addRecords(carDao: CarDao) {
        val model = binding?.etModel?.text.toString()
        val company = binding?.etCompany?.text.toString()
        if(model.isNotEmpty() && company.isNotEmpty()){
            lifecycleScope.launch {
                carDao.insert(CarEntity(model=model, company = company, image = ""))
                runOnUiThread {
                    Toast.makeText(applicationContext,"Record saved",Toast.LENGTH_LONG).show()
                    binding?.etCompany?.text?.clear()
                    binding?.etModel?.text?.clear()
                }
            }
        }else{
            Toast.makeText(applicationContext,"Model or company name can't be empty",Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListOfCarIntoRecyclerView(carList:ArrayList<CarEntity>, carDao: CarDao){
        if (carList.isNotEmpty()){
            carAdapter = CarAdapter(carList,
                {
                addImageId ->
                    addImageDialog(addImageId,carDao)
            },
                {
                    deleteId->
                    deleteRecordDialog(deleteId,carDao)
                }
            )
            binding?.rvCarsList?.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
            binding?.rvCarsList?.adapter = carAdapter

        }
    }

    private fun deleteRecordDialog(deleteId: Int, carDao: CarDao) {
        Log.e("delete","delete")
        try{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete Record")
            lifecycleScope.launch{
                carDao.fetchCarById(deleteId).collect{
                    try {
                        builder.setMessage("Are you sure you want to delete ${it.model}")
                    }catch (e:java.lang.NullPointerException){
                        e.printStackTrace()
                    }
                }
            }
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setPositiveButton("Yes"){
                dialogInterface, _ ->
                lifecycleScope.launch {
                    carDao.delete(CarEntity(deleteId))
                }
                Toast.makeText(applicationContext,"Record deleted successfully",Toast.LENGTH_LONG).show()
                dialogInterface.dismiss()
            }
            builder.setNegativeButton("No"){
                dialogInterface,_ ->
                    dialogInterface.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()
        }catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }

    private fun addImageDialog(addImageId: Int, carDao: CarDao) {
        mUpdateId = addImageId
        val pictureDialog = AlertDialog.Builder(this@CarsListActivity)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery",
            "capture photo from camera")
        pictureDialog.setItems(pictureDialogItems){
                _, which->
            run {
                when (which) {
                    0 -> {
                        choosePhotoFromGallery()
                        if(isReadStorageAllowed()) {
                            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            startActivityForResult(pickIntent, GALLERY_REQUEST_CODE)
                        }
                    }
                    1 -> {
                        Toast.makeText(
                            this@CarsListActivity,
                            "camera opening",
                            Toast.LENGTH_SHORT
                        ).show()
                        if(isCameraAllowed()) {
                            val pickIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            startActivityForResult(pickIntent, CAMERA_REQUEST_CODE)
                        }else{
                            ActivityCompat.requestPermissions(this,
                                arrayOf(Manifest.permission.CAMERA),
                                CAMERA_PERMISSION_CODE
                            )
                        }
                    }
                }
            }
        }
        pictureDialog.show()
    }

    private fun isCameraAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun isReadStorageAllowed():Boolean {
        val result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun choosePhotoFromGallery() {
        Dexter.withContext(this)
            .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if(report.areAllPermissionsGranted()){
                        Toast.makeText(this@CarsListActivity,"" +
                                "permission for storage granted",Toast.LENGTH_LONG).show()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(permissions:MutableList<PermissionRequest>, token: PermissionToken) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this).setMessage("Permissions required, Enable them in setting")
            .setPositiveButton("go to settings"){
                    _,_ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data=uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("cancel"){
                    dialog, _ ->
                try {
                    dialog.dismiss()
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == CAMERA_REQUEST_CODE){
                if(data != null)
                {
                    val image: Bitmap =data.extras!!.get("data") as Bitmap
                    lifecycleScope.launch {
                        saveImageToInternalStorage =saveImageToInternalStorage(image)
                        Log.e("saved image:","path:: $saveImageToInternalStorage")
                            val carDao = (application as CarApp).db.carDao()
                            carDao.fetchCarById(mUpdateId).collect{
                                carDao.update(CarEntity(id=mUpdateId, image = saveImageToInternalStorage.toString(), model = it.model, company = it.company))
                                carDao.fetchAllCars().collect{list->
                                    val arraylist = ArrayList(list)
                                    runOnUiThread {
                                        setupListOfCarIntoRecyclerView(arraylist,carDao)
                                        carAdapter?.notifyDataSetChanged()
                                    }
                                }
                            }
                    }
                }
            }
            else if(requestCode == GALLERY_REQUEST_CODE){
                if(data!=null){
                    val contentUri=data.data
                    val selectedImageBitmap=MediaStore.Images.Media.getBitmap(this.contentResolver,contentUri)
                    lifecycleScope.launch {
                        saveImageToInternalStorage =saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("saved image:","path:: $saveImageToInternalStorage")

                       try {
                            val carDao = (application as CarApp).db.carDao()
                            carDao.fetchCarById(mUpdateId).collect {
                                carDao.update(
                                    CarEntity(
                                        id = mUpdateId,
                                        image = saveImageToInternalStorage.toString(),
                                        model = it.model,
                                        company = it.company
                                    )
                                )
                                carDao.fetchAllCars().collect { list ->
                                    val arraylist = ArrayList(list)
                                    runOnUiThread {
                                        setupListOfCarIntoRecyclerView(arraylist, carDao)
                                        carAdapter?.notifyDataSetChanged()
                                    }
                                }
                            }
                        }catch (e:java.lang.Exception){
                            e.printStackTrace()
                        }

                    }
                }
            }
        }

    }
    private suspend fun saveImageToInternalStorage(mBitmap: Bitmap):Uri{
        var wrapper =  ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")
        withContext(Dispatchers.IO){
            try {
                val stream: OutputStream = FileOutputStream(file)
                mBitmap.compress(Bitmap.CompressFormat.JPEG,30,stream)
                stream.flush()
                stream.close()
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
        return Uri.parse(file.absolutePath)

    }
    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}