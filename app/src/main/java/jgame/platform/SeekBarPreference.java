package jgame.platform;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.preference.DialogPreference;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;


public class SeekBarPreference extends DialogPreference
implements SeekBar.OnSeekBarChangeListener {
	//private static final String androidns="http://schemas.android.com/apk/res/android";

	private SeekBar mSeekBar;
	private TextView mSplashText,mValueText;
	private Context mContext;

	private String mDialogMessage="mDialogMessage", suffix="";
	private int intmax,intstep;
	private double value;
	private double defaultvalue=0; // should not be necessary
	private NumberSetting param;
		//public int decimals;
		//public double lower,upper, step;

	private void calcSeekBarParam() {
		intstep = (int)Math.round(param.step*Math.pow(10,param.decimals));
		intmax = convertToInt(param.upper);
	}

	private int convertToInt(double val) {
		return (int)Math.round((val-param.lower)*Math.pow(10,param.decimals));
	}

	private float convertToFloat(int val) {
		return (float)(param.lower + val/(Math.pow(10,param.decimals)));
	}

	public SeekBarPreference(Context context,NumberSetting param) { 
		super(context,null); 
		mContext = context;
		this.param = param;
		this.mDialogMessage = param.title;
		// XXX suffix not passed, so unused as yet
		setPersistent(true);
	}

	/*public SeekBarPreference(Context context, AttributeSet attrs) { 
		super(context,attrs); 
		mContext = context;

		mDialogMessage = attrs.getAttributeValue(androidns,"dialogMessage");
		mSuffix = attrs.getAttributeValue(androidns,"text");
		mDefault = attrs.getAttributeIntValue(androidns,"defaultValue", 0);
		mMax = attrs.getAttributeIntValue(androidns,"max", 100);

	}*/
	@Override 
	protected View onCreateDialogView() {
		LinearLayout.LayoutParams params;
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6,6,6,6);

		mSplashText = new TextView(mContext);
		if (mDialogMessage != null)
			mSplashText.setText(mDialogMessage);
		layout.addView(mSplashText);

		mValueText = new TextView(mContext);
		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
		mValueText.setTextSize(32);
		params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(mValueText, params);

		mSeekBar = new SeekBar(mContext);
		mSeekBar.setOnSeekBarChangeListener(this);
		layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		if (shouldPersist())
			value = getPersistedFloat((float)defaultvalue);
		calcSeekBarParam();
		mSeekBar.setMax(intmax);
		mSeekBar.setProgress(convertToInt(value));
		return layout;
	}

	@Override 
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		calcSeekBarParam();
		mSeekBar.setMax(intmax);
		mSeekBar.setProgress(convertToInt(value));
	}
	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);
		if (restore) {
			value = shouldPersist() ?
				getPersistedFloat((float)defaultvalue) : defaultvalue;
		} else {
			value = (Float)defaultValue;
		}
	}

	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
		mValueText.setText(convertToFloat(value) + suffix);
		callChangeListener(new Float(this.value));
		this.value = convertToFloat(value);
		if (shouldPersist()) {
			persistFloat((float)this.value);
		}
	}
	public void onStartTrackingTouch(SeekBar seek) {}
	public void onStopTrackingTouch(SeekBar seek) {}

}

