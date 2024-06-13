package com.example.aimessaging

import android.os.Parcel
import android.os.Parcelable

data class Messaging(
    var message: String,
    var sentBy: String
) : Parcelable {
    companion object {
        const val SENT_BY_ME = "me"
        const val SENT_BY_BOT = "bot"

        @JvmField
        val CREATOR = object : Parcelable.Creator<Messaging> {
            override fun createFromParcel(parcel: Parcel): Messaging {
                return Messaging(parcel)
            }

            override fun newArray(size: Int): Array<Messaging?> {
                return arrayOfNulls(size)
            }
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(message)
        parcel.writeString(sentBy)
    }

    override fun describeContents(): Int {
        return 0
    }
}

