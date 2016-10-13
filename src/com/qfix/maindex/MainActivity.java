package com.qfix.maindex;

import com.qfix.seconddex.BridgeObjectB;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	
	private Context mContext;
	
	private BridgeObjectA mObjA = new BridgeObjectA();
	private BridgeObjectB mObjB = new BridgeObjectB();
	
	private Button mExitButton;
	private Button mShowMsgButton;
	
	private CheckBox mCheckBoxPatchA;
	private TextView mTextViewPatchA;
	
	private CheckBox mCheckBoxPatchB;
	private TextView mTextViewPatchB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_main);
		
		mExitButton = (Button)findViewById(R.id.buttonExit);
		mExitButton.setOnClickListener(this);
		mShowMsgButton = (Button)findViewById(R.id.buttonShow);
		mShowMsgButton.setOnClickListener(this);
		
		SharedPreferences sp = getSharedPreferences(MainApplication.SP_PATCH, Context.MODE_PRIVATE);
		
		LinearLayout patchALayout = (LinearLayout) findViewById(R.id.patchALayout);
		mCheckBoxPatchA = (CheckBox)patchALayout.findViewById(R.id.selectPatch);
		mCheckBoxPatchA.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Log.d("QFixDemo", "Click checkbox PatchA");
				boolean isCheck = mCheckBoxPatchA.isChecked();
				SharedPreferences sp = mContext.getSharedPreferences(MainApplication.SP_PATCH, Context.MODE_PRIVATE);
				Editor editor = sp.edit();
				editor.putBoolean(MainApplication.SP_KEY_PATCH_A, isCheck);
				editor.commit();
			}
		});
		mCheckBoxPatchA.setChecked(sp.getBoolean(MainApplication.SP_KEY_PATCH_A, false));
		mCheckBoxPatchA.setText(getString(R.string.patchA_title));
		mTextViewPatchA = (TextView)patchALayout.findViewById(R.id.showResult);
		
		LinearLayout patchBLayout = (LinearLayout) findViewById(R.id.patchBLayout);
		mCheckBoxPatchB = (CheckBox)patchBLayout.findViewById(R.id.selectPatch);
		mCheckBoxPatchB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Log.d("QFixDemo", "Click checkbox PatchB");
				boolean isCheck = mCheckBoxPatchB.isChecked();
				SharedPreferences sp = mContext.getSharedPreferences(MainApplication.SP_PATCH, Context.MODE_PRIVATE);
				Editor editor = sp.edit();
				editor.putBoolean(MainApplication.SP_KEY_PATCH_B, isCheck);
				editor.commit();
			}
		});
		mCheckBoxPatchB.setChecked(sp.getBoolean(MainApplication.SP_KEY_PATCH_B, false));
		mCheckBoxPatchB.setText(getString(R.string.patchB_title));
		mTextViewPatchB = (TextView)patchBLayout.findViewById(R.id.showResult);

		Log.d("QFixDemo", "MainActivity onCreate");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View view) {
		int viewId = view.getId();
		switch(viewId) {
			case R.id.buttonExit:
				Log.d("QFixDemo", "Click button Exit!");
				System.exit(0);
				break;
			case R.id.buttonShow:
				Log.d("QFixDemo", "Click button Show!");
				updatePatchTextAndColor(mTextViewPatchA, mObjA.fun());
				updatePatchTextAndColor(mTextViewPatchB, mObjB.fun());
				break;
			default:
				break;
		}
	}
	
	private void updatePatchTextAndColor(TextView view, String info) {
		view.setText(info);
		if (info.contains("未加载")) {
			view.setTextColor(Color.RED);
		} else if (info.contains("已加载")) {
			view.setTextColor(Color.BLUE);
		} else {
			view.setTextColor(Color.BLACK);
		}
	}
}
