package com.example.alonmuroch.bitcoinjwrapper;

import android.support.annotation.Nullable;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.KeyChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.spongycastle.util.encoders.Hex;

/**
 * Created by alonmuroch on 4/12/15.
 */
public class SPVApi {

    static SPVApi instance;
    public static  SPVApi sharedInstance() {
        if(instance == null) {
            instance = new SPVApi();
        }

        return instance;
    }

    public Coin getBalance() {
        Wallet w = SPVFacade.sharedInstance().getWallet();
        return w.getBalance();
    }

    public Address getFirstAddress() {
        return getAddress(0);
    }

    public Wallet.SendResult sendToAddress(Address address, Coin amount) throws InsufficientMoneyException {
        return send(address, amount);
    }

    public List<String> getPassphrase(@Nullable String password) {
        Wallet w = SPVFacade.sharedInstance().getWallet();

        boolean encryptWalletAfterFetchingThePassphrase = false;
        if(this.isWalletEncrypted()) {
            if(password == null)
                throw new IllegalArgumentException("Wallet is encrypted but you didn't provide a password");

            encryptWalletAfterFetchingThePassphrase = true;

            this.decryptWallet(password);
        }

        byte[] seed = w.getKeyChainSeed().getSeedBytes();
        List<String> ret = Arrays.asList(CounterpartyMnemonic.encodeToPassphrase(seed).split(" "));

        if(encryptWalletAfterFetchingThePassphrase)
            this.encryptWallet(password);

        return ret;
    }

    public void encryptWallet(String password) {
        Wallet w = SPVFacade.sharedInstance().getWallet();
        w.encrypt(password);
    }

    public boolean isWalletEncrypted() {
        Wallet w = SPVFacade.sharedInstance().getWallet();
        return w.isEncrypted();
    }

    // private

    private Address getAddress(int idx) {
        Wallet w = SPVFacade.sharedInstance().getWallet();
        byte[] seed = w.getKeyChainSeed().getSeedBytes();

        HDKeyDerivation HDKey = null;

        // M
        DeterministicKey masterkey = HDKey.createMasterPrivateKey(seed);

        // 0H
        ChildNumber purposeIndex = new ChildNumber(KeyChain.KeyPurpose.RECEIVE_FUNDS.ordinal(), true);
        DeterministicKey purpose = HDKey.deriveChildKey(masterkey, purposeIndex);

        // external chain
        ChildNumber externalIdx = new ChildNumber(0);
        DeterministicKey externalChain = HDKey.deriveChildKey(purpose, externalIdx);

        // key
        ChildNumber addressIdx = new ChildNumber(idx);
        DeterministicKey key =  HDKey.deriveChildKey(externalChain, addressIdx);

        Address address = key.toAddress(w.getParams());
        return address;
    }

    private Wallet.SendResult send(Address address, Coin amount) throws InsufficientMoneyException {
        Wallet w = SPVFacade.sharedInstance().getWallet();
        Wallet.SendRequest sr =  Wallet.SendRequest.to(address, amount);
        return w.sendCoins(sr);
    }

    private void decryptWallet(String password) {
        Wallet w = SPVFacade.sharedInstance().getWallet();
        w.decrypt(password);
    }

}
