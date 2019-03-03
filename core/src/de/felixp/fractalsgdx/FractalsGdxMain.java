package de.felixp.fractalsgdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Matrix3;
import com.kotcrab.vis.ui.VisUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.client.Client;
import de.felixperko.fractals.network.ClientConfiguration;

public class FractalsGdxMain extends ApplicationAdapter {

	SpriteBatch batch;
	Texture img;
	Texture palette;
	ShaderProgram shader;
//	ShaderProgram sobelShader;
	ShaderProgram passthroughShader;
	Matrix3 matrix = new Matrix3(new float[] {1,0,0, 0,1,0, 0,0,1, 0,0,0});

	public static boolean juliaset = false;
	public static boolean burningship = false;

	final static String shader1 = "CalcMandelbrotFragment.glsl";
	final static String shader2 = "SobelDecodeFragment.glsl";
//	final static String shader2 = "PassthroughFragment.glsl";

	public static int iterations = 500;

	long lastIncrease = System.currentTimeMillis();

	public static double xPos = 0;
	public static double yPos = 0;

	public static double velocityX = 0;
	public static double velocityY = 0;

	public static double velocityDecay = 3; //decay factor per second
	public static double velocityDecayFixed = 0.1;
	public static double biasChangeSpeed = 0.1;

	public static double scale = 3;
	public static double scalingFactor = 0;

	public static boolean invertY = false;

	public static boolean postprocessing = true;

	public static float biasReal = 0;
	public static float biasImag = 0;

	public static float colorShift = 0;

	public static boolean forceRefresh = false;

	FrameBuffer fbo;
	FrameBuffer fbo2;

	int width;
	int height;

	int currentRefreshes = 0;



	public static Client client;

	public static Map<Integer, Map<Integer,Texture>> textures = new HashMap<>();
	List<Texture> textureList = new ArrayList<>();

	public static Map<Integer, Map<Integer,Pixmap>> newPixmaps = new HashMap<>();

	@Override
	public void create () {

		client = new Client(this);
		client.start();


		VisUI.load();
		batch = new SpriteBatch();

//		shader = compileShader("passthroughVertexCpu.glsl", "SobelDecodeFragmentCpu.glsl");
		ShaderProgram.pedantic = false;
		shader = compileShader("passthroughVertexCpu.glsl", "SobelDecodeFragmentCpu.glsl");

		img = new Texture("badlogic.jpg");

//		setupShaders();
//		batch.setShader(shader);

		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(new GestureDetector(new FractalsGestureListener()));
		multiplexer.addProcessor(new FractalsInputProcessor());
		Gdx.input.setInputProcessor(multiplexer);

		fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
	}

	public void drawPixmap(Integer startX, Integer startY, Pixmap pixmap){
		synchronized (newPixmaps){
			Map<Integer, Pixmap> pixmapsYMap = getPixmapsYMap(startX);
			pixmapsYMap.put(startY, pixmap);
		}
//			texture = new Texture(pixmap);
//			textureYMap.put(startY, texture)
	}

	private Map<Integer, Texture> getTextureYMap(Integer x){
		Map<Integer, Texture> map = textures.get(x);
		if (map == null) {
			map = new HashMap<>();
			textures.put(x, map);
		}
		return map;
	}

	private Map<Integer, Pixmap> getPixmapsYMap(Integer x){
		Map<Integer, Pixmap> map = newPixmaps.get(x);
		if (map == null) {
			map = new HashMap<>();
			newPixmaps.put(x, map);
		}
		return map;
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor( 0, 0, 0, 1 );
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		synchronized (newPixmaps){
			for (Map.Entry<Integer, Map<Integer, Pixmap>> e : newPixmaps.entrySet()){
				Map<Integer, Texture> textureYMap = getTextureYMap(e.getKey());
				for (Map.Entry<Integer,Pixmap> e2 : e.getValue().entrySet()){
					Texture texture = textureYMap.get(e2.getKey());
					if (texture == null) {
						texture = new Texture(e2.getValue());
						textureYMap.put(e2.getKey(), texture);
						textureList.add(texture);
					}
					else {
						texture.draw(e2.getValue(), 0, 0);
					}
					e2.getValue().dispose();
				}
			}
			newPixmaps.clear();
		}


		fbo.begin();

		Gdx.gl.glClearColor( 0, 0, 0, 1 );
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();
		batch.setShader(null);

		for (Map.Entry<Integer, Map<Integer, Texture>> e : textures.entrySet()){
			for (Map.Entry<Integer,Texture> e2 : e.getValue().entrySet())
				batch.draw(e2.getValue(), e.getKey()+(float)xPos, -e2.getKey()-(float)yPos);
		}

		batch.end();
		fbo.end();

		shader.begin();
		batch.begin();

		batch.setShader(shader);

		shader.setUniformMatrix("u_projTrans", matrix);
		shader.setUniformf("colorShift", 0);
		shader.setUniformf("resolution", 1920f, 1080f);

		Texture texture = fbo.getColorBufferTexture();
		TextureRegion textureRegion = new TextureRegion(texture);
		textureRegion.flip(false, true);
		batch.draw(textureRegion, 0, 0);
		batch.end();

		shader.end();
	}

	private boolean handleInput() {
		boolean refresh = false;

		float deltaTime = Gdx.graphics.getDeltaTime();
		double absFactor = deltaTime * scale;

		if (Gdx.input.isKeyPressed(Keys.C)){
			colorShift += -deltaTime*0.5;
		}

		if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
			refresh = true;
		}

		//scale *= 1 + scalingFactor * deltaTime;

		if (Gdx.input.isKeyPressed(Keys.LEFT)) {
			xPos -= absFactor * FractalsInputProcessor.speedMultiplier_pan;
			refresh = true;
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			xPos += absFactor * FractalsInputProcessor.speedMultiplier_pan;
			refresh = true;
		}
		if (Gdx.input.isKeyPressed(Keys.DOWN)) {
			yPos -= FractalsInputProcessor.yMultiplier * absFactor * FractalsInputProcessor.speedMultiplier_pan;
			refresh = true;
		}
		if (Gdx.input.isKeyPressed(Keys.UP)) {
			yPos += FractalsInputProcessor.yMultiplier * absFactor * FractalsInputProcessor.speedMultiplier_pan;
			refresh = true;
		}

		if (Gdx.input.isKeyPressed(Keys.PLUS) && !Gdx.input.isKeyPressed(Keys.MINUS)){
			FractalsInputProcessor.iterationsStep = FractalsInputProcessor.iterationsChangeSpeed;
			refresh = true;
		} else if (Gdx.input.isKeyPressed(Keys.MINUS)){
			FractalsInputProcessor.iterationsStep = -FractalsInputProcessor.iterationsChangeSpeed;
			refresh = true;
		} else {
			FractalsInputProcessor.lastIterationStepTime = System.currentTimeMillis();
			FractalsInputProcessor.iterationsStep = 0;
		}

		if (Gdx.input.isKeyPressed(Keys.I) && !Gdx.input.isKeyPressed(Keys.K)){
			biasReal += biasChangeSpeed*absFactor;
			refresh = true;
		} else if (Gdx.input.isKeyPressed(Keys.K)){
			biasReal -= biasChangeSpeed*absFactor;
			refresh = true;
		}
		if (Gdx.input.isKeyPressed(Keys.O) && !Gdx.input.isKeyPressed(Keys.L)){
			biasImag += biasChangeSpeed*absFactor;
			refresh = true;
		} else if (Gdx.input.isKeyPressed(Keys.L)){
			biasImag -= biasChangeSpeed*absFactor;
			refresh = true;
		}

		if (Gdx.input.isKeyPressed(Keys.J)){
			juliaset = !juliaset;
		}
		if (Gdx.input.isKeyPressed(Keys.B)){
			burningship = !burningship;
		}

		if (velocityX != 0) {
			xPos += velocityX * absFactor;
			refresh = true;
		}
		if (velocityY != 0) {
			yPos += FractalsInputProcessor.yMultiplier * velocityY * absFactor;
			refresh = true;
		}

		double decayFactor = (velocityDecay*deltaTime);
		if (decayFactor > 1)
			decayFactor = 1;
		double length = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
		if (length > 0) {
			double newLength = length * (1 - decayFactor) - velocityDecayFixed * deltaTime;
			if (newLength < 0) {
				velocityX = 0;
				velocityY = 0;
			} else {
				double factor = newLength / length;
				velocityX *= factor;
				velocityY *= factor;
			}
		}
		if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
			velocityX = 0;
			velocityY = 0;
		}

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

		if (FractalsInputProcessor.iterationsStep != 0){
			long t = System.currentTimeMillis();
			double deltaT = (t - FractalsInputProcessor.lastIterationStepTime)/1000.0;
			int change = (int)(FractalsInputProcessor.iterationsStep * deltaT * (iterations+1000));
			if (change < 0 && iterations < -change)
				change = -iterations;
			if (change != 0) {
				FractalsInputProcessor.lastIterationStepTime = t;
				iterations += change;
				refresh = true;
				System.out.println("set iterations to " + iterations);
			}
		}
		return refresh;
	}

	private void setShaderUniforms() {
		float currentWidth = Gdx.graphics.getWidth();
		float currentHeight = Gdx.graphics.getHeight();
		shader.setUniformMatrix("u_projTrans", matrix);
		shader.setUniformf("ratio", currentWidth/(float)currentHeight);
		shader.setUniformf("resolution", width, height);

		shader.setUniformf("colorShift", colorShift);

//		long t = System.currentTimeMillis();
//		if (t-lastIncrease > 1) {
//			lastIncrease = t;
//			iterations++;
//		}
		shader.setUniformi("iterations", iterations);
		shader.setUniformf("scale", (float)scale);
		float xVariation = (float)((Math.random()-0.5)*(scale/width));
		float yVariation = (float)((Math.random()-0.5)*(scale/width));
		shader.setUniformf("center", (float) xPos + xVariation, (float) yPos + yVariation);
		shader.setUniformf("biasReal", biasReal);
		shader.setUniformf("biasImag", biasImag);
		shader.setUniformf("samples", currentRefreshes+1);
		shader.setUniformf("flip", currentRefreshes%2 == 1 ? -1 : 1);
		//shader.setUniformi("u_texture", 0);
		//palette.bind(1);
		shader.setUniformi("palette", 0);
		shader.setUniformf("burningship", burningship ? 1f : 0f);
		shader.setUniformf("juliaset", juliaset ? 1f : 0f);
	}

	public void setupShaders() {
		ShaderProgram.pedantic = false;
		String vertexPassthrough = "PassthroughVertex.glsl";
		shader = compileShader(vertexPassthrough, shader1);
//		sobelShader = compileShader(vertexPassthrough, shader2);
		passthroughShader = compileShader(vertexPassthrough, "PassthroughFragment.glsl");

		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
	}

	public ShaderProgram compileShader(String vertexPath, String fragmentPath){
		ShaderProgram shader = new ShaderProgram(Gdx.files.internal(vertexPath),
				Gdx.files.internal(fragmentPath));
		if (!shader.isCompiled()) {
			throw new IllegalStateException("Error compiling shaders ("+vertexPath+", "+fragmentPath+"): "+shader.getLog());
		}
		return shader;
	}

	@Override
	public void resize(int width, int height) {
//		shader.begin();
//		shader.setUniformf("resolution", width, height);
//		shader.end();
		fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
	}

//	public void setIterations() {
//		shader.begin();
//		shader.setUniformf("iterations", iterations);
//		shader.end();
//	}

	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}
}
