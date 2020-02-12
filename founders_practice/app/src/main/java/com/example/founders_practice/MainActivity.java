package com.example.founders_practice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import com.samsung.android.sdk.blockchain.*;
import com.samsung.android.sdk.blockchain.account.Account;
import com.samsung.android.sdk.blockchain.account.ethereum.EthereumAccount;
import com.samsung.android.sdk.blockchain.coinservice.CoinNetworkInfo;
import com.samsung.android.sdk.blockchain.coinservice.CoinServiceFactory;
import com.samsung.android.sdk.blockchain.coinservice.ethereum.EthereumService;
import com.samsung.android.sdk.blockchain.exception.SsdkUnsupportedException;
import com.samsung.android.sdk.blockchain.network.EthereumNetworkType;
import com.samsung.android.sdk.blockchain.ui.CucumberWebView;
import com.samsung.android.sdk.blockchain.ui.OnSendTransactionListener;
import com.samsung.android.sdk.blockchain.wallet.HardwareWallet;
import com.samsung.android.sdk.blockchain.wallet.HardwareWalletType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.web3j.protocol.core.Ethereum;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity implements OnSendTransactionListener {

    //webView 사용하기 위해서도 getAccount, generate, connect가 다 제대로 되어 있어야함

    Button connectBtn;
    Button generateAccountBtn;
    Button getAccountsBtn;
    Button paymentSheetBtn;
    Button sendSmartContract;
    Button webViewInit;

    private SBlockchain sBlockchain; //전역변수
    private HardwareWallet wallet;
    private Account generateAccount;

    //web view 를 위한 payment sheet가 따로 있음
    private CucumberWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sblockchain 객체 생성하기 (전역변수로)
        sBlockchain = new SBlockchain();

        //Sblockchain의 initialize 함수를 이용하여 블록체인을 초기화 시킨다.
        //initialize 합수는 try catch 문으로 감싸서 사용해야함

        try {
            sBlockchain.initialize(this);
        } catch (SsdkUnsupportedException e) {
            e.printStackTrace();
        }

        //button을 눌렀을 때 동작하도록 연결해주자.
        //findViewById를 이용해서 버튼과 동작을 연결 시켜줌
        //onCreate 함수 위에 전역변수로 버튼 5개 생성

        connectBtn = findViewById(R.id.connect);
        generateAccountBtn = findViewById(R.id.generateAccount);
        getAccountsBtn = findViewById(R.id.getAccounts);
        paymentSheetBtn = findViewById(R.id.paymentsheet);
        sendSmartContract = findViewById(R.id.sendSmartContract);

        webViewInit = findViewById(R.id.webViewInit);

        webView = findViewById(R.id.cucumberWebView);

        //버튼 클릭시 수행할 동작을 넣는다.
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });

        generateAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generate();
            }
        });

        getAccountsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAccounts();
            }
        });

        paymentSheetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentSheet();
            }
        });

        webViewInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webViewInit();
            }
        });
    }

    private void webViewInit() {

        CoinNetworkInfo coinNetworkInfo = new CoinNetworkInfo(
                CoinType.ETH, EthereumNetworkType.ROPSTEN,
                "https://ropsten.infura.io/v3/70ddb1f89ca9421885b6268e847a459d"
        );

        //account를 가져옴
        List<Account> accounts = sBlockchain.getAccountManager()
                .getAccounts(wallet.getWalletId(), CoinType.ETH, EthereumNetworkType.ROPSTEN);

        //이더리움 서비스 클래스를 받아야함
        //이더리움과 관련된 함수들을 받을 수 있다.
        EthereumService service = (EthereumService) CoinServiceFactory.getCoinService(this, coinNetworkInfo);

        //webview 초기화
        webView.init(service, accounts.get(0), this);

        //webView가 초기화되면 특정 url로 접속하도록 설정함
        webView.loadUrl("https://faucet.metamask.io/");
    }

    private void paymentSheet(){

        CoinNetworkInfo coinNetworkInfo = new CoinNetworkInfo(
                CoinType.ETH, EthereumNetworkType.ROPSTEN,
                "https://ropsten.infura.io/v3/70ddb1f89ca9421885b6268e847a459d"
        );

        //account를 가져옴
        List<Account> accounts = sBlockchain.getAccountManager()
                .getAccounts(wallet.getWalletId(), CoinType.ETH, EthereumNetworkType.ROPSTEN);

        //이더리움 서비스 클래스를 받아야함
        //이더리움과 관련된 함수들을 받을 수 있다.
        EthereumService service = (EthereumService) CoinServiceFactory.getCoinService(this, coinNetworkInfo);

        Intent intent = service
                .createEthereumPaymentSheetActivityIntent(
                        this,
                        wallet,
                        (EthereumAccount) accounts.get(0), //from account
                        "0x7526A6232Be6c7234bF53A3ce87Bda7d2FC530c1", //to account
                        new BigInteger("100000000000000"), //10만이더리움이 아니라 Gwei 단위로 10만을 의미함
                        null,
                        null
                );
                //비트코인같은 경우 fee가 제일 높은 것을 node가 먼저 받는다 (가정 : 논스가 같은 트랜잭션들 사이에서)

        startActivityForResult(intent, 0);
    }

    private void getAccounts(){
        List<Account> accounts = sBlockchain.getAccountManager()
                .getAccounts(wallet.getWalletId(), CoinType.ETH, EthereumNetworkType.ROPSTEN);
        //서버를 다녀오는 것이 아니라 앱 내의 캐시에 접근해서 가져오는 것이므로 비동기할 필요가 없음.

        //list 출력 -> accounts 리스트를 리스트에 넣으면 알아서 출력됨
        //왼쪽에 디버그를 걸면 주소를 알 수 있
        Log.d("MyApp", Arrays.toString(new List[]{accounts}));
    }

    private void generate(){
        //coinNetworkInfo 를 선언
        //노드를 무료로 제공하는 사이트가 있음 (노드 주소가 rpcAddress에 들어가야함)
        //사이트는 Infra.io - 계정 생성해서 넣으면 된다.
        CoinNetworkInfo coinNetworkInfo = new CoinNetworkInfo(
                CoinType.ETH, EthereumNetworkType.ROPSTEN,
                "https://ropsten.infura.io/v3/70ddb1f89ca9421885b6268e847a459d"
        );

        sBlockchain.getAccountManager()
                .generateNewAccount(wallet, coinNetworkInfo)
                .setCallback(new ListenableFutureTask.Callback<Account>() {

                    @Override
                    public void onSuccess(Account account) {
                        generateAccount = account;

                        //Log 를 사용하면 아래의 Logcat 을 통해서 볼 수 있음
                        Log.d("MyApp", account.toString());
                    }

                    @Override
                    public void onFailure(@NotNull ExecutionException e) {

                    }


                    @Override
                    public void onCancelled(@NotNull InterruptedException e) {

                    }
                });
    }

    private void connect(){
        //hard wallet manager : cold wallet을 이용하여 무언가를 하도록 해줌
        //manager 함수 중에서 connect 이용해서 Samsung 월렛과 연결시킨다 (월렛 종류는 렛저 등 총 3가지)
        //뒤에 있는 boolean 은 reset을 할지 말지에 대한 여부
        //connect 함수가 반환하는 것이 하드웨어 월렛이된다.

        //connect 함수는 비동기로 해야함 (usb로 connection시 멈추지 않기 위해서는 별도의 스레드를 만들어서 비동기로 해야함)
        //비동기는 setCallback 함수를 이용해서!
        sBlockchain.getHardwareWalletManager()
                .connect(HardwareWalletType.SAMSUNG, true)
                .setCallback(new ListenableFutureTask.Callback<HardwareWallet>() {

            @Override
            public void onSuccess(HardwareWallet hardwareWallet) {
                //이더리움 account를 불러오자
                //generateAccount 를 이용해서 account를 생성한다
                // -> 우리가 위에서 connect한 결과로 받은 하드웨어 월렛을 넣는다.
                //coinNetworkInfo 를 넣어서 어떤 코인을 사용할지에 대한 정보를 받는다.

                //계좌는 private 키 없이는 만들 수 없고,
                //private 키는 하드월렛에 있으므로, 하드월렛을 만들었던 것이다.
                wallet = hardwareWallet;
                //getAccounts 함수를 사용하면 local에서 가져온다
            }

            @Override
            public void onFailure(ExecutionException e) {

            }

            @Override
            public void onCancelled(InterruptedException e) {

            }
        });

    }


    //OnSendTransactionListener 인터페이스를 구현하는 것
    @Override
    public void onSendTransaction(
            @NotNull String requestId,
            @NotNull EthereumAccount fromAccount,
            @NotNull String toAddress,
            @org.jetbrains.annotations.Nullable BigInteger value,
            @org.jetbrains.annotations.Nullable String data,
            @org.jetbrains.annotations.Nullable BigInteger nonce
    ) {
        HardwareWallet connectedHardwareWallet =
                sBlockchain.getHardwareWalletManager().getConnectedHardwareWallet();
        Intent intent =
                webView.createEthereumPaymentSheetActivityIntent(
                        this,
                        requestId,
                        connectedHardwareWallet,
                        toAddress,
                        value,
                        data,
                        nonce
                );

        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != 0) {
            return;
        }

        webView.onActivityResult(requestCode, resultCode, data);
    }
}

