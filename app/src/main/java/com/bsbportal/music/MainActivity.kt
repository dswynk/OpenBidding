package com.bsbportal.music

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.facebook.ads.AdSettings
import com.google.android.ads.mediationtestsuite.MediationTestSuite
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.google.android.gms.ads.formats.AdManagerAdViewOptions
import com.google.android.gms.ads.formats.OnAdManagerAdViewLoadedListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var programaticView: FrameLayout
    lateinit var nativeAdView: NativeAdView
    lateinit var etAdUnit: EditText
    lateinit var etAdUnitInterstitial: EditText
    lateinit var cbCheck: CheckBox
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        nativeAdView = findViewById<NativeAdView>(R.id.nativeAdView)
        programaticView = findViewById<FrameLayout>(R.id.programaticAdView)
        etAdUnit = findViewById(R.id.etAdUnit)
        etAdUnitInterstitial = findViewById(R.id.etAdUnitInterstitial)
        cbCheck = findViewById(R.id.cbBox)
        MobileAds.initialize(this) { it ->
            it.adapterStatusMap.forEach{
                Log.i("Ads", "Initialize Adapter | ${it.key} ${it.value}")
            }
        }
        MobileAds.openAdInspector(this) {
            Log.i("Ads", "AdsInspector | ${it?.message}")
        }
        cbCheck.setOnCheckedChangeListener { buttonView, isChecked -> enableTestMode() }
    }

    private fun loadNativeAd(nativeAd: AdManagerAdView) {
//        nativeAdView.mediaView = nativeAdView.findViewById<MediaView>(R.id.mediaView).let {
//            it.setMediaContent(nativeAd.mediaContent)
//            it.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
//            it
//        }
//        nativeAdView.setNativeAd(nativeAd)
        programaticView.removeAllViews()
        programaticView.addView(nativeAd)
    }

    fun loadInterstitial(view: View) {
        makeInterstitial()
    }

    private fun enableTestMode() {
        GlobalScope.launch(Dispatchers.IO) {
            if (cbCheck.isChecked) {
                AdSettings.addTestDevice(getGoogleAdvertisingId(this@MainActivity))
                MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(getIdForTestAds(this@MainActivity)).build())
                MobileAds.initialize(this@MainActivity)
            } else {
                AdSettings.clearTestDevices()
                MobileAds.setRequestConfiguration(RequestConfiguration.Builder().build())
                MobileAds.initialize(this@MainActivity)
            }
        }
    }

    fun makeInterstitial() {
        val adRequest = AdManagerAdRequest.Builder().build()

        AdManagerInterstitialAd.load(this, etAdUnitInterstitial.text.toString().trim(), adRequest,
            object : AdManagerInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    Toast.makeText(this@MainActivity, "Error Loading ${p0.code}", Toast.LENGTH_SHORT).show()
                    super.onAdFailedToLoad(p0)
                }

                override fun onAdLoaded(p0: AdManagerInterstitialAd) {
                    super.onAdLoaded(p0)
                    Toast.makeText(this@MainActivity, "Load Ad Completed ${p0}", Toast.LENGTH_SHORT).show()
                    p0.show(this@MainActivity)
                }
            });
    }

    fun loadBanner(view: View) {
        loadBannerView()
    }

    private fun loadBannerView() {
        val adLoader: AdLoader = AdLoader.Builder(this, etAdUnit.text.toString().trim())
//            .forNativeAd(object : NativeAd.OnNativeAdLoadedListener {
//                override fun onNativeAdLoaded(p0: NativeAd) {
//                    Toast.makeText(this@MainActivity, "Load Ad Completed ${p0}", Toast.LENGTH_SHORT).show()
//                    loadNativeAd(p0)
//                }
//            })
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    Toast.makeText(this@MainActivity, "Error Loading ${p0.code}", Toast.LENGTH_SHORT).show()
                }
            }).forAdManagerAdView(object: OnAdManagerAdViewLoadedListener{
                override fun onAdManagerAdViewLoaded(p0: AdManagerAdView) {
                    Toast.makeText(this@MainActivity, "Load Ad Completed ${p0}", Toast.LENGTH_SHORT).show()
                    loadNativeAd(p0)
                }
            },AdSize.BANNER)
            .withAdManagerAdViewOptions(
                AdManagerAdViewOptions.Builder().build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

//        val adUnit = etAdUnit.text.toString().trim()
//        val adView = AdView(this)
//        adView.apply {
//            this.adUnitId = adUnit
//            this.adSize = AdSize.BANNER
//            this.adListener = object: AdListener() {
//                override fun onAdLoaded() {
//                    super.onAdLoaded()
//                    Toast.makeText(this@MainActivity, "Ad Loaded: $adUnit", Toast.LENGTH_SHORT).show()
//
//                    programaticView.removeAllViews()
//                    programaticView.addView(adView)
//                }
//
//                override fun onAdFailedToLoad(p0: LoadAdError) {
//                    super.onAdFailedToLoad(p0)
//                    Toast.makeText(this@MainActivity, "Ad failed: ${p0.message}", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//
//        adView.loadAd(AdRequest.Builder().build())
    }

    fun getGoogleAdvertisingId(context: Context): String? {
        var idToReturn: String? = null
        var idInfo: AdvertisingIdClient.Info? = null
        try {
            idInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        var gaId: String? = null
        if (idInfo != null) {
            gaId = idInfo.id
        }
        idToReturn = gaId
        return idToReturn
    }

    fun getIdForTestAds(appContext: Context): List<String> {
        val testIds = mutableListOf<String>()
        val androidId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        if (androidId != null) {
            testIds.add(md5(androidId).toUpperCase())
        }
        return testIds.toList()
    }

    private fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) {
                var h = Integer.toHexString(0xFF and messageDigest[i].toInt())
                while (h.length < 2) h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    fun startAMSuite(view: View) {
        MediationTestSuite.launch(this)
    }

    fun startGAMSuite(view: View) {
        MediationTestSuite.launchForAdManager(this)
    }
}