package com.ssafy.groute.src.api

import com.ssafy.groute.src.dto.BoardDetail
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BoardApi {

    // 자유게시판, 질문게시판 게시글 리스트 조회
    @GET("/boardDetail/list")
    fun listBoard() : Call<MutableList<BoardDetail>>

    // 게시판 타입에 따른 리스트 조회
    @GET("/boardDetail/list/division")
    fun listBoardDetail(@Query("boardId") boardId : Int) : Call<MutableList<BoardDetail>>
}