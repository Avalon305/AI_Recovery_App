package com.bdl.aisports.activity;

import android.os.Bundle;

import com.bdl.aisports.R;
import com.bdl.aisports.base.BaseActivity;

import org.xutils.view.annotation.ContentView;

@ContentView(R.layout.activity_urgent_stopping)
public class ScramActivity extends BaseActivity {

    //TODO 急停复位
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*Intent intent = new Intent(ScramActivity.this,scramService.class);
        startService(intent);*/
    }

}
