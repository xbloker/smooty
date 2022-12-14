package candybar.lib.utils;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.Constants;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.danimahardhika.android.helpers.core.utils.LogUtil;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import candybar.lib.items.InAppBilling;
import candybar.lib.preferences.Preferences;
import candybar.lib.utils.listeners.InAppBillingListener;
import candybar.lib.utils.listeners.RequestListener;

/*
 * CandyBar - Material Dashboard
 *
 * Copyright (c) 2014-2016 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class InAppBillingProcessor implements BillingProcessor.IBillingHandler {

    private final Context mContext;
    private boolean mIsInitialized;
    private BillingProcessor mBillingProcessor;

    private static String mLicenseKey;
    private static WeakReference<InAppBillingProcessor> mInAppBilling;

    private InAppBillingProcessor(Context context) {
        mContext = context;
    }

    public void init(@NonNull String licenseKey) {
        mLicenseKey = licenseKey;
        getProcessor();
    }

    public BillingProcessor getProcessor() {
        if (mLicenseKey == null) {
            LogUtil.e("InAppBillingProcessor: license key is null, make sure to call InAppBillingProcessor.init() first");
        }

        if (mInAppBilling.get().mBillingProcessor == null || !mInAppBilling.get().mIsInitialized) {
            mInAppBilling.get().mBillingProcessor = new BillingProcessor(
                    mInAppBilling.get().mContext,
                    mLicenseKey,
                    mInAppBilling.get());
        }
        return mInAppBilling.get().mBillingProcessor;
    }

    public void destroy() {
        if (mInAppBilling != null && mInAppBilling.get() != null) {
            if (mInAppBilling.get().getProcessor() != null) {
                mInAppBilling.get().getProcessor().release();
            }

            mInAppBilling.clear();
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mInAppBilling.get().getProcessor().handleActivityResult(requestCode, resultCode, data);
    }

    public static InAppBillingProcessor get(@NonNull Context context) {
        if (mInAppBilling == null || mInAppBilling.get() == null) {
            mInAppBilling = new WeakReference<>(new InAppBillingProcessor(context));
        }
        return mInAppBilling.get();
    }

    @Override
    public void onProductPurchased(@NotNull String productId, TransactionDetails details) {
        if (mInAppBilling == null || mInAppBilling.get() == null) {
            LogUtil.e("InAppBillingProcessor error: not initialized");
            return;
        }

        if (Preferences.get(mInAppBilling.get().mContext).getInAppBillingType() == InAppBilling.DONATE) {
            try {
                InAppBillingListener listener = (InAppBillingListener) mInAppBilling.get().mContext;
                listener.onInAppBillingConsume(Preferences.get(mInAppBilling.get().mContext)
                        .getInAppBillingType(), productId);
            } catch (Exception ignored) {
            }
        } else if (Preferences.get(mInAppBilling.get().mContext).getInAppBillingType() ==
                InAppBilling.PREMIUM_REQUEST) {
            Preferences.get(mInAppBilling.get().mContext).setPremiumRequest(true);
            Preferences.get(mInAppBilling.get().mContext).setPremiumRequestProductId(productId);
            try {
                RequestListener listener = (RequestListener) mInAppBilling.get().mContext;
                listener.onPremiumRequestBought();
            } catch (Exception ignored) {
            }
        }

        Preferences.get(mInAppBilling.get().mContext).setInAppBillingType(-1);
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        if (mInAppBilling == null || mInAppBilling.get() == null) {
            LogUtil.e("InAppBillingProcessor error: not initialized");
            return;
        }

        if (errorCode == Constants.BILLING_RESPONSE_RESULT_USER_CANCELED) {
            if (Preferences.get(mInAppBilling.get().mContext).getInAppBillingType()
                    == InAppBilling.PREMIUM_REQUEST) {
                Preferences.get(mInAppBilling.get().mContext).setPremiumRequestCount(0);
                Preferences.get(mInAppBilling.get().mContext).setPremiumRequestTotal(0);
            }
            Preferences.get(mInAppBilling.get().mContext).setInAppBillingType(-1);
        } else if (errorCode == Constants.BILLING_ERROR_FAILED_TO_INITIALIZE_PURCHASE) {
            mInAppBilling.get().mIsInitialized = false;
        }
    }

    @Override
    public void onBillingInitialized() {
        if (mInAppBilling == null || mInAppBilling.get() == null) {
            LogUtil.e("InAppBillingProcessor error: not initialized");
            return;
        }

        mInAppBilling.get().mIsInitialized = true;
    }
}
