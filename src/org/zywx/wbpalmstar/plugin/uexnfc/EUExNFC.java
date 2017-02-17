package org.zywx.wbpalmstar.plugin.uexnfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

/**
 * 入口类
 * 
 * @author waka
 *
 */
public class EUExNFC extends EUExBase {

    private static final String TAG = "uexNFC";

    // 回调
    private static final String CB_IS_NFC_SUPPORT = "uexNFC.cbIsNFCSupport";// 判断设备是否支持NFC回调
    private static final String CB_IS_NFC_OPEN = "uexNFC.cbIsNFCOpen";// 判断NFC是否开启回调
    private static final String CB_CONFIG_NFC = "uexNFC.cbConfigNFC";// 配置NFC回调
    private static final String CB_START_SCAN_NFC = "uexNFC.cbStartScanNFC";// 开始扫描NFC回调
    private static final String CB_STOP_SCAN_NFC = "uexNFC.cbStopScanNFC";// 停止扫描NFC回调
    private static final String CB_GET_NFC_DATA = "uexNFC.cbGetNFCData";// 得到NFC数据回调

    // 本地广播
    private LocalBroadcastManager mLocalBroadcastManager;
    private EUExNFCLocalReceiver mLocalReceiver;
    private IntentFilter mIntentFilter;
    private boolean isRegister = false;// 是否注册广播接收器标志

    // NFC相关
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;

    // NFC配置项
    private JSONObject mJsonNfcConfiguration;

    /**
     * 构造方法
     * 
     * @param arg0
     * @param arg1
     */
    public EUExNFC(Context arg0, EBrowserView arg1) {
        super(arg0, arg1);

    }

    public static void onActivityPause(Context context) {
        BDebug.i(TAG, "【onActivityPause】");
    }

    /**
     * 判断设备是否支持NFC
     * 
     * @param param
     */
    public boolean isNFCSupport(String[] param) {

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
        if (nfcAdapter == null) {// 为空则不支持
            jsCallback(CB_IS_NFC_SUPPORT, 0, EUExCallback.F_C_TEXT,
                    Constant.STATUS_FAIL);
            return false;
        }
        jsCallback(CB_IS_NFC_SUPPORT, 0, EUExCallback.F_C_TEXT,
                Constant.STATUS_SUCCESS);
        return true;
    }

    /**
     * 判断NFC是否开启
     * 
     * @param param
     */
    public boolean isNFCOpen(String[] param) {

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
        if (nfcAdapter == null) {// 为空则不支持
            jsCallback(CB_IS_NFC_OPEN, 0, EUExCallback.F_C_TEXT,
                    Constant.STATUS_FAIL);
            return false;
        }
        if (!nfcAdapter.isEnabled()) {// isEnabled为false则未打开
            jsCallback(CB_IS_NFC_OPEN, 0, EUExCallback.F_C_TEXT,
                    Constant.STATUS_FAIL);
            return false;
        }
        jsCallback(CB_IS_NFC_OPEN, 0, EUExCallback.F_C_TEXT,
                Constant.STATUS_SUCCESS);
        return true;
    }

    /**
     * 配置NFC
     * 
     * @param param
     */
    public boolean configNFC(String[] param) {

        if (mJsonNfcConfiguration == null) {
            BDebug.i(TAG, "【configNFC】	mJsonNfcConfiguration == null");
        } else {
            BDebug.i(TAG, "【configNFC】	mJsonNfcConfiguration = "
                    + mJsonNfcConfiguration.toString());
        }

        if (param.length < 1) {
            BDebug.e(TAG, "【configNFC】	param.length < 1");
            return false;
        }

        BDebug.i(TAG, "【configNFC】	param[0] = " + param[0]);

        try {

            mJsonNfcConfiguration = new JSONObject(param[0]);
            BDebug.i(TAG, "【configNFC】	mJsonNfcConfiguration = "
                    + mJsonNfcConfiguration.toString());
            jsCallback(CB_CONFIG_NFC, 0, EUExCallback.F_C_TEXT,
                    Constant.STATUS_SUCCESS);
            return true;
        } catch (JSONException e) {

            e.printStackTrace();
            mJsonNfcConfiguration = null;
            BDebug.e(TAG, "【configNFC】	JSONException" + e.getMessage(), e);
            jsCallback(CB_CONFIG_NFC, 0, EUExCallback.F_C_TEXT,
                    Constant.STATUS_FAIL);

        }
        return false;
    }

    /**
     * 开始扫描NFC
     * 
     * 须在主线程中被调用，并且只有在该Activity在前台时（要保证在onResume()方法中调用这个方法）
     * 
     * @param param
     */
    public boolean startScanNFC(String[] param) {

        // 这里将mNfcAdapter作为一个标志
        if (mNfcAdapter != null) {

            BDebug.i(TAG, "【startScanNFC】	mNfcAdapter != null return");

            // 给前端失败回调
            jsCallback(CB_START_SCAN_NFC, 0, EUExCallback.F_C_TEXT,
                    Constant.STATUS_FAIL);

            return false;
        }

        // 注册本地广播接收器
        registerLocalReceiver();

        // init NfcAdapter
        if (mNfcAdapter == null) {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);
        }

        // init PendingIntent
        Intent intent = new Intent(mContext, NFCActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // 如果NFC配置项不为空，放入Intent
        if (mJsonNfcConfiguration != null) {
            intent.putExtra(Constant.KEY_NFC_CONFIGURATION,
                    mJsonNfcConfiguration.toString());
        }
        mPendingIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // 启用前台调度
        // 须在主线程中被调用，并且只有在该Activity在前台时（要保证在onResume()方法中调用这个方法）
        mNfcAdapter.enableForegroundDispatch((Activity) mContext,
                mPendingIntent, null, null);

        // 给前端成功回调
        jsCallback(CB_START_SCAN_NFC, 0, EUExCallback.F_C_TEXT,
                Constant.STATUS_SUCCESS);
        return true;
    }

    /**
     * 停止扫描NFC
     * 
     * @param param
     */
    public boolean stopScanNFC(String[] param) {

        if (mNfcAdapter == null) {

            BDebug.i(TAG, "【stopScanNFC】	mNfcAdapter == null");

            // 给前端失败回调
            jsCallback(CB_STOP_SCAN_NFC, 0, EUExCallback.F_C_TEXT,
                    Constant.STATUS_FAIL);

            return false;
        }

        BDebug.i(TAG, "【stopScanNFC】	mNfcAdapter != null");

        // 取消注册本地广播接收器
        unRegisterLocalReceiver();

        /*
         * 停止前台调度
         * 
         * After calling {@link #enableForegroundDispatch}, an activity must
         * call this method before its {@link Activity#onPause} callback
         * completes.
         */
        mNfcAdapter.disableForegroundDispatch((Activity) mContext);
        mNfcAdapter = null;
        BDebug.i(TAG, "【stopScanNFC】	mNfcAdapter = null");

        // 给前端成功回调
        jsCallback(CB_STOP_SCAN_NFC, 0, EUExCallback.F_C_TEXT,
                Constant.STATUS_SUCCESS);
        return true;
    }

    /**
     * 返回NFC数据
     * 
     * @param nfcData
     */
    private void cbGetNFCData(String nfcData) {

        // 注销本地广播接收器
        unRegisterLocalReceiver();

        // 每次成功之后将mNfcAdapter置为空
        mNfcAdapter = null;
        BDebug.i(TAG, "【cbGetNFCData】	mNfcAdapter = null");

        jsCallback(CB_GET_NFC_DATA, 0, EUExCallback.F_C_TEXT, nfcData);
    }

    /**
     * 注册本地广播接收器
     */
    private void registerLocalReceiver() {

        if (mLocalBroadcastManager == null) {
            mLocalBroadcastManager = LocalBroadcastManager
                    .getInstance(mContext);
        }

        if (mLocalReceiver == null) {
            mLocalReceiver = new EUExNFCLocalReceiver();
        }

        if (mIntentFilter == null) {
            mIntentFilter = new IntentFilter();
            mIntentFilter
                    .addAction(Constant.LOCAL_BROADCAST_ACTION_GET_NFC_INFO_SUCCESS);// 得到NFC信息成功广播
        }

        // 如果未注册
        if (!isRegister) {

            // 注册
            mLocalBroadcastManager.registerReceiver(mLocalReceiver,
                    mIntentFilter);
            isRegister = true;
        }

    }

    /**
     * 注销本地广播接收器
     */
    private void unRegisterLocalReceiver() {

        // 如果已注册
        if (isRegister) {

            // 取消注册
            mLocalBroadcastManager.unregisterReceiver(mLocalReceiver);
            isRegister = false;
        }
    }

    /**
     * clean
     */
    @Override
    protected boolean clean() {

        BDebug.i(TAG, "clean");

        // 注销广播接收器
        unRegisterLocalReceiver();

        // 本地广播相关置空
        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager = null;
        }

        if (mLocalReceiver != null) {
            mLocalReceiver = null;
        }

        if (mIntentFilter != null) {
            mIntentFilter = null;
        }

        // NFC相关置空
        if (mNfcAdapter != null) {

            // 停止前台调度
            mNfcAdapter.disableForegroundDispatch((Activity) mContext);
            mNfcAdapter = null;
        }

        if (mPendingIntent != null) {
            mPendingIntent = null;
        }

        return false;
    }

    /**
     * 本地广播接收器
     * 
     * @author waka
     *
     */
    class EUExNFCLocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            BDebug.i(TAG, "【onReceive】		action = " + action);

            // 获取NFC信息成功广播
            if (action
                    .equals(Constant.LOCAL_BROADCAST_ACTION_GET_NFC_INFO_SUCCESS)) {

                // 获取数据
                String nfcData = intent
                        .getStringExtra(Constant.GET_NFC_INFO_INTENT_EXTRA_NAME);

                // 回调给前端
                cbGetNFCData(nfcData);
            }
        }

    }

}
