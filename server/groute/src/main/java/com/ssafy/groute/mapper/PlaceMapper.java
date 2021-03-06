package com.ssafy.groute.mapper;

import com.ssafy.groute.dto.Place;
import com.ssafy.groute.dto.PlaceLike;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PlaceMapper {
    void insertPlace(Place place) throws Exception;
    Place selectPlace(int id) throws Exception;
    List<Place> selectAllPlace() throws Exception;
    void deletePlace(int id) throws Exception;
    void updatePlace(Place place) throws Exception;
    void likePlace(PlaceLike placeLike) throws Exception;
    void deleteAllPlaceByUId(String userId) throws Exception;
    List<Integer> findAllPlaceByUId(String userId) throws Exception;
    void unLikePlace(int id) throws Exception;
    PlaceLike isLike(PlaceLike placeLike) throws Exception;
    List<Place> bestPlace() throws Exception;
    List<Place> selectAllPlaceIdByUserId(String userId)throws Exception;
    void deletePlaceLikeByPlaceId(int placeId) throws Exception;
    void deletePlaceLikeByUserId(String userId) throws Exception;
}
