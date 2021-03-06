package com.ssafy.groute.src.main.home

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.contains
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.tabs.TabLayoutMediator
import com.ssafy.groute.R
import com.ssafy.groute.config.ApplicationClass
import com.ssafy.groute.config.BaseFragment
import com.ssafy.groute.databinding.FragmentPlaceDetailBinding
import com.ssafy.groute.src.dto.Place
import com.ssafy.groute.src.dto.RouteDetail
import com.ssafy.groute.src.dto.UserPlan
import com.ssafy.groute.src.main.MainActivity
import com.ssafy.groute.src.main.route.BottomSheetRecyclerviewAdapter
import com.ssafy.groute.src.response.PlaceLikeResponse
import com.ssafy.groute.src.service.PlaceService
import com.ssafy.groute.src.service.UserPlanService
import com.ssafy.groute.src.viewmodel.PlaceViewModel
import com.ssafy.groute.src.viewmodel.PlanViewModel
import com.ssafy.groute.util.RetrofitCallback
import kotlinx.coroutines.runBlocking
import java.lang.RuntimeException


// place 하나 선택 했을 때 장소에 대한 정보를 보여주는 화면
private const val TAG = "PlaceDetailF_싸피"
class PlaceDetailFragment : BaseFragment<FragmentPlaceDetailBinding>(FragmentPlaceDetailBinding::bind, R.layout.fragment_place_detail) {
    private lateinit var mainActivity: MainActivity
    private val placeViewModel: PlaceViewModel by activityViewModels()
    private val planViewModel: PlanViewModel by activityViewModels()
    private var placeId = -1
    private var planId = -1
    private var addDay = -1 // 선택한 day에 루트 추가
    private var selectUserPlan = UserPlan() // 선택한 userPlan에 루트 추가
    private lateinit var bottomSheetRecyclerviewAdapter: BottomSheetRecyclerviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity.hideMainProfileBar(true)
        Log.d(TAG, "onCreate: ")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        arguments?.let {
            placeId = it.getInt("placeId", -1)
            planId = it.getInt("planId", -1)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        runBlocking {
            placeViewModel.getPlace(placeId)
            planViewModel.getPlanMyList(ApplicationClass.sharedPreferencesUtil.getUser().id)
            placeViewModel.getPlaceIsLike(PlaceLikeResponse(0,ApplicationClass.sharedPreferencesUtil.getUser().id, placeId))
        }

        placeViewModel.place.observe(viewLifecycleOwner, {
            binding.dto = it
        })

        val heart = binding.placeDetailAbtnHeart

        heart.setOnClickListener {
            val placeLike = PlaceLikeResponse(
                0,
                ApplicationClass.sharedPreferencesUtil.getUser().id,
                placeId
            )
            PlaceService().placeLike(placeLike, PlaceLikeCallback())
        }

        placeViewModel.isPlaceLike.observe(viewLifecycleOwner, {
            Log.d(TAG, "onViewCreated: $it")
            if (it == true) {
                val animator = ValueAnimator.ofFloat(0f,0.4f).setDuration(500)
                animator.addUpdateListener { animation ->
                    heart.progress = animation.animatedValue as Float
                }
                animator.start()

            } else {
                val animator = ValueAnimator.ofFloat(1f,0f).setDuration(500)
                animator.addUpdateListener { animation ->
                    heart.progress = animation.animatedValue as Float
                }
                animator.start()
            }
        })



        val areaTabPagerAdapter = AreaTabPagerAdapter(this)
        val tabList = arrayListOf("Info", "Review")

        areaTabPagerAdapter.addFragment(InfoFragment.newInstance("placeId", placeId))
        areaTabPagerAdapter.addFragment(ReviewFragment.newInstance("placeId", placeId))

        binding.pdVpLayout.adapter = areaTabPagerAdapter
        TabLayoutMediator(binding.pdTablayout, binding.pdVpLayout) { tab, position ->
            tab.text = tabList.get(position)
        }.attach()

        binding.placeDatilIbtnBack.setOnClickListener {
            mainActivity.supportFragmentManager.beginTransaction().remove(this).commit()
            mainActivity.supportFragmentManager.popBackStack()
        }

        if (planId == -1) { // 홈에서 플레이스 디테일 페이지로 왔을 때
            binding.textView14.text = "내 여행에 추가하기"
            binding.placeDetailBtnAddList.setOnClickListener {
                showRouteAddBottomSheet()
            }
        } else { // 계획중인 여행에서 장소추가를 하기위해 플레이스 디테일 페이지로 왔을 때
            binding.textView14.text = "내 카트에 담기"
            planViewModel.planMyList.observe(viewLifecycleOwner, Observer {
                if (it.size > 0) {
                    binding.placeDetailBtnAddList.setOnClickListener {
                        binding.placeDetailLottieAddPlan.playAnimation()
                        placeViewModel.place.observe(viewLifecycleOwner, Observer {
                            planViewModel.insertPlaceShopList(it)
                            Log.d(TAG, "onViewCreated_PlaceSHOP: ${it}")
                            showCustomToast("추가되었습니다.")
                        })
                    }
                } else {
                    binding.placeDetailBtnAddList.setOnClickListener {
                        showCustomToast("추가하실 일정이 없습니다")
                    }
                }
            })
        }
        placeViewModel.placeLikeList.observe(viewLifecycleOwner, {
            for(i in 0..it.size-1){
                if(placeId == it[i].id){
                    heart.progress = 0.5F
                }
            }
        })
        placeViewModel.place.observe(viewLifecycleOwner, {
            binding.placeDetailTvHeart.text = it.heartCnt.toString()
        })
    }

    /**
     * 내 일정에 추가하기 클릭 후 BottomSheet show
     */
    private fun showRouteAddBottomSheet() {
        val dialogView: View =
            LayoutInflater.from(requireContext()).inflate(R.layout.plan_add_bottom_sheet, null)
        Log.d(TAG, "showRouteAddBottomSheet: $planId")
        runBlocking {
            planViewModel.getMyNotendPlan(ApplicationClass.sharedPreferencesUtil.getUser().id)
        }
        planViewModel.setUserNotPlanList() // 리싸이클러뷰에 바인딩 된 변수에 planNotEndList 할당
        planViewModel.userPlan.observe(viewLifecycleOwner, Observer {
            bottomSheetRecyclerviewAdapter =
                BottomSheetRecyclerviewAdapter(viewLifecycleOwner, planViewModel, requireContext(), false)
            bottomSheetRecyclerviewAdapter.setUserPlanList(it)
            Log.d(TAG, "showRouteAddBottomSheet: $it")

            val recyclerview =
                dialogView.findViewById<RecyclerView>(R.id.bottom_sheet_recyclerview)
            recyclerview.apply {
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                adapter = bottomSheetRecyclerviewAdapter
                adapter!!.stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            bottomSheetRecyclerviewAdapter.setItemClickListener(object : BottomSheetRecyclerviewAdapter.ItemClickListener {
                override fun onClickDay(position: Int, day: Int, userPlan: UserPlan) {
                    Log.d(TAG, "onClick day: $day")
                    addDay = day
                    selectUserPlan = userPlan
                }

            })
        })

        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)

        // 일정 등록 버튼을 눌렀을때
        dialogView.findViewById<RelativeLayout>(R.id.bottom_sheet_userplan_create_btn).setOnClickListener {
            dialog.dismiss()
            mainActivity.moveFragment(1)
        }

        // 일정에 추가하기 버튼을 눌렀을때
        dialogView.findViewById<ConstraintLayout>(R.id.bottom_sheet_route_add_btn)
            .setOnClickListener {
                if(addDay != -1 && selectUserPlan.id != 0) {
                    insertPlace(addDay, placeId)
                    dialog.dismiss()
                } else if(addDay == -1 && selectUserPlan.id != 0){
                    showCustomToast("day를 선택해주세요.")
                } else if(selectUserPlan.id == 0 && addDay != -1) {
                    showCustomToast("일정을 선택해주세요.")
                } else{
                    showCustomToast("일정과 day를 선택해주세요.")
                }

            }
        dialog.setTitle("")
        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        dialog.window!!.setGravity(Gravity.BOTTOM)

    }

    private fun insertPlace(day:Int,placeId:Int){
        //placeId, priority,routeId
        runBlocking {
            planViewModel.getPlanById(selectUserPlan.id, 2)
        }
        planViewModel.routeList.observe(viewLifecycleOwner, Observer {
            var routeId = 0
            var priority = 0
            for(i in 0 until it.size){
                if(it[i].day == day){
                    routeId = it[i].id
                    priority = it[i].routeDetailList.size+1
                }
            }
            var routeDatil = RouteDetail(
                placeId = placeId,
                priority = priority,
                routeId = routeId,
            )

            UserPlanService().insertPlaceToUserPlan(routeDatil, object : RetrofitCallback<Boolean> {
                override fun onError(t: Throwable) {
                    Log.d(TAG, "onError: ")
                }

                override fun onSuccess(code: Int, responseData: Boolean) {
                    //planViewModel.removePlaceShopList(placeId)
                    showCustomToast("일정에 추가되었습니다.")
                    Log.d(TAG, "onSuccess: $responseData")

                }

                override fun onFailure(code: Int) {
                    Log.d(TAG, "onFailure: ")
                }

            })

        })
    }

    inner class PlaceLikeCallback() : RetrofitCallback<Boolean> {
        override fun onError(t: Throwable) {
            Log.d(TAG, "onError: ")
        }

        override fun onSuccess(code: Int, responseData: Boolean) {
            Log.d(TAG, "onSuccess: $responseData")
            if(responseData == true) {
                runBlocking {
                    placeViewModel.getPlace(placeId)
                    placeViewModel.getPlaceIsLike(PlaceLikeResponse(0,ApplicationClass.sharedPreferencesUtil.getUser().id, placeId))
                }
            }
        }

        override fun onFailure(code: Int) {
            Log.d(TAG, "onFailure: ")
        }

    }


    companion object {
        @JvmStatic
        fun newInstance(key1: String, value1: Int, key2: String, value2: Int) =
            PlaceDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(key1, value1)
                    putInt(key2, value2)
                }
            }
    }

}