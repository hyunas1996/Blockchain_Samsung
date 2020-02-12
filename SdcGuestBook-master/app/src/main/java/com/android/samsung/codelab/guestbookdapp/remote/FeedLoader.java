package com.android.samsung.codelab.guestbookdapp.remote;

import android.util.Log;

import com.android.samsung.codelab.guestbookdapp.ethereum.FunctionUtil;
import com.android.samsung.codelab.guestbookdapp.model.Feed;
import com.android.samsung.codelab.guestbookdapp.util.AppExecutors;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jnr.ffi.Struct;

import static java.util.Collections.singletonList;

public class FeedLoader {
    private String address;

    public FeedLoader(String address) {
        this.address = address;
    }

    public void loadFeeds(int numOfFeed, Listener listener) {
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                int postCount = getPostCount();
                int lastIndex = postCount - 1 - numOfFeed;
                if (lastIndex < 0)
                    lastIndex = 0;

                ArrayList<Feed> feedList = new ArrayList<>();
                for (int index = postCount - 1; index >= lastIndex; index--) {
                    feedList.add(getPost(index));
                }
                listener.feedDidLoaded(true, feedList, "Success");
            } catch (Exception e) {
                listener.feedDidLoaded(false, null, e.getMessage());
            }
        });

    }

    private int getPostCount() throws Exception {
        Function functionGetPostCount = FunctionUtil.createGetPostCountSmartContractCall();
        String data = FunctionEncoder.encode(functionGetPostCount);
        Transaction tx = Transaction.createEthCallTransaction(address, FunctionUtil.CONTRACT_ADDRESS, data);

        EthCall result = RemoteManager.getInstance().sendEthCall(tx);
        if (result.hasError()) {
            throw new Exception("Get Post count eth call error" + result.getError().getMessage());
        }

        String value = result.getValue();
        List<TypeReference<Type>> outputParameters = functionGetPostCount.getOutputParameters();
        List<Type> types = FunctionReturnDecoder.decode(value, outputParameters);
        Type type = types.get(0);
        BigInteger postCount = (BigInteger) type.getValue();

        return postCount.intValue();
    }

    private Feed getPost(int index) throws Exception {

        // TODO : Make Get Post Smart Contract call(Live code)
        // encode function with FunctionEncoder
        // Make Eth Call Transaction
        // send ETH Call

        Function functionGetPost = new Function( "getPost"
        , singletonList(new Uint(BigInteger.valueOf(index)))
        , Arrays.asList(
                new TypeReference<Utf8String>() {
                }
                , new TypeReference<Utf8String>() {
                }
                , new TypeReference<Utf8String>() {
                }
                , new TypeReference<Utf8String>() {
                }
        ));


        //위의 코드를 다음줄처럼 해도 괜찮음
        //Function functionGetPost = FunctionUtil.createGetPostSmartContractCall(index);

        String data = FunctionEncoder.encode(functionGetPost);
        Transaction tx = Transaction.createEthCallTransaction(address, FunctionUtil.CONTRACT_ADDRESS, data);

        EthCall result = RemoteManager.getInstance().sendEthCall(tx);
        if (result.hasError()) {
            throw new Exception("Get Post eth call error" + result.getError().getMessage());
        }


        String value = result.getValue();
        List<TypeReference<Type>> outputParameters = functionGetPost.getOutputParameters();
        List<Type> types = FunctionReturnDecoder.decode(value, outputParameters);

        Log.d("ExampleCode", "type lenght : "  + types.size());
        Log.d("ExampleCode", "value 1(name) : " + types.get(0).getValue());
        Log.d("ExampleCode", "value 2(commnet) : " + types.get(1).getValue());
        Log.d("ExampleCode", "value 3(date) : " + types.get(2).getValue());
        Log.d("ExampleCode", "value 4(emoji) : " + types.get(3).getValue());

        return new Feed(
                (String) types.get(3).getValue()
                , (String) types.get(0).getValue()
                , (String) types.get(1).getValue()
                , (String) types.get(2).getValue()
        );

    }

    public interface Listener {
        void feedDidLoaded(boolean success, List<Feed> feedList, String message);
    }
}
