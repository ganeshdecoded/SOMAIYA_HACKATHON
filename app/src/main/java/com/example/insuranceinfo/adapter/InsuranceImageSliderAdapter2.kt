package com.example.insuranceinfo.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.R

class InsuranceImageSliderAdapter2(
    private val context: Context,
    private val imageList: List<Int>,
    private val linkList: List<String>
) : RecyclerView.Adapter<InsuranceImageSliderAdapter2.SliderViewHolder>() {

    inner class SliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_slider_image, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        holder.imageView.setImageResource(imageList[position])

        // Set click listener to open URL
        holder.imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkList[position]))
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = imageList.size
}