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
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.VisUI;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import de.felixp.fractalsgdx.remoteclient.Client;
import de.felixp.fractalsgdx.ui.MainStage;

public class FractalsGdxMain extends ApplicationAdapter {

    public final static String UI_PREFS_NAME = "de.felixp.fractalsgdx.FractalsGdxMain.ui";
    public final static String UI_PREFS_SCALE = "uiScale";
	private static final String UI_PREFS_WIDTH = "width";
	private static final String UI_PREFS_HEIGHT = "height";
	private static final String UI_PREFS_VSYNC = "vsync";

	static boolean windowed = true;
    static boolean uiScaleWorkaround = false;
    static int forceWidth = -1;
    static int forceHeight = -1;

	SpriteBatch batch;
//	Texture palette;

//	public static int iterations = 500;

	public static Stage stage;

	public static Client client;

//	public static Map<Integer, Map<Integer,Texture>> textures = new HashMap<>();
//	List<Texture> textureList = new ArrayList<>();

	Viewport viewport;

	public static Map<Integer, Map<Integer,Pixmap>> newPixmaps = new HashMap<>();

	private int uiScale = 1;

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

		setScreenMode(FractalsGdxMain.windowed, width, height);
//		Gdx.graphics.setVSync(vsync);

		batch = new SpriteBatch();

		viewport = new ScreenViewport();
//		((ScreenViewport)viewport).setUnitsPerPixel(0.5f);
		stage = new MainStage(viewport, batch);
		((MainStage) stage).create();

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

		if (Gdx.input.isKeyJustPressed(Input.Keys.F11)){
			boolean isFullScreen = Gdx.graphics.isFullscreen();
            FractalsGdxMain.windowed = !FractalsGdxMain.windowed;
            FractalsGdxMain.uiScaleWorkaround = false;
            setScreenMode(FractalsGdxMain.windowed, 1280, 720);
		}


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

	protected void setScreenMode(Boolean windowed, int width, int height) {
		Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
		if (windowed)
			System.out.println(Gdx.graphics.setWindowedMode(width, height));
		else
			Gdx.graphics.setFullscreenMode(currentMode);
	}

	public int getUiScale() {
		return uiScale;
	}

	//	private boolean handleInput() {
//		boolean refresh = false;
//
//		float deltaTime = Gdx.graphics.getDeltaTime();
//		double absFactor = deltaTime * uiScale;
//
//		if (Gdx.input.isKeyPressed(Keys.C)){
//			colorShift += -deltaTime*0.5;
//		}
//
//		if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
//			refresh = true;
//		}
//
//		//uiScale *= 1 + scalingFactor * deltaTime;
//
//		if (Gdx.input.isKeyPressed(Keys.LEFT)) {
//			xPos -= absFactor * FractalsInputProcessor.speedMultiplier_pan;
//			refresh = true;
//		}
//		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
//			xPos += absFactor * FractalsInputProcessor.speedMultiplier_pan;
//			refresh = true;
//		}
//		if (Gdx.input.isKeyPressed(Keys.DOWN)) {
//			yPos -= FractalsInputProcessor.yMultiplier * absFactor * FractalsInputProcessor.speedMultiplier_pan;
//			refresh = true;
//		}
//		if (Gdx.input.isKeyPressed(Keys.UP)) {
//			yPos += FractalsInputProcessor.yMultiplier * absFactor * FractalsInputProcessor.speedMultiplier_pan;
//			refresh = true;
//		}
//
//		if (Gdx.input.isKeyPressed(Keys.PLUS) && !Gdx.input.isKeyPressed(Keys.MINUS)){
//			FractalsInputProcessor.iterationsStep = FractalsInputProcessor.iterationsChangeSpeed;
//			refresh = true;
//		} else if (Gdx.input.isKeyPressed(Keys.MINUS)){
//			FractalsInputProcessor.iterationsStep = -FractalsInputProcessor.iterationsChangeSpeed;
//			refresh = true;
//		} else {
//			FractalsInputProcessor.lastIterationStepTime = System.currentTimeMillis();
//			FractalsInputProcessor.iterationsStep = 0;
//		}
//
//		if (Gdx.input.isKeyPressed(Keys.I) && !Gdx.input.isKeyPressed(Keys.K)){
//			biasReal += biasChangeSpeed*absFactor;
//			refresh = true;
//		} else if (Gdx.input.isKeyPressed(Keys.K)){
//			biasReal -= biasChangeSpeed*absFactor;
//			refresh = true;
//		}
//		if (Gdx.input.isKeyPressed(Keys.O) && !Gdx.input.isKeyPressed(Keys.L)){
//			biasImag += biasChangeSpeed*absFactor;
//			refresh = true;
//		} else if (Gdx.input.isKeyPressed(Keys.L)){
//			biasImag -= biasChangeSpeed*absFactor;
//			refresh = true;
//		}
//
//		if (Gdx.input.isKeyPressed(Keys.J)){
//			juliaset = !juliaset;
//		}
//		if (Gdx.input.isKeyPressed(Keys.B)){
//			burningship = !burningship;
//		}
//
//		if (velocityX != 0) {
//			xPos += velocityX * absFactor;
//			refresh = true;
//		}
//		if (velocityY != 0) {
//			yPos += FractalsInputProcessor.yMultiplier * velocityY * absFactor;
//			refresh = true;
//		}
//
//		double decayFactor = (velocityDecay*deltaTime);
//		if (decayFactor > 1)
//			decayFactor = 1;
//		double length = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
//		if (length > 0) {
//			double newLength = length * (1 - decayFactor) - velocityDecayFixed * deltaTime;
//			if (newLength < 0) {
//				velocityX = 0;
//				velocityY = 0;
//			} else {
//				double factor = newLength / length;
//				velocityX *= factor;
//				velocityY *= factor;
//			}
//		}
//		if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
//			velocityX = 0;
//			velocityY = 0;
//		}

//		if (velocityX > 0) {
//			velocityX -= Math.min(decayFactor, velocityX);
//		} else if (velocityX < 0) {
//			velocityX += Math.min(decayFactor, -velocityX);
//		}
//		if (velocityY > 0) {
//			velocityY -= Math.min(decayFactor, velocityY);
//		} else if (velocityY < 0) {
//			velocityY += Math.min(decayFactor, -velocityY);
//		}

//		if (FractalsInputProcessor.iterationsStep != 0){
//			long t = System.currentTimeMillis();
//			double deltaT = (t - FractalsInputProcessor.lastIterationStepTime)/1000.0;
//			int change = (int)(FractalsInputProcessor.iterationsStep * deltaT * (iterations+1000));
//			if (change < 0 && iterations < -change)
//				change = -iterations;
//			if (change != 0) {
//				FractalsInputProcessor.lastIterationStepTime = t;
//				iterations += change;
//				refresh = true;
//				System.out.println("set iterations to " + iterations);
//			}
//		}
//		return refresh;
//	}

//	private void setShaderUniforms() {
//		float currentWidth = Gdx.graphics.getWidth();
//		float currentHeight = Gdx.graphics.getHeight();
//		shader.setUniformMatrix("u_projTrans", matrix);
//		shader.setUniformf("ratio", currentWidth/(float)currentHeight);
//		shader.setUniformf("resolution", width, height);
//
//		shader.setUniformf("colorShift", colorShift);
//
////		long t = System.currentTimeMillis();
////		if (t-lastIncrease > 1) {
////			lastIncrease = t;
////			iterations++;
////		}
//		shader.setUniformi("iterations", iterations);
//		shader.setUniformf("uiScale", (float)uiScale);
//		float xVariation = (float)((Math.random()-0.5)*(uiScale/width));
//		float yVariation = (float)((Math.random()-0.5)*(uiScale/width));
//		shader.setUniformf("center", (float) xPos + xVariation, (float) yPos + yVariation);
//		shader.setUniformf("biasReal", biasReal);
//		shader.setUniformf("biasImag", biasImag);
//		shader.setUniformf("samples", currentRefreshes+1);
//		shader.setUniformf("flip", currentRefreshes%2 == 1 ? -1 : 1);
//		//shader.setUniformi("u_texture", 0);
//		//palette.bind(1);
//		shader.setUniformi("palette", 0);
//		shader.setUniformf("burningship", burningship ? 1f : 0f);
//		shader.setUniformf("juliaset", juliaset ? 1f : 0f);
//	}
//
//	public void compileShaders() {
//		ShaderProgram.pedantic = false;
//		String vertexPassthrough = "PassthroughVertex.glsl";
//		shader = compileShader(vertexPassthrough, shader1);
////		sobelShader = compileShader(vertexPassthrough, shader2);
//		passthroughShader = compileShader(vertexPassthrough, "PassthroughFragment.glsl");
//
//		width = Gdx.graphics.getWidth();
//		height = Gdx.graphics.getHeight();
//	}

//	public ShaderProgram compileShader(String vertexPath, String fragmentPath){
//		ShaderProgram shader = new ShaderProgram(Gdx.files.internal(vertexPath),
//				Gdx.files.internal(fragmentPath));
//		if (!shader.isCompiled()) {
//			throw new IllegalStateException("Error compiling shaders ("+vertexPath+", "+fragmentPath+"): "+shader.getLog());
//		}
//		return shader;
//	}

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
