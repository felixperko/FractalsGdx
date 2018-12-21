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
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Matrix3;
import com.kotcrab.vis.ui.VisUI;

public class FractalsGdxMain extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img;
	Texture palette;
	ShaderProgram shader;
	ShaderProgram sobelShader;
	Matrix3 matrix = new Matrix3(new float[] {1,0,0, 0,1,0, 0,0,1, 0,0,0});

	final static boolean juliaset = false;
	final static boolean burningship = false;

	final static String shader1 = "CalcMandelbrotFragment.glsl";
	final static String shader2 = "SobelDecodeFragment.glsl";
//	final static String shader2 = "PassthroughFragment.glsl";

	public static int iterations = 1000;

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

	@Override
	public void create () {
		VisUI.load();
		batch = new SpriteBatch();

		img = new Texture("badlogic.jpg");
		palette = new Texture("palette.png");
		palette.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

		setupShaders();
//		batch.setShader(shader);

		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(new GestureDetector(new FractalsGestureListener()));
		multiplexer.addProcessor(new FractalsInputProcessor());
		Gdx.input.setInputProcessor(new GestureDetector(new FractalsGestureListener()));

		fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
		fbo2 = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
		if (postprocessing)
			FractalsInputProcessor.yMultiplier *= -1;
	}

	int sampleLimit = 255;
	float decode_factor = 1f;

	@Override
	public void render () {
		boolean refresh = handleInput();
		if (forceRefresh) {
			forceRefresh = false;
			refresh = true;
		}

		if (refresh)
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT) ;

		if (postprocessing)
			fbo.begin();

		if (refresh) {
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			currentRefreshes = 0;
			decode_factor = 1f;
		} else {
			currentRefreshes++;
		}

		if (currentRefreshes+1 > sampleLimit)
			return;

		//palette.bind();
		shader.begin();

		setShaderUniforms();
		batch.begin();
		batch.setShader(shader);

		Color c = batch.getColor();
		batch.setColor(c.r, c.g, c.b, 1.0f);

		Texture tex = fbo.getColorBufferTexture();
		TextureRegion texReg =  new TextureRegion(tex);
		texReg.flip(false, true);
		batch.draw(texReg, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		batch.end();

		shader.end();
//		shader.begin();
//
//		shader.setUniformf("scale", 5);
//		shader.setUniformf("center", (float) 0, (float) 0);
		//shader.setUniformf("resolution", (float) 250, (float) 250);
		//if (Gdx.graphics.getWidth() > 600 && Gdx.graphics.getHeight() > 600)
//			batch.begin();
//			batch.draw(palette, Gdx.graphics.getWidth()-300, Gdx.graphics.getHeight()-300, 250, 250);
//		batch.end();

		shader.end();
		if (postprocessing) {
			fbo.end();
//			fbo2.begin();

			sobelShader.begin();
			sobelShader.setUniformf("resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			sobelShader.setUniformf("samples", currentRefreshes+1f);
			sobelShader.setUniformf("colorShift", colorShift);
//			for (int i = 0 ; i < currentRefreshes ; i++){
//				decode_factor += (byte)(1f/(i+1));
//			}
			sobelShader.setUniformf("decode_factor", 1f/decode_factor);

			batch.begin();
			batch.setShader(sobelShader);

			//Texture texture = fbo.getColorBufferTexture();
			//TextureRegion textureRegion = new TextureRegion(texture);
			//textureRegion.flip(false, true);

			Color c2 = batch.getColor();
			batch.enableBlending();
			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			float r = c2.r;
			float g = c2.g;
			float b = c2.b;
			float a = 1.0f/(currentRefreshes+1.0f);
			decode_factor = currentRefreshes+1;
//			if (decode_factor > 1.5f)
//				decode_factor = 1.5f;
			//decode_factor += 0.5f*(2.0f-(decode_factor));
			//decode_factor += ((float)((byte)Math.floor(a*255)))/255f;
			System.out.println(currentRefreshes+"  "+decode_factor+"  "+a+"  "+((float)((byte)(a*255)))/255f);
			batch.setColor(a, a, a, a);

			batch.draw(fbo.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//			batch.disableBlending();
			batch.end();

			sobelShader.end();
//			fbo2.end();
//
//			batch.begin();
//			batch.draw(fbo2.getColorBufferTexture(),0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//			batch.end();
		}
//		// get the graphics rendered to the framebuffer as a texture
//		Texture texture = fbo.getColorBufferTexture();
//		// welcome to the wonderful world of different coordinate systems!
//		// simply put, the framebuffer is upside down to normal textures, so we have to flip it
//		// Use a TextureRegion to do so
//		TextureRegion textureRegion = new TextureRegion(texture);
//		// and.... FLIP!  V (vertical) only
//		textureRegion.flip(false, true);
	}

	private boolean handleInput() {
		boolean refresh = false;

		float deltaTime = Gdx.graphics.getDeltaTime();
		double absFactor = deltaTime * scale;

//		if (Gdx.input.isKeyPressed(Keys.C)){
			colorShift += -deltaTime*0.1;
//		}

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
		shader = new ShaderProgram(Gdx.files.internal("PassthroughVertex.glsl"),
				Gdx.files.internal(shader1));
		if (!shader.isCompiled()) {
			throw new IllegalStateException("Error compiling shaders: "+shader.getLog());
		}

		sobelShader = new ShaderProgram(Gdx.files.internal("PassthroughVertex.glsl"),
				Gdx.files.internal(shader2));
		if (!sobelShader.isCompiled()) {
			throw new IllegalStateException("Error compiling shaders: "+sobelShader.getLog());
		}


		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
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
