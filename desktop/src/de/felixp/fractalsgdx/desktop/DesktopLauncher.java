package de.felixp.fractalsgdx.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import java.util.ArrayList;

import de.felixp.fractalsgdx.FractalsGdxMain;

public class DesktopLauncher {
	public static void main (String[] arg) {
		float scaledown = 0.5f;
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.foregroundFPS = 0;
		config.width = 3840;
		config.height = 2160;
		config.vSyncEnabled = true;
		config.fullscreen = true;
		new LwjglApplication(new FractalsGdxMain(), config);
	}
}
