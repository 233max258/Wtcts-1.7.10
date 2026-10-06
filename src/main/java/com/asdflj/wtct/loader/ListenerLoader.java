package com.asdflj.wtct.loader;

import com.asdflj.wtct.client.ClientHelper;

public class ListenerLoader implements Runnable {

    @Override
    public void run() {
        ClientHelper.register();
    }
}
