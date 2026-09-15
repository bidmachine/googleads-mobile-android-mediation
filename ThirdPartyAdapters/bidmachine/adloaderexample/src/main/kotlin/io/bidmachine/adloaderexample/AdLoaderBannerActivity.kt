package io.bidmachine.adloaderexample

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.nativead.NativeAd

/**
 * Reproduces the banner integration The Weather Channel uses on Android: an Ad Manager
 * `AdLoader` request with the Ad Manager banner and native ad types, no `AdManagerAdView` of its
 * own, and the banner sizes supplied only through `forAdManagerAdView`.
 */
class AdLoaderBannerActivity : Activity() {

  /** The publisher's home screen unit, which carries the BidMachine banner mapping. */
  private val adUnitId = "/7646/app_android_us/thr_display/home_screen/today"

  /**
   * What the request offers Google. The first entry is the primary size: Google puts it on the
   * bid request and hands it to the adapter as the signal and load size, whatever the bid
   * declares. Picked with the Sizes button, or at launch with `--es sizes <name>`.
   */
  enum class SizeSet(val title: String) {
    WEATHER_FEED("Weather feed: 240x133 first, 15 sizes"),
    BANNER_FIRST("320x50 first, then the feed sizes"),
    MREC_FIRST("300x250 first, then the feed sizes"),
    BANNER_ONLY("320x50 only"),
    MREC_ONLY("300x250 only"),
    ANCHORED_ADAPTIVE("Anchored adaptive, screen width"),
    INLINE_ADAPTIVE("Inline adaptive, screen width");

    fun adSizes(context: Context): List<AdSize> {
      val width = screenWidthDp(context)
      return when (this) {
        WEATHER_FEED -> weatherFeedSizes
        BANNER_FIRST -> listOf(AdSize.BANNER) + weatherFeedSizes.filter { it != AdSize.BANNER }
        MREC_FIRST ->
          listOf(AdSize.MEDIUM_RECTANGLE) + weatherFeedSizes.filter { it != AdSize.MEDIUM_RECTANGLE }
        BANNER_ONLY -> listOf(AdSize.BANNER)
        MREC_ONLY -> listOf(AdSize.MEDIUM_RECTANGLE)
        ANCHORED_ADAPTIVE ->
          listOf(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
        INLINE_ADAPTIVE ->
          listOf(AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, width))
      }
    }

    companion object {
      /**
       * The sizes The Weather Channel's iOS feed unit reports, in the order Google forwards them:
       * 240x133 leads, so it is the size Google puts on the bid request; 320x50 is in the list.
       */
      val weatherFeedSizes: List<AdSize> =
        listOf(
          AdSize(240, 133), AdSize(300, 100), AdSize(292, 30), AdSize(300, 250),
          AdSize(240, 120), AdSize(320, 50), AdSize(336, 280), AdSize(300, 57),
          AdSize(320, 100), AdSize(300, 50), AdSize(375, 50), AdSize(220, 90),
          AdSize(300, 31), AdSize(250, 250), AdSize(234, 60),
        )

      fun named(name: String?): SizeSet? = entries.firstOrNull { it.name.equals(name, true) }

      private fun screenWidthDp(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return (metrics.widthPixels / metrics.density).toInt()
      }
    }
  }

  private var sizeSet = SizeSet.WEATHER_FEED
  private lateinit var statusView: TextView
  private lateinit var container: FrameLayout
  private var adView: AdManagerAdView? = null
  private var nativeAd: NativeAd? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    sizeSet = SizeSet.named(intent.getStringExtra("sizes")) ?: sizeSet
    setContentView(buildLayout())
    load()
  }

  override fun onDestroy() {
    adView?.destroy()
    nativeAd?.destroy()
    super.onDestroy()
  }

  private fun buildLayout(): LinearLayout {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    bar.addView(TextView(this).apply {
      text = "AdLoader banner"
      textSize = 20f
      gravity = Gravity.CENTER_VERTICAL
      setPadding(32, 0, 0, 0)
    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
    bar.addView(Button(this).apply { text = "Sizes"; setOnClickListener { sizesTapped() } })
    bar.addView(Button(this).apply { text = "Reload"; setOnClickListener { load() } })
    root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 160))
    statusView = TextView(this).apply { setPadding(32, 16, 32, 16) }
    root.addView(statusView)
    container = FrameLayout(this)
    root.addView(container, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    return root
  }

  /** Picks what the next loads offer Google, then reloads. */
  private fun sizesTapped() {
    val options = SizeSet.entries
    AlertDialog.Builder(this)
      .setTitle("Sizes to offer Google")
      .setSingleChoiceItems(options.map { it.title }.toTypedArray(), options.indexOf(sizeSet)) { dialog, index ->
        sizeSet = options[index]
        dialog.dismiss()
        load()
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun load() {
    adView?.destroy()
    adView = null
    nativeAd?.destroy()
    nativeAd = null
    container.removeAllViews()
    val sizes = sizeSet.adSizes(this)
    Log.i(TAG, "loading $adUnitId with ${sizes.size} sizes: ${sizes.joinToString(" ")}")
    statusView.text = "Loading $adUnitId\nSizes: ${sizeSet.title}"

    val loader =
      AdLoader.Builder(this, adUnitId)
        .forAdManagerAdView({ view -> bannerLoaded(view) }, *sizes.toTypedArray())
        .forNativeAd { ad -> nativeLoaded(ad) }
        .withAdListener(
          object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
              Log.w(TAG, "load failed: $error")
              statusView.text = "Load failed: ${error.code} ${error.message}\n${error.responseInfo}"
            }
          }
        )
        .build()
    loader.loadAd(AdManagerAdRequest.Builder().build())
  }

  private fun bannerLoaded(view: AdManagerAdView) {
    adView = view
    val size = view.adSize
    val adapter = view.responseInfo?.mediationAdapterClassName
    Log.i(TAG, "banner received: ${size?.width}x${size?.height} from $adapter")
    statusView.text = "Banner received: ${size?.width}x${size?.height} from $adapter\nSizes: ${sizeSet.title}"
    container.addView(
      view,
      FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL),
    )
  }

  private fun nativeLoaded(ad: NativeAd) {
    nativeAd = ad
    val adapter = ad.responseInfo?.mediationAdapterClassName
    Log.i(TAG, "native received from $adapter: ${ad.headline}")
    statusView.text = "Native received from $adapter: ${ad.headline}\nSizes: ${sizeSet.title}"
  }
}
