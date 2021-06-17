package de.felixp.fractalsgdx.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import java.util.ArrayList;

import de.felixp.fractalsgdx.FractalsGdxMain;

public class DesktopLauncher {
	public static void main (String[] arg) {
		float scaledown = 0.5f;
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.foregroundFPS = 120;

		config.useGL30 = true;

//		config.width = 1920*4;
//		config.height = 1080*4;

//		config.width = 5760;
//		config.height = 3240;

//		config.width = 1280;
//		config.height = 720;

		config.width = 1920;
		config.height = 1080;

//		config.width = 500;
//		config.height = 500;

		config.fullscreen = false;

		config.samples = 4;

		//fullscreen -> true
		config.setFromDisplayMode(LwjglApplicationConfiguration.getDesktopDisplayMode());

//		config.vSyncEnabled = true;
//		config.vSyncEnabled = false;

		parseCommandLineArguments(arg, config);

		FractalsGdxMain main = new FractalsGdxMain();
		main.applyCommandLineArguments(arg);
		new LwjglApplication(main, config);
	}

	private static void parseCommandLineArguments(String[] args, LwjglApplicationConfiguration config) {
		for (String arg : args){
			if (arg.contains("=")){
				String[] s = arg.split("=", 2);
				String key = s[0];
				String value = s[1];

				if (key.equalsIgnoreCase("msaa"))
					config.samples = Integer.parseInt(value);
				if (key.equalsIgnoreCase("undecorated"))
					config.undecorated = Boolean.parseBoolean(value);
				if (key.equalsIgnoreCase("fps"))
					config.foregroundFPS = Integer.parseInt(value);
			}
		}
	}
}
