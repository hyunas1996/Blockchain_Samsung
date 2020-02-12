package com.android.samsung.codelab.guestbookdapp.presenter;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.samsung.codelab.guestbookdapp.contract.WriteFeedContract;
import com.android.samsung.codelab.guestbookdapp.ethereum.FunctionUtil;
import com.android.samsung.codelab.guestbookdapp.model.Feed;
import com.android.samsung.codelab.guestbookdapp.model.UserInfo;
import com.android.samsung.codelab.guestbookdapp.remote.RemoteManager;
import com.android.samsung.codelab.guestbookdapp.util.AppExecutors;
import com.samsung.android.sdk.coldwallet.ScwCoinType;
import com.samsung.android.sdk.coldwallet.ScwService;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class WriteFeedPresenter implements WriteFeedContract.PresenterContract {

    private static final String TAG = WriteFeedPresenter.class.getSimpleName();
    private WriteFeedContract.ViewContract contract;

    public WriteFeedPresenter(WriteFeedContract.ViewContract contract) {
        this.contract = contract;
    }

    @Override
    public void actionSend() {
        Feed feed = UserInfo.getInstance().getFeedToWrite();
        if (TextUtils.isEmpty(feed.getComment())
                || TextUtils.isEmpty(feed.getName())
                || TextUtils.isEmpty(feed.getEmoji())) {
            contract.toastMessage("Please fill in all fields.");
            return;
        }
        contract.setLoadingProgress(true);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy HH:mm:ss", Locale.US);
        String date = dateFormat.format(System.currentTimeMillis());
        UserInfo.getInstance().getFeedToWrite().setDate(date);

        AppExecutors.getInstance().networkIO().execute(() -> {

            BigInteger nonce = getNonce();


            // TODO : Make post comment Raw Transaction (Live code)
            // make unsigned tx by Web3j TransactionEncoder

            RawTransaction tx = createPostTransaction(nonce);
            byte[] unsignedTx = TransactionEncoder.encode(tx);
            signTransaction(unsignedTx, (success, message) -> {
                if (success) {
                    contract.toastMessage("Success to post your comment");
                } else {
                    contract.toastMessage("Fail to post your comment.");
                }
                contract.setLoadingProgress(false);
                contract.finishActivity();
            });

        });

    }

    public void changeEmoji() {
        contract.setEmojiBottomSheet();
    }

    // create Post Transaction : post 함수를 호출하는 부분
    private RawTransaction createPostTransaction(BigInteger nonce) {
        //feed 객체를 가져옴 -> feed 객체는 Comments 에서 만든 내용이 담겨져 있는 것
        Feed feed = UserInfo.getInstance().getFeedToWrite();

        // TODO : Make Web3j Function to call Post Smart contract call (Live code)
        // Encode function to HEX String


        Function func = new Function("post" //name
                , Arrays.asList(
                new Utf8String(feed.getName())  //string type의 네가지 인자
                , new Utf8String(feed.getComment())
                , new Utf8String(feed.getDate())
                , new Utf8String(feed.getEmoji()))
                , Collections.emptyList());

        //web3js에서 제공하는 function encoder로 encode
        String data = FunctionEncoder.encode(func);

        Log.d("ExampleCode", "encoded func : " + data);

        return RawTransaction.createTransaction(
                nonce
                , BigInteger.valueOf(3_000_000_000L)
                , BigInteger.valueOf(1_000_000L)
                , FunctionUtil.CONTRACT_ADDRESS
                , data);

    }

    private BigInteger getNonce() {
        String address = UserInfo.getInstance().getAddress();
        BigInteger nonce;
        try {
            nonce = RemoteManager.getInstance().getNonce(address);
        } catch (Exception e) {
            nonce = BigInteger.ZERO;
        }

        return nonce;

    }

    private void signTransaction(byte[] unsignedTx, SignTransactionListener listener) {

        // TODO : Sign the transaction with Samsung blockchain keystore
        // Success, Send Eth Transaction > sendSignedTransaction(signedTransaction) and Update listener
        // fail, update listener
        // Update Listener call > listener.transactionDidFinish(result, "");

        String hdPath = ScwService.getHdPath(ScwCoinType.ETH, 0);

        ScwService.getInstance().signEthTransaction(new ScwService.ScwSignEthTransactionCallback() {
            @Override
            public void onSuccess(byte[] bytes) {

                listener.transactionDidFinish(true, "");
                sendSignedTransaction(bytes);
            }

            @Override
            public void onFailure(int i, @Nullable String s) {
                listener.transactionDidFinish(false, "");

            }
        } ,unsignedTx, hdPath);

    }

    private boolean sendSignedTransaction(byte[] bytes) {
        String hex = Numeric.toHexString(bytes);
        try {
            RemoteManager.getInstance().sendRawTransaction(hex);
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            return false;
        }

    }

    interface SignTransactionListener {
        void transactionDidFinish(boolean success, String message);
    }

}
