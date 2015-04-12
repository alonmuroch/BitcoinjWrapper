package com.example.alonmuroch.bitcoinjwrapper;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DownloadListener;
import org.bitcoinj.core.GetDataMessage;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerEventListener;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletEventListener;

import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;


public class MainActivity extends ActionBarActivity {
    static String TAG = "Main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SPVFacade spv = SPVFacade.sharedInstance();
        spv.setContext(getApplicationContext());
        spv.setDownloadListener(new DownloadListener() {
            @Override
            protected void progress(final double pct, int blocksSoFar, Date date) {
                Log.i(TAG, "Blockchain download: " +  pct + "%");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressBar progressbar = (ProgressBar) findViewById(R.id.progressBar);
                        progressbar.setProgress((int) pct);
                    }
                });

            }

            @Override
            protected void doneDownload() {

            }
        });

        spv.getWallet().addEventListener(new SPVFacade.WalletEventAdapter() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                updateBalance();
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                updateBalance();
            }

            private void updateBalance() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView txv2 = (TextView) findViewById(R.id.balance);
                        txv2.setText("Balance: " + SPVApi.sharedInstance().getBalance().toFriendlyString());
                    }
                });
            }

        });

        spv.addListener(new Service.Listener() {
            @Override
            public void running() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView txv = (TextView) findViewById(R.id.address);
                        txv.setText(SPVApi.sharedInstance().getFirstAddress().toString());

                        TextView txv2 = (TextView) findViewById(R.id.balance);
                        txv2.setText("Balance: " + SPVApi.sharedInstance().getBalance().toFriendlyString());
                    }
                });
            }
        }, MoreExecutors.sameThreadExecutor());
        spv.startAsync();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendExample() throws AddressFormatException, InsufficientMoneyException {
        Address to = new Address(SPVFacade.sharedInstance().getWallet().getParams(), "1F1tAaz5x1HUXrCNLbtMDqcw6o5GNn4xqX");
        Coin amount = Coin.FIFTY_COINS;

        Wallet.SendResult result = SPVApi.sharedInstance().sendToAddress(to, amount);

        result.broadcastComplete.addListener(new Runnable() {
            @Override
            public void run() {
                // whatever you like
            }
        }, MoreExecutors.sameThreadExecutor());
    }
}
