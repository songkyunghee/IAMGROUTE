package com.ssafy.groute.src.main.my

import android.R
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.ssafy.groute.config.BaseActivity
import com.ssafy.groute.databinding.ActivityProfileEditBinding
import com.ssafy.groute.src.dto.User
import com.ssafy.groute.src.response.UserInfoResponse
import com.ssafy.groute.src.service.UserService
import com.ssafy.groute.src.viewmodel.MainViewModel
import com.ssafy.groute.util.RetrofitCallback
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.regex.Pattern


private const val TAG = "ProfileEditA_groute"
class ProfileEditActivity : BaseActivity<ActivityProfileEditBinding>(ActivityProfileEditBinding::inflate) {

    private var userEmail : String = ""
    private var userBirth : String = ""
    private var userGender : String = ""
    private lateinit var imgUri: Uri
    private var fileExtension : String? = ""
    private val mainViewModel : MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        val userData = intent.getSerializableExtra("userData") as UserInfoResponse

        initData(userData)
        initListeners()
        userBirth = userData.birth
        userEmail = userData.email
        userGender = userData.gender.toString()


        if (::imgUri.isInitialized) {
            Log.d(TAG, "onCreate: $imgUri")
        } else {
            imgUri = Uri.EMPTY
        }

        binding.profileEditFinish.setOnClickListener {
            val user = isAvailable(userData)
            if(user != null) {
                updateUser(user)
            } else {
                showCustomToast("?????? ?????? ?????? ????????? ?????????")
            }
        }

        // ????????? ????????? ?????? ?????? ??????
        binding.profileEditChangeImgTv.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            filterActivityLauncher.launch(intent)
        }

        // ?????? ?????? ??????
        binding.profileEditCancel.setOnClickListener {
            finish()
        }

    }

    // ??????????????? ????????? ????????? ???
    private val filterActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == RESULT_OK && it.data !=null) {
                var currentImageUri = it.data?.data

                try {
                    currentImageUri?.let {
                        if(Build.VERSION.SDK_INT < 28) {

                            Glide.with(this)
                                .load(currentImageUri)
                                .circleCrop()
                                .into(binding.profileEditImg)
                            imgUri = currentImageUri

                            fileExtension = contentResolver.getType(currentImageUri)

                        } else {

                            Glide.with(this)
                                .load(currentImageUri)
                                .circleCrop()
                                .into(binding.profileEditImg)

                            imgUri = currentImageUri
                            fileExtension = contentResolver.getType(currentImageUri)
                        }
                    }

                }catch(e:Exception) {
                    e.printStackTrace()
                }
            } else if(it.resultCode == RESULT_CANCELED){
                showCustomToast("?????? ?????? ??????")
                imgUri = Uri.EMPTY
            }else{
                Log.d("ActivityResult","something wrong")
            }
        }



    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val focusView = currentFocus
        if(focusView != null) {
            val rect = Rect()
            focusView.getGlobalVisibleRect(rect)
            val x = ev?.x
            val y = ev?.y
            if(!rect.contains(x!!.toInt(), y!!.toInt())) {
                val imm : InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                if(imm != null)
                    imm.hideSoftInputFromWindow(focusView.windowToken, 0)
                focusView.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    // ????????? ?????? ????????? ?????????
    private fun initData(user: UserInfoResponse) {

        val id = user.id
        val password = user.password
        val nickname = user.nickname
        val phone = user.phone
        val u = User(id, password, nickname, phone, user.img.toString())
        binding.user = u

        if(user.email != null) {
            if (user.email == "") {
                binding.profileEditEmailEt.setText("")
                binding.profileEditEmailDomain.setText("")
            } else {
                val emailStr = user.email.split("@")
                binding.profileEditEmailEt.setText(emailStr.get(0))
                binding.profileEditEmailDomain.setText(emailStr.get(1))
            }
        }

        if(user.birth != null) {
            if (user.birth != "") {
                val birthStr = user.birth.split("-")
                binding.profileEditSpinnerYear.setSelection(
                    getSpinnerIndex(
                        binding.profileEditSpinnerYear,
                        birthStr.get(0)
                    )
                )
                binding.profileEditSpinnerMonth.setSelection(
                    getSpinnerIndex(
                        binding.profileEditSpinnerMonth,
                        birthStr.get(1)
                    )
                )
                binding.profileEditSpinnerDay.setSelection(
                    getSpinnerIndex(
                        binding.profileEditSpinnerDay,
                        birthStr.get(2)
                    )
                )
            }
        }

        if(user.gender != null) {
            if (user.gender != "") {
                if (user.gender == "M") {
                    binding.profileEditRadioMan.isChecked = true
                } else if (user.gender == "F") {
                    binding.profileEditRadioWoman.isChecked = true
                }
            }
        }
    }

    fun initListeners(){
        binding.profileEditNicknameEt.addTextChangedListener(TextFieldValidation(binding.profileEditNicknameEt))
        binding.profileEditPhoneEt.addTextChangedListener(TextFieldValidation(binding.profileEditPhoneEt))
        binding.profileEditEmailEt.addTextChangedListener(TextFieldValidation(binding.profileEditEmailEt))
        binding.profileEditEmailDomain.addTextChangedListener(TextFieldValidation(binding.profileEditEmailDomain))

        initDomain() // email domain list adapter
        selectedBirth() // birth spinner selectedItem listener
        selectedGender()    // gender radio group Item Selected
    }

    inner class TextFieldValidation(private val view: View): TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            when(view.id){
                com.ssafy.groute.R.id.profile_edit_nickname_et -> {
                    validatedNickname()
                }
                com.ssafy.groute.R.id.profile_edit_phone_et -> {
                    validatedPhone()
                }
                com.ssafy.groute.R.id.profile_edit_email_et -> {
                    validatedEmail()
                }
                com.ssafy.groute.R.id.profile_edit_email_domain -> {
                    validatedEmail()
                }
            }
        }
    }

    private fun updateUser(user: User) {
        // ?????? ?????? ????????? ????????? ?????? ?????? ??? user ????????? ????????? ??????
        if(imgUri == Uri.EMPTY) {
            val gson : Gson = Gson()
            val json = gson.toJson(user)
            val requestBody_user = RequestBody.create(MediaType.parse("text/plain"), json)
            UserService().updateUserInfo(requestBody_user, null, UserUpdateCallback(user.id))
        }
        // ?????? ?????? + ????????? ?????? ?????? ??? ????????? ????????? ?????? ?????? ????????? ??????
        else {
            val file = File(imgUri.path!!)

            var inputStream: InputStream? = null
            try {
                inputStream = this.contentResolver.openInputStream(imgUri)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream)  // ???????????? ??????
            val requestBody = RequestBody.create(MediaType.parse("image/*"), byteArrayOutputStream.toByteArray())
            val uploadFile = MultipartBody.Part.createFormData("img", "${file.name}.${fileExtension?.substring(6)}", requestBody)
            val gson : Gson = Gson()
            val json = gson.toJson(user)
            val requestBody_user = RequestBody.create(MediaType.parse("text/plain"), json)

            UserService().updateUserInfo(requestBody_user, uploadFile, UserUpdateCallback(user.id))
        }


    }

    inner class UserUpdateCallback(val userId: String): RetrofitCallback<Boolean> {
        override fun onSuccess(code: Int, responseData: Boolean) {
            if(responseData) {
                showCustomToast("????????? ????????? ?????????????????????.")
                finish()
                runBlocking {
                    mainViewModel.getUserInformation(userId, true)
                }
            } else {
                showCustomToast("????????? ?????? ????????? ??????????????????.")
            }
        }

        override fun onError(t: Throwable) {
            Log.d(TAG, t.message ?: "????????? ?????? ?????? ??? ????????????")
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onResponse: Error Code $code")
        }
    }

    /**
     * ?????? ????????? id ????????? ?????? ??? ?????? ??????, pw, nickname, phone ????????? ?????? ?????????
     * ?????? ????????? email, birth, gender ????????? ?????? ?????? ??? ?????? ????????? ???????????? ?????? ??????
     * @return ?????? ????????? ???????????? user ????????? ??????
     */
    private fun isAvailable(user: UserInfoResponse) : User? {
        if(validatedNickname() && validatedPhone()) {
            val nickname = binding.profileEditNicknameEt.text.toString()
            val phone = binding.profileEditPhoneEt.text.toString()

            if(userEmail == null)
                userEmail = ""
            if(userBirth == null)
                userBirth = ""
            if(!(userGender == "M" || userGender == "F"))
                userGender = ""

            var userImg : String = ""
            if(user.img == null) {
                userImg = ""
            } else {
                userImg = user.img!!
            }

            return User(user.id, user.password, nickname, phone, userEmail, userBirth, userGender, user.type, userImg)
        } else {
            return null
        }
    }

    /**
     * ???????????? Birth??? null??? ????????? Spinner??? ????????? ???????????? Index ??????
     */
    private fun getSpinnerIndex(spinner: Spinner, str: String) : Int{

        for(i in 0 until spinner.count) {
            if(spinner.getItemAtPosition(i).toString().equals(str)) {
                return i
            }
        }
        return 0
    }

    /**
     * ????????? nickname ?????? ??? ??? ??? ??????
     * @return ?????? ??? true ??????
     */
    private fun validatedNickname() : Boolean{
        val inputNickname = binding.profileEditNicknameEt.text.toString()

        if(inputNickname.trim().isEmpty()){
            binding.profileEditNicknameLayout.error = "Required Field"
            binding.profileEditNicknameEt.requestFocus()
            return false
        } else if(inputNickname.length > 100) {
            binding.profileEditNicknameLayout.error = "Nickname ????????? 100??? ????????? ????????? ?????????."
            binding.profileEditNicknameEt.requestFocus()
            return false
        }
        else {
            binding.profileEditNicknameLayout.error = null
            return true
        }
    }

    /**
     * ????????? phone number ????????? ??????
     * @return ????????? ?????? ?????? ??? true ??????
     */
    private fun validatedPhone() : Boolean{
        val inputPhone = binding.profileEditPhoneEt.text.toString()

        if(inputPhone.trim().isEmpty()){
            binding.profileEditPhoneLayout.error = "Required Field"
            binding.profileEditPhoneEt.requestFocus()
            return false
        } else if(!Pattern.matches("^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", inputPhone)) {
            binding.profileEditPhoneLayout.error = "????????? ?????? ????????? ??????????????????."
            binding.profileEditPhoneEt.requestFocus()
            return false
        }
        else {
            binding.profileEditPhoneLayout.error = null
            return true
        }
    }


    /**
     * email ?????? ????????? ??????
     * @return email ???????????? email(String), ????????? null
     */
    private fun validatedEmail(){
        val inputEmail = binding.profileEditEmailEt.text.toString()
        val inputDomain = binding.profileEditEmailDomain.text.toString()

        val email = "$inputEmail@$inputDomain"

        if(inputEmail.trim().isEmpty()) {
            binding.profileEditEmailLayout.error = "Required Email Field"
        } else if(inputDomain.trim().isEmpty()) {
            binding.profileEditEmailLayout.error = "Required Domain Field"
        }
        else if(!Pattern.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+.[A-Za-z]{2,}\$", email)) {
            binding.profileEditEmailLayout.error = "????????? ????????? ??????????????????."
        }
        else {
            binding.profileEditEmailLayout.error = null
            binding.profileEditDomainLayout.error = null
            userEmail = email
        }

    }

    /**
     * email domain list set Adapter
     */
    private fun initDomain() {
        // ?????????????????? ????????? ?????????
        val domains = arrayOf("gmail.com", "naver.com", "nate.com", "daum.net")

        val adapter = ArrayAdapter<String>(this, R.layout.simple_dropdown_item_1line, domains)
        binding.profileEditEmailDomain.setAdapter(adapter)
    }

    /**
     * birth spinner item ?????? ?????????
     * year, month, day??? ???????????? birth ???????????? ????????????.
     */
    private fun selectedBirth() {
        var year = ""
        var month = ""
        var day = ""

        val yearSpinner = binding.profileEditSpinnerYear
        val monthSpinner = binding.profileEditSpinnerMonth
        val daySpinner = binding.profileEditSpinnerDay

        // year
        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        year = ""
                    }
                    else -> {
                        year = yearSpinner.selectedItem.toString()
                        // birth ?????? ?????????
                        if(year != "" && month != "" && day != "") {  // ?????? ?????? ????????? ???????????? ???????????????
                            userBirth = "$year-$month-$day"
                        }
                    }
                }
            }
        }

        // month
        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        month = ""
                    }
                    else -> {
                        month = monthSpinner.selectedItem.toString()
                        // birth ?????? ?????????
                        if(year != "" && month != "" && day != "") {  // ?????? ?????? ?????? ???????????? ???????????????
                            userBirth = "$year-$month-$day"
                        }
                    }
                }
            }
        }

        // day
        daySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        day = ""
                    }
                    else -> {
                        day = daySpinner.selectedItem.toString()
                        // birth ?????? ?????????
                        if(year != "" && month != "" && day != "") {  // ?????? ?????? ?????? ???????????? ???????????????
                            userBirth = "$year-$month-$day"
                        }
                    }
                }
            }
        }
    }

    /**
     * radio group ??? ????????? gender item ????????? ??????
     */
    private fun selectedGender() {
        binding.profilEditGenderGroup.setOnCheckedChangeListener { group, checkedId ->
            when(checkedId) {
                com.ssafy.groute.R.id.profile_edit_radio_man -> {
                    userGender = "M"
                }
                com.ssafy.groute.R.id.profile_edit_radio_woman -> {
                    userGender = "F"
                }
            }
        }
    }
}