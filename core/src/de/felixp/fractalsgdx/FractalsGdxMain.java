package de.felixp.fractalsgdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.VisUI;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import de.felixp.fractalsgdx.remoteclient.Client;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.system.parameters.ParamConfiguration;

public class FractalsGdxMain extends ApplicationAdapter {

    public final static String UI_PREFS_NAME = "de.felixp.fractalsgdx.FractalsGdxMain.ui";
    public final static String UI_PREFS_SCALE = "uiScale";
	private static final String UI_PREFS_WIDTH = "width";
	private static final String UI_PREFS_HEIGHT = "height";
	private static final String UI_PREFS_VSYNC = "vsync";

	public static boolean windowed = true;
    public static boolean uiScaleWorkaround = false;
	private static int uiScale = 1;

	public static int getUiScale(){
		return uiScale;
	}

    static int forceWidth = -1;
    static int forceHeight = -1;

	SpriteBatch batch;
//	Texture palette;

//	public static int iterations = 500;

	public static Stage stage;
    public static MainStage mainStage;

	public static Client client;

//	public static Map<Integer, Map<Integer,Texture>> textures = new HashMap<>();
//	List<Texture> textureList = new ArrayList<>();

	Viewport viewport;

	public static Map<Integer, Map<Integer,Pixmap>> newPixmaps = new HashMap<>();


	@Override
	public void create () {

		IntBuffer queryArr = BufferUtils.newIntBuffer(16);
		Gdx.gl.glGetIntegerv(GL20.GL_MAX_FRAGMENT_UNIFORM_VECTORS, queryArr);
		System.out.println("GL_MAX_FRAGMENT_UNIFORM_VECTORS: "+queryArr.get(0));
		Gdx.gl.glGetIntegerv(GL20.GL_MAX_VERTEX_UNIFORM_VECTORS, queryArr);
		System.out.println("GL_MAX_VERTEX_UNIFORM_VECTORS: "+queryArr.get(0));
		Gdx.gl.glGetIntegerv(GL20.GL_MAX_VARYING_VECTORS, queryArr);
		System.out.println("GL_MAX_VARYING_VECTORS: "+queryArr.get(0));
		Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, queryArr);
		System.out.println("GL_MAX_TEXTURE_SIZE: "+queryArr.get(0));
		Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, queryArr);
		System.out.println("GL_MAX_TEXTURE_IMAGE_UNITS: "+queryArr.get(0));

        Preferences uiPrefs = Gdx.app.getPreferences(UI_PREFS_NAME);
		uiScale = 1;
        if (uiPrefs.contains(UI_PREFS_SCALE)){
            uiScale = uiPrefs.getInteger(UI_PREFS_SCALE);
        }
        if (uiScale >= 2)
		    VisUI.load(VisUI.SkinScale.X2);
        else
            VisUI.load(VisUI.SkinScale.X1);

//		Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
		boolean vsync = true;

		int width = 1280;
		int height = 720;
		if (uiPrefs.contains(UI_PREFS_WIDTH)){
			width = uiPrefs.getInteger(UI_PREFS_WIDTH);
			if (forceWidth > 0)
				width = forceWidth;
		} else {
			uiPrefs.putInteger(UI_PREFS_WIDTH, width);
		}
		if (uiPrefs.contains(UI_PREFS_HEIGHT)){
			height = uiPrefs.getInteger(UI_PREFS_HEIGHT);
			if (forceHeight > 0)
				height = forceHeight;
		} else {
			uiPrefs.putInteger(UI_PREFS_HEIGHT, height);
		}
		if (uiPrefs.contains(UI_PREFS_VSYNC)){
			vsync = uiPrefs.getBoolean(UI_PREFS_VSYNC);
		} else {
			uiPrefs.putBoolean(UI_PREFS_VSYNC, vsync);
		}
		uiPrefs.flush();

//		Gdx.graphics.setContinuousRendering(false);

//		setScreenMode(FractalsGdxMain.windowed, width, height);
//		Gdx.graphics.setVSync(vsync);

		batch = new SpriteBatch();

		viewport = new ScreenViewport();
		mainStage = new MainStage(viewport, batch);
		stage = mainStage;
		mainStage.create();

		client = new Client(this);
		client.start();


//		InputMultiplexer multiplexer = new InputMultiplexer();
//		multiplexer.addProcessor(new GestureDetector(new FractalsGestureListener()));
//		multiplexer.addProcessor(new FractalsInputProcessor());
//		Gdx.input.setInputProcessor(multiplexer);
		Gdx.input.setInputProcessor(stage);
	}

	public void applyCommandLineArguments(String[] args) {
		for (String arg : args){
			if (arg.contains("=")){
				String[] s = arg.split("=", 2);
				String key = s[0];
				String value = s[1];

				if (key.equalsIgnoreCase("resolution")) {
					String[] s2 = value.split("x");
					forceWidth = Integer.parseInt(s2[0]);
					forceHeight = Integer.parseInt(s2[1]);
				}
			}
		}
	}

	public static void setWindowed(boolean windowed){
	    FractalsGdxMain.windowed = windowed;
	    FractalsGdxMain.uiScaleWorkaround = true;
    }

	@Override
	public void render () {

		Gdx.gl.glClearColor( 0, 0, 0, 1 );
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		try {
			stage.act(Gdx.graphics.getDeltaTime());
			stage.draw();
		} catch (NullPointerException e){
			e.printStackTrace();
			System.exit(0);
		}

		//(hopefully temporary) workaround for windows 10 ui scaling: start in fullscreen, then set windowed if configured to do so
//		if (uiScaleWorkaround){
//			uiScaleWorkaround = false;
//			setScreenMode(windowed, 1280, 720);
//		}
	}

	public static void setScreenMode(Boolean windowed, int width, int height) {
		Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
		if (windowed)
			System.out.println(Gdx.graphics.setWindowedMode(width, height));
		else
			Gdx.graphics.setFullscreenMode(currentMode);
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		((MainStage)stage).resize(width, height);
		viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	@Override
	public void dispose () {
		batch.dispose();
	}
}
