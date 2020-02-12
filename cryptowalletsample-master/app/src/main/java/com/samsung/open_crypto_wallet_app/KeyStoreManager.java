package com.samsung.open_crypto_wallet_app;

import android.content.Context;
import android.databinding.ObservableBoolean;
import android.support.annotation.Nullable;
import android.util.Log;

import com.samsung.android.sdk.coldwallet.Scw;
import com.samsung.android.sdk.coldwallet.ScwCoinType;
import com.samsung.android.sdk.coldwallet.ScwService;
import com.samsung.open_crypto_wallet_app.view.AlertUtil;
import com.samsung.open_crypto_wallet_app.view_model.AccountViewModel;
import com.samsung.open_crypto_wallet_app.view_model.TransactionViewModel;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.tx.ChainId;
import org.web3j.utils.Numeric;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KeyStoreManager {

    private static KeyStoreManager keyStoreManagerInstance;
    private ScwService mScwService;
    private ScwService.ScwCheckForMandatoryAppUpdateCallback mScwCheckForMandatoryAppUpdateCallback;

    private static final String CREDENTIAL_DIR = ".credentials";
    private static final String CREDENTIAL_FILE_PATH_KEY = "credential_key";
    private static final String HARD_CODED_PASSWORD = "PASSWORD";
    private Credentials mCredential;

    private Context mContext;

    private KeyStoreManager(Context context) {

        mScwService = ScwService.getInstance();
        mContext = context;
    }

    public static KeyStoreManager getInstance(Context context) {
        if (keyStoreManagerInstance == null) {
            keyStoreManagerInstance = new KeyStoreManager(context);
        } else {
            keyStoreManagerInstance.setContext(context);
        }
        return keyStoreManagerInstance;
    }

    private void setContext(Context context) {
        mContext = context;
    }

    public boolean isSBKSupported() {
        mScwService = ScwService.getInstance();
        if (mScwService == null) {
            Log.e(Util.LOG_TAG, "SBK is Not Supported on Device");
            return false;
        } else {
            return true;
        }
    }

    public String getSeedHashFromSBK() {
        if (isSBKSupported()) {
            return mScwService.getSeedHash();
        } else {
            return "SBK not supported";
        }
    }

    public boolean isSBKWalletSet() {
        if (isSBKSupported()) {
            int hashLength = getSeedHashFromSBK().length();
            if (hashLength == 0) {
                return false;
            } else {
                return true;
            }
        } else return false;
    }


    private ScwService.ScwCheckForMandatoryAppUpdateCallback getCheckMandatoryUpdateRequiredCallback(ObservableBoolean mIsMandatoryUpdateRequired, ObservableBoolean mIsCheckUpdateCompleted) {
        mScwCheckForMandatoryAppUpdateCallback = new ScwService.ScwCheckForMandatoryAppUpdateCallback() {
            @Override
            public void onMandatoryAppUpdateNeeded(boolean isUpdateMandatory) {
                Log.i(Util.LOG_TAG, "In SBK, Mandatory Update Required:" + isUpdateMandatory);
                mIsMandatoryUpdateRequired.set(isUpdateMandatory);
                mIsCheckUpdateCompleted.set(true);
            }
        };
        return mScwCheckForMandatoryAppUpdateCallback;
    }

    public void checkMandatoryUpdateRequired(ObservableBoolean mIsMandatoryUpdateRequired, ObservableBoolean mIsCheckUpdateCompleted) {
        if (isSBKSupported()) {
            mScwService.checkForMandatoryAppUpdate(getCheckMandatoryUpdateRequiredCallback(mIsMandatoryUpdateRequired, mIsCheckUpdateCompleted));
        }
    }

    public Integer getKeystoreApiLevel() {
        if (isSBKSupported()) {
            return mScwService.getKeystoreApiLevel();
        } else return -1;       //dummy return
    }

    public Credentials getCredential() throws Exception{
        if(mCredential==null){
            final String CREDENTIAL_PATH = mContext.getFilesDir() + "/" + CREDENTIAL_DIR;;
            File credentialDir = new File(CREDENTIAL_PATH);
            if(!credentialDir.exists()){
                boolean result = credentialDir.mkdir();
                if(!result){
                    throw new Exception("Cannot make dir");
                }
            }

            String credentialFilename = SharedPreferenceManager.getCredentialFilePath(mContext, CREDENTIAL_FILE_PATH_KEY);
            if(credentialFilename==null){
                // need to make
                credentialFilename = WalletUtils.generateNewWalletFile(HARD_CODED_PASSWORD, credentialDir, false);
                SharedPreferenceManager.setCredentialFilePath(mContext, CREDENTIAL_FILE_PATH_KEY, credentialFilename);
            }
            mCredential = WalletUtils.loadCredentials(HARD_CODED_PASSWORD, CREDENTIAL_PATH + "/" + credentialFilename);
        }
        return mCredential;
    }


    public void getPublicAddress(String hdPath) {
        Log.i(Util.LOG_TAG, "Init SBK to read address with " + hdPath);
        if (isSBKSupported()) {
            // TODO : Get My Ethereum Address with Keystore
            // 키스토어로부터 주소 가져오기

            //성공할 경우 주소를 보여주고,
            // success, Update DBManager > DBManager.onGetAddressSuccess(publicAddress);

            //실패할 경우 알림을 띄운다.
            // fail, show alert with errorCode > AlertUtil.handleSBKError(errorCode);

            //ScwService로 시작해야하고, 인스턴스를 받아와야함
            //주소를 가져올 때 list로 받아서 가져옴
            //실습에서는 주소 list에 하나만 받아서 0번째 주소를 가져오기로.

            Log.d("KeyStoremanager", "hdPath : " + hdPath);

            ArrayList<String> hdPathList = new ArrayList<>();
            hdPathList.add(hdPath); //DBManager에서 받아온 hdPath를 리스트에 추가해줌

            ScwService.getInstance().getAddressList(new ScwService.ScwGetAddressListCallback() {
                @Override
                public void onSuccess(List<String> addresslist) {
                    String publicAddress = addresslist.get(0);
                    Log.d("KeyStoremanager", "public address : " + publicAddress);
                    DBManager.onGetAddressSuccess(publicAddress);
                }

                @Override
                public void onFailure(int errorCode, @Nullable String s) {
                    AlertUtil.handleSBKError(errorCode);
                }
            }, hdPathList);
        }
    }

    public void getPublicAddress() {
        Log.i(Util.LOG_TAG, "getPublicAddress through credential ");

        try {
            String publicAddress = getCredential().getAddress();

            Log.i(Util.LOG_TAG, "Address from web3j is : " + publicAddress);
            DBManager.onGetAddressSuccess(publicAddress);


        } catch (Exception e) {
            Log.e(Util.LOG_TAG, "Error when getPublicAddress()");
        }
    }

    public void signTransaction(byte[] unsignedTransaction) {
        //unsignedTransaction이 인자로 들어옴
        ///이걸 만드는 곳은 TransactionViewModel.java
        //RawTransaction 이라는 객체가 있음 -> transaction 내부에 들어가는 내용이 이 객체에 선언되어있음

        //generateUnsignedtransaction 메소드 내에서 transaction을 만들어낸다.
        Log.i(Util.LOG_TAG, "Init SBK to sign transaction");
        if (isSBKSupported()) {
            // TODO : Sign Transaction with Keystore
            // sign transaction 가져오는 것
            // Success, Set signed tx to TransactionViewModel > TransactionViewModel.setSignedTransaction(bytes);
            // Fail, Show alert with error code > AlertUtil.handleSBKError(errorCode);

            String unsignedTxHex = Numeric.toHexString(unsignedTransaction);
            String hdPath = ScwService.getHdPath(ScwCoinType.ETH, 0);
            Log.d("keystoreManager", "unsigned tx : " + unsignedTxHex);

            ScwService.getInstance().signEthTransaction(new ScwService.ScwSignEthTransactionCallback() {
                @Override
                public void onSuccess(byte[] bytes) {
                    String signedTxHex = Numeric.toHexString(bytes);
                    Log.d("keysotreManager", "signed tx : " + signedTxHex);
                    TransactionViewModel.setSignedTransaction(bytes);
                }

                @Override
                public void onFailure(int errorCode, @Nullable String s) {
                    AlertUtil.handleSBKError(errorCode);
                }
            }, unsignedTransaction, hdPath);
        }
    }

    public void signEthTransactionWithWeb3j(RawTransaction rawTransaction) {
        Log.i(Util.LOG_TAG, "Init Web3j to sign transaction");

        String network = SharedPreferenceManager.getDefaultNetwork(mContext);
        byte chainId = -1;
        if(NodeConnector.ROPSTEN.equals(network)) {
            chainId = 3;
        } else if(NodeConnector.KOVAN.equals(network)) {
            chainId = 42;
        } else if(NodeConnector.MAINNET.equals(network)) {
            chainId = 1;
        }

        byte[] signedMessage;

        try {
            if (chainId > ChainId.NONE) {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, mCredential);
            } else {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, mCredential);
            }

            TransactionViewModel.setSignedTransaction(signedMessage);

            Log.i(Util.LOG_TAG, "Success signMessage with web3j");

        } catch (Exception e) {
            Log.i(Util.LOG_TAG, "Exception when signMessage with web3j. message : " + e.getMessage());
        }
    }
}