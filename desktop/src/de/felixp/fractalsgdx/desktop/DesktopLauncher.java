package de.felixp.fractalsgdx.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
//import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
//import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
//import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import de.felixp.fractalsgdx.FractalsGdxMain;

public class DesktopLauncher {

	private static final String SETTING_OVERRIDE_PREFERENCES = "overridePreferences";
	private static final String SETTING_FULLSCREEN = "fullscreen";
	private static final String SETTING_FULLSCREEN_WIDTH = "fullscreenWidth";
	private static final String SETTING_FULLSCREEN_HEIGHT = "fullscreenHeight";
	private static final String SETTING_WINDOWED_WIDTH = "windowedWidth";
	private static final String SETTING_WINDOWED_HEIGHT = "windowedHeight";
	private static final String SETTING_VSYNC = "vsync";
	private static final String SETTING_SHAPE_MSAA = "MSAA";
	private static final String SETTING_FOREGROUND_FPS = "foregroundFPS";
	private static final String SETTING_BACKGROUND_FPS = "backgroundFPS";

	private static final String SETTINGVALUE_DEFAULT = "default";
	private static final String SETTINGVALUE_REFRESHRATE = "refreshrate";
	private static final String SETTINGVALUE_UNLIMITED = "unlimited";

	public static void main (String[] arg) {

		Map<String, String> settings = readSettingsFile();

		FractalsGdxMain main = new FractalsGdxMain();
		main.applyCommandLineArguments(arg);

		boolean fileDropTest = false;

		if (fileDropTest) {
//			Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
//			config.useOpenGL3(true, 3, 2);
//
//			config.setWindowListener(new Lwjgl3WindowAdapter() {
//				@Override
//				public void filesDropped(String[] files) {
//					System.out.println("file(s) dropped " + files[0]);
//				}
//			});
//
//			new Lwjgl3Application(main, config);
		}
		else  {
			LwjglApplicationConfiguration config = initConfig(settings);
			parseCommandLineArguments(arg, config);
			new LwjglApplication(main, config);
		}
	}

	public static LwjglApplicationConfiguration initConfig(Map<String, String> settings) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.useGL30 = true;

		Graphics.DisplayMode defaultDisplayMode = LwjglApplicationConfiguration.getDesktopDisplayMode();
		int refreshRate = defaultDisplayMode.refreshRate;
		String foregroundFPS = settings.getOrDefault(SETTING_FOREGROUND_FPS, refreshRate+"");
		String backgroundFPS = settings.getOrDefault(SETTING_BACKGROUND_FPS, refreshRate+"");
		switch (foregroundFPS){
			case SETTINGVALUE_REFRESHRATE:
				foregroundFPS = refreshRate+"";
				break;
			case SETTINGVALUE_UNLIMITED:
				foregroundFPS = "-1";
				break;
			default:
				break;
		}
		switch (backgroundFPS){
			case SETTINGVALUE_REFRESHRATE:
				backgroundFPS = refreshRate+"";
				break;
			case SETTINGVALUE_UNLIMITED:
				backgroundFPS = "-1";
				break;
			default:
				break;
		}

		config.foregroundFPS = Integer.parseInt(foregroundFPS);
		config.backgroundFPS = Integer.parseInt(backgroundFPS);

		if (settings.containsKey(SETTING_FULLSCREEN))
			config.fullscreen = Boolean.parseBoolean(settings.get(SETTING_FULLSCREEN));
		else
			config.fullscreen = false;

		if (config.fullscreen){
			if (settings.containsKey(SETTING_FULLSCREEN_WIDTH) && settings.containsKey(SETTING_FULLSCREEN_HEIGHT)){
				String fullscreenWidth = settings.get(SETTING_FULLSCREEN_WIDTH);
				String fullscreenHeight = settings.get(SETTING_FULLSCREEN_HEIGHT);
				boolean defaultWidth = fullscreenWidth.equalsIgnoreCase(SETTINGVALUE_DEFAULT);
				boolean defaultHeight = fullscreenHeight.equalsIgnoreCase(SETTINGVALUE_DEFAULT);
				config.width = Integer.parseInt(defaultWidth ? defaultDisplayMode.width+"" : fullscreenWidth);
				config.height = Integer.parseInt(defaultHeight ? defaultDisplayMode.height+"" : fullscreenHeight);
			}
			if (config.width <= 0 || config.height <= 0){
				config.setFromDisplayMode(defaultDisplayMode);
			}
		} else {
			if (settings.containsKey(SETTING_WINDOWED_WIDTH) && settings.containsKey(SETTING_WINDOWED_HEIGHT)){
				String windowedWidth = settings.getOrDefault(SETTING_WINDOWED_WIDTH, "-1");
				String windowedHeight = settings.getOrDefault(SETTING_WINDOWED_HEIGHT, "-1");
				boolean defaultWidth = windowedWidth.equalsIgnoreCase(SETTINGVALUE_DEFAULT);
				boolean defaultHeight = windowedHeight.equalsIgnoreCase(SETTINGVALUE_DEFAULT);
				config.width = defaultWidth ? defaultDisplayMode.width : Integer.parseInt(windowedWidth);
				config.height = defaultHeight ? defaultDisplayMode.height : Integer.parseInt(windowedHeight);
			} else {
				config.width = -1;
				config.height = -1;
			}
			if (config.width <= 0 || config.height <= 0){
				config.width = 1280;
				config.height = 720;
			}
		}

		config.samples = Integer.parseInt(settings.getOrDefault(SETTING_SHAPE_MSAA, "8"));
		config.vSyncEnabled = Boolean.parseBoolean(settings.getOrDefault(SETTING_VSYNC, "true"));
		return config;
	}

	private static int parseSettingInt(Map<String, String> settings, String settingName, int defaultValue, Map<String, Integer> mapSpecialValues){
		try {
			String val = settings.get(settingName);
			if (val == null) //value not present in settings map -> return default
				return defaultValue;
			if (mapSpecialValues.containsKey(val)) //special value -> map and return
				return mapSpecialValues.get(val);
			return Integer.parseInt(val); //parse as int -> return result
		} catch (NumberFormatException e){
			return defaultValue; //parsing failed -> return default
		}
	}

	private static boolean parseSettingBoolean(Map<String, String> settings, String settingName, boolean defaultValue, Map<String, Boolean> mapSpecialValues){
		try {
			String val = settings.get(settingName);
			if (val == null) //value not present in settings map -> return default
				return defaultValue;
			if (mapSpecialValues.containsKey(val)) //special value -> map and return
				return mapSpecialValues.get(val);
			return Boolean.parseBoolean(val); //parse as int -> return result
		} catch (NumberFormatException e){
			return defaultValue; //parsing failed -> return default
		}
	}

	private static Map<String, String> readSettingsFile() {
		Map<String, String> settings = new HashMap<>();
		try {
			File file = new File("");
			System.out.println("[DesktopLauncher] started from: "+file.getAbsolutePath());
			FileReader reader = new FileReader("settings.ini");
			Scanner sc = new Scanner(reader);
			while (sc.hasNextLine()){
				String line = sc.nextLine();
				String[] s  = line.split("=");
				if (s.length != 2)
					continue;
				String keyPart = s[0].trim();
				if (!keyPart.startsWith("//") && !keyPart.startsWith("#") && !keyPart.startsWith(";"))
					settings.put(keyPart, s[1].trim());
			}
			System.out.println("[DesktopLauncher] Read settings.ini with "+settings.size()+" entries.");
		} catch (FileNotFoundException e) {
			System.out.println("[DesktopLauncher] No settings.ini found at start location. Use default settings.");
		}
		return settings;
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
