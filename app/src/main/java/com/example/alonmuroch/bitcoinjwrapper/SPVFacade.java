package com.example.alonmuroch.bitcoinjwrapper;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.common.util.concurrent.AbstractService;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadListener;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.GetDataMessage;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerEventListener;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.DeterministicSeed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * Created by alonmuroch on 4/12/15.
 */
public class SPVFacade extends AbstractService {
    // bitcoinj IVars
    private WalletAppKit bitcoin;
    /*
     * If we restore the wallet from a seed, just assign this var the seed. If not keep it null.
     */
    private @Nullable DeterministicSeed seedToRestore;
    private DownloadListener peerEventListener;
    static  boolean TEST_NET = false;

    // other IVars
    Context context;
    static String TAG = "SPVFacade";
    static long EARLIASET_APP_RELEASE_UNIX = 1388534400; // 1.1.14

    public SPVFacade() {

    }

    private static SPVFacade instance;
    public static SPVFacade sharedInstance() {
        if(instance == null) {
            instance = new SPVFacade();
        }

        return instance;
    }

    public SPVFacade setContext(Context context) {
        this.context = context;
        return this;
    }

    public SPVFacade restoreFromSeed(List<String> words) {
        seedToRestore = new DeterministicSeed(words, null,"", EARLIASET_APP_RELEASE_UNIX);
        return this;
    }

    // Abstract Service Impl

    @Override
    protected void doStart() {
        try { initAndStartWalletKit(); }
        catch (IOException e) { e.printStackTrace();  }
    }

    @Override
    protected void doStop() {
        bitcoin.stopAsync();
    }

    public SPVFacade setDownloadListener(DownloadListener listener) {
        this.peerEventListener = listener;
        return this;
    }

    // Wallet kit methods
    public Wallet getWallet() {
        return bitcoin.wallet();
    }

    private void initAndStartWalletKit() throws IOException {
        NetworkParameters np = null;
        InputStream inCheckpint = null;
        if(TEST_NET == false) {
            np = MainNetParams.get();
            inCheckpint = context.getAssets().open("checkpoints");
        }
        else  {
            np = TestNet3Params.get();
            inCheckpint = context.getAssets().open("checkpoints.testnet");
        }

        if(seedToRestore != null) {
            resotreWallet(seedToRestore, np);
            Log.i(TAG, "Restoring wallet from seed");
        }

        bitcoin = new WalletAppKit(np, getApplicationDirectory(), "GetGems") {
            @Override
            protected void onSetupCompleted() {
                bitcoin.peerGroup().setMaxConnections(11);
                bitcoin.wallet().setKeychainLookaheadSize(0);
                bitcoin.wallet().allowSpendingUnconfirmedTransactions();
                bitcoin.peerGroup().setBloomFilterFalsePositiveRate(0.00001);

                // dump wallet state
                Log.i(TAG, bitcoin.wallet().toString());

                SPVFacade.this.notifyStarted();
            }
        };

        bitcoin.setCheckpoints(inCheckpint);

        if(peerEventListener != null)
            bitcoin.setDownloadListener(peerEventListener);

        bitcoin.setAutoSave(true);
        bitcoin.setAutoStop(true);
        bitcoin.setBlockingStartup(false)
                .setUserAgent("GetGems", "1.0");
        bitcoin.startAsync();
    }

    private File getApplicationDirectory() {
        PackageManager m = context.getPackageManager();
        String s = context.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Error Package name not found ", e);
            return null;
        }

        return new File(s + "/GetGemsWallet/");
    }

    private void deleteFolder(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }
        }
    }

    private void resotreWallet (DeterministicSeed seed, NetworkParameters params) throws IOException{
        deleteFolder(getApplicationDirectory());

        String filePath = getApplicationDirectory().getAbsolutePath() + "/GetGems.wallet";
        File f = new File(filePath);

        // set wallet
        Wallet wallet = Wallet.fromSeed(params, seed);
        wallet.setKeychainLookaheadSize(0);
        wallet.saveToFile(f);
        wallet= null;
    }

    public static class WalletEventAdapter implements WalletEventListener {

        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {

        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {

        }

        @Override
        public void onReorganize(Wallet wallet) {

        }

        @Override
        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {

        }

        @Override
        public void onWalletChanged(Wallet wallet) {

        }

        @Override
        public void onScriptsAdded(Wallet wallet, List<Script> scripts) {

        }

        @Override
        public void onKeysAdded(List<ECKey> keys) {

        }
    }
}
