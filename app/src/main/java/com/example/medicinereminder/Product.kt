package com.example.medicinereminder

import android.os.Parcel
import android.os.Parcelable

data class Product(
    val name: String,
    val price: Double,
    val imageResId: Int,  // Store the image resource ID
    var quantity: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeDouble(price)
        parcel.writeInt(imageResId)
        parcel.writeInt(quantity)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Product> {
        override fun createFromParcel(parcel: Parcel): Product = Product(parcel)
        override fun newArray(size: Int): Array<Product?> = arrayOfNulls(size)
    }
}
