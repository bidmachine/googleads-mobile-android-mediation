package io.bidmachine.adloaderexample

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import io.bidmachine.BidMachine

const val TAG = "AdLoaderExample"

/**
 * Turns on BidMachine logging before Google initialises the adapter, starts the Google Mobile Ads
 * SDK and prints the adapter statuses, and logs the advertising ID to allowlist for test bidders.
 */
class AdLoaderExampleApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    BidMachine.setLoggingEnabled(true)
    MobileAds.initialize(this) { status ->
      status.adapterStatusMap.forEach { (name, adapterStatus) ->
        Log.i(TAG, "adapter $name: ${adapterStatus.initializationState} ${adapterStatus.description}")
      }
    }
    Thread {
      try {
        val info = AdvertisingIdClient.getAdvertisingIdInfo(this)
        Log.i(TAG, "GAID ${info.id}, limit ad tracking ${info.isLimitAdTrackingEnabled}")
      } catch (e: Exception) {
        Log.w(TAG, "GAID unavailable: $e")
      }
    }.start()
  }
}
