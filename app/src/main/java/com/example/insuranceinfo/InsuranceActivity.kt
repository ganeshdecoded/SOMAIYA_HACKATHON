package com.example.insuranceinfo

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.insuranceinfo.adapter.InsuranceImageSliderAdapter
import com.example.medicinereminder.R
import android.os.Handler
import com.example.a7minutesworkout.MainActivityWorkout
import com.example.home.Fall_detection
import com.example.insuranceinfo.adapter.InsuranceImageSliderAdapter2
import com.example.medicinereminder.MainActivity
import com.example.records.Records_Activity
import com.google.android.material.bottomnavigation.BottomNavigationView


class InsuranceActivity : AppCompatActivity() {

    private lateinit var insuranceImageSlider: ViewPager2
    private lateinit var insuranceImageSlider2: ViewPager2
    private val imageList1 = listOf(
        R.drawable.insu1,
        R.drawable.insu2,
        R.drawable.insu3
    )
    private val linkList1 = listOf(
        "https://www.nivabupa.com/family-health-insurance-plans/senior-citizen-family-floater.html",
        "https://www.hdfcergo.com/",
        "https://www.icicilombard.com/health-insurance/senior-citizen-health-insurance"
    )

    private val imageList2 = listOf(
        R.drawable.insu4,
        R.drawable.insu5,
        R.drawable.insu6,
        R.drawable.insu7,
        R.drawable.insu8,
        R.drawable.insu9,
        R.drawable.insu10
    )
    private val linkList2 = listOf(
        "https://www.manipalcigna.com/blog/introducing-manipalcigna-prime-senior",
        "https://www.adityabirlacapital.com/healthinsurance/health-insurance-plans?Category=HealthAndWellnessPlan",
        "https://www.careinsurance.com/product/care-supreme-senior?agentId=20004977&utm_source=google&utm_medium=cpc&utm_campaign=ht_ind_new-101_AD_CSS_BT&utm_content=rta&utm_keyword=care%20supreme%20senior%20plan&s_kwcid=AL!10397!3!701015054103!e!!g!!care%20supreme%20senior%20plan&utm_term=21336581034&utm_adgroup=162594252146&gad_source=1&gclid=Cj0KCQjw99e4BhDiARIsAISE7P-jtz0KOwh38WiTwAJuCakDAPN9-BVzj6aHdxsiDsO7awbfLisirG4aAmp4EALw_wcB",
        "https://www.starhealth.in/lp/senior-citizen-policy/?utm_source=Google_Brand&utm_campaign=GS-Brand-Product-PanIndia&utm_agid=161661932296&utm_term=star%20health%20insurance%20for%20senior%20citizens&creative=699728256612&device=c&gclid=Cj0KCQjw99e4BhDiARIsAISE7P-8QqNMtlj0xoZ1Kays_wKY1ky7WOtEFw2wIbpW3hJNOVB6yxjD-1UaAq9xEALw_wcB",
        "https://www.tataaig.com/health-insurance/senior-citizen-health-insurance",
        "https://www.bajajallianz.com/health-insurance-plans/health-insurance-for-senior-citizens.html",
        "https://www.sbigeneral.in/health-insurance/senior-citizen-health-insurance"
    )

    private val delayMillis: Long = 3000 // 3 seconds for auto-slide delay
    private val handler = Handler(Looper.getMainLooper())

    private val sliderRunnable1 = object : Runnable {
        override fun run() {
            val nextItem = (insuranceImageSlider.currentItem + 1) % imageList1.size
            insuranceImageSlider.currentItem = nextItem
            handler.postDelayed(this, delayMillis)
        }
    }

    private val sliderRunnable2 = object : Runnable {
        override fun run() {
            val nextItem = (insuranceImageSlider2.currentItem + 1) % imageList2.size
            insuranceImageSlider2.currentItem = nextItem
            handler.postDelayed(this, delayMillis)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insurance)

        insuranceImageSlider = findViewById(R.id.insuranceImageSlider)
        insuranceImageSlider2 = findViewById(R.id.insuranceImageSlider2)
        insuranceImageSlider.adapter = InsuranceImageSliderAdapter2(this, imageList1, linkList1)
        insuranceImageSlider2.adapter = InsuranceImageSliderAdapter2(this, imageList2, linkList2)

        // Start auto-sliding for both sliders
        startAutoSliding()

        // Set up bottom navigation
        val bottomNavView = findViewById<BottomNavigationView>(R.id.BottomNavBar)
        bottomNavView.selectedItemId = R.id.nav_insaurance
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_workout -> {
                    startActivity(Intent(this, MainActivityWorkout::class.java))
                    finish()
                    true
                }
                R.id.nav_medical -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_records -> {
                    startActivity(Intent(this, Records_Activity::class.java))
                    finish()
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(this, Fall_detection::class.java))
                    finish()
                    true
                }
                R.id.nav_insaurance -> true
                else -> false
            }
        }
    }

    private fun startAutoSliding() {
        handler.postDelayed(sliderRunnable1, delayMillis)
        handler.postDelayed(sliderRunnable2, delayMillis)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(sliderRunnable1)
        handler.removeCallbacks(sliderRunnable2)
    }
}