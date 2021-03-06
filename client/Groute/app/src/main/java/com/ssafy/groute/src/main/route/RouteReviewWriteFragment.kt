package com.ssafy.groute.src.main.route

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import com.ssafy.groute.R
import com.ssafy.groute.config.ApplicationClass
import com.ssafy.groute.config.BaseFragment
import com.ssafy.groute.databinding.FragmentRouteReviewWriteBinding
import com.ssafy.groute.src.dto.PlanReview
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.jakewharton.rxbinding3.widget.textChanges
import com.ssafy.groute.src.main.MainActivity
import com.ssafy.groute.src.service.UserPlanService
import com.ssafy.groute.src.viewmodel.PlanViewModel
import com.ssafy.groute.util.RetrofitCallback
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

private const val TAG = "RouteReviewWriteFragment"
class RouteReviewWriteFragment : BaseFragment<FragmentRouteReviewWriteBinding>(FragmentRouteReviewWriteBinding::bind, R.layout.fragment_route_review_write) {
    private lateinit var mainActivity: MainActivity
    private val planViewModel: PlanViewModel by activityViewModels()
    private var planId = -1
    private var reviewId = -1
    private var imgSelectedChk = false

    private lateinit var editTextSubscription: Disposable // edit text subscribe

    // ?????? ??????
    private lateinit var imgUri: Uri    // ?????? uri
    private var fileExtension : String? = ""    // ?????? ?????????

    // ?????? ??????
    private var permissionListener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() { // ?????? ????????? ?????? ??? ??????
            selectImg()
        }

        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
            showCustomToast("Permission Denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity.hideMainProfileBar(true)
        mainActivity.hideBottomNav(true)
        arguments?.let {
            planId = it.getInt("planId",-1)
            reviewId = it.getInt("reviewId",-1)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.planViewModel = planViewModel

        runBlocking {
            planViewModel.getPlanById(planId, 2)
            planViewModel.getPlanReviewById(reviewId)
        }

        binding.routeReviewWriteIbtnBack.setOnClickListener{
            mainActivity.supportFragmentManager.beginTransaction().remove(this).commit()
            mainActivity.supportFragmentManager.popBackStack()
        }

        // ?????? ????????? ??????
        if(reviewId > 0) {
            var beforeImg = ""
            binding.routeReviewWriteBtnWrite.text = "?????? ??????"
            planViewModel.review.observe(viewLifecycleOwner, Observer {
                binding.planReview = it // ????????? planReview data setting

                if(!(it.img == "null" || it.img == "" || it.img == null)) {
                    binding.planReviewWriteLLayoutSetImg.visibility = View.VISIBLE
                    beforeImg = it.img.toString()
                    // ?????? set
                    Glide.with(requireContext())
                        .load("${ApplicationClass.IMGS_URL}${beforeImg}")
                        .into(binding.planReviewWriteIvSelectImg)

                    // ?????? ?????? set
                    binding.planReviewWriteTvImgName.text = beforeImg.substring(beforeImg.lastIndexOf("/") + 1, beforeImg.length)
                } else {
                    beforeImg = ""
                    binding.planReviewWriteLLayoutSetImg.visibility = View.GONE
                }
                initModifyButton(beforeImg)
            })
        } else {
            initInsertButton()
        }

        selectImgBtnEvent()
        initTiedListener()
        imgDeleteBtnEvent()
    }

    // init TextInputEditText Listener
    private fun initTiedListener() {
        editTextSubscription = binding.planReviewWriteTietContent
            .textChanges()
            .subscribe {
                textLengthChk(it.toString())
            }
    }

    // planReview Content Text ?????? ??????
    private fun textLengthChk(str : String) : Boolean {
        if(str.trim().isEmpty()){
            binding.planReviewWriteTilContent.error = "Required Field"
            binding.planReviewWriteTietContent.requestFocus()
            return false
        } else if(str.length <= 30 || str.length >= 255) {
            binding.planReviewWriteTilContent.error = "30??? ?????? 255??? ????????? ??????????????????."
            binding.planReviewWriteTietContent.requestFocus()
            return false
        }
        else {
            binding.planReviewWriteTilContent.error = null
            return true
        }
    }

    // ?????? ?????? ?????? ?????????
    private fun initModifyButton(beforeImg : String){
        binding.routeReviewWriteBtnWrite.setOnClickListener {
            if(textLengthChk(binding.planReviewWriteTietContent.text.toString()) == true) {

                val content = binding.planReviewWriteTietContent.text.toString()
                val rate = binding.routeReviewWriteRatingBar.rating.toDouble()
                val userId = ApplicationClass.sharedPreferencesUtil.getUser().id
                val review = PlanReview(
                    planId,
                    userId,
                    content,
                    rate,
                    beforeImg,
                    reviewId
                )
                setData(review, false)  // ???????????? planReview data ????????? ???????????? ?????? ????????? ??????
            } else {
                showCustomToast("?????? ?????? ????????? ?????????")
            }
        }

    }

    // review ?????? ?????? ?????? ?????????
    private fun initInsertButton(){
        binding.routeReviewWriteBtnWrite.setOnClickListener {
            if(textLengthChk(binding.planReviewWriteTietContent.text.toString()) == true) {
                val content = binding.planReviewWriteTietContent.text.toString()
                val rate = binding.routeReviewWriteRatingBar.rating.toDouble()
                val userId = ApplicationClass.sharedPreferencesUtil.getUser().id
                val review = PlanReview(
                    planId,
                    userId,
                    content,
                    rate,
                    ""
                )
                setData(review, true)
            } else {
                showCustomToast("?????? ?????? ????????? ?????????.")
            }
        }
    }

    // ?????? ?????? ?????? ?????? ?????????
    private fun imgDeleteBtnEvent() {
        binding.planReviewWriteIbDeletedImg.setOnClickListener {
            imgUri = Uri.EMPTY
            imgSelectedChk = true
            binding.planReviewWriteLLayoutSetImg.visibility = View.GONE
        }
    }

    /**
     * ?????? ?????? ?????? ?????? ?????????
     */
    private fun selectImgBtnEvent() {

        if (::imgUri.isInitialized) {
            Log.d(TAG, "onCreate: $imgUri")
        } else {
            imgUri = Uri.EMPTY
            Log.d(TAG, "fileUri ?????????  $imgUri")
        }

        binding.routeReviewWriteButtonAddImg.setOnClickListener {
            checkPermissions()
        }
    }

    /**
     * ????????? ?????? ?????? intent launch
     */
    private fun selectImg() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        filterActivityLauncher.launch(intent)
    }

    /**
     * read gallery ?????? ??????
     */
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= 26) { // ????????? ??? ??? ?????? ??? ?????? ?????? ?????????
            val pm: PackageManager = requireContext().packageManager
            if (!pm.canRequestPackageInstalls()) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context?.packageName}")
                    )
                )
            }
        }

        if (Build.VERSION.SDK_INT >= 23) { // ????????????(??????????????? 6.0) ?????? ?????? ??????
            TedPermission.create()
                .setPermissionListener(permissionListener)
                .setRationaleMessage("?????? ???????????? ???????????? ?????? ????????? ???????????????")
                .setDeniedMessage("If you reject permission,you can not use this service\n" +
                        "\n\nPlease turn on permissions at [Setting] > [Permission] ")
                .setPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ).check()
        } else {
            selectImg()
        }
    }

    /**
     * ????????? ?????? ?????? result
     */
    private val filterActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == AppCompatActivity.RESULT_OK && it.data != null) {
                binding.planReviewWriteLLayoutSetImg.visibility = View.VISIBLE
                imgSelectedChk = false
                val currentImageUri = it.data?.data

                try {
                    currentImageUri?.let {
                        if(Build.VERSION.SDK_INT < 28) {
                            imgUri = currentImageUri
                            fileExtension = requireActivity().contentResolver.getType(currentImageUri)
                            // ?????? set
                            Glide.with(this)
                                .load(currentImageUri)
                                .into(binding.planReviewWriteIvSelectImg)
                            val extension = fileExtension!!.substring(fileExtension!!.lastIndexOf("/") + 1, fileExtension!!.length)
                            // ?????? ?????? set
                            binding.planReviewWriteTvImgName.text = "${currentImageUri.lastPathSegment}.$extension"

                        } else {
                            imgUri = currentImageUri
                            fileExtension = requireActivity().contentResolver.getType(currentImageUri)
                            // ?????? set
                            Glide.with(this)
                                .load(currentImageUri)
                                .into(binding.planReviewWriteIvSelectImg)
                            val extension = fileExtension!!.substring(fileExtension!!.lastIndexOf("/") + 1, fileExtension!!.length)

                            // ?????? ?????? set
                            binding.planReviewWriteTvImgName.text = "${currentImageUri.lastPathSegment}.$extension"

                        }
                    }
                } catch(e:Exception) {
                    e.printStackTrace()
                }
            } else if(it.resultCode == AppCompatActivity.RESULT_CANCELED){
                showCustomToast("?????? ?????? ??????")
                if(binding.planReviewWriteTvImgName.length() > 0) {  // ?????? ????????? ????????? visible
                    binding.planReviewWriteLLayoutSetImg.visibility = View.VISIBLE
                } else {
                    binding.planReviewWriteLLayoutSetImg.visibility = View.GONE
                    imgUri = Uri.EMPTY
                }
            } else{
                binding.planReviewWriteLLayoutSetImg.visibility = View.GONE
                Log.d(TAG,"filterActivityLauncher ??????")
            }
        }



    /**
     * insert & update planReview
     * call server
     */
    private fun setData(planReview: PlanReview, chk: Boolean) {

        // ???????????? ????????? ??????
        if(imgUri == Uri.EMPTY) {
            if(imgSelectedChk == true) {
                planReview.img = ""
            }
            val gson : Gson = Gson()
            val json = gson.toJson(planReview)
            val rBody_planReivew = RequestBody.create(MediaType.parse("text/plain"), json)
            if(chk) {   // planReview ????????? ??????
                UserPlanService().insertPlanReview(rBody_planReivew, null, InsertPlanReviewCallback())
            } else {    // planReview ????????? ??????
                UserPlanService().updatePlanReview(rBody_planReivew, null, UpdatePlanReviewCallback())
            }
        }
        // ????????? ?????? + ?????? ????????? ??????
        else {
            val file = File(imgUri.path!!)

            var inputStream: InputStream? = null
            try {
                inputStream = requireActivity().contentResolver.openInputStream(imgUri)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream)  // ???????????? ??????
            val requestBody = RequestBody.create(MediaType.parse("image/*"), byteArrayOutputStream.toByteArray())
            val extension = fileExtension!!.substring(fileExtension!!.lastIndexOf("/") + 1, fileExtension!!.length)
            val uploadFile = MultipartBody.Part.createFormData("img", "${file.name}.${extension}", requestBody)
            val gson : Gson = Gson()
            val json = gson.toJson(planReview)
            val rBody_planReivew = RequestBody.create(MediaType.parse("text/plain"), json)
            if(chk) {   // ????????? ????????? ??????
                UserPlanService().insertPlanReview(rBody_planReivew, uploadFile, InsertPlanReviewCallback())
            } else {    // ????????? ????????? ??????
                UserPlanService().updatePlanReview(rBody_planReivew, uploadFile, UpdatePlanReviewCallback())
            }
        }

    }


    // review insert callback
    inner class InsertPlanReviewCallback : RetrofitCallback<Boolean> {
        override fun onError(t: Throwable) {
            Log.d(TAG, "onError: ")
        }

        override fun onSuccess(code: Int, responseData: Boolean) {
            if (responseData == true) {
                mainActivity.moveFragment(12, "planIdDetail", planId, "planIdUser", -1)
                showCustomToast("?????? ?????? ??????")
            }
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onFailure: ")
        }

    }

    // update planReview Callback
    inner class UpdatePlanReviewCallback : RetrofitCallback<Boolean> {
        override fun onError(t: Throwable) {
            Log.d(TAG, "onError: ")
        }

        override fun onSuccess(code: Int, responseData: Boolean) {
            if (responseData == true) {
                mainActivity.moveFragment(12, "planIdDetail", planId, "planIdUser", -1)
                showCustomToast("?????? ?????? ??????")
            }
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onFailure: ")
        }

    }

    override fun onDestroy() {
        editTextSubscription.dispose()
        mainActivity.hideBottomNav(false)
        super.onDestroy()
    }

    companion object {

        @JvmStatic
        fun newInstance(key1: String, value1: Int, key2: String, value2: Int) =
            RouteReviewWriteFragment().apply {
                arguments = Bundle().apply {
                    putInt(key1, value1)
                    putInt(key2, value2)
                }
            }
    }
}