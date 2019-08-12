package com.bdl.airecovery.activity;

import android.os.Bundle;

import com.bdl.airecovery.R;
import com.bdl.airecovery.base.BaseActivity;

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
