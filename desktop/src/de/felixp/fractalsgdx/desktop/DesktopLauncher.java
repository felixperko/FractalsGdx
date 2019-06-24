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
//		config.width = 1920;
//		config.height = 1080;

		//config.width = 1280;
		//config.height = 720;
		//config.width = 500;
		//config.height = 500;
		//config.fullscreen = false;
		config.setFromDisplayMode(LwjglApplicationConfiguration.getDesktopDisplayMode());
		config.fullscreen = true;

		config.vSyncEnabled = true;
		new LwjglApplication(new FractalsGdxMain(), config);
	}
}
