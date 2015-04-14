package com.example.alonmuroch.bitcoinjwrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
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
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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

    // Preferences
    static String EXTERNAL_CHAIN_KEY_PREFERENCE = "EXTERNAL_CHAIN_KEY_PREFERENCE";
    static String EXTERNAL_CHAIN_KEY_CHAIN_PREFERENCE = "EXTERNAL_CHAIN_KEY_CHAIN_PREFERENCE";

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

    public SPVFacade resotreFromPassphrase(String passphrase) {
        return this.setPassphrase(passphrase, EARLIASET_APP_RELEASE_UNIX);
    }

    public SPVFacade newWalletFromPassphrase(String passphrase) {
        return this.setPassphrase(passphrase, (System.currentTimeMillis() / 100L));
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
                SPVFacade.this.saveExternalChainToDefaults(bitcoin.wallet());

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

        File f =  new File(s + "/wallet/");
        if(!f.exists())
            f.mkdir();
        return f;
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

    private SPVFacade setPassphrase(String passphrase, long creationTime) {
        byte[] seed = CounterpartyMnemonic.decodePassphrase(passphrase);
        List<String> words = Arrays.asList(passphrase.split(" "));
        seedToRestore = new DeterministicSeed(seed, words, creationTime);
        return this;
    }

    private void saveExternalChainToDefaults(Wallet wallet) {
        byte[] seed = wallet.getKeyChainSeed().getSeedBytes();
        HDKeyDerivation HDKey = null;
        // M
        DeterministicKey masterkey = HDKey.createMasterPrivateKey(seed);

        // 0H
        ChildNumber purposeIndex = new ChildNumber(KeyChain.KeyPurpose.RECEIVE_FUNDS.ordinal(), true);
        DeterministicKey purpose = HDKey.deriveChildKey(masterkey, purposeIndex);

        // external chain
        ChildNumber externalIdx = new ChildNumber(0);
        DeterministicKey externalChain = HDKey.deriveChildKey(purpose, externalIdx);

        SharedPreferences sharedPref = context.getSharedPreferences("GetGems", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(EXTERNAL_CHAIN_KEY_PREFERENCE, Hex.toHexString(externalChain.getPubKey()));
        editor.putString(EXTERNAL_CHAIN_KEY_CHAIN_PREFERENCE, Hex.toHexString(externalChain.getChainCode()));
        editor.commit();
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
