package de.felixp.fractalsgdx;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.surfaceview.RatioResolutionStrategy;
import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy;

import de.felixp.fractalsgdx.FractalsGdxMain;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
//		config.hideStatusBar = true;
//		config.useImmersiveMode = true;
		config.useGL30 = true;
		//config.resolutionStrategy = new RatioResolutionStrategy(0.5f);
		initialize(new FractalsGdxMain(), config);
	}
}
