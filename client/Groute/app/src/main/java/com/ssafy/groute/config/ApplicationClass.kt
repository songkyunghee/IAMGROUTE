package com.ssafy.groute.config

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kakao.sdk.common.KakaoSdk
import com.ssafy.groute.R
import com.ssafy.groute.config.intercepter.AddCookiesInterceptor
import com.ssafy.groute.config.intercepter.ReceivedCookiesInterceptor
import com.ssafy.groute.config.intercepter.XAccessTokenInterceptor
import com.ssafy.groute.util.SharedPreferencesUtil
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "ApplicationClass_Groute"
class ApplicationClass : Application() {
    companion object{
//        const val SERVER_URL = "http://'IP Address':8888/"   // local 서버 실행 시
        //AWS servoer
        const val SERVER_URL = "http://i6d109.p.ssafy.io:8888/"
        const val IMGS_URL = "${SERVER_URL}imgs/"

        const val IMGS_URL_USER = "${SERVER_URL}imgs/user/"
        const val IMGS_URL_AREA = "${SERVER_URL}imgs/area/"
        const val IMGS_URL_PLACE = "${SERVER_URL}imgs/place"
        const val IMGS_URL_PLACEREVIEW = "${SERVER_URL}imgs/placereview"
        const val IMGS_URL_ACCOUNT = "${SERVER_URL}imgs/account"
        lateinit var sharedPreferencesUtil: SharedPreferencesUtil
        lateinit var retrofit: Retrofit

        // JWT Token Header 키 값
        const val X_ACCESS_TOKEN = "X-ACCESS-TOKEN"

    }

    override fun onCreate() {
        super.onCreate()
        //shared preference 초기화
        sharedPreferencesUtil = SharedPreferencesUtil(applicationContext)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AddCookiesInterceptor())
            .addInterceptor(ReceivedCookiesInterceptor())
            .addNetworkInterceptor(XAccessTokenInterceptor()) // JWT 자동 헤더 전송
            .connectTimeout(30, TimeUnit.SECONDS).build()

        // Gson 객체 생성 - setLenient 속성 추가
        val gson : Gson = GsonBuilder()
            .setLenient()
            .create()
        
        retrofit = Retrofit.Builder()
            .baseUrl(SERVER_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(okHttpClient)
            .build()

        // Kakao SDK 초기화
        KakaoSdk.init(this, getString(R.string.kakao_nativeapp_key))
    }

}