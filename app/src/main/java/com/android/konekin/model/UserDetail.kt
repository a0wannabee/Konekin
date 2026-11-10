package com.android.konekin.model

import com.google.gson.annotations.SerializedName

data class UserDetail(
    @SerializedName("status")
    val status: String?,
    @SerializedName("data")
    val data: Data?,
    @SerializedName("message")
    val message: String?
)
