package com.ssafy.groute.src.main.route

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.groute.R
import com.ssafy.groute.config.BaseFragment
import com.ssafy.groute.databinding.FragmentRouteDetailInfoBinding
import com.ssafy.groute.src.main.MainActivity
import com.ssafy.groute.src.viewmodel.HomeViewModel
import com.ssafy.groute.src.viewmodel.PlanViewModel
import kotlinx.coroutines.runBlocking
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapPolyline
import net.daum.mf.map.api.MapView
import java.util.ArrayList

private const val TAG = "RouteDetailInfoFragment_groute"
class RouteDetailInfoFragment : BaseFragment<FragmentRouteDetailInfoBinding>(FragmentRouteDetailInfoBinding::bind, R.layout.fragment_route_detail_info) {
    private lateinit var routeDetailDayPerAdapter: RouteDetailDayPerAdapter
    private lateinit var mainActivity: MainActivity
    private val planViewModel: PlanViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private var planId = -1
    private lateinit var mapView:MapView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity.hideMainProfileBar(true)
        mainActivity.hideBottomNav(true)
        arguments?.let {
            planId = it.getInt("planId",-1)
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }


    @SuppressLint("LongLogTag")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = planViewModel
        runBlocking {
            planViewModel.getPlanById(planId, 2)
        }
        findArea()
        var list = mutableListOf<Int>()
        for(i in 1..planViewModel.planList.value!!.totalDate) {
            Log.d(TAG, "onViewCreated: $i")
            list.add(i)
        }
        routeDetailDayPerAdapter = RouteDetailDayPerAdapter(viewLifecycleOwner, list, planViewModel)
        routeDetailDayPerAdapter.setHasStableIds(true)
        binding.RouteDetailDayPerRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = routeDetailDayPerAdapter
        }

        routeDetailDayPerAdapter.setItemClickListener(object : RouteDetailDayPerAdapter.ItemClickListener {
            override fun onClick(position: Int, placeId: Int) {
                mainActivity.moveFragment(4, "placeId", placeId)
            }

        })
    }
    fun findArea(){
        var area = homeViewModel.areaList.value!!
        var plan = planViewModel.planList.value!!
        for(i in 0 until area.size){
            if(area[i].id == plan.areaId){
                initMap(area[i].lat.toDouble(), area[i].lng.toDouble())
            }
        }
    }
    fun initMap(lat:Double, lng:Double){
        mapView = MapView(requireContext())
        if(mapView.parent!=null){
            (mapView.parent as ViewGroup).removeView(mapView)
        }
        binding.RouteDetailMap.addView(mapView)
        var mapPoint = MapPoint.mapPointWithGeoCoord(lat, lng)
        mapView.setMapCenterPoint(mapPoint,true)
        mapView.setZoomLevel(9, true)

        addPing()
    }
    fun addPing(){
        var markerArr = arrayListOf<MapPoint>()
        planViewModel.routeList.observe(viewLifecycleOwner, {
            for(i in 0..it.size-1){
                var dayList = it[i].routeDetailList
                for(j in 0..dayList.size-1){
                    var lat = dayList[j].place.lat.toDouble()
                    var lng = dayList[j].place.lng.toDouble()
                    var mapPoint = MapPoint.mapPointWithGeoCoord(lat,lng)
                    markerArr.add(mapPoint)
                }
                setPing(markerArr)
                addPolyLine(markerArr)
            }
        })
    }
    fun setPing(markerArr: ArrayList<MapPoint>){

        var res = ""
        var list = arrayListOf<MapPOIItem>()
        for(i in 0..markerArr.size-1){
            var marker = MapPOIItem()
            res = "number${i+1}"
            marker.itemName = (i+1).toString()
            marker.mapPoint = markerArr[i]
            marker.markerType = MapPOIItem.MarkerType.RedPin
//            var resources = requireContext().resources.getIdentifier("@drawable/"+res,"drawable",requireContext().packageName)
//            marker.customImageResourceId = resources
//            marker.isCustomImageAutoscale = false
//            marker.setCustomImageAnchor(0.5f,1.0f)
            list.add(marker)
        }
        mapView.addPOIItems(list.toArray(arrayOfNulls(list.size)))

    }
    fun addPolyLine(markerArr: ArrayList<MapPoint>){
        var polyLine = MapPolyline()
        polyLine.tag = 1000
        polyLine.lineColor = Color.parseColor("#2054B3")
        polyLine.addPoints(markerArr.toArray(arrayOfNulls(markerArr.size)))
        mapView.addPolyline(polyLine)
    }
    companion object {

        @JvmStatic
        fun newInstance(key: String, value: Int) =
            RouteDetailInfoFragment().apply {
                arguments = Bundle().apply {
                    putInt(key, value)
                }
            }
    }
}