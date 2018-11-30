package de.felixp.fractalsgdx.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import java.util.ArrayList;

import de.felixp.fractalsgdx.FractalsGdxMain;

public class DesktopLauncher {
	public static void main (String[] arg) {
		float scaledown = 2;
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.foregroundFPS = 0;
		config.width = (int)(1920/scaledown);
		config.height = (int)(1080/scaledown);
		config.fullscreen = true;
		new LwjglApplication(new FractalsGdxMain(), config);
	}
}
