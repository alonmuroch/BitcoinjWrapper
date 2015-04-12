package com.example.alonmuroch.bitcoinjwrapper;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.DownloadListener;
import org.bitcoinj.core.GetDataMessage;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerEventListener;
import org.bitcoinj.core.Transaction;

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
            protected void progress(double pct, int blocksSoFar, Date date) {
                float p = (float)(pct / 100.0);
                Log.i(TAG, "Blockchain download: " +  p);

                ProgressBar progressbar = (ProgressBar) findViewById(R.id.progressBar);
                progressbar.setProgress((int)p);
            }

            @Override
            protected void doneDownload() {

            }
        });

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
}
