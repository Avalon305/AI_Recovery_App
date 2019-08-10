package com.bdl.aisports.base;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;


import org.xutils.x;

/**
 * Created by wyouflf on 15/11/4.
 */
public class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        x.view().inject(this);
    }
}
