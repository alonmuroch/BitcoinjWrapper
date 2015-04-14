package com.example.alonmuroch.bitcoinjwrapper;

import com.subgraph.orchid.encoders.Hex;

/**
 * Created by alonmuroch on 4/14/15.
 */
public class CounterpartyMnemonic {
    public static byte[] decodePassphrase(String passphrase) {
        return Hex.decode("fa614ada0ce42768c16311b0cf7e3da9");
    }

    public static String encodeToPassphrase(byte[] data) {
        return "pull common fright dwell size spoken hero boom shoot mutter bruise state";
    }

    public static String generatePassphrase() {
        return "pull common fright dwell size spoken hero boom shoot mutter bruise state";
    }
}
