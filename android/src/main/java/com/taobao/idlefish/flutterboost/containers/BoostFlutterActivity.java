/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Alibaba Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.taobao.idlefish.flutterboost.containers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.taobao.idlefish.flutterboost.BoostFlutterEngine;
import com.taobao.idlefish.flutterboost.BoostFlutterView;
import com.taobao.idlefish.flutterboost.FlutterBoostPlugin;
import com.taobao.idlefish.flutterboost.interfaces.IFlutterViewContainer;
import com.taobao.idlefish.flutterboost.interfaces.IOperateSyncer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.android.FlutterView;
import io.flutter.plugin.platform.PlatformPlugin;

public abstract class BoostFlutterActivity extends Activity implements IFlutterViewContainer {

    protected BoostFlutterEngine mFlutterEngine;
    protected BoostFlutterView mFlutterView;
    protected IOperateSyncer mSyncer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureWindowForTransparency();

        mSyncer = FlutterBoostPlugin.singleton().containerManager().generateSyncer(this);


        mFlutterEngine = createFlutterEngine();
        mFlutterView = createFlutterView(mFlutterEngine);

        setContentView(mFlutterView);

        mSyncer.onCreate();

        configureStatusBarForFullscreenFlutterExperience();
    }

    protected void configureWindowForTransparency() {
        if (isBackgroundTransparent()) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
        }
    }

    protected void configureStatusBarForFullscreenFlutterExperience() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(0x40000000);
            window.getDecorView().setSystemUiVisibility(PlatformPlugin.DEFAULT_SYSTEM_UI);
        }
    }

    protected BoostFlutterEngine createFlutterEngine(){
        return FlutterBoostPlugin.singleton().engineProvider().createEngine(this);
    }

    protected BoostFlutterView createFlutterView(BoostFlutterEngine engine){
        BoostFlutterView.Builder builder = new BoostFlutterView.Builder(this);

        return builder.flutterEngine(engine)
                .renderMode(FlutterView.RenderMode.surface)
                .transparencyMode(isBackgroundTransparent() ?
                        FlutterView.TransparencyMode.transparent :
                        FlutterView.TransparencyMode.opaque)
                .renderingProgressCoverCreator(new BoostFlutterView.RenderingProgressCoverCreator() {
                    @Override
                    public View createRenderingProgressCover(Context context) {
                        return BoostFlutterActivity.this.createRenderingProgressCover();
                    }
                })
                .build();
    }

    protected boolean isBackgroundTransparent(){
        return false;
    }

    protected View createRenderingProgressCover(){
        FrameLayout frameLayout = new FrameLayout(this);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        frameLayout.addView(linearLayout,layoutParams);

        ProgressBar progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        linearLayout.addView(progressBar,params);

        TextView textView = new TextView(this);
        params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        textView.setText("Frame Rendering...");
        linearLayout.addView(textView,params);

        return frameLayout;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSyncer.onAppear();
    }

    @Override
    protected void onPause() {
        mSyncer.onDisappear();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mSyncer.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mSyncer.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mSyncer.onNewIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mSyncer.onActivityResult(requestCode,resultCode,data);

        Map<String,Object> result = null;
        if(data != null) {
            Serializable rlt = data.getSerializableExtra(RESULT_KEY);
            if(rlt instanceof Map) {
                result = (Map<String,Object>)rlt;
            }
        }

        mSyncer.onContainerResult(requestCode,result);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mSyncer.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        mSyncer.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mSyncer.onLowMemory();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        mSyncer.onUserLeaveHint();
    }

    @Override
    public Activity getContextActivity() {
        return this;
    }

    @Override
    public BoostFlutterView getBoostFlutterView() {
        return mFlutterView;
    }

    @Override
    public void finishContainer(Map<String,Object> result) {
        Intent intent = new Intent();
        if (result != null) {
            intent.putExtra(RESULT_KEY, new HashMap<>(result));
        }
        setResult(Activity.RESULT_OK, intent);
    }

    @Override
    public void onContainerShown() {}

    @Override
    public void onContainerHidden() {}
}
