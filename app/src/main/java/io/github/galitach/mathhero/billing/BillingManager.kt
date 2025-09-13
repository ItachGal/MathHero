package io.github.galitach.mathhero.billing

import android.app.Activity
import android.app.Application
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import io.github.galitach.mathhero.data.Constants
import io.github.galitach.mathhero.data.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val application: Application,
    private val coroutineScope: CoroutineScope
) {
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    private val _isPro = MutableStateFlow(SharedPreferencesManager.isProUser())
    val isPro = _isPro.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                coroutineScope.launch {
                    handlePurchase(purchase)
                }
            }
        } else {
            Log.e("BillingManager", "Purchase error: ${billingResult.debugMessage}")
        }
    }

    private var billingClient: BillingClient

    init {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(application)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing client setup finished.")
                    coroutineScope.launch {
                        queryProductDetails()
                        queryPurchases()
                    }
                } else {
                    Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing service disconnected. Retrying...")
                startConnection()
            }
        })
    }

    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Constants.PRO_SKU)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
        val result = billingClient.queryProductDetails(params.build())
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList?.firstOrNull()
        } else {
            Log.e("BillingManager", "Failed to query product details: ${result.billingResult.debugMessage}")
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val product = _productDetails.value
        if (product == null) {
            Log.e("BillingManager", "Product details not available to launch purchase flow.")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e("BillingManager", "Failed to launch billing flow: ${billingResult.debugMessage}")
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        grantProAccess()
                    } else {
                        Log.e("BillingManager", "Purchase acknowledgment failed: ${billingResult.debugMessage}")
                    }
                }
            } else {
                grantProAccess()
            }
        }
    }

    private suspend fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val proPurchase = purchases.find { purchase ->
                    purchase.products.contains(Constants.PRO_SKU) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (proPurchase != null) {
                    grantProAccess()
                } else {
                    revokeProAccess()
                }
            }
        }
    }

    private fun grantProAccess() {
        SharedPreferencesManager.setProUser(true)
        _isPro.value = true
    }

    private fun revokeProAccess() {
        SharedPreferencesManager.setProUser(false)
        _isPro.value = false
    }
}